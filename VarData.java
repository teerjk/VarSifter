import java.io.*;
import java.util.regex.*;
import java.util.BitSet;
import java.util.HashMap;

public class VarData {
    
    int H_LINES  = 1;   //Number of header lines
    int S_FIELDS = 3;   //Number of columns for each sample

    private String[][] data;            // Fields: [line][var_annotations]
    private String[][] outData;         // Gets returned (can be filtered)
    private String[] dataNamesOrig;     // All data names, for writing purposes
    private String[] dataNames;
    private String[][][] samples;       // Fields: [line][sampleName][genotype:MPGscore:coverage]
    private String[][][] outSamples;    // Gets returned (can be filtered)
    private String[] sampleNamesOrig;   // All sample names, for writing purposes
    private String[] sampleNames;
    private BitSet dataIsIncluded;      // A mask used to filter data, samples
    private BitSet dataIsEditable = new BitSet();      // Which data elements can be edited
    public HashMap<String, Integer> dataTypeAt = new HashMap<String, Integer>();
    private int[] affAt;
    private int[] normAt;

     /*    
    *    Constructor reads in the file specified by full path in String inFile.
    *    It first reads through the file just to count lines for first dimension
    *     of data[][] and samples[][][].  Then, it reads again to fill in the array.
    */

    public VarData(String inFile) {
        String line = new String();
        int lineCount = 0;
        long time = System.currentTimeMillis();
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            while ((line = br.readLine()) != null) {
                lineCount++;
            }
            data = new String[lineCount - H_LINES][];
            samples = new String[lineCount - H_LINES][][];
            dataIsIncluded = new BitSet(lineCount - H_LINES);
            br.close();
            lineCount = 0;
        }
        catch (IOException ioe) {
            System.out.println(ioe);
            System.exit(1);
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            boolean first = true;
            
            while ((line = br.readLine()) != null) {
                String temp[] = line.split("\t", -1);
                String sampleTemp = "";
                String sampleTempOrig = "";
                String dataTemp = "";
                String affPos = "";
                String normPos = "";
                
                //Handle the Header
                if (first) {
                    
                    Pattern samPat = Pattern.compile("NA");
                    Pattern edPat = Pattern.compile("Comments");
                    Pattern samAff = Pattern.compile("aff");
                    Pattern samNorm = Pattern.compile("norm");
                    String[] affPosArr;
                    String[] normPosArr;
                    int dataCount = 0;
                    int sampleCount = 0;
                    
                    for (int i=0; i < temp.length; i++) {
                        if ((samPat.matcher(temp[i])).find()) {

                            //System.out.println(title + "\t");
                            if (sampleCount % S_FIELDS == 0) {  //Sample name, not score, cov, etc
                                sampleTemp += (temp[i] + "\t");
                                
                                if ((samAff.matcher(temp[i])).find()) {
                                    affPos += ( ((i - dataCount)/S_FIELDS) + "\t");
                                }
                                else if ((samNorm.matcher(temp[i])).find()) {
                                    normPos += ( ((i - dataCount)/S_FIELDS) + "\t");
                                }
                            }
                            sampleCount++;
                            sampleTempOrig += (temp[i] + "\t");
                        }
                        else {
                            dataTemp += (temp[i] + "\t");

                            // May want to read a flag from header - then can make checkboxes from this.
                            dataTypeAt.put(temp[i], i);

                            if ((edPat.matcher(temp[i])).find()) {
                                dataIsEditable.set(i);
                            }
                            dataCount++;
                        }
                    }
                    
                    sampleNames = sampleTemp.split("\t");
                    sampleNamesOrig = sampleTempOrig.split("\t");
                    dataNames = dataTemp.split("\t");
                    dataNamesOrig = dataNames; //Will have to change this when not all data included
                    
                    if (affPos.length() > 0 && normPos.length() > 0) {
                        affPosArr = affPos.split("\t");
                        normPosArr = normPos.split("\t");
                        affAt = new int[affPosArr.length];
                        normAt = new int[normPosArr.length];
                        for (int i=0; i < affPosArr.length; i++) { //Only works with norm/aff pairs...
                            affAt[i] = Integer.parseInt(affPosArr[i]);
                            normAt[i] = Integer.parseInt(normPosArr[i]);
                        }
                    }
                    else {
                        affAt = null;
                        normAt = null;
                    }

                    first = false;
                    continue;
                }
                
                //Fill data array (annotations)
                data[lineCount] = new String[dataNames.length];
                System.arraycopy(temp, 0, data[lineCount], 0, dataNames.length);
                
                //Fill samples array (genotypes)
                samples[lineCount] = new String[sampleNames.length][S_FIELDS];
                
                //System.out.println(temp.length);
                for (int i = 0; i < sampleNames.length; i++) {
                    System.arraycopy(temp, (dataNames.length + (i * S_FIELDS)), samples[lineCount][i], 0, S_FIELDS);
                }
                
                lineCount++;
            }
            br.close();
        }
        catch (IOException ioe) {
            System.out.println(ioe);
            System.exit(1);
        }

