import javax.swing.*;
import javax.swing.table.*;

/**
*   Handles blanking of hidden cells in Variant table, sets center alignment
*   @author Jamie K. Teer
*/
public class VarScoreRenderer extends DefaultTableCellRenderer {

    public VarScoreRenderer() { super(); }

    /**
    *   Sets text of cell to be empty if the value of the integer is the HIDDEN flag
    *
    *   @param val The stored value of the cell
    */
    @Override
    public void setValue(Object val) {
        int nv = ((Integer)val).intValue();
        if (nv == VarTableModel.HIDDEN) {
            setText("");
        }
        else {
            setText(Integer.toString(nv));
        }
        setHorizontalAlignment(SwingConstants.CENTER);
    }
}
