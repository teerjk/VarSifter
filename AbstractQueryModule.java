import java.util.BitSet;

public interface AbstractQueryModule {
    public abstract BitSet executeCustomQuery(VarData vdat);
}
