import java.util.BitSet;

/**
*   An object to hold filtering options
*   @author Jamie K. Teer
*/
public class DataFilter {

    private BitSet[] mask;
    
    private String geneFile;
    private String bedFile;
    private int[] spinnerData;
    private String geneQuery;
    
    private int minMPG;
    private float minMPGCovRatio;
    private int genScoreThresh;
    private String geneDelim;

    /**
    *   Constructor to create object
    *
    *   @param inMask Represents the filters to apply { mutation type, fixed }
    *   @param inGeneFile The name of a file of genes, one per line
    *   @param inBedFile The name of a bedfile
    *   @param inSpinnerData An array of values from the spinners.  Indicies defined by constants in VarData.
    *   @param inGeneQuery A regular expression used to filter on gene name, or null to allow all
    *   @param inMinMPG Defined inimum MPG score
    *   @param inMinMPGCovRatio Defined minimum (MPG score / coverage)
    *   @param inGenScoreThresh Genotype Score threshold used for certain tests
    *   @param inGeneDelim  Internal gene delimiter string
    */
    public DataFilter(BitSet[] inMask,
                      String inGeneFile,
                      String inBedFile,
                      int[] inSpinnerData,
                      String inGeneQuery,
                      int inMinMPG,
                      float inMinMPGCovRatio,
                      int inGenScoreThresh,
                      String inGeneDelim
                      ) {

        mask = inMask;
        geneFile = inGeneFile;
        bedFile = inBedFile;
        spinnerData = inSpinnerData;
        geneQuery = inGeneQuery;
        minMPG = inMinMPG;
        minMPGCovRatio = inMinMPGCovRatio;
        genScoreThresh = inGenScoreThresh;
        geneDelim = inGeneDelim;
    }


    /**
    *   Return BitSet with filter information
    *
    *   @return The filter mask BitSet
    */
    public BitSet[] getMask() {
        BitSet[] m = new BitSet[mask.length];
        for (int i=0; i < mask.length; i++) {
            m[i] = (BitSet)mask[i].clone();
        }
        return m;
    }


    /**
    *   Return gene file name
    *   @return Gene File name
    */
    public String getGeneFile() {
        return geneFile;
    }


    /**
    *   Return bed file name
    *   @return Bed File name
    */
    public String getBedFile() {
        return bedFile;
    }


    /**
    *   Return spinner values
    *   @return An array of spinner values = indices set according to VarData constants
    */
    public int[] getSpinnerData() {
        int[] out = new int[spinnerData.length];
        System.arraycopy(spinnerData, 0, out, 0, spinnerData.length);
        return out;
    }


    /**
    *   Return the gene query
    *   @return The Gene query regex (as a string)
    */
    public String getGeneQuery() {
        return geneQuery;
    }


    /**
    *   Return minimum MPG score used for general filtering
    *   @return Minimum MPG score
    */
    public int getMinMPG() {
        return minMPG;
    }


    /**
    *   Return minimum MPG score / coverage ratio
    *   @return Minimum MPG score / coverage
    */
    public float getMinMPGCovRatio() {
        return minMPGCovRatio;
    }


    /**
    *   Return genotype score threshold used for certain tests
    *   @return Genotype Score threshold
    */
    public int getGenScoreThresh() {
        return genScoreThresh;
    }


    /**
    *   Return internal gene delimter
    *   @return Gene delimiter String
    */
    public String getGeneDelim() {
        return geneDelim;
    }
}
