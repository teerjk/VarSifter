import javax.swing.table.*;
import java.util.regex.*;

/* ************
*   VarTableModel describes the data in the table
*  ************
*/

public class VarTableModel extends AbstractTableModel {
    private Object[][] data;
    private String[] columnNames;
    public final static int HIDDEN = 301;   //use this flag to hide a cell
    private Object[] largestInColumn;
    private VarData vdat;
    
    public final static Pattern digits = Pattern.compile("^-?\\d+$");
    
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

    public Object[] getLargestInColumn() {
        return largestInColumn;
    }

    public int getRowCount() {
        return data.length;
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Object getValueAt(int row, int col) {
        return data[row][col];
    }

    public Class getColumnClass(int c) {
        return getValueAt(0,c).getClass();
    }

    public boolean isCellEditable(int row, int col) {
        return vdat.isEditable(col);
    }

    public void setValueAt (Object value, int row, int col) {
        data[row][col] = value;
        fireTableCellUpdated(row,col);
    }
}
