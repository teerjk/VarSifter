import java.awt.*;
import java.net.URL;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.util.BitSet;
import java.util.regex.*;
import java.io.*;
import java.util.Map;
import components.TableSorter;

/** 
*   VarSifter is designed to read a flat file where a row contains a variant, and all sample info
*    and allow sorting, and filtering of the data. This class is a wrapper that handles the GUI
*    via Swing.
*   @author Jamie K. Teer
*/
public class VarSifter extends JFrame implements ListSelectionListener, ActionListener, TableModelListener {
    
    final static String version = "1.0";
    final static String id = "$Id$";

    final String govWork = "PUBLIC DOMAIN NOTICE\n" +
    "National Human Genome Research Institute\n" +
    "This software/database is \"United States Government Work\" under the terms of\n" +
    "the United States Copyright Act.  It was written as part of the authors'\n" +
    "official duties for the United States Government and thus cannot be copyrighted.\n" +
    "This software/database is freely available to the public for use without a copyright\n" +
    "notice.  Restrictions cannot be placed on its present or future use.\n\n" +
    "Although all reasonable efforts have been taken to ensure the accuracy and\n" +
    "reliability of the software and data, the National Human Genome Research Institute\n" +
    "(NHGRI) and the U.S. Government does not and cannot warrant the performance or results\n" +
    "that may be obtained by using this software or data.  NHGRI and the U.S. Government\n" +
    "disclaims all warranties as to performance, merchantability or fitness for any\n" +
    "particular purpose.\n\n" +
    "In any work or product derived from this material, proper attribution of the authors\n" +
    "as the source of the software or data should be made.";

    //This class uses TableSorter.java, a class to sort a JTable.
    //The two subsequent statements are required to be distributed with the 
    //source and binary re-distributions of the TableSorter class,

    final static String sunCopyright = "Copyright (c) 1995 - 2008 Sun Microsystems, Inc.  All rights reserved.";
    final static String sunDisclaimer = "Redistribution and use in source and binary forms, with or without\n" +
    "modification, are permitted provided that the following conditions\n" +
    "are met:\n" +
    "    \n" +
    "  - Redistributions of source code must retain the above copyright\n" +
    "    notice, this list of conditions and the following disclaimer.\n" +
    "       \n" +
    "  - Redistributions in binary form must reproduce the above copyright\n" +
    "    notice, this list of conditions and the following disclaimer in the\n" +
    "    documentation and/or other materials provided with the distribution.\n" +
    "           \n" +
    "  - Neither the name of Sun Microsystems nor the names of its\n" +
    "    contributors may be used to endorse or promote products derived\n" +
    "    from this software without specific prior written permission.\n\n" +
    "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS\n" +
    "IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,\n" +
    "THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR\n" +
    "PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR\n" +
    "CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,\n" +
    "EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,\n" +
    "PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR\n" +
    "PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF\n" +
    "LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING\n" +
    "NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS\n" +
    "SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.";

    final String geneNotFoundError  = "It looks like you may have switched columns! "
        + "Switch them so the gene names are in the first column!";

    final static int VARIANT_FILE = 0;
    final static int GENE_FILTER_FILE = 1;
    final static int BED_FILTER_FILE = 2;
    final static int MAX_COLUMN_SIZE = 150;

    final String[] sampleTableLabels = {"Sample", "Genotype", "Genotype score", "coverage"};


    final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    final int w = (int)dim.getWidth();
    final int h = (int)dim.getHeight();

    // Test using object[][], titles[]
    private VarData vdat = null;
    private JTable outTable;
    private TableSorter sorter;
    private JTable sampleTable;
    private TableSorter sampleSorter;
    private JScrollPane dataScroller;
    private JScrollPane sampleScroller;
    private ListSelectionModel lsm;
    private JLabel lines = new JLabel();

    private JCheckBox dbsnp = new JCheckBox("dbID");
    private JCheckBox mendRec = new JCheckBox("Hom. Recessive");
    private JCheckBox mendDom = new JCheckBox("Dominant");
    private JCheckBox mendBad = new JCheckBox("Inconsistent");
    private JCheckBox mendCompHet = new JCheckBox("Mend. Compound Het");
    private JCheckBox uniqInAff = new JCheckBox("Affected different from Norm");
    private JCheckBox caseControl = new JCheckBox("Case / Control");
    private JCheckBox filterFile = new JCheckBox("Include Gene File");
    private JCheckBox notFilterFile = new JCheckBox("Exclude Gene File");
    private JCheckBox bedFilterFile = new JCheckBox("Include Bed File Regions");
    private JCheckBox customQuery = new JCheckBox("Custom Query");

    private JCheckBox[] typeCBox;

    private JCheckBox[] cBox = { dbsnp,
                                 mendRec,
                                 mendDom,
                                 mendBad,
                                 mendCompHet,
                                 uniqInAff,
                                 caseControl,
                                 filterFile,
                                 notFilterFile,
                                 bedFilterFile,
                                 customQuery
                               };
    public final static int MENDHETREC = 4; //index of mendCompHet

    private JMenuItem openItem;
    private JMenuItem saveAsItem;
    private JMenuItem saveViewItem;
    private JMenuItem preferViewItem;
    private JMenuItem exitItem;
    private JMenuItem compHetViewItem;
    private JMenuItem customQueryViewItem;
    private JMenuItem aboutItem;

    private JButton apply = new JButton("Apply Filter");
    private JButton selectAll = new JButton("Select All");
    private JButton clear = new JButton("Clear All");
    private JRadioButton showVar = new JRadioButton("Show Variants", true);
    private JRadioButton showGene = new JRadioButton("Show Genes");
    private JButton check = new JButton("Check");
    private JButton filterFileButton = new JButton("Choose Gene File Filter");
    private JButton bedFilterFileButton = new JButton("Choose Bed File Filter");
    private JButton geneViewButton = new JButton("View Variants for Selected Gene");
    private JButton prefApply = new JButton("Apply Preferences");

    private JCheckBox compHetSamples = new JCheckBox("Show Sample Details");
    private JButton compHetGeneViewButton = new JButton("View by Gene");
    private JButton compHetPairViewButton = new JButton("View Pairs of Selected Position");
    private JButton compHetAllButton = new JButton("View All Compound Hets");

    private JFrame compHetParent;
    private JFrame customQueryParent;
    private CustomQueryView cqPane;
    private JFrame preferViewParent;

    private int[] spinnerData = new int[5]; //Hold data for Spinner values (use int values from VarData)
    
    private JLabel filterFileLabel = new JLabel("No Gene File Selected");
    private JLabel bedFilterFileLabel = new JLabel("No Bed File Selected");

