import org.json.simple.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
*   This class uses a JSON file to properly parse complex Strings.
*   Designed to handle complex annotation structures, allowing one to 
*   isolate certain fields.
*   @author Jamie K. Teer
*/
public class CustomAnnotation {

    private List<String> delims = new ArrayList<String>();
    private List<Integer> levels = new ArrayList<Integer>();
    private String geneNameString[] = {""};
    private String typeString[] = {""};
    private Set<String> allowedTypes = new HashSet<String>();
    private int geneNameIndex;
    private int typeIndex;
    private boolean isMultiAllelic = false;

    final static String EMPTY = "-";
    final static String TYPE_DELIM = "/";

    protected String format = EMPTY;
    protected String columnKey = EMPTY;

    final static int MULTIDELIM =  0;
    final static int SINGLEDELIM = 1;
    
    /**
    *   Constructor 
    *   @param filename Absolute path to JSON file
    */
    public CustomAnnotation(String filename) {
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line = "";
            while ( (line = br.readLine()) != null ) {
                sb.append(line);
            }
        }
        catch (IOException ioe) {
            VarSifter.showError(ioe.toString());
            System.err.println(ioe);
            System.exit(1);
        }

        Object obj = JSONValue.parse(sb.toString());
        JSONObject jO = (JSONObject)obj;

        if ( jO.containsKey("main") ) {
            JSONObject mainO = (JSONObject)jO.get("main");
            if (mainO.containsKey("format") && mainO.containsKey("columnKey")) {
                format = (String)mainO.get("format");
                columnKey = (String)mainO.get("columnKey");
            }
            else {
                VarSifter.showError("Error parsing custom annotation file " + filename + 
                    ": missing entries in \"main\" object.");
                System.exit(1);
            }
        }
        else {
            VarSifter.showError("Error parsing custom annotation file " + filename + 
                ": \"main\" object missing from file.");
            System.exit(1);
        }

