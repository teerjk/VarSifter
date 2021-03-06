import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.BitSet;
import java.util.Collections;
import java.util.regex.*;

/**
*   Subclass of AbstractMapper to handle Strings
*   @author Jamie K. Teer
*/
public class StringMapper implements AbstractMapper {
    private Map<String, Integer> dataMap;
    private Map<Integer, String> indexMap;
    private final static int dataType = VarData.STRING;
    private int lastIndex = 0;

    /**
    *   Constructor
    *
    */
    public StringMapper() {
        dataMap = new HashMap<String, Integer>(30000, 0.75f);
        indexMap = new HashMap<Integer, String>(30000, 0.75f);
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
        int index = getIndexOf(inS);
        if (index == -1) {
            dataMap.put(inS, Integer.valueOf(lastIndex));
            indexMap.put(Integer.valueOf(lastIndex), inS);
            lastIndex++;
            return lastIndex - 1; //remove 1 to get index
        }
        else {
            return index;
        }
    }


    /**
    *   Return requested String
    *
    *   @param index The index of String to return
    *   @return The desired String
    */
    public String getString(int index) {
        return indexMap.get(Integer.valueOf(index));
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

    public String[] getSortedEntries() {
        List<String> list = new ArrayList<String>(dataMap.keySet());
        Collections.sort(list);
        String[] s = list.toArray(new String[list.size()]);
        return s;
    }

}