    private JSpinner minAffSpinner = new JSpinner();
    private JLabel affSpinnerLabel = new JLabel("Diff. in at least:");
    private JTextField geneRegexField = new JTextField("");
    private JTextField minMPGField = new JTextField(10);
    private JTextField minMPGCovRatioField = new JTextField(10);
    private JTextField genScoreThreshField = new JTextField(10);
    private JTextField genScoreCovRatioThreshField = new JTextField(10);

    private JSpinner caseSpinner = new JSpinner();
    private JLabel caseSpinnerLabel = new JLabel("Var in cases (at least):");
    private JSpinner controlSpinner = new JSpinner();
    private JLabel controlSpinnerLabel = new JLabel("Var in controls (this or fewer):");
    private JSpinner minMPGSpinner = new JSpinner();
    private JSpinner minMPGCovRatioSpinner = new JSpinner();

    private JLabel linesl = new JLabel("Number of Variant Positions: ");
    
    final static String newLine = System.getProperty("line.separator");
    private String geneFile = null;
    private String bedFile = null;

    private BitSet[] mask = new BitSet[2]; // holds { type filter mask (typeCBox), preset (cBox) }
    private boolean isShowVar = true;
    private DataFilter df = null;
    public final static Pattern fDigits = Pattern.compile("^-?[0-9]+\\.[0-9]+$");
    private final Pattern vcfPat = Pattern.compile("\\.vcf$");
    private final Pattern gzPat = Pattern.compile("\\.gz$");

    //Default score cutoff thresholds
    private int minMPG = 0;
    private float minMPGCovRatio = 0f;
    private int genScoreThresh = 10;
    private float genScoreCovRatioThresh = 0.5f;

    private Map<String, Integer> typeMap;
    private AbstractMapper[] annotMapper;
    private AbstractMapper[] sampleMapper;


    /** 
    *   Initiate GUI, instantiate VarData
    *   Constructor using FileName
    *  
    *  @param inFile path to input file (VS format)
    */
    public VarSifter(String inFile) {
        
        //initiate parent window
        super("VarSifter - " + inFile);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(0, (h/8), (w/2), (h/2));

        if (inFile == null) {
            inFile = openData(VARIANT_FILE);
        }
        outTable = new JTable();
        redrawOutTable(inFile);
        initTable();
    }

    /** 
    *   Constructor using VarData object
    *  
    *  @param vdatTemp VarData object
    */
    public VarSifter(VarData vdatTemp) {
        
        super("VarSifter SubSet");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(w-(3* w/4), (h/2), (w/2), (h/2));
        //setBounds(0, (h/4), (w/2), (h/2));
        vdat = vdatTemp;
        outTable = new JTable();
        redrawOutTable(null);
        initTable();
    }
    

