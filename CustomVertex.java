import java.io.Serializable;

/**
*   Defines a CustomVertex for the JUNG tree structure
*   @author Jamie K. Teer
*/
public class CustomVertex implements Serializable {
    private String id;
    private String query;

    /**
    *   Construct a CustomVertex
    *
    *   @param id The id of the vertex (a name to refer to it)
    *   @param query The query portion stored by this vertex
    */
    public CustomVertex(String id, String query) {
        this.id = id;
        this.query = query;
    }

    /**
    *   Return the id of this vertex
    *
    *   @return The id of this string
    */
    public String toString() {
        return id;
    }
    
    /**
    *   The data of this vertex
    *
    *   @return The query stored in this vertex
    */
    public String getQuery() {
        return query;
    }
}
