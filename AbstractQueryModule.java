import java.util.BitSet;

/**
*   Interface defining the custom compiled QueryModule object
*   @author Jamie K. Teer
*/
public interface AbstractQueryModule {

    /**
    *   Execute the QueryModule
    */
    public abstract BitSet executeCustomQuery();
}