    /** 
    *   Handle Actions
    *  
    *  @param e resulting from clicking a button, menuitem
    */
    public void actionPerformed(ActionEvent e) {
        Object es = e.getSource();

        if (es == apply || es == geneRegexField) {
            
            sorter.setTableHeader(null);    //Must do this to avoid memory leak
            mask = getFilterMask();

            if (showVar.isSelected()) {
                isShowVar = true;
                linesl.setText("Number of Variant Positions: ");
            }
            else if (showGene.isSelected()) {
                isShowVar = false;
                linesl.setText("Number of Genes: ");
            }
            
            spinnerData[vdat.AFF_NORM_PAIR] = ((Integer)minAffSpinner.getValue()).intValue();
            spinnerData[vdat.CASE] = ((Integer)caseSpinner.getValue()).intValue();
            spinnerData[vdat.CONTROL] = ((Integer)controlSpinner.getValue()).intValue();
            spinnerData[vdat.MIN_MPG] = ((Integer)minMPGSpinner.getValue()).intValue();
            spinnerData[vdat.MIN_MPG_COV] = ((Integer)minMPGCovRatioSpinner.getValue()).intValue();
           
            df = new DataFilter(mask, geneFile, bedFile, spinnerData, getRegex(), minMPG, minMPGCovRatio, genScoreThresh);
            vdat.filterData(df);
            redrawOutTable(null);
            
        }

        else if (es == selectAll) {
            for (JCheckBox cb : cBox) {
                if (cb.isEnabled()) {
                    cb.setSelected(true);
                }
            }
        }

        else if (es == clear) {
            for (JCheckBox cb : typeCBox) {
                cb.setSelected(false);
            }
            for (JCheckBox cb : cBox) {
                cb.setSelected(false);
            }
            geneRegexField.setText("");
        }

        else if (es == filterFileButton) {
            String temp = openData(GENE_FILTER_FILE);
            if (temp != null) {
                geneFile = temp;
            }
        }

        else if (es == bedFilterFileButton) {
            String temp = openData(BED_FILTER_FILE);
            if (temp != null) {
                bedFile = temp;
            }
        }

        else if (es == geneViewButton) {

            //Use this button to return a VarSifter view of one gene
            String geneRegex = "";
            if (isShowVar) {
                int dataRowIndex = sorter.modelIndex(outTable.getSelectedRow());
                String geneName = vdat.returnDataValueAt(dataRowIndex, "Gene_name");
                geneRegex = "^" + geneName + "$";
            }
            else {
                try {
                    geneRegex = "^" + (String)outTable.getValueAt(outTable.getSelectedRow(), 0) + "$";
                }
                catch (ClassCastException cce) {
                    showError(geneNotFoundError);
                    return;
                }
            }

            BitSet[] emptyBS = { new BitSet(), new BitSet() };
            vdat.filterData(new DataFilter(emptyBS, null, null, spinnerData, geneRegex, minMPG, minMPGCovRatio, genScoreThresh));
            VarData tempVdat = vdat.returnSubVarData(vdat, null);
            VarSifter vs = new VarSifter(tempVdat);
            
            //Must return the filtered state to what it was, to avoid data mapping errors!
            vdat.filterData(df);
        }

        else if (es == openItem) {
            String fName = openData(VARIANT_FILE);

            if (fName != null) {
                customQueryParent.dispose();
                dispose();
                frameInit();
                redrawOutTable(fName);
                initTable();
            }
        }

        else if (es == saveAsItem) {
            saveData(null, vdat);
        }

        else if (es == saveViewItem) {
            VarData subVdat = vdat.returnSubVarData(vdat, null);
            saveData(null, subVdat);
        }

        else if (es == preferViewItem) {
            preferViewParent.setVisible(true);
        }

        else if (es == prefApply) {
            try {
                int mM = Integer.parseInt(minMPGField.getText());
                float mMCR = Float.parseFloat(minMPGCovRatioField.getText());
                int gST = Integer.parseInt(genScoreThreshField.getText());
                float gSCRT = Float.parseFloat(genScoreCovRatioThreshField.getText());

                minMPG = mM;
                minMPGCovRatio = mMCR;
                genScoreThresh = gST;
                genScoreCovRatioThresh = gSCRT;
                sampleTable.setDefaultRenderer(Object.class, new SampleScoreRenderer(genScoreCovRatioThresh));
                sampleTable.setDefaultRenderer(Number.class, new SampleScoreRenderer(genScoreCovRatioThresh));
                sampleTable.setDefaultRenderer(Float.class, new SampleScoreRenderer(genScoreCovRatioThresh));

            }
            catch (NumberFormatException nfe) {
                showError("<html>You have entered an inappropriate number!<p>Please use an integer for Genotype Score " 
                          + "and a floating point number for Score / Coverage Ratio.</html>");
                System.out.println(nfe);
            }
        }

        else if (es == exitItem) {
            processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }

        else if (es == compHetViewItem) {
            compHetParent.setVisible(true);
        }

        else if (es == customQueryViewItem) {
            customQueryParent.setVisible(true);
        }

        else if (es == compHetGeneViewButton) {
            String geneRegex = new String("");
            int indexIndex = ((Integer)typeMap.get("Index")).intValue();
            int mendHetRecIndex = ((Integer)typeMap.get("MendHetRec")).intValue();
            
            int dataRowIndex = sorter.modelIndex(outTable.getSelectedRow());
            String geneName = vdat.returnDataValueAt(dataRowIndex, "Gene_name");
            if (isShowVar) {
                geneRegex = "^" + geneName + "$";
            }
            else {
                try {
                    geneRegex = "^" + (String)outTable.getValueAt(outTable.getSelectedRow(), 0) + "$";
                }
                catch (ClassCastException cce) {
                    showError(geneNotFoundError);
                    return;
                }
            }
            BitSet[] tempBS = { mask[0], (BitSet)(mask[1].clone()) };
            tempBS[1].set(MENDHETREC);
            vdat.filterData(new DataFilter(tempBS, geneFile, bedFile, spinnerData, geneRegex, minMPG, minMPGCovRatio, genScoreThresh));
            int temp[][] = vdat.returnData();

            //Must return the filtered state to what it was, to avoid data mapping errors!
            vdat.filterData(df);

            if (temp.length > 0) {
                String[] index = new String[temp.length];
                for (int i=0; i<temp.length; i++) {
                    index[i] = annotMapper[indexIndex].getString(temp[i][indexIndex]) + "," 
                        + annotMapper[mendHetRecIndex].getString(temp[i][mendHetRecIndex]);
                }
                CompHetView c = new CompHetView(index, vdat, compHetSamples.isSelected());
            }
            else {
                showError("Gene does not have any compound heterozygous positions.");
            }
        }

        else if (es == compHetPairViewButton) {
            if (isShowVar) {
                int dataRowIndex = sorter.modelIndex(outTable.getSelectedRow());
                String mhrString = vdat.returnDataValueAt(dataRowIndex, "MendHetRec");
                if (mhrString.equals("0,")) {
                    showError("This variant is not in a compound heterozygous pair.");
                }
                else {
                    String index = vdat.returnDataValueAt(dataRowIndex, "Index");
                    index += ("," + mhrString);
                    CompHetView c = new CompHetView(index, vdat, compHetSamples.isSelected());
                }
            }
            else {
                showError("Not available for \"Show Gene\" view - please re-filter with \"Show Variants\"");
            }

        }

        else if (es == compHetAllButton) {
            int indexIndex = ((Integer)typeMap.get("Index")).intValue();
            int mendHetRecIndex = ((Integer)typeMap.get("MendHetRec")).intValue();
            String geneRegex = (getRegex() == null) ? "." : getRegex();
            BitSet[] tempBS = { mask[0], (BitSet)(mask[1].clone()) };
            tempBS[1].set(MENDHETREC);
            vdat.filterData(new DataFilter(tempBS, geneFile, bedFile, spinnerData, geneRegex, minMPG, minMPGCovRatio, genScoreThresh));
            int temp[][] = vdat.returnData();

            //Must return the filtered state to what it was, to avoid data mapping errors!
            vdat.filterData(df);

            if (temp.length > 0) {
                String[] index = new String[temp.length];
                for (int i=0; i<temp.length; i++) {
                    index[i] = annotMapper[indexIndex].getString(temp[i][indexIndex]) + "," 
                        + annotMapper[mendHetRecIndex].getString(temp[i][mendHetRecIndex]);
                }
                CompHetView c = new CompHetView(index, vdat, compHetSamples.isSelected());
            }
            else {
                showError("No compound heterozygous positions. Check your filters!");
            }
        }
        
        else if (es == aboutItem) {
            JTextArea tPane = new JTextArea("VarSifter v" + version + "\n" +
                "Jamie K. Teer, 2010\n\n" + govWork + "\n" + id +
                "\n\n--------------------------------------------------------" +
                "\n\nThis program uses the JTable sorting class TableSorter.java from Sun\n" +
                "and must include the following copyright notification:\n\n" +
                sunCopyright + "\n" + sunDisclaimer);

            JScrollPane sPane = new JScrollPane(tPane);
            sPane.setPreferredSize(new Dimension(600,600));
            JOptionPane.showMessageDialog(null, sPane, "About VarSifter", JOptionPane.PLAIN_MESSAGE);
        }

        else if (es == check) {
            int row = outTable.getSelectedRow();
            int col = outTable.getSelectedColumn();
            int sRow = sampleTable.getSelectedRow();
            int sCol = sampleTable.getSelectedColumn();
            System.out.println("Ann: " + row + "\t" + col + "\t" + outTable.getValueAt(row,col).getClass() +
                "\t" + outTable.getColumnClass(col));
            System.out.println("Sample: " + sRow + "\t" + sCol + "\t" + sampleTable.getValueAt(sRow,sCol).getClass() + 
                "\t" + sampleTable.getColumnClass(sCol));

        }
    }
    

