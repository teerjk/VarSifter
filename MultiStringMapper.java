import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.BitSet;
import java.util.Collections;
import java.util.regex.*;

/**
*   Subclass of AbstractMapper to handle Multiple Strings using bitmasking
*   @author Jamie K. Teer
*/
public class MultiStringMapper implements AbstractMapper {
    private Map<String, Integer> dataMap;
    private Map<Integer, String> indexMap;
    private final static int dataType = VarData.MULTISTRING;
    private int lastIndex = 0;
    private String stringSepChar = ";";
    private final static int MAX = 31; //As I store flags in a signed int, can only have 31.
                                       //Will look into using sign bit to extend to 32.

    /**
    *   Constructor
    *
    *   @param sep The String to use as a separator.  Null is allowed, and then default is used.
    */
    public MultiStringMapper(String sep) {
        dataMap = new HashMap<String, Integer>(30000, 0.75f);
        indexMap = new HashMap<Integer, String>(30000, 0.75f);
        if (sep != null) {
            stringSepChar = sep;
        }
    }

    public BitSet filterWithPattern(Pattern pat) {
        BitSet bs = new BitSet(dataMap.size());
        for (String s: dataMap.keySet()) {
            if (pat.matcher(s).find()) {
                bs.set(dataMap.get(s));
            }
        }
        return bs;
    }

    public int getDataType() {
        return dataType;
    }

    public int getIndexOf( Object obj ) {
        if (dataMap.get(obj) == null) {
            return -1;
        }
        else {
            return dataMap.get(obj).intValue();
        }
    }


    /**
    *   Add new String and return index
    *
    *   @param obj The String to add
    *   @return Index of newly added String
    */
    public int addData(Object obj) {
        String inS = (String)obj;
        String[] inS_Ar = inS.split(stringSepChar, 0);
        int bitMask = 0;
        for (String s: inS_Ar) {
            int ind = getIndexOf(s);
            if (ind == -1) {
                // Check int bit bounds                
                if (lastIndex == MAX) {
                    VarSifter.showError("The number of entries in this MultiStringMapper Object has exceeded " + MAX
                        + "\nThe program cannot handle this number of entries.");
                    for (String key : dataMap.keySet()) {
                        System.out.println(key);
                    }
                    System.out.println("Failed to add: " + s);
                    System.exit(1);
                }
                dataMap.put(s, Integer.valueOf(lastIndex));
                indexMap.put(Integer.valueOf(lastIndex), s);
                bitMask |= (int)Math.pow(2, lastIndex); 
                lastIndex++;
            }
            else {
                bitMask |= (int)Math.pow(2,ind);
            }
        }
        return bitMask;
    }


    /**
    *   Return requested String
    *
    *   @param index The index of String to return
    *   @return The desired String
    */
    public String getString(int index) {
        String out = "";
        for (int i = 0; i<lastIndex; i++) {
            if ( (index & (int)Math.pow(2,i)) > 0) {
                out += (indexMap.get(Integer.valueOf(i)) + ";");
            }
        }
        if (out.length() == 0) {
            out = "-";
        }
        else {
            out = out.substring(0, out.length()-1);
        }
        return out;
    }


    /**
    *   Not Used
    *
    *   @return Null
    */
    public float getFloat(int index) {
        return -1f;
    }


    public int getLength() {
        return lastIndex;
    }

    /**
    *   Return an array of sorted data entries being stored in this object
    *   @return An array of type String with the elements stored in this object
    */
    public String[] getSortedEntries() {
        List<String> list = new ArrayList<String>(dataMap.keySet());
        Collections.sort(list);
        String[] s = list.toArray(new String[list.size()]);
        return s;
    }

}

