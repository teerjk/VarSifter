import javax.swing.table.*;
import java.util.regex.*;

/**
*   VarTableModel describes the data in the table
*/
public class VarTableModel extends AbstractTableModel {
    private Object[][] data;
    private String[] columnNames;
    public final static int HIDDEN = 301;   //use this flag to hide a cell
    private Object[] largestInColumn;
    private VarData vdat;
    
    public final static Pattern digits = Pattern.compile("^-?\\d+$");
    
    /**
    *   Constructor
    *
    *   @param inData An array of arrays [row][column]
    *   @param colN An array of column names
    *   @param v A VarData object holding all of the data
    */
    public VarTableModel(String[][] inData, String[] colN, VarData v) {
        vdat = v;
        
        if (inData.length == 0) {
            data = new Object[][]{ {"No Results to Show"} };
            columnNames = new String[]{""};
            largestInColumn = new Object[]{data[0][0]};
        }
        else {
            data = new Object[inData.length][inData[0].length];
            largestInColumn = new Object[inData[0].length];
            columnNames = colN;
            boolean first = true;

            
            for ( int i = 0; i < inData.length; i++) {
                for (int j = 0; j < inData[i].length; j++) {
                        
                    if (digits.matcher(inData[i][j]).find() && !vdat.isEditable(j)) {
                        data[i][j] = Integer.parseInt(inData[i][j]);
                        
                        if (first || inData[i][j].length() > largestInColumn[j].toString().length()) {
                            largestInColumn[j] = Integer.parseInt(inData[i][j]);
                        }
                    }
                    else {
                        data[i][j] = inData[i][j];
                        
                        if (first || inData[i][j].length() > largestInColumn[j].toString().length()) {
                            largestInColumn[j] = inData[i][j];
                        }
                    }
                }
                first = false;
            }
        }
    }

    /**
    *   Returns the largest element in the column
    *
    *   @return Array of objects of the largest elements in each column
    */
    public Object[] getLargestInColumn() {
        return largestInColumn;
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

    /**
    *   Return the class of a given column
    *
    *   @param c The column number
    *   @return The class of the data in the column
    */
    public Class getColumnClass(int c) {
        return getValueAt(0,c).getClass();
    }

    /**
    *   Returns true if specified cell is editable
    *
    *   @param row The row of the desired cell
    *   @param col The column of the desired cell
    *   @return True if the cell is editable
    */
    @Override
    public boolean isCellEditable(int row, int col) {
        return vdat.isEditable(col);
    }

    /**
    *   Sets the value of the specified column
    *   
    *   @param value The value to set
    *   @param row The row of the target cell
    *   @param col The column of the target cell
    */
    @Override
    public void setValueAt (Object value, int row, int col) {
        data[row][col] = value;
        fireTableCellUpdated(row,col);
    }
}
