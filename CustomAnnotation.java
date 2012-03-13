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
    private String geneNameString = "";
    private String typeString = "";
    private Set<String> allowedTypes = new HashSet<String>();
    private int geneNameIndex;
    private int typeIndex;

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
    */
    public void loadAnnot(String inS) {
        Set<String> typeSet = new HashSet<String>();
        Set<String> geneNameSet = new HashSet<String>();
        parseAnnotString( inS, typeSet, geneNameSet, 0 );

        if (typeSet.size() > 0) {
            StringBuilder tSB = new StringBuilder();
            String[] tArray = typeSet.toArray(new String[typeSet.size()]);
            java.util.Arrays.sort(tArray);
            for (String s: tArray) {
                tSB.append(s + TYPE_DELIM);
            }
            tSB.deleteCharAt(tSB.length() - 1);
            typeString = tSB.toString();
        }
        else {
            typeString = EMPTY;
        }

        if (geneNameSet.size() > 0) {
            StringBuilder gnSB = new StringBuilder();
            String[] gnArray = geneNameSet.toArray(new String[geneNameSet.size()]);
            java.util.Arrays.sort(gnArray);
            for (String s: gnArray) {
                gnSB.append(s + ";");
            }
            gnSB.deleteCharAt(gnSB.length() - 1);
            geneNameString = gnSB.toString();
        }
        else {
            geneNameString = EMPTY;
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
    */
    private void parseAnnotString(String substring, Set<String> tSet, Set<String> gnSet, int levelIndex) {
        String[] splitAnnot;
        splitAnnot = substring.split(delims.get(levelIndex));

        switch (levels.get(levelIndex).intValue()) {
            case MULTIDELIM:
                levelIndex++;
                for (String s : splitAnnot) {
                    parseAnnotString(s, tSet, gnSet, levelIndex);
                }
                break;
            case SINGLEDELIM:
                if ( typeIndex < splitAnnot.length
                     && (allowedTypes.isEmpty() || allowedTypes.contains(splitAnnot[typeIndex])) ) {
                    tSet.add( splitAnnot[typeIndex] );
                }
                try {
                    if (geneNameIndex < splitAnnot.length) {
                        gnSet.add( splitAnnot[geneNameIndex] );
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
    *   @return A String with gene names.
    */
    public String getGeneName() {
        return geneNameString;
    }

    /**
    *   Extracts the annotation type
    *   @return A String with annotation types.
    */
    public String getType() {
        return typeString;
    }


    public static void main(String[] args) {
        CustomAnnotation ca = new CustomAnnotation("snpEFF.json");
        String test1 = "STOP_LOST(High|x|TAT/TAG|Y/X|MyGene|someBioType|CODING|tx_1|Exon2)";
        String test2 = "STOP_LOST(High|x|TAT/TAG|Y/X|MyGene|someBioType|CODING|tx_1|Exon2)"
                     + ",START_LOST(High|x|ATG/ATT|M/I|OtherGene|someType|CODING|tx_2|Exon1)";
        String test3 = "";
        
        String[] tests = {test1, test2, test3};
        
        for (String t: tests) {
            ca.loadAnnot(t);
            System.out.println(t);
            System.out.println("type: " + ca.getType() + "  GeneName: " + ca.getGeneName());
        }
    }
}
