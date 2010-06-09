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

        super("Compound Het Pair View");
        
        //System.out.println(index + " " + vdat.getClass());
        String[] temp = index.split(",", 0);

        if (temp.length == 2 && temp[1].equals("0")) {
            data = new String[][]{ { "Not a compound Het pair!" } };
            columnName = new String[]{""};
        }
        else {
            data = vdat.returnIndexPairs(temp);
            columnName = new String[]{"refseq", "Chr", "A_pos", "A_score", "A_type",  "B_pos", "B_score", "B_type"};
        }

        //for (int i=0; i<data.length; i++) {
        //    System.out.println(data[i][0] + " <-> " + data[i][1]);
        //}

        initTable();
        
    }

    public CompHetView(String[] indices, VarData vdat) {

        super("Compound Het Gene View");
        int pairs = 0;
        
        if (indices.length == 0) {
            data = new String[][]{ { "No compound hets" } };
            columnName = new String[] {""};
        }
        else {
            String[][] temp = new String[indices.length][];
            for (int i=0; i<indices.length;i++) {
                temp[i] = indices[i].split(",", 0);
                pairs += (temp[i].length - 1);
            }
            data = new String[pairs][];
            pairs = 0;

            for (int i=0; i<temp.length; i++) {
                String[][] outTemp = vdat.returnIndexPairs(temp[i]);
                for (int j=0; j<outTemp.length; j++) {
                    data[pairs+j] = new String[outTemp[j].length];
                    //System.out.println(outTemp[j][0] + " " + outTemp[j].length);
                    System.arraycopy(outTemp[j], 0, data[pairs+j], 0, outTemp[j].length);
                }
                pairs += outTemp.length;
            }
            columnName = new String[]{"refseq", "Chr", "A_pos", "A_score", "A_type",  "B_pos", "B_score", "B_type"};
        }
        initTable();

    }


    private void initTable() {

        //initiate parent window
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
        pane.setPreferredSize(new Dimension(630, 400));
        //pane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        outTable = new JTable();
        sorter = new TableSorter( new CompHetTableModel(data, columnName));
        outTable.setModel(sorter);
        sorter.setTableHeader(outTable.getTableHeader());
        //outTable = new JTable(data, columnName);
        JScrollPane sPane = new JScrollPane(outTable,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        //sPane.setPreferredSize(new Dimension(610,400));
        outTable.setPreferredSize(new Dimension(610,400));
        outTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        outTable.setDefaultRenderer(Number.class, new VarScoreRenderer());
        pane.add(sPane);
        add(pane);
        pack();
        setVisible(true);
    }
              
}
