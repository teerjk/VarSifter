import javax.swing.*;
import javax.swing.table.*;

public class VarScoreRenderer extends DefaultTableCellRenderer {

    public VarScoreRenderer() { super(); }

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
