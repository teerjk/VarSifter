/**
*   SampleTableModel describes samples in the table
*   @author Jamie K. Teer
*/
public class SampleTableModel extends VarTableModel {

    /**
    *   Custom Constructor
    *
    *   @param inData An array of arrays [row][column]
    *   @param colN An array of column names
    *   @param v A VarData object holding all of the data
    */
    public SampleTableModel(int[][] inData, String[] colN, VarData v) {
        vdat = v;
        mapper = vdat.returnSampleMap();
        if (inData.length != 0) {
            data = new Object[inData.length][inData[0].length];
            largestInColumn = new Object[inData[0].length];
            String[] names = v.returnSampleNames();

            // Since this is a sample, 1st column is a String - handle here
            for (int i=0; i<inData.length; i++) {
                data[i][0] = names[inData[i][0]];
                largestInColumn[0] = data[i][0];
            }
        }

        initModel(inData, colN, 1);
    }
}