    /** 
    *   Handle List Actions
    *  
    *  @param e - changing the choice on the top table
    */
    public void valueChanged(ListSelectionEvent e) {
        ListSelectionModel lsme = (ListSelectionModel)e.getSource();

        //May remove this test if its the only List Action
        if ( e.getSource() == lsm && lsme.getValueIsAdjusting() == false) {
            int rowIndex = outTable.getSelectedRow();
            if (rowIndex < 0) {
                rowIndex = 0;
            }
            int dataIndex = sorter.modelIndex(rowIndex);
            sampleSorter.setTableHeader(null); //Do this to avoid a memory leak

            if (isShowVar) {
                sampleSorter = new TableSorter((new SampleTableModel(vdat.returnSample(dataIndex),
                    sampleTableLabels, vdat )));
                sampleTable.setModel(sampleSorter);
                sampleSorter.setTableHeader(sampleTable.getTableHeader());
            }
            else {
                sampleSorter = new TableSorter((new SampleTableModel(new int[0][], null, vdat)));
                sampleTable.setModel(sampleSorter);
            }
            
            outTable.setRowSelectionInterval(rowIndex,rowIndex);
            initColSizes(sampleTable, (SampleTableModel)((TableSorter)sampleTable.getModel()).getTableModel() );
        }
    }


    /** 
    *   Handle Table Actions
    *  
    *  @param e (changing a value in the table)
    */
    public void tableChanged(TableModelEvent e) {

        if (e.getSource().getClass() == VarTableModel.class) {
            int row = e.getFirstRow();
            int lastRow = e.getLastRow();
            int col = e.getColumn();
            VarTableModel model = (VarTableModel)e.getSource();
            Object data = model.getValueAt(row, col);
            vdat.setData(row, col, (String)data);
        }
    }


    /** 
    *   Determine which boxes are checked
    *  
    *   @return Array of BitSets describing which boxes are checked
    */
    private BitSet[] getFilterMask() {
        BitSet[] m = new BitSet[2];
        m[0] = new BitSet(typeCBox.length); 
        for (int i=0; i < typeCBox.length; i++) {
            if (typeCBox[i].isSelected()) {
                m[0].set(i);
            }
        }
        
        m[1] = new BitSet(cBox.length);
        for (int i=0; i < cBox.length; i++) {
            if (cBox[i].isSelected()) {
                m[1].set(i);
            }
        }
        return m;
    }



