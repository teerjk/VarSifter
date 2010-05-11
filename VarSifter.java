import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.util.BitSet;
import java.util.ArrayList;
import java.io.*;
import java.util.HashMap;
import components.TableSorter;

/* ****************
*   VarSifter is designed to read a flat file where a row contains a variant, and all sample info
*    and allow sorting, and filtering of the data. This class is a wrapper that handles the GUI
*    via Swing.
*  ****************
*/

public class VarSifter extends JFrame implements ListSelectionListener, ActionListener, TableModelListener {
    
    final String version = "0.4";
    final String id = "$Id$";

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

    final String sunCopyright = "Copyright (c) 1995 - 2008 Sun Microsystems, Inc.  All rights reserved.";
    final String sunDisclaimer = "Redistribution and use in source and binary forms, with or without\n" +
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

    final int VARIANT_FILE = 0;
    final int GENE_FILTER_FILE = 1;
    final int BED_FILTER_FILE = 2;
    final int MAX_COLUMN_SIZE = 150;


    final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    final int w = (int)dim.getWidth();
    final int h = (int)dim.getHeight();

    // Test using object[][], titles[]
    private VarData vdat = null;
    private JTable outTable;
    private TableSorter sorter;
    private JTable sampleTable;
    private JScrollPane dataScroller;
    private JScrollPane sampleScroller;
    private ListSelectionModel lsm;
    private JLabel lines = new JLabel();

    private JCheckBox stop = new JCheckBox("Stop");
    private JCheckBox div = new JCheckBox("DIV");
    private JCheckBox splice = new JCheckBox("Splice-Site");
    private JCheckBox nonsyn = new JCheckBox("Non-synonymous");
    private JCheckBox syn = new JCheckBox("Synonymous");
    private JCheckBox noncod = new JCheckBox("Non-Coding");
    private JCheckBox utr = new JCheckBox("UTR");
    private JCheckBox dbsnp = new JCheckBox("dbSNP130");

    private JCheckBox mendRec = new JCheckBox("Hom. Recessive");
    private JCheckBox mendDom = new JCheckBox("Dominant");
    private JCheckBox mendBad = new JCheckBox("Inconsistent");
    private JCheckBox uniqInAff = new JCheckBox("Tumor different from Norm");
    private JCheckBox filterFile = new JCheckBox("No Gene file selected");
    private JCheckBox bedFilterFile = new JCheckBox("No bed file selected");

    private JCheckBox[] cBox = { stop,
                                 div, 
                                 splice, 
                                 nonsyn, 
                                 syn,
                                 noncod,
                                 utr,
                                 dbsnp, 
                                 mendRec,
                                 mendDom,
                                 mendBad,
                                 uniqInAff,
                                 filterFile,
                                 bedFilterFile
                               };

    private JMenuItem openItem;
    private JMenuItem saveAsItem;
    private JMenuItem saveViewItem;
    private JMenuItem exitItem;
    private JMenuItem aboutItem;

    private JButton apply = new JButton("Apply Filter");
    private JButton selectAll = new JButton("Select All");
    private JButton clear = new JButton("Clear All");
    private JRadioButton showVar = new JRadioButton("Show Variants", true);
    private JRadioButton showGene = new JRadioButton("Show Genes");
    private JButton check = new JButton("Check");
    private JButton filterFileButton = new JButton("Choose Gene File Filter");
    private JButton bedFilterFileButton = new JButton("Choose Bed File Filter");
    private JButton compoundHetButton = new JButton("View Variants for Selected Gene");

    private JSpinner minAffSpinner = new JSpinner();
    private JLabel affSpinnerLabel = new JLabel("Diff. in at least:");
    private JTextField geneRegexField = new JTextField();
    
    final String newLine = System.getProperty("line.separator");
    private String geneFile = null;
    private String bedFile = null;

    private BitSet mask; //store selected filters on "Apply Filter" press
    private boolean isShowVar = true;

    //int lastRow = 0;    //Last row selected

    /* ******************
    *   Initiate GUI, instantiate VarData
    *   Constructor using FileName
    *  ******************
    */
    public VarSifter(String inFile) {
        
        //initiate parent window
        super("VarSifter - " + inFile);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(0, (h/8), (w/2), (h/2));

        if (inFile == null) {
            //vdat = new VarData(openData());
            inFile = openData(VARIANT_FILE);
        }
        else {
            //vdat = new VarData(inFile);
        }
        outTable = new JTable();
        redrawOutTable(inFile);
        initTable();
    }

