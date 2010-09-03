import java.util.BitSet;

/**
*   Interface defining the custom compiled QueryModule object
*/
public interface AbstractQueryModule {

    /**
    *   Execute the QueryModule
    *
    *   @param vdat VarData Object holding the data
    */
    public abstract BitSet executeCustomQuery(VarData vdat, int refIndex);
}
