import java.io.*;
import java.net.URL;
import javax.swing.*;
import java.text.NumberFormat;
import java.util.regex.*;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
*   A VarData subclass for loading VCF files
*   @author Jamie K. Teer
*/
public class VCFVarData extends VarData {

    private Map<String, Map<String, String>> infoMetaVCF = new HashMap<String, Map<String, String>>();
    private Map<String, Map<String, String>> formatMetaVCF = new HashMap<String, Map<String, String>>();

    private CustomAnnotation ca = null;

    /**
    *   Interpret VCF file - load VarData data structures
    *   @param inFile Absolute pathe of VCF file to load
    */
    public VCFVarData(String inFile) {
        dataFile = inFile;
        
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
        final Pattern genoSep_pat = Pattern.compile("([0-9])[/\\|]([0-9])");

        final String[] fixedNames = { "Chr",
                                      "LeftFlank",
                                      "RightFlank",
                                      "Gene_name",
                                      "type",
                                      "muttype",
                                      "dbID",
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

        //Fill dataTypeAt with fixed names
        for (int i=0; i<fixedNames.length; i++) {
            dataTypeAt.put(fixedNames[i], i);
        }

        String line = "";
        boolean indel;
        boolean noSamples = false;
        boolean loadAll = false;
        int lineCount = 0;
        int infoCount = 0;
        int headCount = 0;
        int sampleCount = 0;
        String geneNameKey = "";
        String typeKey = "";
        String typeDelim = "/";
        final int annotCount = 8;

        List<String> tempNames = new ArrayList<String>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            while ((line = br.readLine()) != null) {
                
                String tempLine[] = line.split("\t", 0);

                if (info_pat.matcher(line).find()) {
                    int pos = fixedNames.length + infoCount;
                    String key = updateVCFMetaHash(line, infoMetaVCF);
                    Map<String, String> tempMeta = infoMetaVCF.get(key);
                    String descTemp = tempMeta.get("Description");

                    if (dataTypeAt.containsKey(descTemp)) {
                        Matcher m = colSuffixPat.matcher(descTemp);

                        if (m.find()) {
                            char suffix = m.group(1).toCharArray()[0];
                            if (suffix == 'z') {
                                VarSifter.showError("<html>INFO column has already been seen or has the same "
                                    + "Description as a reserved name in VarSifter,<p>"
                                    + "As VarSifter uses the Description to identify the column, this will not work.<p><p>"
                                    + "Cannot increment column name suffix to make it unique:<p>"
                                    + "ID=" + tempMeta.get("ID") + "     Description=" 
                                    + descTemp + "<p>Please rename this column.</html>");
                                System.exit(1);
                            }
                            else {
                                suffix++;
                                descTemp = descTemp.substring(0, (descTemp.length() - 2));
                                descTemp = (descTemp + "_" + suffix);
                                if (dataTypeAt.containsKey(descTemp)) {
                                    VarSifter.showError("<html>INFO column has already been seen or has the same "
                                        + "Description as a reserved name in VarSifter,<p>"
                                        + "As VarSifter uses the Description to identify the column, this will not work.<p><p>"
                                        + "Failed to add a unique identifier, so please change the following:"
                                        + "ID=" + tempMeta.get("ID") + "     Description=" 
                                        + tempMeta.get("Description") + "</html>");
                                    System.exit(1);
                                }
                            }
                        }
                        else {
                            descTemp += "_a";
                            if (dataTypeAt.containsKey(descTemp)) {
                                VarSifter.showError("<html>INFO column has already been seen or has the same "
                                    + "Description as a reserved name in VarSifter,<p>"
                                    + "As VarSifter uses the Description to identify the column, this will not work.<p><p>"
                                    + "Failed to add a unique identifier, so please change the following:"
                                    + "ID=" + tempMeta.get("ID") + "     Description="
                                    + tempMeta.get("Description") + "</html>");
                                    System.exit(1);
                            }
                        }
                                
                        VarSifter.showMessage("<html>INFO column has the same Description as a reserved name in VarSifter,<p>"
                            + "or as the INFO description of another INFO field.<p>"
                            + "As VarSifter uses the Description to identify the column, this will not work.<p><p>" 
                            + "To fix this, the column name has been appended with a unique identifier<p>"
                            + "ID=" + tempMeta.get("ID") + "     Description=" + tempMeta.get("Description") 
                            + "<p>New Description: " + descTemp
                            + "</html>");
                        tempMeta.put("Description", descTemp);


                    }
                    dataTypeAt.put(tempMeta.get("Description"), pos);
                    tempNames.add(key);
                    infoCount++;
                }
                else if (format_pat.matcher(line).find()) {
                    String key = updateVCFMetaHash(line, formatMetaVCF);
                }
                else if (head_pat.matcher(line).find()) {

                    if (tempLine.length < annotCount) {
                        VarSifter.showError("<html>Header line (#CHROM...) column count is less than required."
                            + "<p>Check the file format, and make sure the text file is tab-delimited!");
                        System.exit(1);
                    }

                    // Load any Custom Annotation JSON files.
                    CustomAnnotation[] caGroup = loadCustomAnnotation();
                    List<String> annotFormats = new ArrayList<String>();

                    for (CustomAnnotation cTemp : caGroup) {
                        if (infoMetaVCF.containsKey(cTemp.columnKey)) {
                            annotFormats.add(cTemp.format);
                        }
                    }

                    // display formats available, have user choose one.
                    if (annotFormats.size() > 0) {
                        String annotChoice = (String)JOptionPane.showInputDialog(
                            null,
                            "<html>The following special annotation formats were detected.<p>"
                                + "If you want VarSifter to interpret one of these, select it and click \"OK\".<p>"
                                + "Otherwise, click \"Cancel\".</html>",
                            "Choose Custom Annotation Format",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            annotFormats.toArray(new String[annotFormats.size()]),
                            null);
                        if (annotChoice != null && annotChoice.length() > 0) {
                            for (int cIndex=0; cIndex < caGroup.length; cIndex++) {
                                if (annotChoice.equals(caGroup[cIndex].format)) {
                                    ca = caGroup[cIndex];
                                }
                            }
                        }
                    }

                    // Ask User to give more info about data
                    InputTableDialog itd = new InputTableDialog(infoMetaVCF, inFile);
                    infoMetaVCF = itd.runDialog();
                    itd = null;

                    // determine custom Gene_name, type infoMetaVCF key (if any)
                    for (int i=0; i<tempNames.size(); i++) {
                        if ( Boolean.parseBoolean(infoMetaVCF.get(tempNames.get(i)).get("Gene_Name_Field")) 
                             && ca == null) {
                            if ( Boolean.parseBoolean(infoMetaVCF.get(tempNames.get(i)).get("Type_Field")) ) {
                                VarSifter.showError("<html>You cannot use the same column for both \"Gene Name\" and \"Type\""
                                    + ".<p>Please restart the program, and select distinct columns.</html>");
                                System.exit(1);
                            }

                            geneNameKey = tempNames.get(i);
                        }
                        else if ( Boolean.parseBoolean(infoMetaVCF.get(tempNames.get(i)).get("Gene_Name_Field"))
                                  && ca != null) {
                            VarSifter.showMessage("<html>You have indicated the special " + ca.format + " annotations "
                                + "should be used by VarSifter.<p>Therefore, the \"Gene_Name_Field\" selection will "
                                + "be ignored.<html>");
                        }
                        if ( Boolean.parseBoolean(infoMetaVCF.get(tempNames.get(i)).get("Type_Field")) 
                             && ca == null) {
                            typeKey = tempNames.get(i);
                            typeDelim = infoMetaVCF.get(tempNames.get(i)).get("Sub-delimiter");
                            if (typeDelim == null || typeDelim.equals("")) {
                                typeDelim = "/";
                            }
                        }
                        else if ( Boolean.parseBoolean(infoMetaVCF.get(tempNames.get(i)).get("Type_Field")) 
                             && ca != null) {
                            VarSifter.showMessage("<html>You have indicated the special " + ca.format + " annotations "
                                + "should be used by VarSifter.<p>Therefore, the \"Type_Field\" selection will "
                                + "be ignored.<html>");
                        }
                    }

                    // Allow user to select columns for loading / viewing
                    String[] toKeep = {geneNameKey, typeKey}; 
                    if (ca != null) {
                        toKeep = new String[] {ca.columnKey};
                        ca.setMultiAllelic( Boolean.parseBoolean(infoMetaVCF.get(ca.columnKey).get("MultiAllele")) );
                    }
                    ColumnSelectionDialog csd = new ColumnSelectionDialog(
                        tempNames.toArray(new String[tempNames.size()]), 
                        toKeep);

                    colMask = csd.runDialog();
                    csd = null;

                    if ( colMask.cardinality() == tempNames.size() && geneNameKey.equals("") ) {
                        loadAll = true;
                    }
                    else {
                        List<String> maskedTempNames = new ArrayList<String>();
                        int colCount = fixedNames.length;
                        Map<String, Integer> maskedDataTypeAt = 
                            new HashMap<String, Integer>(colMask.cardinality() + colCount);
                        //Fill maskedDataTypeAt with fixed names
                        for (int i=0; i<fixedNames.length; i++) {
                            maskedDataTypeAt.put(fixedNames[i], i);
                        }

                        // Remove undesired columns 
                        // (except Gene_name: remove from temp fields, but preserve info for fixed fields)
                        // (Do NOT remove field asssigned to "type" - we will include both to preserve order,
                        //  which is not preserved in the MULTISTRING type field)
                        for (int i=0; i<tempNames.size(); i++) {
                            if ( ! colMask.get(i) ) {
                                infoMetaVCF.remove(tempNames.get(i));
                            }
                            else if ( geneNameKey.equals(tempNames.get(i)) ) {
                                // Do nothing: don't remove from infoMetaVCF, but don't load to tempNames, dataTypeAt
                                // (This will be the Gene_name field, and will have that dataTypeAt.)
                            }
                            else {
                                maskedTempNames.add(tempNames.get(i));
                                maskedDataTypeAt.put(infoMetaVCF.get(tempNames.get(i)).get("Description"), colCount);
                                colCount++;
                            }
                        }
                        dataTypeAt = maskedDataTypeAt;
                        tempNames = maskedTempNames;
                    }

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
                        sampleNamesOrig[ (i * S_FIELDS) + 0 ] = sampleNames[i] + ".NA";
                        sampleNamesOrig[ (i * S_FIELDS) + 1 ] = sampleNames[i] + ".NA.score";
                        sampleNamesOrig[ (i * S_FIELDS) + 2 ] = sampleNames[i] + ".NA.coverage";
                    }

                    //Fill dataNames
                    System.arraycopy(fixedNames, 0, dataNames, 0, fixedNames.length);
                    System.arraycopy(fixedClassList, 0, classList, 0, fixedClassList.length);
                    
                    for (int i=0; i<tempNames.size(); i++) {
                        dataNames[i + fixedNames.length] = infoMetaVCF.get(tempNames.get(i)).get("Description");

                        //TESTING
                        //if (infoMetaVCF.get(tempNames.get(i)).get("MultiAllele").equals("true")) {;
                        //    System.out.println(infoMetaVCF.get(tempNames.get(i)).get("ID") 
                        //        + " " + infoMetaVCF.get(tempNames.get(i)).get("Type") + " is multi-allelic.");
                        //}

                        String tempType = infoMetaVCF.get(tempNames.get(i)).get("Type");
                        String tempNum = infoMetaVCF.get(tempNames.get(i)).get("Number");
                        boolean multiAllelic = Boolean.parseBoolean(infoMetaVCF.get(tempNames.get(i)).get("MultiAllele"));
                        String subdelim = infoMetaVCF.get(tempNames.get(i)).get("Sub-delimiter");

                        //assign class
                        if ( !subdelim.equals("") ) {
                            classList[i + fixedClassList.length] = STRING;
                        }
                        else if ( (tempType.equals("Integer") 
                                  && (tempNum.equals("1") || multiAllelic)) 
                                  || tempType.equals("Flag")) {
                            classList[i + fixedClassList.length] = INTEGER;
                        }
                        else if (tempType.equals("Float") 
                                 && (tempNum.equals("1") || multiAllelic)) {
                            classList[i + fixedClassList.length] = FLOAT;
                        }
                        else {
                            classList[i + fixedClassList.length] = STRING;
                        }
                        
                    }
                    dataNamesOrig = dataNames;

                    //change genotype qual mapper to float if needed
                    if (formatMetaVCF.containsKey("GQ")) {

                        String tempType = formatMetaVCF.get("GQ").get("Type");
                        String tempNum = formatMetaVCF.get("GQ").get("Number");

                        //assign class
                        if (tempType.equals("Float") && tempNum.equals("1")) {
                            sampleMapper[1] = new FloatMapper();
                        }
                    }


                    //TESTING
                    //for (int i=0; i<dataNames.length; i++) {
                    //    String in = "_";
                    //    String type = "_";
                    //    if (i >= fixedClassList.length) {
                    //        in = tempNames.get(i-fixedClassList.length);
                    //        type = infoMetaVCF.get(in).get("Type");
                    //    }
                    //    System.out.println(dataNames[i] + " " + in + " " 
                    //        + type + " "
                    //        + classList[i]);
                    //}

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
                                annotMapper[i] = new MultiStringMapper(typeDelim);
                                break;
                        }
                    }

                }
                else if (! comment.matcher(line).find()) {
                    
                    if (tempLine.length < annotCount) {
                        VarSifter.showError("<html>Data line column count is less than required."
                            + "<p>Check the file format, and make sure the text file is tab-delimited!");
                        System.exit(1);
                    }


                    //include multiple lines
                    String varAllele = tempLine[4];
                    if (varAllele.contains(",")) {
                        for (int i=0; i<varAllele.length(); i++) {
                            if (varAllele.charAt(i) == ',') {
                                lineCount++;
                            }
                        }
                    }
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
        
            //Ensure required columns are present (hopefully, as they are filled in by this class).
            checkReqHeaders();

            //System.out.println(lineCount); //TESTING
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
                    List<String> alleles = new ArrayList<String>();

                    //Check for multiallelic line
                    String varAllele = tempLine[4];
                    int altAlleleCount = 1;
                    if (varAllele.contains(",")) {
                        for (int i=0; i<varAllele.length(); i++) {
                            if (varAllele.charAt(i) == ',') {
                                altAlleleCount++;
                            }
                        }
                    }


                    // First, load INFO fields to hash (so they are available for parsing)
                    String[] infoTemp = tempLine[7].split(";");
                    Map<String, String> infoHash = new HashMap<String, String>(tempNames.size() + 2);
                    for (String s : infoTemp) {
                        String[] pairs = s.split("=",2);
                        if (pairs.length == 2) {
                            infoHash.put(pairs[0], pairs[1]);
                        }
                        else if (pairs.length == 1) {
                            infoHash.put(pairs[0], "1");
                        }
                    }

                    // Load Custom Annotation data string to object
                    if (ca != null) {
                        if (infoHash.containsKey(ca.columnKey)) {
                            ca.loadAnnot(infoHash.get(ca.columnKey), altAlleleCount);
                        }
                        else {
                            ca.loadAnnot("", altAlleleCount);
                        }
                    }

                    //Run loop once for each alt allele
                    for (int altI = 0; altI < altAlleleCount; altI++) {

                        int tempLineCount = lineCount + altI;
                        data[tempLineCount] = new int[dataNames.length];


                        //Chr
                        if ( !tempLine[0].contains("chr") ) {
                            tempLine[0] = "chr" + tempLine[0];
                        }
                        data[tempLineCount][0] = annotMapper[0].addData(tempLine[0]);

                        //LeftFlank / RightFlank
                        data[tempLineCount][1] = Integer.parseInt(tempLine[1]) - 1;
                        data[tempLineCount][2] = Integer.parseInt(tempLine[1]) + tempLine[3].length();

                        //Gene_name
                        if (ca != null) {
                            data[tempLineCount][3] = annotMapper[3].addData(ca.getGeneName(altI));
                        }
                        else if ( !geneNameKey.equals("") ) {
                            if ( infoHash.get(geneNameKey) != null ) {
                                data[tempLineCount][3] = annotMapper[3].addData(
                                    infoHash.get(geneNameKey));
                            }
                            else {
                                data[tempLineCount][3] = annotMapper[3].addData("-");
                            }
                        }
                        else {
                            data[tempLineCount][3] = annotMapper[3].addData("-");
                        }

                        //type
                        if (ca != null) {
                            // This is now split based on allele
                            data[tempLineCount][4] = annotMapper[4].addData(ca.getType(altI));
                        }
                        else if ( !typeKey.equals("") ) {
                            if ( infoHash.get(typeKey) != null ) {
                                if (infoMetaVCF.get(typeKey).get("MultiAllele").equals("true")) {
                                    //split values, enter correct one for this allele
                                    String[] multiValues = infoHash.get(typeKey).split(",",0);
                                    String s = "-";
                                    if (altI < multiValues.length) {
                                        s = multiValues[altI];
                                    }
                                    data[tempLineCount][4] = annotMapper[4].addData(s);
                                }
                                else {                                        
                                    //Not multiallele, so add complete value
                                    data[tempLineCount][4] = annotMapper[4].addData(
                                        infoHash.get(typeKey));
                                }

                            }
                            else {
                                data[tempLineCount][4] = annotMapper[4].addData("-");
                            }
                        }
                        else {
                            data[tempLineCount][4] = annotMapper[4].addData("-");
                        }

                        //dbID
                        if (tempLine[2].equals(".")) {
                            tempLine[2] = "-";
                        }
                        data[tempLineCount][6] = annotMapper[6].addData(tempLine[2]);

                        //ref_allele
                        data[tempLineCount][7] = annotMapper[7].addData(tempLine[3]);
                        alleles.add(tempLine[3]);

                        //var_allele
                        String[] varTemp = tempLine[4].split(",", 0);
                        data[tempLineCount][8] = annotMapper[8].addData(varTemp[altI]);

                        //muttype and assingment of INDEL (and further parsing of var_allele)
                        indel = (tempLine[3].length() != 1) ? true : false;
                        if (tempLine[3].length() != varTemp[altI].length() ) {
                            indel = true;
                        }
                        // Uncommenting below lines breaks things - SNVs should be 1,2 char.
                        //else if (tempLine[3].length() == varTemp[altI].length() ) {
                        //    indel = false;
                        //}
                        if (altI == 0) { //only load alleles once!
                            for (int i=0; i<varTemp.length; i++) {
                                alleles.add(varTemp[i]);
                            }
                        }
                        int index;
                        if (indel) {
                            index = annotMapper[5].addData("INDEL");
                        }
                        else {
                            index = annotMapper[5].addData("SNP");
                        }
                        data[tempLineCount][5] = index;
                        

                        //QUAL
                        if (tempLine[5].equals(".")) {
                            tempLine[5] = "NaN";
                        }
                        data[tempLineCount][9] = annotMapper[9].addData(Float.parseFloat(tempLine[5]));

                        //FILTER
                        data[tempLineCount][10] = annotMapper[10].addData(tempLine[6]);


                        //INFO field
                        for (int i=0; i<tempNames.size(); i++) {
                            int pos = i + fixedNames.length;
                            String key = tempNames.get(i);

                            if (infoMetaVCF.get(key).get("MultiAllele").equals("true")) { 
                                //split these values, enter correct value for alt allele (or 0/- if no value)
                                String[] multiValues = {""};
                                if (infoHash.containsKey(key)) {
                                    multiValues = infoHash.get(key).split(",",0);
                                }
                                //TESTING System.out.println(key + " " + classList[pos] + " " + " " + pos + " " + infoMetaVCF.get(key).get("Type"));

                                switch (classList[pos]) {
                                    case INTEGER:
                                        if (infoHash.containsKey(key) && altI < multiValues.length) {
                                            data[tempLineCount][pos] = Integer.parseInt(multiValues[altI]);
                                        }
                                        else {
                                            data[tempLineCount][pos] = 0;
                                        }
                                        break;
                                    case FLOAT:
                                        float f = 0f;
                                        if (infoHash.containsKey(key) && altI < multiValues.length) {
                                            if (floatNaN.matcher(infoHash.get(key)).find()) {
                                                f = Float.parseFloat("NaN");
                                            }
                                            else {
                                                f = Float.parseFloat(multiValues[altI]);
                                            }
                                        }
                                        data[tempLineCount][pos] = annotMapper[pos].addData(f);
                                        break;
                                    case STRING:
                                        String s = "-";
                                        if (infoHash.containsKey(key) && altI < multiValues.length) {
                                            s = multiValues[altI];
                                        }
                                        data[tempLineCount][pos] = annotMapper[pos].addData(s);
                                        break;
                                }
                            }
                            else {
                                //Not multiallele, so add complete value to each line
                                switch (classList[pos]) {
                                    case INTEGER:
                                        if (infoHash.containsKey(key)) { 
                                            data[tempLineCount][pos] = Integer.parseInt(infoHash.get(key));
                                        }
                                        else {
                                            data[tempLineCount][pos] = 0;
                                        }
                                        break;
                                    case FLOAT:
                                        float f = 0f;
                                        if (infoHash.containsKey(key)) {
                                            if (floatNaN.matcher(infoHash.get(key)).find()) {
                                                f = Float.parseFloat("NaN");
                                            }
                                            else {
                                                f = Float.parseFloat(infoHash.get(key));
                                            }
                                        }
                                        data[tempLineCount][pos] = annotMapper[pos].addData(f);
                                        break;
                                    case STRING:
                                        String s = "-";
                                        if (infoHash.containsKey(key)) {
                                            s = infoHash.get(key);
                                        }
                                        data[tempLineCount][pos] = annotMapper[pos].addData(s);
                                        break;
                                }
                            }
                        }


                        // Handle Samples
                        samples[tempLineCount] = new int[sampleNames.length][3];

                        if (noSamples) {
                            samples[tempLineCount][0][0] = sampleMapper[0].getIndexOf("NA");
                            samples[tempLineCount][0][1] = (sampleMapper[1].getDataType() == FLOAT) 
                                ? sampleMapper[1].addData(Float.parseFloat("NaN")) : 0;
                            samples[tempLineCount][0][2] = 0;
                        }
                        else {
                            String[] sampTemp = tempLine[8].split(":");
                            Map<String, Integer> sampHash = new HashMap<String,Integer>(7);
                            for (int i=0; i < sampTemp.length; i++) {
                                sampHash.put(sampTemp[i], i);
                            }

                            if ( (tempLine.length - (annotCount+1)) != sampleNames.length) {
                                System.out.println("INTERNAL ERROR: inconsistent sample counting at dataline " 
                                    + tempLineCount);
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
                                        System.out.println("Malformed genotype on line " + (tempLineCount + 1) + ": " + geno );
                                    }
                                }

                                samples[tempLineCount][i - (annotCount + 1)][0] = sampleMapper[0].addData(geno);

                                // Qual score
                                if (sampHash.get("GQ") == null || sampTemp.length == 1 || sampTemp[sampHash.get("GQ")].equals(".")) {
                                    samples[tempLineCount][i - (annotCount + 1)][1] 
                                    = (sampleMapper[1].getDataType() == FLOAT)
                                    ? sampleMapper[1].addData(Float.parseFloat("NaN"))
                                    : sampleMapper[1].addData(0);
                                }
                                else {
                                    samples[tempLineCount][i - (annotCount + 1)][1] 
                                        = (sampleMapper[1].getDataType() == FLOAT) 
                                        ? sampleMapper[1].addData(Float.parseFloat(sampTemp[sampHash.get("GQ")])) 
                                        : sampleMapper[1].addData(Integer.parseInt(sampTemp[sampHash.get("GQ")]));
                                }

                                // Read depth
                                if (sampHash.get("DP") == null || sampTemp.length == 1 || sampTemp[sampHash.get("DP")].equals(".")) {
                                    samples[tempLineCount][i - (annotCount + 1)][2] = 0;
                                }
                                else {
                                    samples[tempLineCount][i - (annotCount + 1)][2] 
                                        = Integer.parseInt(sampTemp[sampHash.get("DP")]);
                                }
                            }
                        }
                        
                    }
                    lineCount += altAlleleCount;

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
        catch (Exception e) {
            VarSifter.showError("<html>Ooops - VarSifter encountered an unexpected error when loading your "
                + "VCF file.<p>Check the terminal output for full details:<p>" + e.toString());
            e.printStackTrace();
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
    private String updateVCFMetaHash(String line, Map<String, Map<String, String>> mHash) {
        String key = "";
        Map<String, String> temp = new HashMap<String, String>(3);
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
            temp.put(pairs[0], pairs[1].replaceAll("\"", ""));
        }
        mHash.put(key, temp);
        return key;
    }

    /**
    *   Detect any JSON files in current working directory, return objects
    *
    *   @return array of CustomAnnotation objects representing available custom annotation descriptions
    */
    private CustomAnnotation[] loadCustomAnnotation() {
        URL cDir = getClass().getProtectionDomain().getCodeSource().getLocation();
        File file = new File(cDir.getFile());
        if ( ! file.isDirectory() ) {
            file = file.getParentFile();
        }
        //System.out.println(java.util.Arrays.toString(file.list()));
        //System.out.println(cDir);
        String[] files = file.list();
        List<CustomAnnotation> out = new ArrayList<CustomAnnotation>();
        for (String s : files) {
            if (s.endsWith(".vs.json")) {
                out.add(new CustomAnnotation(file + "/" + s));
                //System.out.println(file + "/" + s);
            }
        }

        /*
        for (CustomAnnotation c: out) {
            System.out.println(c.format + "  " + c.columnKey);
        }
        System.exit(0);
        */

        return out.toArray(new CustomAnnotation[out.size()]);
    }

}