        resetOutput();  //Initialize outData and outSamples
        
        //System.out.println((System.currentTimeMillis() - time));
                
    }

    /* ***********
    *   Returns 2d array of all data
    *  ***********
    */
    public String[][] dataDump() {
        boolean first = true;
        String[][] out = new String[data.length + 1][dataNamesOrig.length + sampleNamesOrig.length];
        
        //header
        System.arraycopy(dataNamesOrig, 0, out[0], 0, dataNamesOrig.length);
        System.arraycopy(sampleNamesOrig, 0, out[0], dataNamesOrig.length, sampleNamesOrig.length);

        for (int i=0; i < data.length; i++) {
            System.arraycopy(data[i], 0, out[i+1], 0, dataNamesOrig.length);
            for (int j=0; j < sampleNames.length; j++) {
                System.arraycopy(samples[i][j], 0, out[i+1], (dataNamesOrig.length + (j * S_FIELDS)), S_FIELDS);
            }
        }
        return out;
    }
    
    /* ***********
    *   Filter mutation type
    *  ***********
    */
    public void filterData(BitSet mask) {
        dataIsIncluded.clear();
        BitSet temp = new BitSet(data.length);
        BitSet mendRecTemp = new BitSet(data.length);
        BitSet mendDomTemp = new BitSet(data.length);
        BitSet mendBadTemp = new BitSet(data.length);
        BitSet sampleTemp = new BitSet(data.length);
        int typeIndex = dataTypeAt.get("type");
        int dbSNPIndex = dataTypeAt.get("RS#");
        int mendRecIndex = (dataTypeAt.containsKey("MendHomRec")) ? dataTypeAt.get("MendHomRec") : -1;
        int mendDomIndex = (dataTypeAt.containsKey("MendDom")) ? dataTypeAt.get("MendDom") : -1;
        int mendBadIndex = (dataTypeAt.containsKey("MendInconsis")) ? dataTypeAt.get("MendInconsis") : -1;
        for (int i = 0; i < data.length; i++) {
           
            if ( (mask.get(0) && data[i][typeIndex].equals("Stop")           ) ||
                 (mask.get(1) && data[i][typeIndex].equals("DIV")            ) ||
                 (mask.get(2) && data[i][typeIndex].equals("Splice-site")    ) ||
                 (mask.get(3) && data[i][typeIndex].equals("Non-synonymous") ) ||
                 (mask.get(4) && data[i][typeIndex].equals("Synonymous")     ) ||
                 (mask.get(5) && data[i][typeIndex].equals("NC")             ) 
                ) {

                dataIsIncluded.set(i);
            }

            if ( (mask.get(6) && data[i][dbSNPIndex].equals("-")             )
                ) {
                temp.set(i);
            }
            
            if (mask.get(7) && Integer.parseInt(data[i][mendRecIndex]) > 0) {
                mendRecTemp.set(i);
            }
            
            if (mask.get(8) && Integer.parseInt(data[i][mendDomIndex]) > 0) {
                mendDomTemp.set(i);
            }

            if (mask.get(9) && Integer.parseInt(data[i][mendBadIndex]) > 0) {
                mendBadTemp.set(i);
            }
                

            if (mask.get(10)) {
                int count = 0;
                for (int j=0; j < affAt.length; j++) {
                    String affTemp = samples[i][affAt[j]][0];
                    String normTemp = samples[i][normAt[j]][0];
                    if (!affTemp.equals(normTemp) &&
                        !affTemp.equals("NA") &&
                        !normTemp.equals("NA") ) {

                        count++;
                    }
                }
                //if (count == affAt.length) {
                if (count == 1) {
                    sampleTemp.set(i);
                }
            }
                        
        }
        
        if (! temp.isEmpty()) {
            dataIsIncluded.and(temp);
        }
        if (! sampleTemp.isEmpty()) {
            dataIsIncluded.and(sampleTemp);
        }
        if (! mendRecTemp.isEmpty()) {
            dataIsIncluded.and(mendRecTemp);
        }
        if (! mendDomTemp.isEmpty()) {
            dataIsIncluded.and(mendDomTemp);
        }
        if (! mendBadTemp.isEmpty()) {
            dataIsIncluded.and(temp);
        }
        filterOutput();
    }

    /* ***********
    *   Handle the filtering
    *  ***********
    */
    private void filterOutput() {
         
        if (dataIsIncluded.cardinality() == data.length) {
            outData = data;
            outSamples = samples;
        }
        else {
            outData = new String[dataIsIncluded.cardinality()][];
            outSamples = new String[dataIsIncluded.cardinality()][][];
            int j = 0;
            for (int i=0; i < data.length; i++) {
                if (dataIsIncluded.get(i)) {
                    outData[j] = data[i];
                    outSamples[j] = samples[i];
                    j++;
                }
            }
        }
    }


    /* ************
    *   Returns true if column is editable
    *  ************
    */
    public boolean isEditable(int col) {
        return dataIsEditable.get(col);
    }


    /* ************
    *   Returns true if we have normal affected pairs
    *  ************
    */
    public boolean isAffNorm() {
        return (affAt != null && normAt != null);
    }

    /* ************
    *   Returns true if we have mendelian filter columns
    *  ************
    */
    public boolean isMendFilt() {
        return (dataTypeAt.containsKey("MendHomRec"));
    }


    /* ************
    *   Remove all filtering
    *  ************
    */
    public void resetOutput() {
        dataIsIncluded.set(0, data.length);
        filterOutput();
    }

    

    /* ************
    *   Return annotation data
    *  ************
    */
    public String[][] returnData() {
        return outData;
    }


    public String[] returnDataNames() {
        return dataNames;
    }

    /* **********
    *   Return samples
    *  **********
    */
    public String[][] returnSample(int i) {
        String[][] tempOutSamples;
        
        if (outSamples.length == 0) {
            tempOutSamples = new String[0][];
        }
        else {
            tempOutSamples = new String[S_FIELDS][sampleNames.length];
            for (int j = 0; j < sampleNames.length; j++) {
                for (int k = 0; k < S_FIELDS; k++) {
                    tempOutSamples[k][j] = outSamples[i][j][k];
                }
            }
        }
        return tempOutSamples;
    }
    
    public String[] returnSampleNames() {
        return sampleNames;
    }


    /* **********
    *   Overwrite field in data[][] (comments for now)
    *  **********
    */
    public void setData(int row, int col, String newData) {
        int lastIndex = 0;
        for (int i = 0; i <= row; i++) {
            lastIndex = ( dataIsIncluded.nextSetBit(lastIndex) + 1 );
        }
        data[lastIndex - 1][col] = newData;
    }


    public static void main(String args[]) {
        String input = "test_data.txt";
        if (args.length > 0) {
            input = args[0];
        }
        VarData vdat = new VarData(input);
        String[][] outData = vdat.returnData();
        String[] outNames = vdat.returnDataNames();
        
        for (String title : outNames) {
            System.out.print( title + "\t");
        }
        System.out.println();

        for (int i = 0; i < outData.length; i++) {
            System.out.print((i+1) + "\t");
            for (int j = 0; j < outData[i].length; j++) {
                if (outData[i][j].equals("")) {
                    System.out.print( "Err" + "\t");
                }
                else {
                    System.out.print(outData[i][j] + "\t");
                }
            }
            System.out.println();
        }

        //Test - unique and not NA between first 2 samples
        //for (int i = 0; i < vdat.samples.length; i++) {
        //    if (!vdat.samples[i][0][0].equals(vdat.samples[i][1][0]) && (!vdat.samples[i][0][0].equals("NA") 
        //        && !vdat.samples[i][1][0].equals("NA"))) {

        //        StringBuilder out = new StringBuilder();
        //        for (String s : vdat.data[i]) {
        //            out.append(s + "\t");
        //        }
        //        for (String[] s : vdat.samples[i]) {
        //            out.append((String)s[0] + "\t");
        //        }
        //        out.delete((out.length()-1), (out.length()));
        //        System.out.println(out.toString());
        //    }
        //}

    }
}
