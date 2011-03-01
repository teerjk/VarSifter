import javax.swing.*;
import javax.swing.table.*;
import java.awt.Component;


/**
*   Renderer for the displaying of JCheckBoxes in a table
*/
public class JCheckBoxRenderer extends JCheckBox implements TableCellRenderer {

    public JCheckBoxRenderer() {
        super();
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object inCb, boolean isSelected, boolean hasFocus,
                                                   int row, int col) {
        JCheckBox cb = (JCheckBox)inCb;
        if (isSelected) {
            setForeground(t.getSelectionForeground());
            setBackground(t.getSelectionBackground());
        }
        else {
            setForeground(t.getForeground());
            setBackground(t.getBackground());
        }

        setEnabled( cb.isEnabled() );
        setSelected( cb.isSelected() );
        return this;
    }
}
