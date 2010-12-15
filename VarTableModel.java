import javax.swing.table.*;
import java.util.regex.*;

/**
*   VarTableModel describes the data in the table
*   @author Jamie K. Teer
*/
public class VarTableModel extends AbstractTableModel {
    protected Object[][] data;
    private String[] columnNames;
    public final static int HIDDEN = 301;   //use this flag to hide a cell
    protected Object[] largestInColumn;
    protected VarData vdat;
    protected AbstractMapper[] mapper;
    
    public final static Pattern digits = Pattern.compile("^-?\\d+$");

    /**
    *   Dummy Constructor - only for use when subclassing!
    */
    public VarTableModel() {
        vdat = null;
        mapper = null;
        data = null;
        largestInColumn = null;

    }
    
    /**
    *   Constructor
    *
    *   @param inData An array of arrays [row][column]
    *   @param colN An array of column names
    *   @param inMap An array of Abstract Mapper objects, one for each column of data
    *   @param v A VarData object holding all of the data
    */
    public VarTableModel(int[][] inData, String[] colN, AbstractMapper[] inMap, VarData v) {
        vdat = v;
        mapper = inMap;
        if (inData.length !=0) {
            data = new Object[inData.length][inData[0].length];
            largestInColumn = new Object[inData[0].length];
        }

        initModel(inData, colN, 0);
    }


    /**
    *   Common constructor
    *   @param offset This determine where to start sample handling - use 1 to ignore sample name (1st column)
    */
    protected void initModel(int[][] inData, String[] colN, int offset) {
        
        if (inData.length == 0) {
            data = new Object[][]{ {"No Results to Show"} };
            columnNames = new String[]{""};
            largestInColumn = new Object[]{data[0][0]};
        }
        else {
            columnNames = colN;
            boolean first = true;
                        
            for ( int i = 0; i < inData.length; i++) {
                for (int j = offset; j < inData[i].length; j++) {
                    int mapIndex = j - offset; //Must do this to ensure we get the right sampleMapper index!
                    
                    /*
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
                    */

                    // ###new
                    switch (mapper[mapIndex].getDataType()) {
                        case VarData.INTEGER:
                            data[i][j] = new Integer(inData[i][j]);
                            if (first || (Integer)data[i][j] > (Integer)largestInColumn[j]) {
                                largestInColumn[j] = data[i][j];
                            }
                            break;
                        case VarData.FLOAT:
                            data[i][j] = new Float(mapper[mapIndex].getFloat(inData[i][j]));
                            if (first || (Float)data[i][j] > (Float)largestInColumn[j]) {
                                largestInColumn[j] = data[i][j];
                            }
                            break;
                        case VarData.STRING:
                            data[i][j] = mapper[mapIndex].getString(inData[i][j]);
                            if (first || ((String)data[i][j]).length() > largestInColumn[j].toString().length()) {
                                largestInColumn[j] = data[i][j];
                            }
                            break;
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
    *   !!! Note that this affects TableCellRenderer behavior!!!
    *   As the class is no longer "Object", Class type must be explicit.
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
