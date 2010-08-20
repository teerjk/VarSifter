import javax.swing.table.*;
import java.util.regex.*;


/**
*   CompHetTableModel sets up data for comp het view
*/
public class CompHetTableModel extends AbstractTableModel {
    private Object data[][];
    private String[] columnNames;
    
    /**
    *   Constructor
    *
    *   @param inData An array of arrays [row][column]
    *   @param colN An array of column names
    */
    public CompHetTableModel(String[][] inData, String[] colN) {
        
        data = new Object[inData.length][inData[0].length];
        columnNames = colN;


        for (int i=0; i < inData.length; i++) {
            for (int j=0; j < inData[i].length; j++) {
                
                if (VarTableModel.digits.matcher(inData[i][j]).find()) {
                    data[i][j] = Integer.parseInt(inData[i][j]);
                }
                else {
                    data[i][j] = inData[i][j];
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
