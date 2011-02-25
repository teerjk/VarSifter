import java.io.*;
import java.util.regex.*;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.ArrayList;

/**
*   A VarData subclass for loading VCF files
*   @author Jamie K. Teer
*/
public class VCFVarData extends VarData {

    private HashMap<String, HashMap<String, String>> infoMetaVCF = new HashMap<String, HashMap<String, String>>();
    private HashMap<String, HashMap<String, String>> formatMetaVCF = new HashMap<String, HashMap<String, String>>();

    /**
    *   Interpret VCF file - load VarData data structures
    *   @param inFile Absolute pathe of VCF file to load
    */
    public VCFVarData(String inFile) {
        
        try {
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            String line = br.readLine();
            br.close();

            if (vcf.matcher(line).find()) {
                loadVCFFile(inFile);
            }
            else {
                VarSifter.showError("VCF file doesn't look like a VCF - first header line not as expected.");
            }
        }
        catch (IOException ioe) {
            VarSifter.showError(ioe.toString());
            System.out.println(ioe);
            System.exit(1);
        }

        resetOutput();
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
                                      "Gene_name",
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
                
                String tempLine[] = line.split("\t", 0);

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
                    //String varAllele = tempLine[3];
                    //if (varAllele.contains(",")) {
                    //    for (int i=0; i<varAllele.length(); i++) {
                    //        if (varAllele.charAt(i) == ',') {
                    //            lineCount++;
                    //        }
                    //    }
                    //}
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

                    //Gene_name   !!! Dummy for now !!!
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
                            if (sampHash.get("GQ") == null || sampTemp.length == 1) {
                                samples[lineCount][i - (annotCount + 1)][1] = 0;
                            }
                            else {
                                samples[lineCount][i - (annotCount + 1)][1] = Integer.parseInt(sampTemp[sampHash.get("GQ")]);
                            }

                            // Read depth
                            if (sampHash.get("DP") == null || sampTemp.length == 1) {
                                samples[lineCount][i - (annotCount + 1)][2] = 0;
                            }
                            else {
                                samples[lineCount][i - (annotCount + 1)][2] = Integer.parseInt(sampTemp[sampHash.get("DP")]);
                            }
                        }
                    }
                    
                    lineCount ++;

                }
                if (lineCount % 1000 == 0) {
                    System.out.print(".");
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
}
