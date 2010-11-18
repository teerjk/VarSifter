import java.io.*;
import java.util.regex.*;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.ArrayList;


/**
*   The VarData class handles interaction with the data.  It loads the data, filters, and returns results.
*/
public class VarData {
    
    final static int H_LINES  = 1;   //Number of header lines
    final static int S_FIELDS = 3;   //Number of columns for each sample

    final int AFF_NORM_PAIR = 0;
    final int CASE = 1;
    final int CONTROL = 2;
    final int MIN_MPG = 3;
    final int MIN_MPG_COV = 4;

    final int THRESHOLD = 10;
    
    final String[] geneDataHeaders = {"refseq", "Var Count"};
    final String[] ALLELES = {"A", "C", "G", "T"};

    final static int INTEGER = 0;
    final static int FLOAT = 1;
    final static int STRING = 2;

    //private String[][] data;            // Fields: [line][var_annotations]
    //private String[][] outData;         // Gets returned (can be filtered)
    private String[] dataNamesOrig;     // All data names, for writing purposes
    private String[] dataNames;
    //private String[][][] samples;       // Fields: [line][sampleName][genotype:MPGscore:coverage]
    //private String[][][] outSamples;    // Gets returned (can be filtered)
    private String[] sampleNamesOrig;   // All sample names, for writing purposes
    private String[] sampleNames;

    //New fields
    private int[][] data;           // Fields: [line][var_annotation col]
    private int[][] outData;        // Gets returned (can be filtered)
    private int[][][] samples;      // Fields: [line][sampleName][genotype:MPGscore:coverage]
    private int[][][] outSamples;   // Gets returned (can be filtered)
    private int[] classList = null;
    private ArrayList<AbstractMapper> annotMapperBuilder = new ArrayList<AbstractMapper>();  //Build an array of AbstractMappers for annotations
    private AbstractMapper[] annotMapper = new AbstractMapper[0];                  //the annotation AbstractMapper array
    private AbstractMapper[] sampleMapper = new AbstractMapper[S_FIELDS];      //The SampleMapper array

    private int[] compHetFields;

    private final static Pattern fDigits = VarSifter.fDigits;
    private final static Pattern digits = VarTableModel.digits;

    private BitSet dataIsIncluded;      // A mask used to filter data, samples
    private BitSet dataIsEditable = new BitSet();      // Which data elements can be edited
    private HashMap<String, Integer> dataTypeAt = new HashMap<String, Integer>();
    private int[] affAt;
    private int[] normAt;
    private int[] caseAt;
    private int[] controlAt;
    private VarData parentVarData = null;
    private int numCols = 0;         // Number of columns.  Set from first line, used to check subseq. lines
    private String customQuery = "";

    /**    
    *    Constructor reads in the file specified by full path in String inFile.
    *    It first reads through the file just to count lines for first dimension
    *     of data[][] and samples[][][].  Then, it reads again to fill in the array.
    *
    *   @param inFile Absolute path to VS file to load.
    */
    public VarData(String inFile) {
        String line = new String();
        int lineCount = 0;
        long time = System.currentTimeMillis();
        boolean first = true;
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            while ((line = br.readLine()) != null) {
                lineCount++;
                String[] temp = line.split("\t", 0);
                if (first) {
                    numCols = temp.length;
                    classList = new int[temp.length];
                    for (int i=0; i<classList.length; i++) {
                        classList[i] = INTEGER;
                    }
                    first = false;
                    continue;
                }
                    
                if (temp.length != numCols) {
                    VarSifter.showError("*** Input file appears to be malformed - column number not same as header! " +
                        "Line: " + (lineCount+2) + " ***");
                    System.out.println("*** Input file appears to be malformed - column number not same as header! " +
                        "Line: " + (lineCount+2) + " ***");
                    System.exit(1);
                }
                
                //Determine class of each column, change if not int
                for (int i=0; i<temp.length; i++) {
                    if (classList[i] == STRING) {
                        continue;
                    }
                        if (fDigits.matcher(temp[i]).matches()) {
                            classList[i] = FLOAT;
                        }
                        else if (!digits.matcher(temp[i]).matches()) {
                            classList[i] = STRING;
                        }
                }

            }
            data = new int[lineCount - H_LINES][];
            samples = new int[lineCount - H_LINES][][];
            dataIsIncluded = new BitSet(lineCount - H_LINES);

            br.close();
            lineCount = 0;
        }
        catch (IOException ioe) {
            VarSifter.showError(ioe.toString());
            System.out.println(ioe);
            System.exit(1);
        }
        first = true;
        //TESTING System.out.println("Parse finished: " + (System.currentTimeMillis() - time));