    /** 
    *   Sets fitted column sizes
    *  
    *   @param table the JTable holding the data
    *   @param model the VarTableModel holding the data
    */
    public void initColSizes(JTable table, VarTableModel model) {
        
        TableColumn col = null;
        Component comp = null;
        int headerWidth = 0;
        int cellWidth = 0;
        Object[] largestInCol = model.getLargestInColumn();
        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();

        for (int i=0; i < largestInCol.length; i++) {
            col = table.getColumnModel().getColumn(i);

            comp = headerRenderer.getTableCellRendererComponent(
                table, col.getHeaderValue(), false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;
            if (headerWidth > MAX_COLUMN_SIZE) {
                headerWidth = MAX_COLUMN_SIZE;
            }

            comp = table.getDefaultRenderer(model.getColumnClass(i)).getTableCellRendererComponent(
                table, largestInCol[i], false, false, 0, i);
            cellWidth = comp.getPreferredSize().width;
            if (cellWidth > MAX_COLUMN_SIZE) {
                cellWidth = MAX_COLUMN_SIZE;
            }

            col.setPreferredWidth( (Math.max(headerWidth, cellWidth) + 25) );
        }
    }


    /** 
    *   Initialize Table
    *  
    */
    private void initTable() {
        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout());
        pane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        //Menu
        JMenuBar mBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu viewMenu = new JMenu("View");
        JMenu helpMenu = new JMenu("Help");
        openItem = new JMenuItem("Open File");
        saveAsItem = new JMenuItem("Save File As");
        saveViewItem = new JMenuItem("Save Current View As");
        preferViewItem = new JMenuItem("Preferences");
        exitItem = new JMenuItem("Exit");
        compHetViewItem = new JMenuItem("Viewing Compound Hets");
        customQueryViewItem = new JMenuItem("Custom Query");
        aboutItem = new JMenuItem("About VarSifter");
        fileMenu.add(openItem);
        fileMenu.add(saveAsItem);
        fileMenu.add(saveViewItem);
        fileMenu.add(preferViewItem);
        fileMenu.add(exitItem);
        viewMenu.add(compHetViewItem);
        viewMenu.add(customQueryViewItem);
        helpMenu.add(aboutItem);
        mBar.add(fileMenu);
        mBar.add(viewMenu);
        mBar.add(helpMenu);
        setJMenuBar(mBar);

        drawMinAffSpinner();
        drawCaseControlSpinner();
        drawMinMPGSampleSpinner();
        drawMinMPGCovRatioSampleSpinner();
        
        outTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        outTable.setRowSelectionInterval(0,0);
        outTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        DefaultTableCellRenderer tcr = new DefaultTableCellRenderer();
        tcr.setHorizontalAlignment(SwingConstants.CENTER);
        outTable.setDefaultRenderer(String.class, tcr);
        outTable.setDefaultRenderer(Float.class, new FloatScoreRenderer(6));
        lsm = outTable.getSelectionModel();
        lsm.addListSelectionListener(this);
        
        dataScroller = new JScrollPane(outTable,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dataScroller.setPreferredSize(new Dimension((w/2), (h/4)));
        
        sampleSorter = new TableSorter( new SampleTableModel(vdat.returnSample(outTable.getSelectedRow()),
            sampleTableLabels, vdat ));
        sampleTable = new JTable(sampleSorter);
        sampleSorter.setTableHeader(sampleTable.getTableHeader());
        sampleTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        sampleScroller = new JScrollPane(sampleTable,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sampleScroller.setPreferredSize(new Dimension((w/2), 80));
        sampleScroller.setAlignmentY(Component.TOP_ALIGNMENT);
        sampleTable.setDefaultRenderer(Object.class, new SampleScoreRenderer(genScoreCovRatioThresh));
        sampleTable.setDefaultRenderer(Number.class, new SampleScoreRenderer(genScoreCovRatioThresh));
        sampleTable.setDefaultRenderer(Float.class, new SampleScoreRenderer(genScoreCovRatioThresh));
        initColSizes(sampleTable, (SampleTableModel)((TableSorter)sampleTable.getModel()).getTableModel() );
        
        //Sample display
        JPanel samplePane = new JPanel();
        samplePane.setLayout(new BoxLayout(samplePane, BoxLayout.X_AXIS));
        samplePane.add(sampleScroller);
        
        //Filters
        JPanel filtPane = new JPanel();
        filtPane.setLayout(new BoxLayout(filtPane, BoxLayout.Y_AXIS));
        JPanel includePane = new JPanel();
        includePane.setLayout(new BoxLayout(includePane, BoxLayout.Y_AXIS));
        includePane.setBorder(BorderFactory.createLineBorder(Color.black));

        for (JCheckBox cb : typeCBox) {
            includePane.add(cb);
        }
        JPanel excludePane = new JPanel();
        excludePane.setLayout(new BoxLayout(excludePane, BoxLayout.Y_AXIS));
        excludePane.setBorder(BorderFactory.createLineBorder(Color.black));
        excludePane.add(dbsnp);
        excludePane.add(notFilterFile);
        JPanel sampleFiltPane = new JPanel();
        sampleFiltPane.setLayout(new BoxLayout(sampleFiltPane, BoxLayout.Y_AXIS));
        sampleFiltPane.setBorder(BorderFactory.createLineBorder(Color.black));
        sampleFiltPane.add(mendRec);
        sampleFiltPane.add(mendDom);
        sampleFiltPane.add(mendBad);
        sampleFiltPane.add(mendCompHet);
        sampleFiltPane.add(filterFile);
        sampleFiltPane.add(bedFilterFile);
        sampleFiltPane.add(uniqInAff);
        JPanel affSpinnerPane = new JPanel();
        affSpinnerPane.setLayout(new BoxLayout(affSpinnerPane, BoxLayout.X_AXIS));
        affSpinnerPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        affSpinnerPane.add(Box.createRigidArea(new Dimension(15,0)));
        affSpinnerPane.add(affSpinnerLabel);
        affSpinnerPane.add(minAffSpinner);
        sampleFiltPane.add(affSpinnerPane);
        
        JPanel casePane = new JPanel();
        casePane.setLayout(new BoxLayout(casePane, BoxLayout.X_AXIS));
        casePane.setAlignmentX(Component.LEFT_ALIGNMENT);
        casePane.add(Box.createRigidArea(new Dimension(15,0)));
        casePane.add(caseSpinnerLabel);
        casePane.add(caseSpinner);
        JPanel controlPane = new JPanel();
        controlPane.setLayout(new BoxLayout(controlPane, BoxLayout.X_AXIS));
        controlPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlPane.add(Box.createRigidArea(new Dimension(15,0)));
        controlPane.add(controlSpinnerLabel);
        controlPane.add(controlSpinner);
        sampleFiltPane.add(caseControl);
        sampleFiltPane.add(casePane);
        sampleFiltPane.add(controlPane);

        JPanel regexPane = new JPanel();
        regexPane.setLayout(new BoxLayout(regexPane, BoxLayout.Y_AXIS));
        regexPane.add(new JLabel("Search gene names for:"));
        geneRegexField.setMaximumSize( new Dimension( (int)geneRegexField.getMaximumSize().getWidth(), 
            (int)geneRegexField.getMinimumSize().getHeight()));
        regexPane.add(geneRegexField);
        
        JPanel selClearPane = new JPanel();
        selClearPane.setLayout(new BoxLayout(selClearPane, BoxLayout.X_AXIS));
        selClearPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        selClearPane.add(selectAll);
        selClearPane.add(Box.createRigidArea(new Dimension(5,0)));
        selClearPane.add(clear);
        JPanel fFiltPane = new JPanel();
        fFiltPane.setLayout(new BoxLayout(fFiltPane, BoxLayout.Y_AXIS));
        fFiltPane.add(filterFileLabel);
        fFiltPane.add(filterFileButton);

        JPanel bFiltPane = new JPanel();
        bFiltPane.setLayout(new BoxLayout(bFiltPane, BoxLayout.Y_AXIS));
        bFiltPane.add(bedFilterFileLabel);
        bFiltPane.add(bedFilterFileButton);
        
        JPanel showPane = new JPanel();
        showPane.setLayout(new BoxLayout(showPane, BoxLayout.X_AXIS));
        showPane.setBorder(BorderFactory.createLineBorder(Color.black));
        showPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        ButtonGroup showGroup = new ButtonGroup();
        showGroup.add(showVar);
        showGroup.add(showGene);
        showPane.add(showVar);
        showPane.add(showGene);

        
        filtPane.add(new JLabel("Include:"));
        filtPane.add(includePane);
        filtPane.add(Box.createRigidArea(new Dimension(0,15)));
        filtPane.add(new JLabel("Exclude:"));
        filtPane.add(excludePane);
        filtPane.add(Box.createRigidArea(new Dimension(0,15)));
        filtPane.add(new JLabel("Include:"));
        filtPane.add(sampleFiltPane);
        filtPane.add(Box.createRigidArea(new Dimension(0,15)));
        filtPane.add(regexPane);
        filtPane.add(Box.createRigidArea(new Dimension(0,15)));
        filtPane.add(showPane);
        filtPane.add(Box.createRigidArea(new Dimension(0,15)));
        filtPane.add(selClearPane);
        filtPane.add(Box.createRigidArea(new Dimension(0,10)));
        apply.setMnemonic(KeyEvent.VK_F);
        filtPane.add(apply);
        filtPane.add(Box.createRigidArea(new Dimension(0,15)));
        filtPane.add(Box.createVerticalGlue());
        filtPane.add(fFiltPane);
        filtPane.add(Box.createRigidArea(new Dimension(0,10)));
        filtPane.add(bFiltPane);
        filtPane.add(Box.createRigidArea(new Dimension(0,10)));
        filtPane.add(geneViewButton);
        //filtPane.add(Box.createRigidArea(new Dimension(0,10)));
        //filtPane.add(check);  //TESTING
        JScrollPane filtScroll = new JScrollPane(filtPane, 
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        filtScroll.setBorder(null);

        //Stats (line count)
        JPanel stats = new JPanel();
        stats.add(linesl);
        stats.add(lines);


        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.setPreferredSize(new Dimension((w/2), (h/3)));
        tablePanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        tablePanel.add(dataScroller);
        tablePanel.add(Box.createRigidArea(new Dimension(0,15)));
        tablePanel.add(samplePane);
        tablePanel.add(stats);

        //Icons
        setIconImage(createImageIcon("images/vs.png", "VarSifter").getImage());
        apply.setIcon(createImageIcon("images/sift_a.png", "Sift variants"));

        //Listener Registration
        apply.addActionListener(this);
        selectAll.addActionListener(this);
        clear.addActionListener(this);
        openItem.addActionListener(this);
        saveAsItem.addActionListener(this);
        saveViewItem.addActionListener(this);
        preferViewItem.addActionListener(this);
        prefApply.addActionListener(this);
        compHetViewItem.addActionListener(this);
        customQueryViewItem.addActionListener(this);
        exitItem.addActionListener(this);
        aboutItem.addActionListener(this);
        check.addActionListener(this);
        filterFileButton.addActionListener(this);
        bedFilterFileButton.addActionListener(this);
        geneViewButton.addActionListener(this);
        geneRegexField.addActionListener(this);

        //Disable unused buttons
        filterFile.setEnabled(false);
        notFilterFile.setEnabled(false);
        bedFilterFile.setEnabled(false);
        selectAll.setVisible(false);    //Should eventually remove this
        if (vdat.returnParent() != null) {
            geneViewButton.setVisible(false);
        }
        compHetGeneViewButton.setEnabled(false);
        maskCBox();
                
        pane.add(tablePanel, BorderLayout.CENTER);
        pane.add(filtScroll, BorderLayout.LINE_END);
        add(pane);
        pack();
        setVisible(true);


        //Initialize (but don't display) compHetPane
        JPanel compHetPane = new JPanel();
        compHetPane.setLayout(new BoxLayout(compHetPane, BoxLayout.Y_AXIS));
        compHetPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.Y_AXIS));
        buttonPane.add(new JLabel("Choose Compound Het View:"));
        buttonPane.add(Box.createRigidArea(new Dimension(0,15)));
        buttonPane.add(compHetSamples);
        buttonPane.add(Box.createRigidArea(new Dimension(0,10)));
        buttonPane.add(compHetGeneViewButton);
        buttonPane.add(Box.createRigidArea(new Dimension(0,10)));
        buttonPane.add(compHetPairViewButton);
        buttonPane.add(Box.createRigidArea(new Dimension(0,10)));
        buttonPane.add(compHetAllButton);

        compHetGeneViewButton.addActionListener(this);
        compHetPairViewButton.addActionListener(this);
        compHetAllButton.addActionListener(this);

        compHetPane.add(buttonPane);
        compHetParent = new JFrame("Compound Het View");
        compHetParent.add(compHetPane);
        compHetParent.pack();
        //compHetParent.setVisible(true);

        //Initialize (but don't display) preferViewPane
        JPanel preferViewPane = new JPanel();
        preferViewParent = new JFrame("Preferences");
        preferViewPane.setLayout(new BoxLayout(preferViewPane, BoxLayout.Y_AXIS));
        preferViewPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        setJComponentSize(minMPGField);
        setJComponentSize(minMPGCovRatioField);
        setJComponentSize(genScoreThreshField);
        setJComponentSize(genScoreCovRatioThreshField);
        minMPGField.setText(Integer.toString(minMPG));
        minMPGCovRatioField.setText(Float.toString(minMPGCovRatio));
        genScoreThreshField.setText(Integer.toString(genScoreThresh));
        genScoreCovRatioThreshField.setText(Float.toString(genScoreCovRatioThresh));
        JPanel minMPGPane = new JPanel();
        minMPGPane.setLayout(new BoxLayout(minMPGPane, BoxLayout.X_AXIS));
        minMPGPane.add(new JLabel("Minimum Genotype Score: "));
        minMPGPane.add(minMPGField);
        minMPGPane.add(new JLabel(" in at least "));
        minMPGPane.add(minMPGSpinner);
        minMPGPane.add(new JLabel(" samples."));
        JPanel minMPGCovRatioPane = new JPanel();
        minMPGCovRatioPane.setLayout(new BoxLayout(minMPGCovRatioPane, BoxLayout.X_AXIS));
        minMPGCovRatioPane.add(new JLabel("Minimum (Genotype Score / coverage) ratio: "));
        minMPGCovRatioPane.add(minMPGCovRatioField);
        minMPGCovRatioPane.add(new JLabel(" in at least "));
        minMPGCovRatioPane.add(minMPGCovRatioSpinner);
        minMPGCovRatioPane.add(new JLabel(" samples."));

        JPanel preferFilterPane = new JPanel();
        preferFilterPane.setLayout(new BoxLayout(preferFilterPane, BoxLayout.Y_AXIS));
        preferFilterPane.setBorder(BorderFactory.createLineBorder(Color.black));
        preferFilterPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        preferFilterPane.add(Box.createRigidArea(new Dimension(0,15)));
        preferFilterPane.add(minMPGPane);
        preferFilterPane.add(Box.createRigidArea(new Dimension(0,10)));
        preferFilterPane.add(minMPGCovRatioPane);
        preferFilterPane.add(Box.createRigidArea(new Dimension(0,10)));

        JPanel preferSettingsPane = new JPanel();
        preferSettingsPane.setLayout(new BoxLayout(preferSettingsPane, BoxLayout.Y_AXIS));
        preferSettingsPane.setBorder(BorderFactory.createLineBorder(Color.black));
        preferSettingsPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        preferSettingsPane.add(Box.createRigidArea(new Dimension(0,15)));
        preferSettingsPane.add(new JLabel("Genotype Score Threshold for Aff/Norm, Case/Control filters:"));
        preferSettingsPane.add(genScoreThreshField);
        preferSettingsPane.add(Box.createRigidArea(new Dimension(0,10)));
        preferSettingsPane.add(new JLabel("Genotype Score / Coverage Ratio Threshold for low-lighting:"));
        preferSettingsPane.add(genScoreCovRatioThreshField);
        preferSettingsPane.add(Box.createRigidArea(new Dimension(0,10)));

        preferViewPane.add(new JLabel("Choose minimum scores for general filtering:"));
        preferViewPane.add(preferFilterPane);
        preferViewPane.add(Box.createRigidArea(new Dimension(0,15)));
        preferViewPane.add(new JLabel("Choose minimum scores for certain filters and for lowlighting:"));
        preferViewPane.add(preferSettingsPane);
        preferViewPane.add(Box.createRigidArea(new Dimension(0,15)));
        preferViewPane.add(prefApply);

        preferViewParent.add(preferViewPane);
        preferViewParent.pack();
        //preferViewParent.setVisible(true);
                
        initCustomQueryParent();

    }



    /** 
    *   Redraw Table
    *  
    *   @param newData VS file
    */
    private void redrawOutTable(String newData) {
        
        if (newData != null) {
            vdat = getNewVarData(newData);
            typeMap = null;
        }

        if (typeMap == null) {
            typeMap = vdat.returnDataTypeAt();
            annotMapper = vdat.returnAnnotMap();
            sampleMapper = vdat.returnSampleMap();

            String[] types = annotMapper[typeMap.get("type")].getSortedEntries();
            typeCBox = new JCheckBox[types.length];
            for (int i=0; i<types.length; i++) {
                typeCBox[i] = new JCheckBox(types[i]);
            }

            maskCBox();
            mask = getFilterMask();
            clear.doClick();
            drawMinAffSpinner();
            drawCaseControlSpinner();
            drawMinMPGSampleSpinner();
            drawMinMPGCovRatioSampleSpinner();
            df = new DataFilter(mask, geneFile, bedFile, spinnerData, getRegex(), minMPG, minMPGCovRatio, genScoreThresh);
        }

        if (sorter != null) {
            sorter.setTableHeader(null);  //Must do this to avoid memory leak
        }
        int geneNameIndex = ((Integer)typeMap.get("Gene_name")).intValue();
        if (isShowVar) {
            sorter = new TableSorter( new VarTableModel(vdat.returnData(),
                                          vdat.returnDataNames(), 
                                          annotMapper,
                                          vdat ));
        }
        else {
            AbstractMapper[] geneMap = { annotMapper[geneNameIndex], new IntMapper() };
            sorter = new TableSorter( new VarTableModel(vdat.returnGeneData(),
                                          vdat.returnGeneNames(), 
                                          geneMap,
                                          vdat ));
        }
        //TableListener to detect data changes added in redrawOutTable(String)
        ((VarTableModel)sorter.getTableModel()).addTableModelListener(this);
        outTable.setModel(sorter);
        sorter.setTableHeader(outTable.getTableHeader());
        initColSizes(outTable, (VarTableModel)((TableSorter)outTable.getModel()).getTableModel() );
        lines.setText(Integer.toString(outTable.getRowCount()));
        outTable.requestFocusInWindow();

    }


    /**
    *   Initialize the Custom Query Window
    */
    private void initCustomQueryParent() {
        //Initialize (but don't display) customQueryParent
        try {
            customQueryParent = new JFrame("Custom Query");
            cqPane = new CustomQueryView(vdat);
            JPanel customQueryPane = new JPanel();
            customQueryPane.setPreferredSize(new Dimension(w/2, h/2));
            customQueryPane.setLayout(new BoxLayout(customQueryPane, BoxLayout.Y_AXIS));
            customQueryPane.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
            customQueryPane.add(cqPane);
            customQueryPane.add(Box.createRigidArea(new Dimension(0,10)));
            customQueryPane.add(customQuery);
            customQueryParent.add(customQueryPane);
            customQueryParent.pack();
            //customQueryParent.setVisible(true);
        }
        catch (NoClassDefFoundError e) {
            showError("<html>Required JUNG files are not in the same directory as the VarSifter.jar file."
                + "<p>Disabling custom querying.");
            customQueryViewItem.setEnabled(false);
            customQuery.setEnabled(false);
            e.printStackTrace();
        }
    }


    /** 
    *   Disable unused CheckBoxes
    *  
    */
    private void maskCBox() {
        if (typeMap.containsKey("dbID")) {
            dbsnp.setEnabled(true);
        }
        else {
            dbsnp.setEnabled(false);
        }

        if (typeMap.containsKey("MendHomRec")) {
            mendRec.setEnabled(true);
        }
        else {
            mendRec.setEnabled(false);
        }

        if (typeMap.containsKey("MendHetRec")) {
            mendCompHet.setEnabled(true);
            compHetGeneViewButton.setEnabled(true);
            compHetPairViewButton.setEnabled(true);
            compHetAllButton.setEnabled(true);
        }
        else {
            mendCompHet.setEnabled(false);
            compHetGeneViewButton.setEnabled(false);
            compHetPairViewButton.setEnabled(false);
            compHetAllButton.setEnabled(false);
        }
        
        if (typeMap.containsKey("MendDom")) {
            mendDom.setEnabled(true);
        }
        else {
            mendDom.setEnabled(false);
        }
        
        if (typeMap.containsKey("MendInconsis")) {
            mendBad.setEnabled(true);
        }
        else {
            mendBad.setEnabled(false);
        }

        
        if (vdat.countSampleType(vdat.AFF_NORM_PAIR) == 0) {
            uniqInAff.setEnabled(false);
            minAffSpinner.setEnabled(false);
            affSpinnerLabel.setEnabled(false);
        }
        else {
            uniqInAff.setEnabled(true);
            minAffSpinner.setEnabled(true);
            affSpinnerLabel.setEnabled(true);
        }

        if (vdat.countSampleType(vdat.CASE) == 0) {
            caseControl.setEnabled(false);
            caseSpinner.setEnabled(false);
        }
        else {
            caseControl.setEnabled(true);
            caseSpinner.setEnabled(true);
        }
        if (vdat.countSampleType(vdat.CONTROL) == 0) {
            caseControl.setEnabled(false);
            controlSpinner.setEnabled(false);
        }
        else {
            caseControl.setEnabled(true);
            controlSpinner.setEnabled(true);
        }

    }      

    /** 
    *   get regex or null if nothing
    *  
    */
    private String getRegex() {
        if (geneRegexField.getText().equals("")) {
            return null;
        }
        else {
            return geneRegexField.getText();
        }
    }


    /**
    *   Determine what type of file to interpret, call the correct VarData object
    */
    private VarData getNewVarData(String in) {
        VarData v = null;
        if (vcfPat.matcher(in).find()) {
            v = new VCFVarData(in);
        }
        else if (gzPat.matcher(in).find()) {
            VarSifter.showError("Cannot read .gz files.  Please uncompress and load the uncompressed file.");
            System.exit(1);
        }
        else {
            v = new VarData(in);
        }
        return v;
    }


    /** 
    *   initialize  minAffSpinner
    *  
    */    
    private void drawMinAffSpinner() {
        if (vdat.countSampleType(vdat.AFF_NORM_PAIR) > 0) {
            minAffSpinner.setModel(new SpinnerNumberModel(1, 1, vdat.countSampleType(vdat.AFF_NORM_PAIR), 1));
        }
        setJComponentSize(minAffSpinner);
        spinnerData[vdat.AFF_NORM_PAIR] = ((Integer)minAffSpinner.getValue()).intValue();
    }


    /** 
    *   initialize case, control spinners
    *  
    */
    private void drawCaseControlSpinner() {
        int caseCount = vdat.countSampleType(vdat.CASE);
        int controlCount = vdat.countSampleType(vdat.CONTROL);
        if (caseCount > 0) {
            caseSpinner.setModel(new SpinnerNumberModel(1, 0, caseCount, 1));
        }
        if (controlCount > 0) {
            controlSpinner.setModel(new SpinnerNumberModel(0, 0, controlCount, 1));
        }
        setJComponentSize(caseSpinner);
        setJComponentSize(controlSpinner);
        spinnerData[vdat.CASE] = ((Integer)caseSpinner.getValue()).intValue();
        spinnerData[vdat.CONTROL] = ((Integer)controlSpinner.getValue()).intValue();
    }


    /**
    *   Initialize minMPG sample Spinner
    */
    private void drawMinMPGSampleSpinner() {
        int count = vdat.countSampleType(vdat.MIN_MPG);
        if (count > 0) {
            minMPGSpinner.setModel(new SpinnerNumberModel(0, 0, count, 1));
        }
        setJComponentSize(minMPGSpinner);
        spinnerData[vdat.MIN_MPG] = ((Integer)minMPGSpinner.getValue()).intValue();
    }


    /**
    *   Initialize minMPGCovRatio sample Spinner
    */
    private void drawMinMPGCovRatioSampleSpinner() {
        int count = vdat.countSampleType(vdat.MIN_MPG_COV);
        if (count > 0) {
            minMPGCovRatioSpinner.setModel(new SpinnerNumberModel(0, 0, count, 1));
        }
        setJComponentSize(minMPGCovRatioSpinner);
        spinnerData[vdat.MIN_MPG_COV] = ((Integer)minMPGCovRatioSpinner.getValue()).intValue();
    }


    /** 
    *   Create an ImageIcon from a path relative to this class.
    *   @param path Relative path of image file.
    *   @param desc Assistive tech description
    *   @return ImageIcon or null if path invalid.
    */
    private ImageIcon createImageIcon(String path, String desc) {
        java.net.URL url = getClass().getResource(path);
        if (url != null) {
            return new ImageIcon(url, desc);
        }
        else {
            showError("Couldn't find icon path: " + path);
            return null;
        }
    }


    /** 
    *   Display error as dialog
    *  
    *   @param err An error message to display as a MessageDialog
    */
    public static void showError(String err) {
        JOptionPane.showMessageDialog(null, err, "Error!", JOptionPane.ERROR_MESSAGE);
    }


    /**
    *   Set Component size
    *   @param comp Component for which to modify size
    */
    private void setJComponentSize(JComponent comp) {
        Dimension d = comp.getPreferredSize();
        d.width = 60;
        comp.setPreferredSize(d);
        comp.setMaximumSize(comp.getPreferredSize());
    }


    /** 
    *   Open data
    *  
    *   @param openType Type of file to open
    *   @return Absolute path to file
    */
    private String openData(int openType) {
        File fcFile;
        String fileName;
        String[] dTitle = { "Open variant list",
                            "Open gene filter list",
                            "Open bed filter list"
                          };
        
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setDialogTitle(dTitle[openType]);
        int fcReturnVal = fc.showOpenDialog(VarSifter.this);
        if (fcReturnVal == JFileChooser.APPROVE_OPTION) {
            fcFile = fc.getSelectedFile();
            fileName = fcFile.getAbsolutePath();
        }
        else {
            System.out.println("No File opened.");

            if (vdat == null) {
                showError("This program really only works if you open a file. Exiting.");
                System.out.println("This program really only works if you open a file. Exiting.");
                System.exit(0);
            }
            return null;
        }

        System.out.println(fileName);
        if (openType == VARIANT_FILE) {
            VarSifter.this.setTitle("VarSifter - " + fileName);
        }
        else if (openType == GENE_FILTER_FILE) {
            filterFileLabel.setText(fileName);
            filterFile.setEnabled(true);
            notFilterFile.setEnabled(true);
        }
        else if (openType == BED_FILTER_FILE) {
            bedFilterFileLabel.setText(fileName);
            bedFilterFile.setEnabled(true);
        }
        return fileName;
    }

    
    /** 
    *   Save data, either in place, or as new
    *  
    *   @param fileName Name of file to save to or null to open a FileChooser
    *   @param vdTemp The VarData object containing data to save
    */
    private void saveData(String fileName, VarData vdTemp) {
        File fcFile;
        int ovwResult = JOptionPane.YES_OPTION;
        
        if (fileName == null) {
            JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
            fc.setDialogTitle("Save File As");
            int fcReturnVal = fc.showSaveDialog(VarSifter.this);
            if (fcReturnVal == JFileChooser.APPROVE_OPTION) {
                fcFile = fc.getSelectedFile();
                fileName = fcFile.getAbsolutePath();
            }
            else {
                System.out.println("File NOT SAVED!!!");
                return;
            }
        }
        else {
            fcFile = new File(System.getProperty("user.dir") + fileName);
            fileName = fcFile.getAbsolutePath();
        }
        
        if (fcFile.exists()) {
            ovwResult = JOptionPane.showConfirmDialog(null, fileName + " already exists.  " +
                "Do you want to overwrite it?", "Overwrite Warning",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        }

        if (ovwResult == JOptionPane.YES_OPTION) {
            try {
                
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fcFile)));
                int[][] outData = vdTemp.dataDump();
                String[] dataNames = vdTemp.returnDataNames();
                String[] sampleNamesOrig = vdTemp.returnSampleNamesOrig();
                String[] commentList = vdTemp.returnCommentList();

                //comments
                for (String c : commentList) {
                    pw.println(c);
                }

                //header
                StringBuilder outString = new StringBuilder(100);
                for (int j=0; j < dataNames.length; j++) {
                    outString.append(dataNames[j] + "\t");
                }
                for (int j=0; j < sampleNamesOrig.length; j++) {
                    outString.append(sampleNamesOrig[j] + "\t");
                }
                outString.deleteCharAt(outString.length() - 1 );
                pw.println(outString.toString());


                for (int i=0; i < outData.length; i++) {
                    outString = new StringBuilder(100);
                                    
                    for (int j=0; j < dataNames.length; j++) {
                        outString.append(annotMapper[j].getString(outData[i][j]) + "\t"); 
                    }
                    for (int j=dataNames.length; j < outData[i].length; j++) {
                                outString.append(sampleMapper[(j-dataNames.length) % VarData.S_FIELDS].getString(
                                                 outData[i][j]) + "\t"); 
                    }
                    outString.deleteCharAt(outString.length() - 1);

                    pw.println(outString.toString());
                }
                pw.close();
                if (pw.checkError()) {
                    showError("Error Detected writing file! File NOT saved!");
                    System.out.println("Error Detected writing file!");
                }
                else {
                    System.out.println("File " + fileName + " written");
                }
            }
            catch (IOException ioe) {
                JOptionPane.showMessageDialog(null, "File write error: " + ioe.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else {
            System.out.println(fileName + " not overwritten!");
        }

    }

    

    public static void main(final String args[]) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                VarSifter v;
                if (args.length > 0) {
                    v = new VarSifter(args[0]);
                }
                else {
                    v = new VarSifter((String)null);
                }
            }
        });
    }
}
