import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/**
*   Creates a window to handle GUI selection of columns to load (and display)
*/
public class ColumnSelectionDialog {

    public static final String[] colNames = { "Annotation Column",
                                              "Display"
                                            };

    private Object[][] data;    // (column names, flags)
    private BitSet colMask; // Only load/show columns if bit is SET

    
    /**
    *   Constructor
    *
    *   @param inCols Array of column names
    *   @param reqHeaders  String array containing the required headers
    */
    public ColumnSelectionDialog(String[] inCols, String[] reqHeaders) {
        data = new Object[inCols.length][colNames.length];
        colMask = new BitSet(inCols.length);
        Set<String> reqSet = new HashSet<String>(Arrays.asList(reqHeaders));
        
        for (int i=0; i<inCols.length; i++) {
            data[i][0] = inCols[i];
            data[i][1] = new JCheckBox("", true);
            if (reqSet.contains(inCols[i])) {
                ((JCheckBox)data[i][1]).setEnabled(false);
            }
        }
    }
    

    /**
    *   Prepare and draw dialog box
    */
    public BitSet runDialog() {

        JOptionPane oPane = new JOptionPane();
        JTable mapTable = getTable();
        JScrollPane s = new JScrollPane(mapTable);
        oPane.setMessage(s);
        oPane.setOptionType(JOptionPane.OK_CANCEL_OPTION);

        JDialog d = oPane.createDialog(null, "Choose annotation columns to load and display");
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.setResizable(true);
        d.setVisible(true);
        d.dispose();

        int dialogOpt;
        Object oR = oPane.getValue();
        if (oR == null) {
            dialogOpt = JOptionPane.CANCEL_OPTION;
        }
        else if (oR instanceof Integer) {
            dialogOpt = (Integer)oR;
        }
        else {
            dialogOpt = JOptionPane.CANCEL_OPTION;
        }

        //Handle input data
        if (dialogOpt == JOptionPane.OK_OPTION) {
            DialogTableModel dtm = (DialogTableModel)mapTable.getModel();

            for (int i=0; i<dtm.getRowCount(); i++) {
                if ( ((JCheckBox)dtm.getValueAt(i,1)).isSelected() ) {
                    colMask.set(i);
                }
            }
        }
        else {
            System.out.println("All Columns will be displayed");
            colMask.set(0, data.length);
        }

        return colMask;
    }


    /**
    *   Initiate JTable to place in dialog
    *   @return The JTable with data and listener
    */
    private JTable getTable() {
        JTable t = new JTable(new DialogTableModel(data, colNames, 1));
        t.setDefaultRenderer(JCheckBox.class, new JCheckBoxRenderer());
        t.setDefaultEditor(JCheckBox.class, new JCheckBoxCellEditor());
        return t;
    }
}
