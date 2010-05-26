import java.io.*;
import java.util.regex.*;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

public class VarData {
    
    final int H_LINES  = 1;   //Number of header lines
    final int S_FIELDS = 3;   //Number of columns for each sample

    final int AFF_NORM_PAIR = 0;
    final int CASE = 1;
    final int CONTROL = 2;

    final int THRESHOLD = 10;
    
    final String[] geneDataHeaders = {"refseq", "Var Count"};
    final String[] ALLELES = {"A", "C", "G", "T"};

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
    private HashMap<String, Integer> dataTypeAt = new HashMap<String, Integer>();
    //public HashMap<String, Integer> dataTypeAt = new HashMap<String, Integer>();
    private int[] affAt;
    private int[] normAt;
    private int[] caseAt;
    private int[] controlAt;
    private VarData parentVarData = null;
    private int numCols = 0;         // Number of columns.  Set from first line, used to check subseq. lines

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
                String casePos = "";
                String controlPos = "";
                
                //Handle the Header
                if (first) {
                    
                    numCols = temp.length;
                    Pattern samPat = Pattern.compile("NA");
                    Pattern edPat = Pattern.compile("Comments");
                    Pattern samAff = Pattern.compile("aff");
                    Pattern samNorm = Pattern.compile("norm");
                    Pattern genePat = Pattern.compile("refseq");
                    Pattern casePat = Pattern.compile("case");
                    Pattern controlPat = Pattern.compile("control");
                    String[] affPosArr;
                    String[] normPosArr;
                    String[] casePosArr;
                    String[] controlPosArr;
                    int dataCount = 0;
                    int sampleCount = 0;
                    
                    for (int i=0; i < temp.length; i++) {
                        if ((samPat.matcher(temp[i])).find()) {

                            //System.out.println(title + "\t");
                            if (sampleCount % S_FIELDS == 0) {  //Sample name, not score, cov, etc
                                sampleTemp += (temp[i] + "\t");
                                int samPos = (i - dataCount)/S_FIELDS;
                                
                                if ((samAff.matcher(temp[i])).find()) {
                                    affPos += ( samPos + "\t");
                                }
                                else if ((samNorm.matcher(temp[i])).find()) {
                                    normPos += ( samPos + "\t");
                                }

                                if ((casePat.matcher(temp[i])).find()) {
                                    casePos += ( samPos + "\t");
                                }
                                else if ((controlPat.matcher(temp[i])).find()) {
                                    controlPos += ( samPos + "\t");
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

                    if (casePos.length() > 0) {
                        casePosArr = casePos.split("\t");
                        caseAt = new int[casePosArr.length];
                        for (int i=0; i < casePosArr.length; i++) {
                            caseAt[i] = Integer.parseInt(casePosArr[i]);
                        }
                    }
                    if (controlPos.length() > 0) {
                        controlPosArr = controlPos.split("\t");
                        controlAt = new int[controlPosArr.length];
                        for (int i=0; i< controlPosArr.length; i++) {
                            controlAt[i] = Integer.parseInt(controlPosArr[i]);
                        }
                    }

                    first = false;
                    continue;
                }

                if (temp.length != numCols) {
                    System.out.println("*** Input file appears to be malformed - column number not same as header! " +
                        "Line: " + (lineCount+2) + " ***");
                    System.exit(1);
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
    *   Constructor for making subsetted copies using 
    *   the factory method returnSubVarData
    *  ***********
    */
    private VarData(String[][] dataIn,
                    String[] dataNamesOrigIn,
                    String[] dataNamesIn,
                    String[][][] samplesIn,
                    String[] sampleNamesOrigIn,
                    String[] sampleNamesIn,
                    BitSet dataIsEditableIn,
                    HashMap<String, Integer> dataTypeAtIn,
                    int[] affAtIn,
                    int[] normAtIn,
                    int[] caseAtIn,
                    int[] controlAtIn,
                    VarData parentVarDataIn
                    ) {
        data = dataIn;
        dataNamesOrig = dataNamesOrigIn;
        dataNames = dataNamesIn;
        samples = samplesIn;
        sampleNamesOrig = sampleNamesOrigIn;
        sampleNames = sampleNamesIn;
        dataIsEditable = (BitSet)dataIsEditableIn.clone();
        dataTypeAt = (HashMap<String, Integer>)dataTypeAtIn.clone();
        affAt = affAtIn;
        normAt = normAtIn;
        caseAt = caseAtIn;
        controlAt = controlAtIn;
        parentVarData = parentVarDataIn;

        dataIsIncluded = new BitSet(data.length);

        resetOutput();

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
    *
    *   To add new filters, must do the following:
    *   -Add entry to JCheckBox[] VarSifter.cBox
    *   -change this.mask indices (so that correct bit is being read - same order as cbox)
    *   -change this.filterSet indices (so that correct bitset is being used)
    *   -increment TOTAL_FILTERS (or TOTAL_TYPE_FILTERS)
    *   -add test with correct this.mask index
    *  ***********
    */
    public void filterData(BitSet mask, String geneFile, String bedFile, int[] spinnerData, String geneQuery) {
        dataIsIncluded.set(0,data.length);
        //dataIsIncluded.clear();
        final int TOTAL_FILTERS = 10 + 1; //Number of non-type filters plus 1 (all type filters)
        final int TOTAL_TYPE_FILTERS = 7;
        BitSet[] filterSet = new BitSet[TOTAL_FILTERS];
        BitSet geneFilter = new BitSet(data.length);
        geneFilter.set(0, data.length);
        Pattern geneQueryPat = null;
        
        int typeIndex = dataTypeAt.get("type");
        int refAlleleIndex = dataTypeAt.get("ref_allele");
        int varAlleleIndex = dataTypeAt.get("var_allele");
        int dbSNPIndex = dataTypeAt.get("RS#");
        int mendRecIndex = (dataTypeAt.containsKey("MendHomRec")) ? dataTypeAt.get("MendHomRec") : -1;
        int mendHetRecIndex = (dataTypeAt.containsKey("MendHetRec")) ? dataTypeAt.get("MendHetRec") : -1;
        int mendDomIndex = (dataTypeAt.containsKey("MendDom")) ? dataTypeAt.get("MendDom") : -1;
        int mendBadIndex = (dataTypeAt.containsKey("MendInconsis")) ? dataTypeAt.get("MendInconsis") : -1;
        int geneIndex = dataTypeAt.get("refseq");
        int chrIndex = dataTypeAt.get("Chr");
        int lfIndex = dataTypeAt.get("LeftFlank");
        HashSet<String> geneSet = new HashSet<String>();
        HashMap[] bedHash = null;

        //Set up type filters (filterSet[0], as all types are folded into one filter)
        filterSet[0] = new BitSet(data.length + 1);
        for (int i=0; i < TOTAL_TYPE_FILTERS; i++) {
            if (mask.get(i)) {
                filterSet[0].set(data.length + 1);
                break;
            }
        }
        
        //Set up remaining filters (filterSet[x>0])
        for (int i=1; i < TOTAL_FILTERS; i++) {
            filterSet[i] = new BitSet(data.length + 1);
            if (mask.get(i + TOTAL_TYPE_FILTERS - 1)) {  //must have -1 since mask is 0-based
                filterSet[i].set(data.length + 1);
            }
        }
        
        //Start filtering!
        
        //filterFile
        if (mask.get(14) || mask.get(15)) {
            if (geneFile != null) {
                geneSet = returnGeneSet(geneFile);
            }
            else {
                System.out.println("!!! geneFile not defined, so can't use it to filter !!!");
            }
        }

        //bedFilterFile
        if (mask.get(16)) {
            if (bedFile != null) {
                bedHash = returnBedHash(bedFile);
            }
            else {
                System.out.println("!!! bedFile not defined, so nothing to filter with !!!");
            }
        }

        if (geneQuery != null) {
            geneQueryPat = Pattern.compile(geneQuery, Pattern.CASE_INSENSITIVE);
            geneFilter.clear();
        }
        
        for (int i = 0; i < data.length; i++) {
            String[] tempGeno = { data[i][refAlleleIndex], data[i][varAlleleIndex] };
            java.util.Arrays.sort(tempGeno);
            String genotype = "";
            for (String s : tempGeno) {
                genotype += s;
            }
            //System.out.println(genotype);
           
            if ( (mask.get(0) && data[i][typeIndex].equals("Stop")           ) ||
                 (mask.get(1) && data[i][typeIndex].equals("DIV")            ) ||
                 (mask.get(2) && data[i][typeIndex].equals("Splice-site")    ) ||
                 (mask.get(3) && data[i][typeIndex].equals("Non-synonymous") ) ||
                 (mask.get(4) && data[i][typeIndex].equals("Synonymous")     ) ||
                 (mask.get(5) && data[i][typeIndex].equals("NC")             ) ||
                 (mask.get(6) && data[i][typeIndex].contains("UTR")          )
                ) {

                filterSet[0].set(i);
            }

            if ( (mask.get(7) && data[i][dbSNPIndex].equals("-")             )
                ) {
                filterSet[1].set(i);
            }
            
            if (mask.get(8) && Integer.parseInt(data[i][mendRecIndex]) > 0) {
                filterSet[2].set(i);
            }
            
            if (mask.get(9) && Integer.parseInt(data[i][mendDomIndex]) > 0) {
                filterSet[3].set(i);
            }

            if (mask.get(10) && Integer.parseInt(data[i][mendBadIndex]) > 0) {
                filterSet[4].set(i);
            }
                
            if (mask.get(11) && ! data[i][mendHetRecIndex].equals("0,")) {
                filterSet[5].set(i);
            }

            if (mask.get(12)) {
                int count = 0;
                for (int j=0; j < affAt.length; j++) {
                    String affTemp = samples[i][affAt[j]][0];
                    String normTemp = samples[i][normAt[j]][0];
                    if (!affTemp.equals(normTemp) &&
                        !affTemp.equals("NA") &&
                        !normTemp.equals("NA") &&
                        Integer.parseInt(samples[i][affAt[j]][1]) >= THRESHOLD &&
                        Integer.parseInt(samples[i][normAt[j]][1]) >= THRESHOLD) {

                        count++;
                    }
                }
                //if (count == affAt.length) {
                if (count >= spinnerData[AFF_NORM_PAIR]) {
                    filterSet[6].set(i);
                }
            }

            if (mask.get(13)) {
                int caseCount = 0;
                int controlCount = 0;
                for (int j=0; j < caseAt.length; j++) {
                    String caseTemp = samples[i][caseAt[j]][0].replaceAll(":", "");
                    if (caseTemp.equals(genotype) &&
                        Integer.parseInt(samples[i][caseAt[j]][1]) >= THRESHOLD) {
                        caseCount++;
                    }
                }
                for (int j=0; j < controlAt.length; j++) {
                    String controlTemp = samples[i][controlAt[j]][0].replaceAll(":","");
                    if (controlTemp.equals(genotype) &&
                        Integer.parseInt(samples[i][controlAt[j]][1]) >= THRESHOLD) {
                        controlCount++;
                    }
                }
                if (caseCount >= spinnerData[CASE] && controlCount <= spinnerData[CONTROL]) {
                    filterSet[7].set(i);
                }
            }

                        

            if (mask.get(14) && geneSet.contains(data[i][geneIndex])) {
                filterSet[8].set(i);
            }

            if (mask.get(15) && !geneSet.contains(data[i][geneIndex])) {
                filterSet[9].set(i);
            }
            
            
            if (mask.get(16) && bedHash[0].get(data[i][chrIndex]) != null) {
                Object[] starts = ((HashMap<String, Vector<Integer>>)bedHash[0]).get(data[i][chrIndex]).toArray();
                Object[] ends = ((HashMap<String, Vector<Integer>>)bedHash[1]).get(data[i][chrIndex]).toArray();
                int lf = Integer.parseInt(data[i][lfIndex]);

                for (int j=0; j<starts.length;j++) {
                    if (lf < (Integer)starts[j]) {
                        continue;
                    }

                    if (lf <= (Integer)ends[j]) {
                        filterSet[10].set(i);
                        break;
                    }
                }
            }


            if (geneQuery != null) {
                if ((geneQueryPat.matcher(data[i][geneIndex])).find()) {
                    //System.out.println(i + "\t" + data[i][geneIndex]);
                    geneFilter.set(i);
                }
            }
                
                        
        }
        
        for (BitSet fs : filterSet) {
            if (fs.get(data.length + 1)) {
                dataIsIncluded.and(fs);
            }
        }
        dataIsIncluded.and(geneFilter);
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
    *   Returns number of normal affected pairs or null
    *   !!!!!!!! Deprecated in favor of countSampleType(int) !!!!!!!!!!!!
    *  ************
    */
    public int countAffNorm() {
        //return (affAt != null && normAt != null);
        if (affAt != null && normAt != null) {
            return affAt.length;
        }
        else {
            return 0;
        }
    }


    /* ************
    *   Returns number of requested sample type
    *  ************
    */
    public int countSampleType(int sType) {
        switch (sType) {
            case 0: // Aff/normal pairs
                return (affAt != null && normAt != null) ? affAt.length : 0;
            case 1: // Case
                return (caseAt != null) ? caseAt.length : 0;
            case 2: // Control
                return (controlAt != null) ? controlAt.length : 0;
            default:
                return 0;
        }
    }


    /* ************
    *   Returns true if we have mendelian filter columns
    *   !!!!!!  Really only checks for "MendHomRec", which isn't right.!!!!
    *   !!!!!!  Deprecated !!!!!!!
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
    *   Return a hash of Vectors containing start positions in a bedfile
    *  ************
    */
    private HashMap[] returnBedHash(String inFile) {
        HashMap<String, Vector<Integer>> outStart = new HashMap<String, Vector<Integer>>();
        HashMap<String, Vector<Integer>> outEnd   = new HashMap<String, Vector<Integer>>();
        HashMap[] outHash = new HashMap[2];

        try {
            String line = new String();
            Pattern chr = Pattern.compile("^chr");
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            while ((line = br.readLine()) != null) {
                if ( (chr.matcher(line)).find() ) {
                    String[] lineArray = line.split("\\s+");
                    if (outStart.get(lineArray[0]) == null) {
                        outStart.put(lineArray[0], new Vector<Integer>());
                        outEnd.put(lineArray[0], new Vector<Integer>());
                    }

                    outStart.get(lineArray[0]).add(Integer.valueOf(lineArray[1]) + 1);
                    outEnd.get(lineArray[0]).add(Integer.valueOf(lineArray[2]));
                }
            }
        }
        catch (IOException ioe) {
            System.out.println(ioe);
            System.exit(1);
        }

        outHash[0] = outStart;
        outHash[1] = outEnd;
        return outHash;
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
    *   Return a clone of dataTypeAt
    *  **********
    */
    public HashMap<String, Integer> returnDataTypeAt() {
        return (HashMap<String, Integer>)dataTypeAt.clone();
    }

    
    /* **********
    *   Return the parent VarData or null if this is not a copy
    *  **********
    */
    public VarData returnParent() {
        return parentVarData;
    }


    /* **********
    *   Return data collapsed on gene name
    *  **********
    */
    public String[][] returnGeneData() {
        String[][] tempGeneData;
        HashMap<String, Integer> tempGeneHash = new HashMap<String, Integer>();

        for (int i=0; i<outData.length; i++) {
            String geneName = outData[i][dataTypeAt.get("refseq")];
            if (tempGeneHash.containsKey(geneName)) {
                tempGeneHash.put(geneName, tempGeneHash.get(geneName) + 1);
            }
            else {
                tempGeneHash.put(geneName, 1);
            }
        }

        tempGeneData = new String[tempGeneHash.size()][geneDataHeaders.length];
        int i = 0;
        for (String j : tempGeneHash.keySet()) {
            tempGeneData[i][0] = j;
            tempGeneData[i][1] = tempGeneHash.get(j).toString();
            i++;
        }
        return tempGeneData;
    }

    public String[] returnGeneNames() {
        return geneDataHeaders;
    }




    /* **********
    *   Get a HashSet from a file of gene names
    *  **********
    */
    private HashSet<String> returnGeneSet(String inFile) {
        HashSet<String> outSet = new HashSet<String>();
        try {
            String line = new String();
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            while ((line = br.readLine()) != null) {
                outSet.add(line);
            }
            br.close();
        }
        catch (IOException ioe) {
            System.out.println(ioe);
            System.exit(1);
        }
        return outSet;
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
    *   Returns a new Object with a subset of the data
    *  **********
    */
    public VarData returnSubVarData(VarData vdatIn, BitSet isInSubset) {
        if (isInSubset == null) {
            isInSubset = dataIsIncluded;
        }
        String[][] subsetData = new String[isInSubset.cardinality()][data[0].length];
        String[][][] subsetSamples = new String[subsetData.length][][];
        int lastPos = 0;
        for (int i=0; i < data.length; i++) {
            if (isInSubset.get(i)) {
                System.arraycopy(data[i], 0, subsetData[lastPos], 0, data[i].length);
                subsetSamples[lastPos] = samples[i];
                lastPos++;
            }
        }
        return new VarData(subsetData,
                           dataNamesOrig,
                           dataNames,
                           subsetSamples,
                           sampleNamesOrig,
                           sampleNames,
                           dataIsEditable,
                           dataTypeAt,
                           affAt,
                           normAt,
                           caseAt,
                           controlAt,
                           vdatIn
                           );
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
