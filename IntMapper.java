import java.util.HashMap;

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
}
