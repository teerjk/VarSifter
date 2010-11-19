import java.util.HashMap;

/**
*   Subclass of AbstractMapper to handle Strings
*/
public class StringMapper implements AbstractMapper {
    private HashMap<String, Integer> dataMap;
    private HashMap<Integer, String> indexMap;
    private final int dataType = VarData.STRING;
    private int lastIndex = 0;

    /**
    *   Constructor
    *
    */
    public StringMapper() {
        dataMap = new HashMap<String, Integer>(30000, 0.75f);
        indexMap = new HashMap<Integer, String>(30000, 0.75f);
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
        dataMap.put(inS, Integer.valueOf(lastIndex));
        indexMap.put(Integer.valueOf(lastIndex), inS);
        lastIndex++;
        return lastIndex - 1; //remove 1 to get index
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

}
