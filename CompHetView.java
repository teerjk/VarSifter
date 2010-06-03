import java.awt.*;
import javax.swing.*;
import java.util.HashSet;
import java.awt.event.*;
import components.TableSorter;


/* *******************
*    Displays alternate view for compound het pairs
*  *******************
*/

public class CompHetView extends JFrame {

    private JTable outTable;
    private TableSorter sorter;
    private String[] columnName;
    private String[][] data;
    

    /* ****************
    *   Initiate GUI
    *  ****************
    */
    public CompHetView(String index, VarData vdat) {

        //initiate parent window
        super("Compound Het View");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel pane = new JPanel();
        pane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
               
        
        //System.out.println(index + " " + vdat.getClass());
        String[] temp = index.split(",", 0);

        if (temp.length == 2 && temp[1].equals("0")) {
            System.out.println("Not a compound Het pair!");
            data = new String[][]{ { "Not a compound Het pair!" } };
            columnName = new String[]{""};
        }
        else {
            data = vdat.returnIndexPairs(temp);
            columnName = new String[]{"A_score", "A_pos", "B_score", "B_pos"};
        }

        //for (int i=0; i<data.length; i++) {
        //    System.out.println(data[i][0] + " <-> " + data[i][1]);
        //}
        
        outTable = new JTable();
        sorter = new TableSorter( new CompHetTableModel(data, columnName));
        outTable.setModel(sorter);
        sorter.setTableHeader(outTable.getTableHeader());
        outTable.setDefaultRenderer(Number.class, new VarScoreRenderer());
        //outTable = new JTable(data, columnName);
        JScrollPane sPane = new JScrollPane(outTable,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.add(sPane);
        add(pane);
        pack();
        setVisible(true);
    }
}
