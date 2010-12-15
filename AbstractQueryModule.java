import java.util.BitSet;

/**
*   Interface defining the custom compiled QueryModule object
*   @author Jamie K. Teer
*/
public interface AbstractQueryModule {

    /**
    *   Execute the QueryModule
    *
    *   @param vdat VarData Object holding the data
    */
    public abstract BitSet executeCustomQuery(VarData vdat);
}