        try {
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            
            while ((line = br.readLine()) != null) {
                String temp[] = line.split("\t", 0);
                String sampleTemp = "";
                String sampleTempOrig = "";
                String dataTemp = "";
                String affPos = "";
                String normPos = "";
                String casePos = "";
                String controlPos = "";

                long startT = System.currentTimeMillis();
                long annotT;
                long sampleT;
                
                //Handle the Header
                if (first) {
                    
                    final Pattern samPat = Pattern.compile("NA");
                    final Pattern edPat = Pattern.compile("Comments");
                    final Pattern samAff = Pattern.compile("aff");
                    final Pattern samNorm = Pattern.compile("norm");
                    final Pattern genePat = Pattern.compile("refseq");
                    final Pattern casePat = Pattern.compile("case");
                    final Pattern controlPat = Pattern.compile("control");
                    String[] affPosArr;
                    String[] normPosArr;
                    String[] casePosArr;
                    String[] controlPosArr;
                    int dataCount = 0;
                    int sampleCount = 0;
                    
                    for (int i=0; i < temp.length; i++) {
                        // Is column a sample?
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
                        //Is column an annotation?
                        else {
                            dataTemp += (temp[i] + "\t");

                            // May want to read a flag from header - then can make checkboxes from this.
                            if (dataTypeAt.containsKey(temp[i])) {
                                VarSifter.showError("Multiple columns have the same name, which is not allowed.  "
                                    + "Please rename column #" + (i+1) + ": \"" + temp[i] + "\"");
                                System.exit(1);
                            }
                            dataTypeAt.put(temp[i], i);

                            if ((edPat.matcher(temp[i])).find()) {
                                dataIsEditable.set(i);
                            }
                            
                            switch (classList[i]) {
                                case INTEGER:
                                    annotMapperBuilder.add(new IntMapper());
                                    break;
                                case FLOAT:
                                    annotMapperBuilder.add(new FloatMapper());
                                    break;
                                case STRING:
                                    annotMapperBuilder.add(new StringMapper());
                                    break;
                            }

                            dataCount++;
                        }
                    }
                    
                    sampleMapper[0] = new StringMapper();
                    sampleMapper[1] = new IntMapper();
                    sampleMapper[2] = new IntMapper();

                    sampleNames = sampleTemp.split("\t");
                    sampleNamesOrig = sampleTempOrig.split("\t");
                    dataNames = dataTemp.split("\t");
                    dataNamesOrig = dataNames; //Will have to change this when not all data included
                    annotMapper = annotMapperBuilder.toArray(annotMapper);
                    
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

                    //Handle map file if available

                    File f = new File(inFile + ".map");
                    if (f.exists()) {
                        String mapLine = new String();
                        HashMap<String, String> mapHash = new HashMap<String, String>();
                        BufferedReader mapReader = new BufferedReader(new FileReader(f));
                        while ((mapLine = mapReader.readLine()) != null) {
                            if (mapLine.contains("=")) {
                                String[] mapTemp = mapLine.split("=");
                                mapHash.put(mapTemp[0], mapTemp[1]);
                            }
                        }
                        mapReader.close();

                        for (int i=0; i < sampleNames.length; i++) {
                            String newName;
                            if ((newName = mapHash.get(sampleNames[i])) != null) {
                                sampleNames[i] = newName;
                            }
                        }

                    }


                    first = false;
                    continue;
                }

                
                //Fill data array (annotations)
                data[lineCount] = new int[dataNames.length];
                //System.arraycopy(temp, 0, data[lineCount], 0, dataNames.length);
                for (int i=0; i < dataNames.length; i++) {
                    int index;
                    switch (classList[i]) {
                        case INTEGER:
                            data[lineCount][i] = Integer.parseInt(temp[i]);
                            break;
                        case FLOAT:
                            float f = Float.parseFloat(temp[i]);
                            index = annotMapper[i].getIndexOf(f);
                            if (index == -1) {
                                index = annotMapper[i].addData(f);
                            }
                            data[lineCount][i] = index;
                            break;
                        case STRING:
                            index = annotMapper[i].getIndexOf(temp[i]);
                            if (index == -1) {
                                index = annotMapper[i].addData(temp[i]);
                            }
                            data[lineCount][i] = index;
                            break;
                    }
                }

                annotT = (System.currentTimeMillis() - startT);

                
                //Fill samples array (genotypes)
                samples[lineCount] = new int[sampleNames.length][S_FIELDS];
                
                //System.out.println(temp.length);
                for (int i = 0; i < sampleNames.length; i++) {
                    //System.arraycopy(temp, (dataNames.length + (i * S_FIELDS)), samples[lineCount][i], 0, S_FIELDS);
                    for (int j=0; j<S_FIELDS; j++) {
                        int dataIndex = dataNames.length + (i * S_FIELDS) + j;
                        int index;
                        switch(classList[dataIndex]) {
                            case INTEGER:
                                samples[lineCount][i][j] = Integer.parseInt(temp[dataIndex]);
                                break;
                            case FLOAT:
                                float f = Float.parseFloat(temp[dataIndex]);
                                index = sampleMapper[j].getIndexOf(f);
                                if (index == -1) {
                                    index = sampleMapper[j].addData(f);
                                }
                                samples[lineCount][i][j] = index;
                                break;
                            case STRING:
                                //System.out.println("sampleMappers: " + sampleMapper.length + "   " +temp[dataIndex]);
                                index = sampleMapper[j].getIndexOf(temp[dataIndex]);
                                if (index == -1) {
                                    index = sampleMapper[j].addData(temp[dataIndex]);
                                }
                                samples[lineCount][i][j] = index;
                                break;
                        }
                    }
                }

                sampleT = (System.currentTimeMillis() - startT - annotT);
                
                lineCount++;
                if (lineCount % 1000 == 0) {
                    //TESTING System.out.println(lineCount + " Annot: " + annotT + " Sample: " + sampleT );
                    System.out.print(".");
                }
            }
            br.close();
            System.out.println();
        }
        catch (IOException ioe) {
            VarSifter.showError(ioe.toString());
            System.out.println(ioe);
            System.exit(1);
        }