    /* *****************
    *   Constructor using VarData object
    *  *****************
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
    

    /* *************
    *   Handle Actions
    *  *************
    */
    public void actionPerformed(ActionEvent e) {
        Object es = e.getSource();


        if (es == apply) {
            
            sorter.setTableHeader(null);    //Must do this to avoid memory leak
            
            mask = getFilterMask();

            if (showVar.isSelected()) {
                isShowVar = true;
                compoundHetButton.setEnabled(false);
            }
            else if (showGene.isSelected()) {
                isShowVar = false;
                compoundHetButton.setEnabled(true);
            }
            
            String tempRegex;
            if (geneRegexField.getText().equals("")) {
                tempRegex = null;
            }
            else {
                tempRegex = geneRegexField.getText();;
            }
           
            vdat.filterData(mask, geneFile, bedFile, ((Integer)minAffSpinner.getValue()).intValue(), tempRegex);
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

        else if (es == compoundHetButton) {

            /* ************
            *   Testing only - must replace!!!
            *  ************
            */
            /*
            BitSet test = new BitSet(vdat.returnData().length);
            for (int i=0; i < 5; i++) {
                test.set(i);
            }
            test.set(6);
            VarSifter vs = new VarSifter(vdat.returnSubVarData(vdat, test));
            */

            //Use this button to return a VarSifter view of one gene
            //int l = vdat.dataDump().length - 1;
            String geneRegex = "^" + (String)outTable.getValueAt(outTable.getSelectedRow(), 0) + "$";
            vdat.filterData(new BitSet(), null, null, 0, geneRegex);
                //(String)outTable.getValueAt(outTable.getSelectedRow(), 0));
            VarData tempVdat = vdat.returnSubVarData(vdat, null);
            VarSifter vs = new VarSifter(tempVdat);
            
            //Must return the filtered state to what it was, to avoid data mapping errors!
            vdat.filterData(mask, geneFile, bedFile, ((Integer)minAffSpinner.getValue()).intValue(), null);
        }

        else if (es == openItem) {
            String fName = openData(VARIANT_FILE);

            if (fName != null) {
                redrawOutTable(fName);
            }
        }

        else if (es == saveAsItem) {
            saveData(null, vdat);
        }

        else if (es == saveViewItem) {
            VarData subVdat = vdat.returnSubVarData(vdat, null);
            saveData(null, subVdat);
        }

        else if (es == exitItem) {
            processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
        
        else if (es == aboutItem) {
            JOptionPane.showMessageDialog(null, "VarSifter v" + version + "\n" +
                "Jamie K. Teer, 2010\n\n" + govWork + "\n" + id +
                "\n\n--------------------------------------------------------" +
                "\n\nThis program uses the JTable sorting class TableSorter.java from Sun\n" +
                "and must include the following copyright notification:\n\n" +
                sunCopyright + "\n" + sunDisclaimer, "About VarSifter", JOptionPane.PLAIN_MESSAGE);
        }

        else if (es == check) {
            int row = outTable.getSelectedRow();
            int col = outTable.getSelectedColumn();
            System.out.println(row + "\t" + col + "\t" + outTable.getValueAt(row,col).getClass() +
                "\t" + outTable.getColumnClass(col));
        }
    }
    

    /* *************
    *   Handle List Actions
    *  *************
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

            if (isShowVar) {
                sampleTable.setModel(new VarTableModel(vdat.returnSample(dataIndex),
                    vdat.returnSampleNames(), vdat ));
            }
            else {
                sampleTable.setModel(new VarTableModel(new String[0][], null, vdat));
            }
            
            outTable.setRowSelectionInterval(rowIndex,rowIndex);
            //lastRow = rowIndex;
            initColSizes(sampleTable, (VarTableModel)sampleTable.getModel());
            
            //System.out.println(outTable.getSelectedRow() + "\t" + rowIndex + "\t" + dataIndex);
        }
    }


    /* *************
    *   Handle Table Actions
    *  *************
    */
    public void tableChanged(TableModelEvent e) {

        //System.out.println(e.getSource().toString());
            
        if (e.getSource().getClass() == VarTableModel.class) {
            int row = e.getFirstRow();
            int lastRow = e.getLastRow();
            int col = e.getColumn();
            VarTableModel model = (VarTableModel)e.getSource();
            Object data = model.getValueAt(row, col);
            //System.out.println("Row: " + row + "/" + lastRow + " Col: " + col + " value: " + e.getType() +
            //    " data: "+ data);
            //System.out.println(data.getClass());
            vdat.setData(row, col, (String)data);
        }
    }


    /* *************
    *   Determine which boxes are checked
    *  *************
    */
    private BitSet getFilterMask() {
        BitSet m = new BitSet(cBox.length);
        for (int i=0; i < cBox.length; i++) {
            if (cBox[i].isSelected()) {
                //System.out.println(cBox[i].getText());
                m.set(i);
            }
        }
        return m;
    }



    /* *************
    *   Sets fitted column sizes
    *  *************
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


    /* *************
    *   Initialize Table
    *  *************
    */
    private void initTable() {
        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout());
        pane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        //Menu
        JMenuBar mBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu helpMenu = new JMenu("Help");
        openItem = new JMenuItem("Open File");
        saveAsItem = new JMenuItem("Save File As");
        saveViewItem = new JMenuItem("Save Current View As");
        exitItem = new JMenuItem("Exit");
        aboutItem = new JMenuItem("About VarSifter");
        fileMenu.add(openItem);
        fileMenu.add(saveAsItem);
        fileMenu.add(saveViewItem);
        fileMenu.add(exitItem);
        helpMenu.add(aboutItem);
        mBar.add(fileMenu);
        mBar.add(helpMenu);
        setJMenuBar(mBar);

        drawMinAffSpinner();
        
        //outTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        outTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        outTable.setRowSelectionInterval(0,0);
        outTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        outTable.setDefaultRenderer(Number.class, new VarScoreRenderer());
        lsm = outTable.getSelectionModel();
        lsm.addListSelectionListener(this);
        
        dataScroller = new JScrollPane(outTable,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dataScroller.setPreferredSize(new Dimension((w/2), (h/4)));
        
        sampleTable = new JTable( new VarTableModel(vdat.returnSample(outTable.getSelectedRow()),
            vdat.returnSampleNames(), vdat ));
        sampleTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        sampleScroller = new JScrollPane(sampleTable,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sampleScroller.setPreferredSize(new Dimension((w/2), 80));
        sampleScroller.setAlignmentY(Component.TOP_ALIGNMENT);
        sampleTable.setDefaultRenderer(Object.class, new SampleScoreRenderer());
        initColSizes(sampleTable, (VarTableModel)sampleTable.getModel());
        
        //Sample display
        JPanel samplePane = new JPanel();
        samplePane.setLayout(new BoxLayout(samplePane, BoxLayout.X_AXIS));
        JLabel sL = new JLabel("<html>Genotype<p>MPG Score<p>Coverage</html>");
        sL.setAlignmentY(Component.TOP_ALIGNMENT);
        sL.setPreferredSize(new Dimension(80,80));
        sL.setMaximumSize(new Dimension(80,80));
        samplePane.add(sL);
        samplePane.add(sampleScroller);
        
        //Filters
        JPanel filtPane = new JPanel();
        filtPane.setLayout(new BoxLayout(filtPane, BoxLayout.Y_AXIS));
        JPanel includePane = new JPanel();
        includePane.setLayout(new BoxLayout(includePane, BoxLayout.Y_AXIS));
        includePane.setBorder(BorderFactory.createLineBorder(Color.black));
        includePane.add(stop);
        includePane.add(div);
        includePane.add(splice);
        includePane.add(utr);
        includePane.add(nonsyn);
        includePane.add(syn);
        includePane.add(noncod);
        JPanel excludePane = new JPanel();
        excludePane.setLayout(new BoxLayout(excludePane, BoxLayout.Y_AXIS));
        excludePane.setBorder(BorderFactory.createLineBorder(Color.black));
        excludePane.add(dbsnp);
        JPanel sampleFiltPane = new JPanel();
        sampleFiltPane.setLayout(new BoxLayout(sampleFiltPane, BoxLayout.Y_AXIS));
        sampleFiltPane.setBorder(BorderFactory.createLineBorder(Color.black));
        sampleFiltPane.add(mendRec);
        sampleFiltPane.add(mendDom);
        sampleFiltPane.add(mendBad);
        sampleFiltPane.add(uniqInAff);
        JPanel affSpinnerPane = new JPanel();
        affSpinnerPane.setLayout(new BoxLayout(affSpinnerPane, BoxLayout.X_AXIS));
        affSpinnerPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        //affSpinnerPane.add(Box.createHorizontalGlue());
        affSpinnerPane.add(Box.createRigidArea(new Dimension(15,0)));
        //affSpinnerPane.add(new JLabel("Diff. in at least:"));
        affSpinnerPane.add(affSpinnerLabel);
        affSpinnerPane.add(minAffSpinner);
        sampleFiltPane.add(affSpinnerPane);

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
        //fFiltPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        fFiltPane.add(filterFile);
        fFiltPane.add(filterFileButton);

        JPanel bFiltPane = new JPanel();
        bFiltPane.setLayout(new BoxLayout(bFiltPane, BoxLayout.Y_AXIS));
        bFiltPane.add(bedFilterFile);
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
        filtPane.add(Box.createVerticalGlue());
        filtPane.add(fFiltPane);
        filtPane.add(bFiltPane);
        filtPane.add(compoundHetButton);

        //Stats (line count)
        JPanel stats = new JPanel();
        JLabel linesl = new JLabel("Number of Variant Positions: ");
        stats.add(linesl);
        stats.add(lines);


        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.setPreferredSize(new Dimension((w/2), (h/3)));
        //tablePanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        tablePanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,10));
        tablePanel.add(dataScroller);
        tablePanel.add(Box.createRigidArea(new Dimension(0,15)));
        tablePanel.add(samplePane);
        tablePanel.add(stats);

        //Listener Registration
        //TableListener to detect data changes added in redrawOutTable(String)
        apply.addActionListener(this);
        selectAll.addActionListener(this);
        clear.addActionListener(this);
        openItem.addActionListener(this);
        saveAsItem.addActionListener(this);
        saveViewItem.addActionListener(this);
        exitItem.addActionListener(this);
        aboutItem.addActionListener(this);
        check.addActionListener(this);
        filterFileButton.addActionListener(this);
        bedFilterFileButton.addActionListener(this);
        compoundHetButton.addActionListener(this);

        //Disable unused buttons
        filterFile.setEnabled(false);
        bedFilterFile.setEnabled(false);
        compoundHetButton.setEnabled(false);
        if (vdat.returnParent() != null) {
            compoundHetButton.setVisible(false);
        }
        maskCBox();
                
        pane.add(tablePanel, BorderLayout.CENTER);
        pane.add(filtPane, BorderLayout.LINE_END);
        add(pane);
        pack();
        setVisible(true);
    }



    /* *************
    *   Redraw Table
    *  *************
    */
    private void redrawOutTable(String newData) {
        if (newData != null) {
            vdat = new VarData(newData);
            maskCBox();
            clear.doClick();
            drawMinAffSpinner();

        }
        if (isShowVar) {
            sorter = new TableSorter( new VarTableModel(vdat.returnData(),
            vdat.returnDataNames(), vdat ));
        }
        else {
            sorter = new TableSorter( new VarTableModel(vdat.returnGeneData(),
            vdat.returnGeneNames(), vdat ));
        }
        //sorter.addTableModelListener(this); //Probably wrong...
        ((VarTableModel)sorter.getTableModel()).addTableModelListener(this);
        outTable.setModel(sorter);
        sorter.setTableHeader(outTable.getTableHeader());
        initColSizes(outTable, (VarTableModel)((TableSorter)outTable.getModel()).getTableModel() );
        lines.setText(Integer.toString(outTable.getRowCount()));
        outTable.requestFocusInWindow();

    }


    /* *************
    *   Disable unused CheckBoxes
    *  *************
    */
    private void maskCBox() {
        HashMap<String, Integer> typeMap = vdat.returnDataTypeAt();
        if (typeMap.containsKey("MendHomRec")) {
            mendRec.setEnabled(true);
        }
        else {
            mendRec.setEnabled(false);
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

        
        if (vdat.countAffNorm() == 0) {
            uniqInAff.setEnabled(false);
            minAffSpinner.setEnabled(false);
            affSpinnerLabel.setEnabled(false);
        }
        else {
            uniqInAff.setEnabled(true);
            minAffSpinner.setEnabled(true);
            affSpinnerLabel.setEnabled(true);
        }

    }      

    /* *************
    *   initialize  minAffSpinner
    *  *************
    */    
    private void drawMinAffSpinner() {
        if (vdat.countAffNorm() > 0) {
            minAffSpinner.setModel(new SpinnerNumberModel(1, 1, vdat.countAffNorm(), 1));
        }
        Dimension d = minAffSpinner.getPreferredSize();
        d.width = 60;
        minAffSpinner.setPreferredSize(d);
        minAffSpinner.setMaximumSize(minAffSpinner.getPreferredSize());
    }
       


    /* *************
    *   Open data
    *  *************
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
            filterFile.setText(fileName);
            filterFile.setEnabled(true);
        }
        else if (openType == BED_FILTER_FILE) {
            bedFilterFile.setText(fileName);
            bedFilterFile.setEnabled(true);
        }
        return fileName;
    }

    
    /* *************
    *   Save data, either in place, or as new
    *  *************
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
                String[][] outData = vdTemp.dataDump();

                for (int i=0; i < outData.length; i++) {
                    StringBuilder outString = new StringBuilder(100);
                                    
                    for (int j=0; j < outData[i].length; j++) {
                        outString.append(outData[i][j] + "\t");
                    }
                    outString.deleteCharAt(outString.length() - 1);
                    //outString.append(newLine);

                    pw.println(outString.toString());
                }
                pw.close();
                if (pw.checkError()) {
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
                    //VarData vd = new VarData(args[0]);
                    //v = new VarSifter(vd);
                }
                else {
                    v = new VarSifter((String)null);
                }
                //System.out.println("Width: " + v.w + "\tHeight: " + v.h);
            }
        });
    }
}
