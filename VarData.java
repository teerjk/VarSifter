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

    private int genScoreThresh;
    
    final String[] geneDataHeaders = {"refseq", "Var Count"};
    final String[] ALLELES = {"A", "C", "G", "T"};

    final static int INTEGER = 0;
    final static int FLOAT = 1;
    final static int STRING = 2;
    final static int MULTISTRING = 3;

    private String[] dataNamesOrig;     // All data names, for writing purposes
    private String[] dataNames;
    private String[] sampleNamesOrig;   // All sample names, for writing purposes
    private String[] sampleNames;

    //data fields
    private int[][] data;           // Fields: [line][var_annotation col]
    private int[][] outData;        // Gets returned (can be filtered)
    private int[][][] samples;      // Fields: [line][sampleName][genotype:MPGscore:coverage]
    private int[][][] outSamples;   // Gets returned (can be filtered)
    private int[] classList = null;
    private ArrayList<AbstractMapper> annotMapperBuilder = new ArrayList<AbstractMapper>();  //Build an array of AbstractMappers for annotations
    private AbstractMapper[] annotMapper = new AbstractMapper[0];                  //the annotation AbstractMapper array
    private AbstractMapper[] sampleMapper = new AbstractMapper[S_FIELDS];      //The SampleMapper array

    private int[] compHetFields;

    private final static Pattern vcf = Pattern.compile("^##fileformat=VCF");
    private final static Pattern fDigits = VarSifter.fDigits;
    private final static Pattern digits = VarTableModel.digits;

    private BitSet dataIsIncluded;      // A mask used to filter data, samples
    private BitSet dataIsEditable = new BitSet();      // Which data elements can be edited
    
    private HashMap<String, Integer> dataTypeAt = new HashMap<String, Integer>();
    private HashMap<String, HashMap<String, String>> infoMetaVCF = new HashMap<String, HashMap<String, String>>();
    private HashMap<String, HashMap<String, String>> formatMetaVCF = new HashMap<String, HashMap<String, String>>();

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

        try {
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            String line = br.readLine();
            br.close();
            
            if (vcf.matcher(line).find()) { //VCF file
                loadVCFFile(inFile);
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
    *   Load data structures by parsing a VarSifter file
    *
    *   @param file Absolute path to VarSifter file
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
    *   Parses a VCF file to fill in data structures
    *    It first reads through the file to count lines for first dimension
    *    of data[][] and samples[][][].  Then, it reads again to fill in the array.
    *
    *   @param inFile Absolute path to VCF file name
    */
    private void loadVCFFile(String inFile) {
        final Pattern info_pat = Pattern.compile("^##INFO");
        final Pattern format_pat = Pattern.compile("^##FORMAT");
        final Pattern head_pat = Pattern.compile("^#CHROM");
        final Pattern comment = Pattern.compile("^#");
        final Pattern genoSep_pat = Pattern.compile("([0-9])[/\\|]([0-9])");

        final String[] fixedNames = { "Chr",
                                      "LeftFlank",
                                      "RightFlank",
                                      "refseq",
                                      "type",
                                      "muttype",
                                      "RS#",
                                      "ref_allele",
                                      "var_allele",
                                      "QUAL",
                                      "FILTER"
                                     };

        final int[] fixedClassList = { STRING,
                                       INTEGER,
                                       INTEGER,
                                       STRING,
                                       MULTISTRING,
                                       STRING,
                                       STRING,
                                       STRING,
                                       STRING,
                                       FLOAT,
                                       STRING
                                     };

        if (fixedNames.length != fixedClassList.length) {
            System.out.println("fixedName size different from fixed class list size! Tell developer!!");
            System.exit(1);
        }

        String line = new String();
        boolean indel;
        boolean noSamples = false;
        int lineCount = 0;
        int infoCount = 0;
        int headCount = 0;
        int sampleCount = 0;
        final int annotCount = 8;

        ArrayList<String> tempNames = new ArrayList<String>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            while ((line = br.readLine()) != null) {

                if (info_pat.matcher(line).find()) {
                    int pos = fixedNames.length + infoCount;
                    String key = updateVCFMetaHash(line, infoMetaVCF);
                    HashMap<String, String> tempMeta = infoMetaVCF.get(key);
                    dataTypeAt.put(tempMeta.get("Description"), pos);
                    tempNames.add(key);
                    infoCount++;
                }
                else if (format_pat.matcher(line).find()) {
                    String key = updateVCFMetaHash(line, formatMetaVCF);
                }
                else if (head_pat.matcher(line).find()) {
                    String tempLine[] = line.split("\t", 0);
                    sampleCount = tempLine.length - (annotCount + 1);
                    sampleMapper[0] = new StringMapper();
                    sampleMapper[1] = new IntMapper();
                    sampleMapper[2] = new IntMapper();
                    classList = new int[ fixedNames.length + tempNames.size() ];
                    dataNames = new String[fixedNames.length + tempNames.size()];

                    if (sampleCount <= 0) {
                        noSamples = true;
                        sampleCount = 0;
                        sampleNames = new String[] {"NA"};
                        sampleNamesOrig = new String[] {"NA","NA","NA"};
                        sampleMapper[0].addData("NA");
                    }
                    else {
                        sampleNames = new String[sampleCount];
                        sampleNamesOrig = new String[sampleCount*3];
                    }

                    for (int i=0; i < sampleCount; i++) {
                        sampleNames[i] = tempLine[i + annotCount + 1];
                        sampleNamesOrig[ (i * S_FIELDS) + 0 ] = sampleNames[i] + "_NA";
                        sampleNamesOrig[ (i * S_FIELDS) + 1 ] = sampleNames[i] + "_NA.score";
                        sampleNamesOrig[ (i * S_FIELDS) + 2 ] = sampleNames[i] + "_NA.coverage";
                    }

                    //Fill dataNames, dataTypeAt
                    System.arraycopy(fixedNames, 0, dataNames, 0, fixedNames.length);
                    System.arraycopy(fixedClassList, 0, classList, 0, fixedClassList.length);
                    for (int i=0; i<fixedNames.length; i++) {
                        dataTypeAt.put(dataNames[i], i);
                    }
                    
                    for (int i=0; i<tempNames.size(); i++) {
                        dataNames[i + fixedNames.length] = infoMetaVCF.get(tempNames.get(i)).get("Description");

                        String tempType = infoMetaVCF.get(tempNames.get(i)).get("Type");
                        String tempNum = infoMetaVCF.get(tempNames.get(i)).get("Number");
                        if ( (tempType.equals("Integer") && tempNum.equals("1")) || tempType.equals("Flag")) {
                            classList[i + fixedClassList.length] = INTEGER;
                        }
                        else if (tempType.equals("Float") && tempNum.equals("1")) {
                            classList[i + fixedClassList.length] = FLOAT;
                        }
                        else {
                            classList[i + fixedClassList.length] = STRING;
                        }
                        
                    }
                    dataNamesOrig = dataNames;

                    //Fill annotMapper, sampleMapper
                    annotMapper = new AbstractMapper[classList.length];
                    for (int i=0; i<classList.length; i++) {
                        switch (classList[i]) {
                            case INTEGER:
                                annotMapper[i] = new IntMapper();
                                break;
                            case FLOAT:
                                annotMapper[i] = new FloatMapper();
                                break;
                            case STRING:
                                annotMapper[i] = new StringMapper();
                                break;
                            case MULTISTRING:
                                annotMapper[i] = new MultiStringMapper("/");
                                break;
                        }
                    }

                }
                else if (! comment.matcher(line).find()) {
                    lineCount++;
                }
                if (lineCount % 1000 == 0) {
                    System.out.print(".");
                }
            }
            data = new int[lineCount][];
            samples = new int[lineCount][][];
            dataIsIncluded = new BitSet(lineCount);
            br.close();
            lineCount = 0;
            System.out.println();
            System.out.println("File parsing completed - loading file");
        }
        catch (IOException ioe) {
            System.out.println(ioe);
            VarSifter.showError(ioe.toString());
            System.exit(1);
        }

        //Open again - fill data
        try {
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            while ((line = br.readLine()) != null) {
                if (! comment.matcher(line).find()) {
                    String tempLine[] = line.split("\t", 0);
                    data[lineCount] = new int[dataNames.length];
                    ArrayList<String> alleles = new ArrayList<String>();
                    
                    //Chr
                    if ( !tempLine[0].contains("chr") ) {
                        tempLine[0] = "chr" + tempLine[0];
                    }
                    data[lineCount][0] = annotMapper[0].addData(tempLine[0]);

                    //LeftFlank / RightFlank
                    data[lineCount][1] = Integer.parseInt(tempLine[1]) - 1;
                    data[lineCount][2] = Integer.parseInt(tempLine[1]) + tempLine[3].length();

                    //refseq   !!! Dummy for now !!!
                    data[lineCount][3] = annotMapper[3].addData("-");

                    //type    !!! Dummy for now !!!
                    data[lineCount][4] = annotMapper[4].addData("-");

                    //RS#
                    if (tempLine[2].equals(".")) {
                        tempLine[2] = "-";
                    }
                    data[lineCount][6] = annotMapper[6].addData(tempLine[2]);

                    //ref_allele
                    data[lineCount][7] = annotMapper[7].addData(tempLine[3]);
                    alleles.add(tempLine[3]);

                    //var_allele
                    data[lineCount][8] = annotMapper[8].addData(tempLine[4]);

                    //muttype and assingment of INDEL (and further parsing of var_allele)
                    String[] varTemp = tempLine[4].split(",", 0);
                    indel = (tempLine[3].length() != 1) ? true : false;
                    for (int i=0; i<varTemp.length; i++) {
                        alleles.add(varTemp[i]);
                        if (tempLine[3].length() != varTemp[i].length() ) {
                            indel = true;
                        }
                    }
                    int index;
                    if (indel) {
                        index = annotMapper[5].addData("INDEL");
                    }
                    else {
                        index = annotMapper[5].addData("SNP");
                    }
                    data[lineCount][5] = index;
                    

                    //QUAL
                    if (tempLine[5].equals(".")) {
                        tempLine[5] = "NaN";
                    }
                    data[lineCount][9] = annotMapper[9].addData(Float.parseFloat(tempLine[5]));

                    //FILTER
                    data[lineCount][10] = annotMapper[10].addData(tempLine[6]);


                    //INFO field
                    String[] infoTemp = tempLine[7].split(";");
                    HashMap<String, String> infoHash = new HashMap<String, String>(tempNames.size());
                    for (String s : infoTemp) {
                        String[] pairs = s.split("=",2);
                        if (pairs.length == 2) {
                            infoHash.put(pairs[0], pairs[1]);
                        }
                        else if (pairs.length == 1) {
                            infoHash.put(pairs[0], "1");
                        }
                    }
                    for (int i=0; i<tempNames.size(); i++) {
                        int pos = i + fixedNames.length;
                        String key = tempNames.get(i);
                        switch (classList[pos]) {
                            case INTEGER:
                                if (infoHash.containsKey(key)) { 
                                    data[lineCount][pos] = Integer.parseInt(infoHash.get(key));
                                }
                                else {
                                    data[lineCount][pos] = 0;
                                }
                                break;
                            case FLOAT:
                                float f = 0f;
                                if (infoHash.containsKey(key)) {
                                    f = Float.parseFloat(infoHash.get(key));
                                }
                                data[lineCount][pos] = annotMapper[pos].addData(f);
                                break;
                            case STRING:
                                String s = "-";
                                if (infoHash.containsKey(key)) {
                                    s = infoHash.get(key);
                                }
                                data[lineCount][pos] = annotMapper[pos].addData(s);
                                break;
                        }
                    }


                    // Handle Samples
                    samples[lineCount] = new int[sampleNames.length][3];

                    if (noSamples) {
                        samples[lineCount][0][0] = sampleMapper[0].getIndexOf("NA");
                        samples[lineCount][0][1] = -1;
                        samples[lineCount][0][2] = -1;
                    }
                    else {
                        String[] sampTemp = tempLine[8].split(":");
                        HashMap<String, Integer> sampHash = new HashMap<String,Integer>(7);
                        for (int i=0; i < sampTemp.length; i++) {
                            sampHash.put(sampTemp[i], i);
                        }

                        if ( (tempLine.length - (annotCount+1)) != sampleNames.length) {
                            System.out.println("INTERNAL ERROR: inconsistent sample counting");
                            System.exit(1);
                        }

                        for (int i = annotCount + 1; i < tempLine.length; i++) {
                            sampTemp = tempLine[i].split(":");
                            String geno = sampTemp[sampHash.get("GT")];
                            Matcher m = genoSep_pat.matcher(geno);
                            
                            // Genotype
                            if (geno.contains(".")) {
                                geno = "NA";
                            }
                            else if (m.find()) {
                                String[] genoTemp = { alleles.get(Integer.parseInt(m.group(1))), 
                                                      alleles.get(Integer.parseInt(m.group(2))) 
                                                    };
                                java.util.Arrays.sort(genoTemp);

                                // DIV handling
                                if (indel) {
                                    geno = genoTemp[0] + ":" + genoTemp[1];
                                }
                                else {
                                    geno = genoTemp[0] + genoTemp[1];
                                }
                            }
                            else {
                                try {
                                    geno = alleles.get(Integer.parseInt(geno));
                                }
                                catch (NumberFormatException nfe) {
                                    System.out.println("Malformed genotype on line " + (lineCount + 1) + ": " + geno );
                                }
                            }

                            samples[lineCount][i - (annotCount + 1)][0] = sampleMapper[0].addData(geno);

                            // Qual score
                            if (sampHash.get("GQ") == null) {
                                samples[lineCount][i - (annotCount + 1)][1] = 0;
                            }
                            else {
                                samples[lineCount][i - (annotCount + 1)][1] = Integer.parseInt(sampTemp[sampHash.get("GQ")]);
                            }

                            // Read depth
                            if (sampHash.get("DP") == null) {
                                samples[lineCount][i - (annotCount + 1)][2] = 0;
                            }
                            else {
                                samples[lineCount][i - (annotCount + 1)][2] = Integer.parseInt(sampTemp[sampHash.get("DP")]);
                            }
                        }
                    }
                    
                    lineCount ++;

                    if (lineCount % 1000 == 0) {
                        System.out.print(".");
                    }
                }

            }
            br.close();
            System.out.println();
        }
        catch (IOException ioe) {
            System.out.println(ioe);
            VarSifter.showError(ioe.toString());
            System.exit(1);
        }
        

    }

    /**
    *   Add to a hash of VCF metadata values
    *
    *   @param line VCF metadata line
    *   @param mHash VCF metadata hash
    *   @return Metadata key
    */
    private String updateVCFMetaHash(String line, HashMap<String, HashMap<String, String>> mHash) {
        String key = "";
        HashMap<String, String> temp = new HashMap<String, String>(3);
        Pattern p = Pattern.compile("<(.*)>");
        Matcher m = p.matcher(line);
        String sub = "";

        if (m.find()) {
            sub = m.group(1);
        }
        else {
            System.out.println("VCF file may have malformed Headers: no \"<>\"");
            System.exit(1);
        }
        String[] tags = sub.split(",", 4);
        for (String s : tags) {
            String[] pairs = s.split("=");
            if (pairs[0].equals("ID")) {
                key = pairs[1];
            }
            else {
                temp.put(pairs[0], pairs[1].replaceAll("\"", ""));
            }
        }
        mHash.put(key, temp);
        return key;
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
        int geneIndex = dataTypeAt.get("refseq");
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
        
        ArrayList<Integer> inSet = new ArrayList<Integer>();
        int[][] out = new int[inPair.length-1][];
        int[][] eachPair = new int[inPair.length][];
        int indexIndex = (dataTypeAt.containsKey("Index")) ? dataTypeAt.get("Index") : -1;

        for (int i=0; i<inPair.length; i++) {
            inSet.add(new Integer(inPair[i]));
        }

        for (int i=0; i<data.length; i++) {
           
            if (inSet.contains(data[i][indexIndex])) {
                int p = inSet.indexOf(data[i][indexIndex]);
                
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
                    if (compHetFields[j] == -1) { //Field isn't in file
                        eachPair[p][j] = -1; //Hold place, show "-" in CompHetTableModel
                    }
                    else {
                        eachPair[p][j] = data[i][compHetFields[j]];
                    }
                }

            }
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
