import javax.swing.*;
import javax.swing.table.*;
import java.awt.Component;
import java.awt.event.*;

/**
*   Custom Editor to handle JRadioButton (as itself, not as string)
*/
public class JRadioButtonCellEditor extends AbstractCellEditor implements TableCellEditor,ItemListener {

    private JRadioButton rB;

    public JRadioButtonCellEditor() {
    }


    public Object getCellEditorValue() {
        rB.removeItemListener(this);
        return rB;
    }


    public Component getTableCellEditorComponent(JTable t,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int col) {
        rB = (JRadioButton)value;
        rB.addItemListener(this);
        rB.setHorizontalAlignment(SwingConstants.CENTER);
        return rB;
    }

    public void itemStateChanged (ItemEvent e) {
        super.fireEditingStopped();
    }
}