        //indices for CompHet view
        compHetFields = new int[5];
        compHetFields[0] = (dataTypeAt.containsKey("refseq")) ? dataTypeAt.get("refseq") : -1;  //Gene name
        compHetFields[1] = dataTypeAt.get("Chr");    //chrom
        compHetFields[2] = dataTypeAt.get("LeftFlank"); //left flank
        compHetFields[3] = (dataTypeAt.containsKey("CDPred_score")) ? dataTypeAt.get("CDPred_score") : -1; //cdPred
        compHetFields[4] = dataTypeAt.get("type"); //variant type

        resetOutput();  //Initialize outData and outSamples
        
        //TESTING System.out.println("File Read finished: " + (System.currentTimeMillis() - time));
        /* TESTING
        for (int i =0; i<dataNames.length; i++) {
            System.out.println(i + " " + classList[i] + " " + annotMapper[i].getDataType() + " " + annotMapper[i].getLength());
        }

        for (int i=dataNames.length; i<classList.length; i++) {
            System.out.println(i + " " + classList[i] + " " + sampleMapper[(i-dataNames.length) % S_FIELDS].getDataType() + " " + sampleMapper[(i-dataNames.length) % S_FIELDS].getLength());
        }
        */
                
    }

    /** 
    *   Constructor for making subsetted copies using 
    *   the factory method returnSubVarData
    *  
    */
    private VarData(int[][] dataIn,
                    String[] dataNamesOrigIn,
                    String[] dataNamesIn,
                    int[][][] samplesIn,
                    String[] sampleNamesOrigIn,
                    String[] sampleNamesIn,
                    BitSet dataIsEditableIn,
                    HashMap<String, Integer> dataTypeAtIn,
                    int[] affAtIn,
                    int[] normAtIn,
                    int[] caseAtIn,
                    int[] controlAtIn,
                    VarData parentVarDataIn,
                    AbstractMapper[] annotMapperIn,
                    AbstractMapper[] sampleMapperIn                    
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
        annotMapper = annotMapperIn;
        sampleMapper = sampleMapperIn;

        dataIsIncluded = new BitSet(data.length);

        resetOutput();

    }



    /** 
    *   Returns 2d array of all data
    *  
    *   @return A two-dimension array of the data (1st Dimension is row, 2nd is column)
    */
    public int[][] dataDump() {
        boolean first = true;
        int[][] out = new int[data.length][dataNamesOrig.length + sampleNamesOrig.length];
        
        //header
        //System.arraycopy(dataNamesOrig, 0, out[0], 0, dataNamesOrig.length);
        //System.arraycopy(sampleNamesOrig, 0, out[0], dataNamesOrig.length, sampleNamesOrig.length);

        for (int i=0; i < data.length; i++) {
            System.arraycopy(data[i], 0, out[i], 0, dataNamesOrig.length);
            for (int j=0; j < sampleNames.length; j++) {
                System.arraycopy(samples[i][j], 0, out[i], (dataNamesOrig.length + (j * S_FIELDS)), S_FIELDS);
            }
        }
        return out;
    }
    
    /**
    *   Filter the data
    *
    *   @param df DataFilter object with the filtering options
    */
    /*
    *   @param mask A BitSet describing which filters should be applied.  The filter number should be synchronized with the JCheckBox number in VarSifter.class!
    *   @param geneFile Absolute path to a file containing a list of genes to filter on.
    *   @param bedFile Absolute path to a file containing regions with which to filter in BED format.
    *   @param spinnerData An array of ints representing the values in the VarSifter spinners: Indices defined by AFF_NORM_PAIR, CASE, CONTROL. 
    *   @param geneQuery A regular expression used to filter on geneName
    */
    /* 
    *   Filter mutation type
    *
    *   To add new filters, must do the following:
    *   -Add new JCheckBox
    *   -Add entry to JCheckBox[] VarSifter.cBox
    *   -Display new JCheckBox
    *   -change this.mask indices (so that correct bit is being read - same order as cbox)
    *   -change this.filterSet indices (so that correct bitset is being used)
    *   -increment TOTAL_FILTERS (or TOTAL_TYPE_FILTERS)
    *   -add a test block with correct this.mask index
    *  
    */
    //public void filterData(BitSet mask, String geneFile, String bedFile, int[] spinnerData, String geneQuery) {
    public void filterData(DataFilter df) {
        BitSet mask = df.getMask();
        String geneFile = df.getGeneFile();
        String bedFile = df.getBedFile();
        int[] spinnerData = df.getSpinnerData();
        String geneQuery = df.getGeneQuery();
        int minMPG = df.getMinMPG();
        float minMPGCovRatio = df.getMinMPGCovRatio();
        //System.out.println("min: " + minMPG + " mc: " + minMPGCovRatio + " minSpin:" + spinnerData[MIN_MPG] + 
        //    " minCovSpin: " + spinnerData[MIN_MPG_COV]);


        dataIsIncluded.set(0,data.length);
        //dataIsIncluded.clear();
        final int TOTAL_FILTERS = 11 + 1; //Number of non-type filters plus 1 (all type filters)
        final int TOTAL_TYPE_FILTERS = 8;
        BitSet[] filterSet = new BitSet[TOTAL_FILTERS];
        BitSet geneFilter = new BitSet(data.length);
        geneFilter.set(0, data.length);
        BitSet qualFilter = new BitSet(data.length);
        qualFilter.set(0, data.length);
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
        int notMendHetRec = -1;
        HashSet<String> geneSet = new HashSet<String>();
        HashMap[] bedHash = null;   //<String, Vector<Integer>>

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
        
        //Prepare certain tests

        // Type filters
        int[] types = new int[TOTAL_TYPE_FILTERS+1];
        types[0] = annotMapper[typeIndex].getIndexOf("Stop");
        types[1] = annotMapper[typeIndex].getIndexOf("DIV-fs");
        types[2] = annotMapper[typeIndex].getIndexOf("DIV-c");
        types[3] = annotMapper[typeIndex].getIndexOf("Splice-site");
        types[4] = annotMapper[typeIndex].getIndexOf("Non-synonymous");
        types[5] = annotMapper[typeIndex].getIndexOf("Synonymous");
        types[6] = annotMapper[typeIndex].getIndexOf("NC");
        types[7] = annotMapper[typeIndex].getIndexOf("3'UTR");
        types[8] = annotMapper[typeIndex].getIndexOf("5'UTR");

        //dbSNP
        int nodbSNP = annotMapper[dbSNPIndex].getIndexOf("-");

        //menHetRec
        if (mask.get(12)) {
            notMendHetRec = annotMapper[mendHetRecIndex].getIndexOf("0,");
        }

        //aff/norm
        int naInt = annotMapper[refAlleleIndex].getIndexOf("NA");


        //filterFile
        if (mask.get(15) || mask.get(16)) {
            if (geneFile != null) {
                geneSet = returnGeneSet(geneFile);
            }
            else {
                VarSifter.showError("!!! geneFile not defined, so can't use it to filter !!!");
                System.out.println("!!! geneFile not defined, so can't use it to filter !!!");
            }
        }

        //bedFilterFile
        if (mask.get(17)) {
            if (bedFile != null) {
                bedHash = returnBedHash(bedFile);
            }
            else {
                VarSifter.showError("!!! bedFile not defined, so nothing to filter with !!!");
                System.out.println("!!! bedFile not defined, so nothing to filter with !!!");
            }
        }

        //Gene name filter
        if (geneQuery != null) {
            geneQueryPat = Pattern.compile(geneQuery, Pattern.CASE_INSENSITIVE);
            geneFilter.clear();
        }
        

        //Start filtering!
        
        // variant type
        for (int i = 0; i < data.length; i++) {
            String[] tempGeno = { annotMapper[refAlleleIndex].getString(data[i][refAlleleIndex]), 
                                  annotMapper[varAlleleIndex].getString(data[i][varAlleleIndex]) 
                                };
            String homNonRefGen = (tempGeno[1] + tempGeno[1]);
            java.util.Arrays.sort(tempGeno);
            String hetNonRefGen = "";
            for (String s : tempGeno) {
                hetNonRefGen += s;
            }
            //System.out.println(hetNonRefGen);
           
            if ( (mask.get(0) && data[i][typeIndex] == types[0] ) ||
                 (mask.get(1) && data[i][typeIndex] == types[1] ) ||
                 (mask.get(2) && data[i][typeIndex] == types[2] ) ||
                 (mask.get(3) && data[i][typeIndex] == types[3] ) ||
                 (mask.get(4) && data[i][typeIndex] == types[4] ) ||
                 (mask.get(5) && data[i][typeIndex] == types[5] ) ||
                 (mask.get(6) && data[i][typeIndex] == types[6] ) ||
                 (mask.get(7) && data[i][typeIndex] == types[7] ) ||
                 (mask.get(7) && data[i][typeIndex] == types[8] )       
                ) {

                filterSet[0].set(i);
            }

            //dbSNP
            if ( mask.get(8) && ( annotMapper[dbSNPIndex].getString(data[i][dbSNPIndex]).matches("^0|-$") )
                ) {
                filterSet[1].set(i);
            }
            
            //Mendelian recessive (Hom recessive)
            if (mask.get(9) && data[i][mendRecIndex] == 1) {
                filterSet[2].set(i);
            }
            
            //Mendelian Dominant
            if (mask.get(10) && data[i][mendDomIndex] == 1) {
                filterSet[3].set(i);
            }

            //Mendelian Inconsistant
            if (mask.get(11) && data[i][mendBadIndex] == 1) {
                filterSet[4].set(i);
            }
                
            //Mendelian Compound Het (Het Recessive)
            if (mask.get(12) && data[i][mendHetRecIndex] != notMendHetRec) {
                filterSet[5].set(i);
            }

            //Affected different from Normal
            if (mask.get(13)) {
                int count = 0;
                for (int j=0; j < affAt.length; j++) {
                    int affTemp = samples[i][affAt[j]][0];
                    int normTemp = samples[i][normAt[j]][0];
                    if (affTemp != normTemp &&
                        affTemp != naInt &&
                        normTemp != naInt &&
                        samples[i][affAt[j]][1] >= THRESHOLD &&
                        samples[i][normAt[j]][1] >= THRESHOLD) {

                        count++;
                    }
                }
                //if (count == affAt.length) {
                if (count >= spinnerData[AFF_NORM_PAIR]) {
                    filterSet[6].set(i);
                }
            }

            // Variant allele in >=x cases, <=y controls
            if (mask.get(14)) {
                int caseCount = 0;
                int controlCount = 0;
                for (int j=0; j < caseAt.length; j++) {
                    String caseTemp = sampleMapper[0].getString(samples[i][caseAt[j]][0]).replaceAll(":", "");
                    if ( (caseTemp.equals(hetNonRefGen) || caseTemp.equals(homNonRefGen)) &&
                        samples[i][caseAt[j]][1] >= THRESHOLD) {
                        caseCount++;
                    }
                }
                for (int j=0; j < controlAt.length; j++) {
                    String controlTemp = sampleMapper[0].getString(samples[i][controlAt[j]][0]).replaceAll(":","");
                    if ( (controlTemp.equals(hetNonRefGen) || controlTemp.equals(homNonRefGen)) &&
                        samples[i][controlAt[j]][1] >= THRESHOLD) {
                        controlCount++;
                    }
                }
                if (caseCount >= spinnerData[CASE] && controlCount <= spinnerData[CONTROL]) {
                    filterSet[7].set(i);
                }
            }

            //Gene Filter File (include)                        
            if (mask.get(15) && geneSet.contains(annotMapper[geneIndex].getString(data[i][geneIndex]).toLowerCase())) {
                filterSet[8].set(i);
            }
            
            //Gene Filter File (exclude)
            if (mask.get(16) && !geneSet.contains(annotMapper[geneIndex].getString(data[i][geneIndex]).toLowerCase())) {
                filterSet[9].set(i);
            }
            
            //Bed Filter File (include)
            if (mask.get(17)) {
                String chrString = annotMapper[chrIndex].getString(data[i][chrIndex]);
                if (bedHash[0].get(chrString) != null) {

                    Object[] starts = ((HashMap<String, Vector<Integer>>)bedHash[0]).get(chrString).toArray();
                    Object[] ends = ((HashMap<String, Vector<Integer>>)bedHash[1]).get(chrString).toArray();
                    int pos = data[i][lfIndex] + 1;

                    for (int j=0; j<starts.length;j++) {
                        if (pos < (Integer)starts[j]) {
                            continue;
                        }

                        if (pos <= (Integer)ends[j]) {
                            filterSet[10].set(i);
                            break;
                        }
                    }
                }
            }

            // Gene name Filter (TextArea)
            if (geneQuery != null) {
                if ((geneQueryPat.matcher(annotMapper[geneIndex].getString(data[i][geneIndex]))).find()) {
                    //System.out.println(i + "\t" + data[i][geneIndex]);
                    geneFilter.set(i);
                }
            }

            // Qual filters
            if (minMPG != 0 || minMPGCovRatio != 0) {
                int minMPGCount = 0;
                int minMPGCovCount = 0;
                for (int j=0; j < sampleNames.length; j++) {
                    if (samples[i][j][1] >= minMPG) {
                        minMPGCount++;
                    }
                    if ( samples[i][j][2] != 0 &&
                         ((float)samples[i][j][1] / (float)samples[i][j][2]) >= minMPGCovRatio) {
                        minMPGCovCount++;
                    }
                }
                if (minMPGCount < spinnerData[MIN_MPG] || minMPGCovCount < spinnerData[MIN_MPG_COV]) {
                    qualFilter.clear(i);
                }
            }
                
                        
        }

        //Custom Query - outside data loop (it will loop by itself

        if (mask.get(18)) {
            try {
                CompileCustomQuery c = new CompileCustomQuery();
                if ( c.compileCustom(customQuery) ) {
                    filterSet[11] = c.run(this);
                    filterSet[11].set(data.length + 1);
                }
                else {
                    VarSifter.showError("Error with custom query - not applied!!");
                }
            }
            catch (NoClassDefFoundError e) {
                VarSifter.showError("<html>Couldn't find a class needed for custom querying - most likely you are"
                    + "<p>not running Java JDK 1.6 or greater.  See console for more details.");
                System.out.println(e.toString());
            }
        }

        
        //Apply all filters; intersection if that filter was used
        for (BitSet fs : filterSet) {
            if (fs.get(data.length + 1)) {
                dataIsIncluded.and(fs);
            }
        }

        dataIsIncluded.and(geneFilter);
        dataIsIncluded.and(qualFilter);
        filterOutput();
    }

    /** 
    *   Handle the filtering
    *  
    */
    private void filterOutput() {
         
        if (dataIsIncluded.cardinality() == data.length) {
            outData = data;
            outSamples = samples;
        }
        else {
            outData = new int[dataIsIncluded.cardinality()][];
            outSamples = new int[dataIsIncluded.cardinality()][][];
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


    /** 
    *   Returns true if column is editable
    *  
    *   @param col The number of the column to determine editable status
    *   @return True if editable
    */
    public boolean isEditable(int col) {
        return dataIsEditable.get(col);
    }


    /** 
    *   Returns number of normal affected pairs or null
    *  
    *   @deprecated Deprecated in favor of {@link #countSampleType(int)}
    *   @return The number of affected/normal pairs
    *   
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


    /** 
    *   Returns number of requested sample type
    *  
    *   @param sType AFF_NORM_PAIR, CASE, or CONTROL
    *   @return The number of samples of the requested type
    */
    public int countSampleType(int sType) {
        switch (sType) {
            case 0: // Aff/normal pairs
                return (affAt != null && normAt != null) ? affAt.length : 0;
            case 1: // Case
                return (caseAt != null) ? caseAt.length : 0;
            case 2: // Control
                return (controlAt != null) ? controlAt.length : 0;
            case 3: // minMPG
            case 4: // minMPGCovRatio
                return (sampleNames != null) ? sampleNames.length : 0;
            default:
                return 0;
        }
    }


    /** 
    *   Returns true if we have mendelian filter columns
    *   @deprecated Deprecated Really only checks for "MendHomRec", which isn't right. Use {@link #returnDataTypeAt()} instead, and interrogate the hash for the desired filters.
    *  
    */
    public boolean isMendFilt() {
        return (dataTypeAt.containsKey("MendHomRec"));
    }


    /** 
    *   Remove all filtering
    *  
    */
    public void resetOutput() {
        dataIsIncluded.set(0, data.length);
        filterOutput();
    }

    

    /** 
    *   Return a hash of Vectors containing start positions in a bedfile
    *  
    *   @param inFile The Bedfile to load
    *   @return An array of hashmaps.  Element 0: key=chrom, value = vector of starts.
    *                                  Element 1: key=chrom, value = vector of ends.
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


    /**
    *   Return array of indices used in CompHetView
    *
    *   @return Array of annotation indices used in CompHetView
    */
    public int[] returnCompHetFields() {
        return compHetFields;
    }
        

    /** 
    *   Return annotation data
    *  
    *   @return Returns the annotaion data as a 2d array: [line][annotation column]
    */
    public int[][] returnData() {
        return outData;
    }


    /**
    *   Return the Annotation Column Names
    *   @return Returns an array of the column header names
    */
    public String[] returnDataNames() {
        return dataNames;
    }


    /**
    *   Return annotation lookup map
    *   @return array of lookup maps (in order of annotation column)
    */
    public AbstractMapper[] returnAnnotMap() {
        return annotMapper;
    }


    /**
    *   Return sample lookup map
    *   @return array of sample lookup maps (entry for each unique sample field)
    */
    public AbstractMapper[] returnSampleMap() {
        return sampleMapper;
    }


    /** 
    *   Return a clone of dataTypeAt
    *  
    *   @return A HashMap: key = Annotation column name  value: column number (0-based)
    */
    public HashMap<String, Integer> returnDataTypeAt() {
        return (HashMap<String, Integer>)dataTypeAt.clone();
    }


    /** 
    *   Return the data value at a given row, column 
    *  
    *   @param row The row with the desired data (in the VarData object - not necessarily in the VarSifter view)
    *   @param colType The header name of the column with the desired data
    *   @return The data at the given position, or null if colType doesn't exist
    */
    public String returnDataValueAt(int row, String colType) {
        if (dataTypeAt.containsKey(colType)) {
            int index = dataTypeAt.get(colType);
            return annotMapper[index].getString(outData[row][index]);
        }
        else {
            return null;
        }
    }

    
    /** 
    *   Return the parent VarData or null if this is not a copy
    *  
    *   @return The parent VarData object of this object, or null if none
    */
    public VarData returnParent() {
        return parentVarData;
    }


    /** 
    *   Return data collapsed on gene name
    *  
    *   @return Data in a 2d array: [gene name key][number of variants]
    */
    public int[][] returnGeneData() {
        int[][] tempGeneData;
        HashMap<Integer, Integer> tempGeneHash = new HashMap<Integer, Integer>();

        for (int i=0; i<outData.length; i++) {
            int geneName = outData[i][dataTypeAt.get("refseq")];
            if (tempGeneHash.containsKey(geneName)) {
                tempGeneHash.put(geneName, tempGeneHash.get(geneName) + 1);
            }
            else {
                tempGeneHash.put(geneName, 1);
            }
        }

        tempGeneData = new int[tempGeneHash.size()][geneDataHeaders.length];
        int i = 0;
        for (Integer j : tempGeneHash.keySet()) {
            tempGeneData[i][0] = j.intValue();
            tempGeneData[i][1] = tempGeneHash.get(j).intValue();
            i++;
        }
        return tempGeneData;
    }

    /**
    *   Return column name for the Gene view
    *   @return Array of column names
    */
    public String[] returnGeneNames() {
        return geneDataHeaders;
    }




    /** 
    *   Get a HashSet from a file of gene names
    *  
    *   @param inFile The gene file to read (one gene per line)
    *   @return A Hashset of the gene names
    */
    private HashSet<String> returnGeneSet(String inFile) {
        HashSet<String> outSet = new HashSet<String>();
        try {
            String line = new String();
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            while ((line = br.readLine()) != null) {
                outSet.add(line.toLowerCase());
            }
            br.close();
        }
        catch (IOException ioe) {
            System.out.println(ioe);
            System.exit(1);
        }
        return outSet;
    }

    
    /** 
    *   Return pairs of positions based on index
    *  
    *   @param inPair An array of indices where the others are in CompoundHet status with the first
    *   @param isSamples True if sample data is to be inlcluded in view
    *   @return A 2-d array with the data [pair][columns]
    */
    public int[][] returnIndexPairs(String[] inPair, boolean isSamples) {
        
        //HashMap<String, String> inSet = new HashMap<String, String>();
        ArrayList<Integer> inSet = new ArrayList<Integer>();
        int[][] out = new int[inPair.length-1][];
        int[][] eachPair = new int[inPair.length][];
        int indexIndex = (dataTypeAt.containsKey("Index")) ? dataTypeAt.get("Index") : -1;
        //int cdPredIndex = (dataTypeAt.containsKey("CDPred_score")) ? dataTypeAt.get("CDPred_score") : -1;
        //int lfIndex = (dataTypeAt.containsKey("LeftFlank")) ? dataTypeAt.get("LeftFlank") : -1;
        //int geneIndex = (dataTypeAt.containsKey("refseq")) ? dataTypeAt.get("refseq") : -1;
        //int chromIndex = dataTypeAt.get("Chr");
        //int typeIndex = dataTypeAt.get("type");

        for (int i=0; i<inPair.length; i++) {
            inSet.add(new Integer(inPair[i]));
        }

        for (int i=0; i<data.length; i++) {
            //StringBuilder sb = new StringBuilder(64);
           
            if (inSet.contains(data[i][indexIndex])) {
                int p = inSet.indexOf(data[i][indexIndex]);

                /*
                sb.append ((data[i][geneIndex] + ";" +
                           data[i][chromIndex] + ";" + 
                           data[i][lfIndex] + ";" +
                           data[i][cdPredIndex] + ";" + 
                           data[i][typeIndex]) );
                
                if (isSamples) {
                    sb.append(";");
                    for (int j=0; j<sampleNames.length; j++) {
                        for (int k=0; k<S_FIELDS; k++) {
                            sb.append(samples[i][j][k] + ";");
                        }
                    }
                    sb.deleteCharAt(sb.length() - 1);
                }
                inSet.put(data[i][indexIndex], sb.toString());
                */
                
                if (isSamples) {
                    eachPair[p] = new int[compHetFields.length + (sampleNames.length * S_FIELDS)];
                    for (int j = compHetFields.length; j < eachPair[p].length; j+=S_FIELDS) {
                        int sampleIndex = (j - compHetFields.length) / S_FIELDS;
                        eachPair[p][j]   = samples[i][sampleIndex][0];
                        eachPair[p][j+1] = samples[i][sampleIndex][1];
                        eachPair[p][j+2] = samples[i][sampleIndex][2];
                    }
                }
                else {
                    eachPair[p] = new int[5];
                }
                for (int j=0; j<compHetFields.length; j++) {
                    eachPair[p][j] = data[i][compHetFields[j]];
                }
                //eachPair[p][0] = data[i][geneIndex];
                //eachPair[p][1] = data[i][chromIndex];
                //eachPair[p][2] = data[i][lfIndex];
                //eachPair[p][3] = data[i][cdPredIndex];
                //eachPair[p][4] = data[i][typeIndex];

            }
        }

        //String[] firstTemp = inSet.get(inPair[0]).split(";", 0);
        for (int i=0; i<out.length; i++) {
            //System.arraycopy( inSet.get(inPair[0]).split(";", 0), 0, out[i], 0, 5);
            //System.arraycopy( inSet.get(inPair[i+1]).split(";", 0), 2, out[i], 5, 3);
            out[i] = new int[ ((eachPair[0].length * 2) - 2) ];
            //String[] nextTemp = inSet.get(inPair[i+1]).split(";",0);

            System.arraycopy( eachPair[0], 0, out[i], 0, eachPair[0].length );
            System.arraycopy( eachPair[i+1], 2, out[i], eachPair[0].length, (eachPair[i+1].length - 2) );
        }
        return out;

    }



    /** 
    *   Return samples
    *  
    *   @param i The row (in this VarData object) for which to display sample information
    *   @return A 2-d array of sample data indices [sample_index][data type(SampleName, Genotype, MPG score, Coverage)]
    */
    public int[][] returnSample(int i) {
        int[][] tempOutSamples;
        
        if (outSamples.length == 0) {
            tempOutSamples = new int[0][];
        }
        else {
            tempOutSamples = new int[sampleNames.length][S_FIELDS+1];
            for (int j = 0; j < sampleNames.length; j++) {
                for (int k = 0; k < S_FIELDS; k++) {
                    tempOutSamples[j][k+1] = outSamples[i][j][k];
                }
                tempOutSamples[j][0] = j;
            }
        }
        return tempOutSamples;
    }
    
    /**
    *   Return Samples Names
    *   @return Array of Sample Names
    */
    public String[] returnSampleNames() {
        return sampleNames;
    }

    
    /**
    *   Return Original sample column headers (not just names, but what was in the original file)
    *   @return An array of original sample column headers
    */
    public String[] returnSampleNamesOrig() {
        return sampleNamesOrig;
    }


    /** 
    *   Returns a new Object with a subset of the data
    *  
    *   @param vdatIn The VarData object to use as a basis for a Sub VarData object
    *   @param isInSubset BitSet where set bits determine which rows to include
    *   @return A child VarData object (usually with a subset of data) that knows its parent
    */
    public VarData returnSubVarData(VarData vdatIn, BitSet isInSubset) {
        if (isInSubset == null) {
            isInSubset = dataIsIncluded;
        }
        int[][] subsetData = new int[isInSubset.cardinality()][data[0].length];
        int[][][] subsetSamples = new int[subsetData.length][][];
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
                           vdatIn,
                           annotMapper,
                           sampleMapper
                           );
    }

    /** 
    *   Set the customized part of a custom query
    *  
    *   @param in The logical statement to use as a filter in the custom compiled QueryModule object
    */
    public void setCustomQuery(String in) {
        customQuery = in;
    }


    /** 
    *   Overwrite field in data[][] (comments for now)
    *  
    *   @param row The row (in VarData.data rows that are set in dataIsIncluded) to modify
    *   @param col The column (in VarData.data) to modify
    *   @param newData The data to supplant to old data
    */
    public void setData(int row, int col, String newData) {
        int lastIndex = 0;
        for (int i = 0; i <= row; i++) {
            lastIndex = ( dataIsIncluded.nextSetBit(lastIndex) + 1 );
        }
        int index = annotMapper[col].getIndexOf(newData);
        if (index == -1) {
            index = annotMapper[col].addData(newData);
        }
        data[lastIndex - 1][col] = index;
    }


    public static void main(String args[]) {
        /*
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
        */

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
