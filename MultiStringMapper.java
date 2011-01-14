import java.util.HashMap;

/**
*   Subclass of AbstractMapper to handle Multiple Strings using bitmasking
*   @author Jamie K. Teer
*/
public class MultiStringMapper implements AbstractMapper {
    private HashMap<String, Integer> dataMap;
    private HashMap<Integer, String> indexMap;
    private final int dataType = VarData.MULTISTRING;
    private int lastIndex = 0;
    private String stringSepChar = ";";

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

}