        nextObject(jO);

    }

    private void nextObject(JSONObject inJO) {
        if (inJO.containsKey("data")) {
            nextObject( (JSONObject)inJO.get("data") );
        }

        if (inJO.containsKey("multidelim") && inJO.containsKey("singledelim")) {
            VarSifter.showError("Error parsing custom annotation file: object has multidelim and singledelim keys");
            System.err.println("Error parsing custom annotation file: object has multidelim and singledelim keys");
            System.exit(1);
        }
        else if (inJO.containsKey("multidelim")) {
           levels.add(0, MULTIDELIM);
           delims.add(0, (String)inJO.get("multidelim"));
        }
        else if (inJO.containsKey("singledelim")) {
            levels.add(0, SINGLEDELIM);
            delims.add(0, (String)inJO.get("singledelim"));
        }

        if (inJO.containsKey("type")) {
            Number n = (Number)inJO.get("type");
            if (isInt(n)) {
                typeIndex = n.intValue();
            }
            else {
                System.err.println("Error parsing custom annot file: \"type\" value is not an integer!");
                System.exit(1);
            }
            
            if (inJO.containsKey("type_allowed")) {
                JSONArray allowedArray = (JSONArray)inJO.get("type_allowed");
                for (Object s : allowedArray) {
                    allowedTypes.add( (String)s );
                }
            }
        
        }

        if (inJO.containsKey("Gene_name")) {
            Number n = (Number)inJO.get("Gene_name");
            if (isInt(n)) {
                geneNameIndex = n.intValue();
            }
            else {
                System.err.println("Error parsing custom annot file: \"Gene_name\" value is not an integer!");
                System.exit(1);
            }
        }

    }


    /**
    *   Add a data string for parsing
    *   @param inS The data string from the annotation column: this will be parsed, allowing field extraction.
    *   @param alleleCount The number of alleles in this line
    */
    public void loadAnnot(String inS, int alleleCount) {
        List<Set<String>> typeSet     = new ArrayList<Set<String>>();
        List<Set<String>> geneNameSet = new ArrayList<Set<String>>();
        //Set<String> typeSet = new HashSet<String>();
        //Set<String> geneNameSet = new HashSet<String>();
        parseAnnotString( inS, typeSet, geneNameSet, 0, 0);

        //initialize type, gene for each allele. Later, fill with data, and access based on whether 
        // annotation was multiallelic or not
        typeString = new String[alleleCount];
        geneNameString = new String[alleleCount];
        for (int i=0; i < alleleCount; i++) {
            typeString[i] = EMPTY;
            geneNameString[i] = EMPTY;
        }

        //TESTING
        //System.err.println(isMultiAllelic);
        //System.err.println("alleles: " + alleleCount);
        //if (typeSet.size() != alleleCount && typeSet.size() != 1) {
        //    System.err.println("WARN: " + alleleCount + " alleles, but " + typeSet.size() + " annotations "
        //        + inS);
        //}

        for (int i=0; i < typeSet.size(); i++) {
            Set<String> alleleTypeSet = typeSet.get(i);
            if ( alleleTypeSet.size() > 0) {
                StringBuilder tSB = new StringBuilder();
                String[] tArray = alleleTypeSet.toArray(new String[alleleTypeSet.size()]);
                java.util.Arrays.sort(tArray);
                for (String s: tArray) {
                    tSB.append(s + TYPE_DELIM);
                }
                tSB.deleteCharAt(tSB.length() - 1);
                typeString[i] = tSB.toString();
            }
            else {
                typeString[i] = EMPTY;
            }
        }

        for (int i=0; i < geneNameSet.size(); i++) {
            Set<String> alleleGeneNameSet = geneNameSet.get(i);

            if (alleleGeneNameSet.size() > 0) {
                StringBuilder gnSB = new StringBuilder();
                String[] gnArray = alleleGeneNameSet.toArray(new String[alleleGeneNameSet.size()]);
                java.util.Arrays.sort(gnArray);
                for (String s: gnArray) {
                    gnSB.append(s + ";");
                }
                gnSB.deleteCharAt(gnSB.length() - 1);
                geneNameString[i] = gnSB.toString();
            }
            else {
                geneNameString[i] = EMPTY;
            }
        }

    }

    /**
    *   Check if a Number is an int while parsing JSON
    *   @param n The Number to check
    *   @return true if int
    */
    private boolean isInt(Number n) {
        if (Math.floor(n.doubleValue()) == n.doubleValue()) {
            return true;
        }
        else {
            return false;
        }
    }


    /**
    *   Split multiple levels of data, and extract fields according to the previously loaded JSON file.
    *   @param substring The string to parse.
    *   @param tSet A Set containing annotation types.
    *   @param gnSet A Set containing gene names.
    *   @param levelIndex The level to split on (when multiple levels present.) 0-based.
    *   @param alleleIndex The allele to examine.  Use '0' is not multiallelic
    */
    private void parseAnnotString(String substring, List<Set<String>> tSet, List<Set<String>> gnSet, int levelIndex, int alleleIndex) {
        String[] splitAnnot;
        splitAnnot = substring.split(delims.get(levelIndex));

        //initial multiallele division, setup
        if (levelIndex == 0) {
            if (isMultiAllelic) {
                //parse specially - each allele gets a different index
                levelIndex++;
                for (int i=0; i<splitAnnot.length; i++) {
                    tSet.add(i, new HashSet<String>());
                    gnSet.add(i, new HashSet<String>());
                    parseAnnotString(splitAnnot[i], tSet, gnSet, levelIndex, i);
                    //exit here, to avoid re-parsing
                }
                return;
            }
            else {
                //parse normally, in case there is no multidelim level
                tSet.add(0, new HashSet<String>());
                gnSet.add(0, new HashSet<String>());
            }

        }

        switch (levels.get(levelIndex).intValue()) {
            case MULTIDELIM:
                levelIndex++;
                for (String s : splitAnnot) {
                    parseAnnotString(s, tSet, gnSet, levelIndex, alleleIndex);
                }
                break;
            case SINGLEDELIM:
                if ( typeIndex < splitAnnot.length
                     && (allowedTypes.isEmpty() || allowedTypes.contains(splitAnnot[typeIndex])) ) {
                    //tSet.get(alleleIndex).add( splitAnnot[typeIndex] );
                    if (splitAnnot[typeIndex] == "") {
                        splitAnnot[typeIndex] = EMPTY;
                    }
                    Set<String> tempS = tSet.get(alleleIndex);
                    tempS.add( splitAnnot[typeIndex] );
                    tSet.set(alleleIndex, tempS);
                }
                try {
                    if (geneNameIndex < splitAnnot.length) {
                        //gnSet.get(alleleIndex).add( splitAnnot[geneNameIndex] );
                        if (splitAnnot[geneNameIndex] == "") {
                            splitAnnot[geneNameIndex] = EMPTY;
                        }
                        Set<String> tempS = gnSet.get(alleleIndex);
                        tempS.add( splitAnnot[geneNameIndex] );
                        gnSet.set(alleleIndex, tempS);
                    }
                }
                catch (Exception e) {
                    System.out.println(delims.get(levelIndex));
                    for (String s : splitAnnot) {
                        System.out.println(s);
                    }
                    System.out.println(e.toString());
                }
                break;
        }
    }

    /**
    *   Extracts the Gene Name
    *   @param i Allele index (0-based)
    *   @return A String with gene names.
    */
    public String getGeneName(int i) {
        if (! isMultiAllelic) {
            i=0;
        }
        //try {
            return geneNameString[i];
        //}
        //catch (Exception e) {
        //    System.err.println("Error in getGeneName()");
        //    for (String s : geneNameString) {
        //        System.err.println("gene " + s);
        //    }
        //    for (String s : typeString) {
        //        System.err.println("type" + s);
        //    }
        //    System.exit(1);
        //}
        //return "0";

    }

    /**
    *   Extracts the annotation type
    *   @param i Allele index (0-based)
    *   @return A String with annotation types.
    */
    public String getType(int i) {
        if (! isMultiAllelic) {
            i=0;
        }
        return typeString[i];
    }


    /**
    *   Set parsing to split on multiple alleles (using "multidelim" in highest level of json file)
    *   @param isMA True if the annotation column to parse is multi-allelic
    **/
    public void setMultiAllelic(boolean isMA) {
        isMultiAllelic = isMA;
    }


    public static void main(String[] args) {
        CustomAnnotation ca = new CustomAnnotation("snpEFF.json");
        String test1 = "STOP_LOST(High|x|TAT/TAG|Y/X|MyGene|someBioType|CODING|tx_1|Exon2)";
        String test2 = "STOP_LOST(High|x|TAT/TAG|Y/X|MyGene|someBioType|CODING|tx_1|Exon2)"
                     + ",START_LOST(High|x|ATG/ATT|M/I|OtherGene|someType|CODING|tx_2|Exon1)";
        String test3 = "";
        
        String[] tests = {test1, test2, test3};
        
        for (String t: tests) {
            ca.loadAnnot(t, t.split(",").length );
            System.out.println(t);
            System.out.println("type: " + ca.getType(0) + "  GeneName: " + ca.getGeneName(0));
        }
    }
}
