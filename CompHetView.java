import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.*;
import components.TableSorter;


/**
*   Displays alternate view for compound het pairs
*   @author Jamie K. Teer
*/
public class CompHetView extends JFrame {

    private JTable outTable;
    private TableSorter sorter;
    private final String[] columnDefaultName = new String[]{"Gene_name", "Chr", 
                                                            "A_left flank", "A_score", "A_type",  
                                                            "B_left flank", "B_score", "B_type"};
    private final static int COLUMN_NUMBER = 3;
    private String[] columnName;
    private int[][] data;
    private VarData v;
    

    /**
    *   Construct object using a single variant and its associated positions
    *
    *   @param index A comma separated string of indices of positions linked with a single variant
    *   @param vdat VarData object holding the data
    *   @param isSamples True if sample information is desired
    */
    public CompHetView(String index, VarData vdat, boolean isSamples) {

        super("Compound Het Pair View");
        
        String[] temp = index.split(",", 0);

        if (temp.length == 2 && temp[1].equals("0")) {
            data = new int[][]{ { } };
            columnName = new String[]{"Not a compound Het pair!"};
        }
        else {
            data = vdat.returnIndexPairs(temp, isSamples);
            if (isSamples) {
                String[] sampleNames = vdat.returnSampleNamesOrig();
                int defaultNameLength = columnDefaultName.length;
                int sampleNameLength = sampleNames.length;
                columnName = new String[defaultNameLength + (sampleNameLength * 2)];
                System.arraycopy(columnDefaultName, 0, columnName, 0, defaultNameLength - COLUMN_NUMBER);
                System.arraycopy(sampleNames, 0, columnName, defaultNameLength - COLUMN_NUMBER, sampleNameLength);
                System.arraycopy(columnDefaultName, defaultNameLength - COLUMN_NUMBER, columnName, 
                                 (defaultNameLength - COLUMN_NUMBER + sampleNameLength), COLUMN_NUMBER);
                System.arraycopy(sampleNames, 0, columnName, (defaultNameLength + sampleNameLength), sampleNameLength);
            }
            else {
                columnName = columnDefaultName;
            }
        }

        v = vdat;
        initTable();
        
    }

    /**
    *   Construct object using an array of variants and their associated positions
    *
    *   @param indices An array of comma separated strings of indices of positions
    *   @param vdat VarData object holding the data
    *   @param isSamples True if sample information is desired
    */
    public CompHetView(String[] indices, VarData vdat, boolean isSamples) {

        super("Compound Het Multi View");
        int pairs = 0;
        List<int[][]> tData = new ArrayList<int[][]>();
        
        if (indices.length == 0) {
            data = new int[][]{ { } };
            columnName = new String[] {"No compound hets!"};
        }
        else {
            String[][] temp = new String[indices.length][];
            for (int i=0; i<indices.length;i++) {
                temp[i] = indices[i].split(",", 0);
            }

            for (int i=0; i<temp.length; i++) {
                int[][] outTemp = vdat.returnIndexPairs(temp[i], isSamples);
                if (outTemp != null) {
                    pairs += outTemp.length;
                    tData.add(outTemp);
                }
            }
            data = new int[pairs][];
            pairs = 0;
            for ( int[][] t: tData ) {
                for (int i=0; i< t.length; i++) {
                    data[pairs+i] = new int[ t[i].length ];
                    System.arraycopy( t[i], 0, data[pairs+i], 0, t[i].length );
                }
                pairs += t.length;
            }

            if (isSamples) {
                String[] sampleNames = vdat.returnSampleNamesOrig();
                int defaultNameLength = columnDefaultName.length;
                int sampleNameLength = sampleNames.length;
                columnName = new String[defaultNameLength + (sampleNameLength * 2)];
                System.arraycopy(columnDefaultName, 0, columnName, 0, defaultNameLength - COLUMN_NUMBER);
                System.arraycopy(sampleNames, 0, columnName, defaultNameLength - COLUMN_NUMBER, sampleNameLength);
                System.arraycopy(columnDefaultName, defaultNameLength - COLUMN_NUMBER, columnName, 
                                 (defaultNameLength - COLUMN_NUMBER + sampleNameLength), COLUMN_NUMBER);
                System.arraycopy(sampleNames, 0, columnName, (defaultNameLength + sampleNameLength), sampleNameLength);
            }
            else {
                columnName = columnDefaultName;
            }
        }
        v = vdat;
        initTable();

    }


    /**
    *   Initialize the GUI
    */
    private void initTable() {

        //initiate parent window
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.setPreferredSize(new Dimension(630, 400));

        outTable = new JTable();
        sorter = new TableSorter( new CompHetTableModel(data, columnName, v));
        outTable.setModel(sorter);
        sorter.setTableHeader(outTable.getTableHeader());
        JScrollPane sPane = new JScrollPane(outTable,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sPane.setPreferredSize(new Dimension(610,400));
        outTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        DefaultTableCellRenderer tcr = new DefaultTableCellRenderer();
        tcr.setHorizontalAlignment(SwingConstants.CENTER);
        outTable.setDefaultRenderer(String.class, tcr);

        JLabel linesl = new JLabel("Number of positions (Every pair is seen twice): ");
        JLabel lines = new JLabel( (Integer.toString(outTable.getRowCount())));
        JPanel stats = new JPanel();
        stats.add(linesl);
        stats.add(lines);
        
        pane.add(sPane);
        pane.add(stats);
        add(pane);
        pack();
        setVisible(true);
    }
              
}
