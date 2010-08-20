import javax.swing.*;
import javax.swing.table.*;

/**
*   Handle blanking of hidden cells in sample table
*/
public class SampleScoreRenderer extends DefaultTableCellRenderer {

    public SampleScoreRenderer() { super(); }

    /**
    *   Sets text of cell to be empty if the value of the integer is the HIDDEN flag
    *
    *   @param val The stored value of the cell
    */
    @Override
    public void setValue(Object val) {
        if (val.toString().equals( ((Integer)VarTableModel.HIDDEN).toString() )) {
            setText("");
        }
        else {
            setText(val.toString());
        }

    }
}
