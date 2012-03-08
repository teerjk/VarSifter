import javax.swing.JCheckBox;
import javax.swing.table.AbstractTableModel;

/**
*   Simple TableModel for a JTable in a JDialog
*/
public class DialogTableModel extends AbstractTableModel {
    protected Object[][] data;
    protected String[] colNames;
    protected int editableColCount; //Last this many columns are editable

    /**
    *   Constructor
    *   @param inData 2-d array of Objects
    *   @param inCol Array of column names
    *   @param inCount Number of editable columns (counting from the right)
    */
    public DialogTableModel(Object[][] inData, String[] inCol, int inCount) {
        data = inData;
        colNames = inCol;
        editableColCount = inCount;
    }

    /**
    *   Return number of rows
    *   @return Number of rows in model
    */
    public int getRowCount() {
        return data.length;
    }

    /**
    *   Return number of columns
    *   @return Number of columns in model
    */
    public int getColumnCount() {
        return colNames.length;
    }

    /**
    *   Return column names
    *   @param col Column number
    *   @return The name of the column
    */
    @Override
    public String getColumnName(int col) {
        return colNames[col];
    }

    /**
    *   Return Value of cell
    *   @param row  Row of desired cell
    *   @param col  Column of desired cell
    *   @return The Object stored in this cell
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
    @Override
    public Class getColumnClass(int c) {
        return getValueAt(0,c).getClass();
    }

    /**
    *   Returns whether or not cell is editable.  Only last two column editable.
    *   @param row Row of desired cell
    *   @param col Column of desired cell
    *   @return True is cell is editable.
    */
    @Override
    public boolean isCellEditable(int row, int col) {
        if (col >= getColumnCount() - editableColCount) {
            if (getColumnClass(col) == JCheckBox.class && !((JCheckBox)getValueAt(row, col)).isEnabled() ) {
                return false;
            }
            else {
                return true;
            }
        }
        else {
            return false;
        }
    }

    /**
    *   Set a new value for the cell
    *   @param newVal The new Value
    *   @param row The row of desired cell
    *   @param col The column of desired cell
    */
    @Override
    public void setValueAt(Object newVal, int row, int col) {
        data[row][col] = newVal;
        fireTableCellUpdated(row, col);
        fireTableDataChanged();
    }
}
