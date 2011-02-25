import java.io.*;
import java.util.regex.*;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.ArrayList;


/**
*   The VarData class handles interaction with the data.  It loads the data, filters, and returns results.
*   @author Jamie K. Teer
*/
public class VarData {
    
    //final static int H_LINES  = 1;   //Number of header lines
    final static int S_FIELDS = 3;   //Number of columns for each sample

    final int AFF_NORM_PAIR = 0;
    final int CASE = 1;
    final int CONTROL = 2;
    final int MIN_MPG = 3;
    final int MIN_MPG_COV = 4;

    protected int genScoreThresh;
    
    final String[] geneDataHeaders = {"Gene_name", "Var Count"};
    final String[] ALLELES = {"A", "C", "G", "T"};

    final static int INTEGER = 0;
    final static int FLOAT = 1;
    final static int STRING = 2;
    final static int MULTISTRING = 3;

    protected String[] dataNamesOrig;     // All data names, for writing purposes
    protected String[] dataNames;
    protected String[] sampleNamesOrig;   // All sample names, for writing purposes
    protected String[] sampleNames;

    //data fields
    protected int[][] data;           // Fields: [line][var_annotation col]
    protected int[][] outData;        // Gets returned (can be filtered)
    protected int[][][] samples;      // Fields: [line][sampleName][genotype:MPGscore:coverage]
    protected int[][][] outSamples;   // Gets returned (can be filtered)
    protected int[] classList = null;
    protected ArrayList<AbstractMapper> annotMapperBuilder = new ArrayList<AbstractMapper>();  //Build an array of AbstractMappers for annotations
    protected AbstractMapper[] annotMapper = new AbstractMapper[0];                  //the annotation AbstractMapper array
    protected AbstractMapper[] sampleMapper = new AbstractMapper[S_FIELDS];      //The SampleMapper array

    protected int[] compHetFields;

    protected final static Pattern vcf = Pattern.compile("^##fileformat=VCF");
    protected final static Pattern fDigits = VarSifter.fDigits;
    protected final static Pattern digits = VarTableModel.digits;

    protected BitSet dataIsIncluded;      // A mask used to filter data, samples
    protected BitSet dataIsEditable = new BitSet();      // Which data elements can be edited
    
    protected HashMap<String, Integer> dataTypeAt = new HashMap<String, Integer>();

    protected int[] affAt;
    protected int[] normAt;
    protected int[] caseAt;
    protected int[] controlAt;
    protected VarData parentVarData = null;
    protected int numCols = 0;         // Number of columns.  Set from first line, used to check subseq. lines
    protected String customQuery = "";
    protected BitSet[] bitSets;

    /**    
    *    Constructor reads in the file specified by full path in String inFile.
    *    It first reads through the file just to count lines for first dimension
    *     of data[][] and samples[][][].  Then, it reads again to fill in the array.
    *
    *   @param inFile Absolute path to VS file to load.
    */
    public VarData(String inFile) {

        try {
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            String line = br.readLine();
            br.close();
            
            if (vcf.matcher(line).find()) { //VCF file
                VarSifter.showError("This looks like a VCF file - please append .vcf to filename and load again.");
                System.exit(1);
            }
            else {  //VarSifter file
                loadVSFile(inFile);
            }
        }
        catch (IOException ioe) {
            VarSifter.showError(ioe.toString());
            System.out.println(ioe);
            System.exit(1);
        }


        //indices for CompHet view
        compHetFields = new int[5];
        compHetFields[0] = (dataTypeAt.containsKey("Gene_name")) ? dataTypeAt.get("Gene_name") : -1;  //Gene name
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
    *   Empty constructor - Do not use!! (
    */
    protected VarData() {
    }


    /**
    *   Load data structures by parsing a VarSifter file
    *
    *   @param inFile Absolute path to VarSifter file
    */
    private void loadVSFile(String inFile) {
        final int header_lines = 1;
        String line = new String();
        int lineCount = 0;
        long time = System.currentTimeMillis();
        boolean first = true;
        boolean noSamples = false;
        
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
                        "Line: " + (lineCount) + " ***");
                    System.out.println("*** Input file appears to be malformed - column number not same as header! " +
                        "Line: " + (lineCount) + " ***");
                    System.exit(1);
                }
                
                //Determine class of each column, change if not int; for now, do NOT set MULTISTRING here
                for (int i=0; i<temp.length; i++) {
                    if (classList[i] == STRING || classList[i] == MULTISTRING) {
                        continue;
                    }
                        if (fDigits.matcher(temp[i]).matches()) {
                            classList[i] = FLOAT;
                        }
                        else if (!digits.matcher(temp[i]).matches()) {
                            classList[i] = STRING;
                        }
                }

                if (lineCount % 1000 == 0) {
                    System.out.print(".");
                }

            }
            data = new int[lineCount - header_lines][];
            samples = new int[lineCount - header_lines][][];
            dataIsIncluded = new BitSet(lineCount - header_lines);
            System.out.println();
            System.out.println("File Parsing completed - loading file");

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
                            
