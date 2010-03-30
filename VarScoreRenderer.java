import javax.swing.*;
import javax.swing.table.*;

public class VarScoreRenderer extends DefaultTableCellRenderer {

    public VarScoreRenderer() { super(); }

    public void setValue(Object negVal) {
        int nv = ((Integer)negVal).intValue();
        if (nv < 0) {
            setText("");
        }
        else {
            setText(Integer.toString(nv));
        }
        setHorizontalAlignment(SwingConstants.CENTER);
    }
}
