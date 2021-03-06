import javax.swing.table.*;
import java.util.regex.*;


/**
*   CompHetTableModel sets up data for comp het view
*   @author Jamie K. Teer
*/
public class CompHetTableModel extends AbstractTableModel {
    private Object data[][];
    private String[] columnNames;
    private String[] sampleValueName;
    private AbstractMapper[] annotMapper;
    private AbstractMapper[] sampleMapper;

    /**
    *   Constructor
    *
    *   @param inData An array of arrays [row][column]
    *   @param colN An array of column names
    */
    public CompHetTableModel(int[][] inData, String[] colN, VarData v) {
        annotMapper = v.returnAnnotMap();
        sampleMapper = v.returnSampleMap();
        sampleValueName = v.returnSampleValueNames();

        int[] compHetFields = v.returnCompHetFields();
        int chl = compHetFields.length;
        int sampleFieldsLength;

        if (inData.length == 0) {
            data = new Object[][]{ {"No Results to Show"} };
            columnNames = new String[]{""};
        }
        else {
            data = new Object[inData.length][inData[0].length];
            columnNames = colN;
            sampleFieldsLength = (inData[0].length - ( (chl*2) - 2)) / 2;


            for (int i=0; i < inData.length; i++) {
                int l = inData[i].length;
                
                //Annotations - 1st member of pair
                for (int j=0; j < chl; j++) {
                    if (compHetFields[j] == -1) {
                        data[i][j] = "-";
                        continue;
                    }

                    AbstractMapper aM = annotMapper[compHetFields[j]];
                    switch (aM.getDataType()) {
                        case VarData.INTEGER:
                            data[i][j] = Integer.valueOf(inData[i][j]);
                            break;
                        case VarData.FLOAT:
                            data[i][j] = new Float(aM.getFloat(inData[i][j]));
                            break;
                        case VarData.STRING:
                        case VarData.MULTISTRING:
                            data[i][j] = aM.getString(inData[i][j]);
                            break;
                    }
                }


                //samples - 1st member of pair
                for (int j = chl; j < (chl+sampleFieldsLength); j++) {
                    AbstractMapper sM = sampleMapper[ (j-chl) % sampleValueName.length ];
                    switch (sM.getDataType()) {
                        case VarData.INTEGER:
                            data[i][j] = Integer.valueOf(inData[i][j]);
                            break;
                        case VarData.FLOAT:
                            data[i][j] = new Float(sM.getFloat(inData[i][j]));
                            break;
                        case VarData.STRING:
                            data[i][j] = sM.getString(inData[i][j]);
                            break;
                    }
                }

                //Annotations, 2nd member of pair - fields 0,1 omitted
                for (int j = 2; j < chl; j++) {

                    int correctedIndex = j + chl + sampleFieldsLength - 2; //Subtract 2 for the fields we omit
                    if (compHetFields[j] == -1) {
                        data[i][correctedIndex] = "-";
                        continue;
                    }

                    AbstractMapper aM = annotMapper[compHetFields[j]];
                    switch (aM.getDataType()) {
                        case VarData.INTEGER:
                            data[i][correctedIndex] = Integer.valueOf(inData[i][correctedIndex]);
                            break;
                        case VarData.FLOAT:
                            data[i][correctedIndex] = new Float(aM.getFloat(inData[i][correctedIndex]));
                            break;
                        case VarData.STRING:
                        case VarData.MULTISTRING:
                            data[i][correctedIndex] = aM.getString(inData[i][correctedIndex]);
                            break;
                    }
                }

                //Samples - 2nd member of pair
                for (int j = (chl*2) - 2 + sampleFieldsLength; j < inData[i].length; j++) {
                    AbstractMapper sM = sampleMapper[ (j - ((chl*2)-2) - sampleFieldsLength) % sampleValueName.length ];
                    switch (sM.getDataType()) {
                        case VarData.INTEGER:
                            data[i][j] = Integer.valueOf(inData[i][j]);
                            break;
                        case VarData.FLOAT:
                            data[i][j] = new Float(sM.getFloat(inData[i][j]));
                            break;
                        case VarData.STRING:
                            data[i][j] = sM.getString(inData[i][j]);
                            break;
                    }
                }

            }
        }
    }

    /**
    *   Return the number of rows
    *
    *   @return The number of rows 
    */
    public int getRowCount() {
        return data.length;
    }

    /**
    *   Return the class of a given column
    *   !!! Note that this affects TableCellRenderer behavior!!!
    *   As the class is no longer "Object", Class type must be explicit.
    *
    *   @param c The column number
    *   @return The class of the data in the column
    */
    @Override
    public Class getColumnClass(int c) {
        return getValueAt(0,c).getClass();
    }

    /**
    *   Return the number of columns
    *
    *   @return The number of columns
    */
    public int getColumnCount() {
        return columnNames.length;
    }

    
    /**
    *   Return the name of a given column
    *
    *   @param col The column number
    *   @return The name of the column
    */
    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    /**
    *   Return the value of the specified cell
    *
    *   @param row The row of the desired cell
    *   @param col The column of the desired cell
    */
    public Object getValueAt(int row, int col) {
        return data[row][col];
    }

}