                            //Handle legacy files: refseq -> Gene_name
                            if (temp[i].equals("refseq")) {
                                temp[i] = "Gene_name";
                            }

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

                            //For now, only "type" field can be MULTISTRING
                            if (temp[i].equals("type")) {
                                classList[i] = VarData.MULTISTRING;
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
                                case MULTISTRING:
                                    annotMapperBuilder.add(new MultiStringMapper(";"));
                                    break;
                            }

                            dataCount++;
                        }
                    }

                    if (sampleCount == 0) {
                        noSamples = true;
                    }

                    
                    sampleMapper[0] = new StringMapper();
                    sampleMapper[1] = new IntMapper();
                    sampleMapper[2] = new IntMapper();

                    if (noSamples) {
                        sampleNames = new String[] {"NA"};
                        sampleNamesOrig = new String[] {"NA","NA","NA"};
                        sampleMapper[0].addData("NA");
                    }
                    else {
                        sampleNames = sampleTemp.split("\t");
                        sampleNamesOrig = sampleTempOrig.split("\t");
                    }
                    
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
                                String[] mapTemp = mapLine.split("=" , 2);
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
                for (int i=0; i < dataNames.length; i++) {
                    switch (classList[i]) {
                        case INTEGER:
                            data[lineCount][i] = Integer.parseInt(temp[i]);
                            break;
                        case FLOAT:
                            float f = Float.parseFloat(temp[i]);
                            data[lineCount][i] = annotMapper[i].addData(f); 
                            break;
                        case STRING:
                            data[lineCount][i] = annotMapper[i].addData(temp[i]);
                            break;
                        case MULTISTRING:
                            data[lineCount][i] = annotMapper[i].addData(temp[i]);
                            break;
                    }
                }

                annotT = (System.currentTimeMillis() - startT);

                
                //Fill samples array (genotypes)
                samples[lineCount] = new int[sampleNames.length][S_FIELDS];
                
                if (noSamples) {
                    samples[lineCount][0][0] = sampleMapper[0].getIndexOf("NA");
                    samples[lineCount][0][1] = -1;
                    samples[lineCount][0][2] = -1;
                }
                else {
                    for (int i = 0; i < sampleNames.length; i++) {
                        for (int j=0; j<S_FIELDS; j++) {
                            int dataIndex = dataNames.length + (i * S_FIELDS) + j;
                            switch(classList[dataIndex]) {
                                case INTEGER:
                                    samples[lineCount][i][j] = Integer.parseInt(temp[dataIndex]);
                                    break;
                                case FLOAT:
                                    float f = Float.parseFloat(temp[dataIndex]);
                                    samples[lineCount][i][j] = sampleMapper[j].addData(f);
                                    break;
                                case STRING:
                                    samples[lineCount][i][j] = sampleMapper[j].addData(temp[dataIndex]);
                                    break;
                            }
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
    *   Filter mutation type
    *
    *   To add new filters, must do the following:
    *   -Add new JCheckBox
    *   -Add entry to JCheckBox[] VarSifter.cBox
    *   -Display new JCheckBox
    *   -change this.mask indices (so that correct bit is being read - same order as cbox)
    *   -change this.filterSet indices (so that correct bitset is being used)
    *   -increment TOTAL_FILTERS
    *   -add a test block with correct this.mask index
    *  
    */
    public void filterData(DataFilter df) {
        BitSet[] mask = df.getMask();
        String geneFile = df.getGeneFile();
        String bedFile = df.getBedFile();
        int[] spinnerData = df.getSpinnerData();
        String geneQuery = df.getGeneQuery();
        int minMPG = df.getMinMPG();
        float minMPGCovRatio = df.getMinMPGCovRatio();
        genScoreThresh = df.getGenScoreThresh();
        //System.out.println("min: " + minMPG + " mc: " + minMPGCovRatio + " minSpin:" + spinnerData[MIN_MPG] + 
        //    " minCovSpin: " + spinnerData[MIN_MPG_COV]);


        dataIsIncluded.set(0,data.length);
        final int TOTAL_FILTERS = 11 + 1; //Number of non-type filters plus 1 (all type filters)
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
        int geneIndex = dataTypeAt.get("Gene_name");
        int chrIndex = dataTypeAt.get("Chr");
        int lfIndex = dataTypeAt.get("LeftFlank");
        int notMendHetRec = -1;
        HashSet<String> geneSet = new HashSet<String>();
        HashMap[] bedHash = null;   //<String, Vector<Integer>>

        //Set up type filters (filterSet[0], as all types are folded into one filter)
        filterSet[0] = new BitSet(data.length + 1);
        for (int i=0; i < mask[0].size(); i++) {
            if (mask[0].get(i)) {
                filterSet[0].set(data.length + 1);
                break;
            }
        }
        
        //Set up remaining filters (filterSet[x>0])
        for (int i=1; i < TOTAL_FILTERS; i++) {
            filterSet[i] = new BitSet(data.length + 1);
            if (mask[1].get(i - 1)) {  //must have -1 since mask is 0-based
                filterSet[i].set(data.length + 1);
            }
        }
        
        //Prepare certain tests

        // Type filters
        String[] typeNames = annotMapper[typeIndex].getSortedEntries();
        int[] types = new int[typeNames.length];
        for (int i=0; i < typeNames.length; i++) {
            types[i] = annotMapper[typeIndex].getIndexOf(typeNames[i]);
        }

        //dbSNP
        int nodbSNP = annotMapper[dbSNPIndex].getIndexOf("-");

        //menHetRec
        if (mask[1].get(VarSifter.MENDHETREC)) {
            notMendHetRec = annotMapper[mendHetRecIndex].getIndexOf("0,");
        }

        //aff/norm
        int naInt = sampleMapper[0].getIndexOf("NA");


        //filterFile
        if (mask[1].get(7) || mask[1].get(8)) {
            if (geneFile != null) {
                geneSet = returnGeneSet(geneFile);
            }
            else {
                VarSifter.showError("!!! geneFile not defined, so can't use it to filter !!!");
                System.out.println("!!! geneFile not defined, so can't use it to filter !!!");
            }
        }

        //bedFilterFile
        if (mask[1].get(9)) {
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
           
           
            // variant type
            for (int j=0; j < types.length; j++) {
                if (mask[0].get(j) && (data[i][typeIndex] & (int)Math.pow(2, types[j])) > 0 ) {

                    filterSet[0].set(i);
                    break;
                }
            }

            //dbSNP
            if ( mask[1].get(0) && ( annotMapper[dbSNPIndex].getString(data[i][dbSNPIndex]).matches("^0|-$") )
                ) {
                filterSet[1].set(i);
            }
            
            //Mendelian recessive (Hom recessive)
            if (mask[1].get(1) && data[i][mendRecIndex] == 1) {
                filterSet[2].set(i);
            }
            
            //Mendelian Dominant
            if (mask[1].get(2) && data[i][mendDomIndex] == 1) {
                filterSet[3].set(i);
            }

            //Mendelian Inconsistant
            if (mask[1].get(3) && data[i][mendBadIndex] == 1) {
                filterSet[4].set(i);
            }
                
            //Mendelian Compound Het (Het Recessive)
            if (mask[1].get(VarSifter.MENDHETREC) && data[i][mendHetRecIndex] != notMendHetRec) {
                filterSet[5].set(i);
            }

            //Affected different from Normal
            if (mask[1].get(5)) {
                int count = 0;
                for (int j=0; j < affAt.length; j++) {
                    int affTemp = samples[i][affAt[j]][0];
                    int normTemp = samples[i][normAt[j]][0];
                    if (affTemp != normTemp &&
                        affTemp != naInt &&
                        normTemp != naInt &&
                        samples[i][affAt[j]][1] >= genScoreThresh &&
                        samples[i][normAt[j]][1] >= genScoreThresh) {

                        count++;
                    }
                }
                if (count >= spinnerData[AFF_NORM_PAIR]) {
                    filterSet[6].set(i);
                }
            }

            // Variant allele in >=x cases, <=y controls
            if (mask[1].get(6)) {
                int caseCount = 0;
                int controlCount = 0;
                for (int j=0; j < caseAt.length; j++) {
                    String caseTemp = sampleMapper[0].getString(samples[i][caseAt[j]][0]).replaceAll(":", "");
                    if ( (caseTemp.equals(hetNonRefGen) || caseTemp.equals(homNonRefGen)) &&
                        samples[i][caseAt[j]][1] >= genScoreThresh) {
                        caseCount++;
                    }
                }
                for (int j=0; j < controlAt.length; j++) {
                    String controlTemp = sampleMapper[0].getString(samples[i][controlAt[j]][0]).replaceAll(":","");
                    if ( (controlTemp.equals(hetNonRefGen) || controlTemp.equals(homNonRefGen)) &&
                        samples[i][controlAt[j]][1] >= genScoreThresh) {
                        controlCount++;
                    }
                }
                if (caseCount >= spinnerData[CASE] && controlCount <= spinnerData[CONTROL]) {
                    filterSet[7].set(i);
                }
            }

            //Gene Filter File (include)                        
            if (mask[1].get(7) && geneSet.contains(annotMapper[geneIndex].getString(data[i][geneIndex]).toLowerCase())) {
                filterSet[8].set(i);
            }
            
            //Gene Filter File (exclude)
            if (mask[1].get(8) && !geneSet.contains(annotMapper[geneIndex].getString(data[i][geneIndex]).toLowerCase())) {
                filterSet[9].set(i);
            }
            
            //Bed Filter File (include)
            if (mask[1].get(9)) {
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

        if (mask[1].get(10)) {
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
    protected void filterOutput() {
         
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
    *   Returns number of requested sample type
    *  
    *   @param sType AFF_NORM_PAIR, CASE, or CONTROL
    *   @return The number of samples of the requested type
    */
    public int countSampleType(int sType) {
        switch (sType) {
            case AFF_NORM_PAIR: 
                return (affAt != null && normAt != null) ? affAt.length : 0;
            case CASE: 
                return (caseAt != null) ? caseAt.length : 0;
            case CONTROL: 
                return (controlAt != null) ? controlAt.length : 0;
            case MIN_MPG:
            case MIN_MPG_COV:
                return (sampleNames != null) ? sampleNames.length : 0;
            default:
                return 0;
        }
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
    protected HashMap[] returnBedHash(String inFile) {
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
            int geneName = outData[i][dataTypeAt.get("Gene_name")];
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
    protected HashSet<String> returnGeneSet(String inFile) {
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
        
        HashSet<Integer> inSet = new HashSet<Integer>();    //List of indices from MendHetRec columns
        int[][] out;
        int[][] eachPair;
        int indexIndex = (dataTypeAt.containsKey("Index")) ? dataTypeAt.get("Index") : -1;
        int pairCount = 0;
        int firstInPair; 
        ArrayList<Integer> rowSet = new ArrayList<Integer>();   //List of outData row indices.

        try {
            firstInPair = Integer.parseInt(inPair[0]);
            for (int i=0; i<inPair.length; i++) {
                inSet.add(new Integer(inPair[i]));
            }
        }
        catch (NumberFormatException nfe) {
            VarSifter.showError("MendHetRec field formatting is incorrect - must be comma-separated integers!");
            return null;
        }

        // store matches in outData (not necessarily inPair.length!)
        // ensure inPair[0] is in rowSet[0]!! Later, will return 0 1, 0 2, 0 3 etc pairs
        for (int i=0; i<outData.length; i++) {
            if (inSet.contains(outData[i][indexIndex])) {
                if (outData[i][indexIndex] == firstInPair) {
                    rowSet.add(0, i);   //Add rowIndex for line with mendhetrec Index inPair[0]
                }
                else {
                    rowSet.add(i);
                }
            }
        }

        eachPair = new int[rowSet.size()][];
        out = new int[rowSet.size() - 1][];

        for (Integer it: rowSet) {
            int i = it.intValue();
           
            if (isSamples) {
                eachPair[pairCount] = new int[compHetFields.length + (sampleNames.length * S_FIELDS)];
                for (int j = compHetFields.length; j < eachPair[pairCount].length; j+=S_FIELDS) {
                    int sampleIndex = (j - compHetFields.length) / S_FIELDS;
                    eachPair[pairCount][j]   = outSamples[i][sampleIndex][0];
                    eachPair[pairCount][j+1] = outSamples[i][sampleIndex][1];
                    eachPair[pairCount][j+2] = outSamples[i][sampleIndex][2];
                }
            }
            else {
                eachPair[pairCount] = new int[5];
            }
            for (int j=0; j<compHetFields.length; j++) {
                if (compHetFields[j] == -1) { //Field isn't in file
                    eachPair[pairCount][j] = -1; //Hold place, show "-" in CompHetTableModel
                }
                else {
                    eachPair[pairCount][j] = outData[i][compHetFields[j]];
                }
            }

            pairCount++;
        }

        for (int i=0; i<out.length; i++) {
            out[i] = new int[ ((eachPair[0].length * 2) - 2) ];
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
    *   Set the array of BitSets to use for a custom query
    *
    *   @param inBS An array of BitSets to be used in custom querying
    */
    public void setCustomBitSet(BitSet[] inBS) {
        if (inBS == null) {
            bitSets = new BitSet[0];
        }
        else {
            bitSets = inBS;
        }
    }


    /**
    *   returns the array of BitSets to use for a custom query
    *
    *   @return An array of BitSets to interrogate for custom querying
    */
    public BitSet[] getCustomBitSet() {
        return bitSets;
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
        data[lastIndex - 1][col] = annotMapper[col].addData(newData);
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
