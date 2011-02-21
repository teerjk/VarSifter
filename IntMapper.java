import java.util.BitSet;
import java.util.HashMap;
import java.util.regex.*;

/**
*   Subclass of AbstractMapper to handle ints
*   **Only intializes dataType, as we currently are not mapping ints**
*   @author Jamie K. Teer
*/
public class IntMapper implements AbstractMapper {
    private HashMap<Integer, Integer> dataList;
    private final int dataType = VarData.INTEGER;

    /**
    *   Constructor
    */
    public IntMapper() {
        dataList = null;
    }

    public BitSet filterWithPattern(Pattern pat) {
        //for now, as no data is being stored here, return null
        return null;
    }

    public int getDataType() {
        return dataType;
    }


    public int getIndexOf( Object obj ) {
        return -1;
    }


    /**
    *   Not used, returns null
    */
    public int addData(Object obj) {
        return -1;
    }

    /**
    *   Returns String form of int
    *   @return String form of int
    */
    public String getString(int index) {
        return String.valueOf(index);
    }

    /**
    *   Not used 
    */
    public float getFloat(int index) {
        return -1f;
    }

    public int getLength() {
        return 0;
    }

    /**
    *   Not used (no data stored in IntMapper), returns null
    */
    public String[] getSortedEntries() {
        return null;
    }
}
