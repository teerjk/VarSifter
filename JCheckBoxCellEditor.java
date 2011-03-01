import javax.swing.*;
import javax.swing.table.*;
import java.awt.Component;

/**
*   Custom Editor to handle JCheckBox (as itself, not as boolean)
*/
public class JCheckBoxCellEditor extends AbstractCellEditor implements TableCellEditor {

    private JCheckBox cB;

    public JCheckBoxCellEditor() {
    }


    public Object getCellEditorValue() {
        return cB;
    }


    public Component getTableCellEditorComponent(JTable t,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int col) {
        cB = (JCheckBox)value;
        cB.setHorizontalAlignment(SwingConstants.CENTER);
        return cB;
    }
}
