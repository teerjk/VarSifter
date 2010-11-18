import java.awt.*;
import javax.swing.*;
import java.util.HashSet;
import java.awt.event.*;
import components.TableSorter;


/**
*    Displays alternate view for compound het pairs
*/
public class CompHetView extends JFrame {

    private JTable outTable;
    private TableSorter sorter;
    private final String[] columnDefaultName = new String[]{"refseq", "Chr", 
                                                            "A_left flank", "A_score", "A_type",  
                                                            "B_left flank", "B_score", "B_type"};
    private final int COLUMN_NUMBER = 3;
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
        
        //System.out.println(index + " " + vdat.getClass());
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

        //for (int i=0; i<data.length; i++) {
        //    System.out.println(data[i][0] + " <-> " + data[i][1]);
        //}

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

        super("Compound Het Gene View");
        int pairs = 0;
        
        if (indices.length == 0) {
            data = new int[][]{ { } };
            columnName = new String[] {"No compound hets!"};
        }
        else {
            String[][] temp = new String[indices.length][];
            for (int i=0; i<indices.length;i++) {
                temp[i] = indices[i].split(",", 0);
                pairs += (temp[i].length - 1);
            }
            data = new int[pairs][];
            pairs = 0;

            for (int i=0; i<temp.length; i++) {
                int[][] outTemp = vdat.returnIndexPairs(temp[i], isSamples);
                for (int j=0; j<outTemp.length; j++) {
                    data[pairs+j] = new int[outTemp[j].length];
                    //System.out.println(outTemp[j][0] + " " + outTemp[j].length);
                    System.arraycopy(outTemp[j], 0, data[pairs+j], 0, outTemp[j].length);
                }
                pairs += outTemp.length;
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
        //pane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        outTable = new JTable();
        sorter = new TableSorter( new CompHetTableModel(data, columnName, v));
        outTable.setModel(sorter);
        sorter.setTableHeader(outTable.getTableHeader());
        //outTable = new JTable(data, columnName);
        JScrollPane sPane = new JScrollPane(outTable,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sPane.setPreferredSize(new Dimension(610,400));
        //outTable.setPreferredSize(new Dimension(610,400));
        outTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        outTable.setDefaultRenderer(Number.class, new VarScoreRenderer());

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
