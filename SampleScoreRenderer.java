import javax.swing.*;
import javax.swing.table.*;

public class SampleScoreRenderer extends DefaultTableCellRenderer {

    public SampleScoreRenderer() { super(); }

    public void setValue(Object val) {
        if (val.toString().equals( ((Integer)VarTableModel.HIDDEN).toString() )) {
            setText("");
        }
        else {
            setText(val.toString());
        }

    }
}
