import javax.swing.*;
import javax.swing.table.*;
import java.awt.Component;


/**
*   Renderer for the displaying of JRadioButtones in a table
*/
public class JRadioButtonRenderer extends JRadioButton implements TableCellRenderer {

    public JRadioButtonRenderer() {
        super();
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object inRb, boolean isSelected, boolean hasFocus,
                                                   int row, int col) {
        JRadioButton rb = (JRadioButton)inRb;
        if (isSelected) {
            setForeground(t.getSelectionForeground());
            setBackground(t.getSelectionBackground());
        }
        else {
            setForeground(t.getForeground());
            setBackground(t.getBackground());
        }

        setEnabled( rb.isEnabled() );
        setSelected( rb.isSelected() );
        return this;
    }
}
