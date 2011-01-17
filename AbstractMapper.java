import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;

/**
*   Interface defining the common methods for different Mapper objects
*   @author Jamie K. Teer
*/
public interface AbstractMapper {

    /**
    *   Return type of data as Class
    *
    *   @return int representing the data in this object - fields in VarData
    */
    public int getDataType();


    /**
    *   Returns index of object
    *
    *   @param obj Object for which to return index
    *   @return Index of object is present, or -1
    */
    public int getIndexOf( Object obj );


    /**
    *   Add new element and return its index
    *   
    *   @param obj element to add
    *   @return Index of newly added element
    */
    public int addData(Object obj);


    /**
    *   Return requested String
    *
    *   @param index The index of String to return
    *   @return Returns a string, or null if not a String subclass
    */
    public String getString(int index);


    /**
    *   Return requested Float
    *   
    *   @param index The index of float to return
    *   @return Returns a float, or null if not a Float subclass
    */
    public float getFloat(int index);


    /**
    *   Return length of data object
    *
    *   @return Length of data
    */
    public int getLength();

    /**
    *   Return a sorted array of entries in this Object
    *
    *   @return An array of type String with the elements stored in this object
    */
    public String[] getSortedEntries();

}
