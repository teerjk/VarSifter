import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.util.BitSet;

/* ****************
*   VarSifter is designed to read a flat file where a row contains a variant, and all sample info
*    and allow sorting, and filtering of the data. This class is a wrapper that handles the GUI
*    via Swing.
*  ****************
*/

public class VarSifter extends JFrame implements ListSelectionListener, ActionListener {
    
    final String version = "0.1";
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


    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    int w = (int)dim.getWidth();
    int h = (int)dim.getHeight();


    // Test using object[][], titles[]
    VarData vdat;
    JTable outTable;
    TableSorter sorter;
    JTable sampleTable;
    JScrollPane dataScroller;
    JScrollPane sampleScroller;
    ListSelectionModel lsm;
    JLabel lines = new JLabel();

    JCheckBox div = new JCheckBox("DIV");
    JCheckBox splice = new JCheckBox("Splice-Site");
    JCheckBox nonsyn = new JCheckBox("Non-synonymous");
    JCheckBox dbsnp = new JCheckBox("dbSNP130");
    JCheckBox stop = new JCheckBox("Stop");
    JCheckBox[] cBox = { div, splice, nonsyn, dbsnp, stop };

    //JCheckBox[] cBox = { new JCheckBox("DIV"),
    //                     new JCheckBox("Splice-Site"),
    //                     new JCheckBox("Non-synonymous"),
    //                     new JCheckBox("dbSNP130"),
    //                     new JCheckBox("Stop")
    //                   };
    
    JButton apply = new JButton("Apply Filter");
    JButton check = new JButton("Check");
    
    //int lastRow = 0;    //Last row selected

    /* ******************
    *   Initiate GUI, instantiate VarData
    *  ******************
    */

    public VarSifter(String inFile) {
        
        //initiate parent window
        super("VarSifter - TEST");
        setBounds(0, (h/4), (w/2), (h/2));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout());
        pane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        vdat = new VarData(inFile);
        sorter = new TableSorter(new VarTableModel(vdat.returnData(), vdat.returnDataNames()));
        outTable = new JTable(sorter);
        sorter.setTableHeader(outTable.getTableHeader());
        //outTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        outTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        outTable.setRowSelectionInterval(0,0);
        outTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        outTable.setDefaultRenderer(Number.class, new VarScoreRenderer());
        initColSizes(outTable, (VarTableModel)((TableSorter)outTable.getModel()).getTableModel() );
        lsm = outTable.getSelectionModel();
        lsm.addListSelectionListener(this);
        
        dataScroller = new JScrollPane(outTable,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dataScroller.setPreferredSize(new Dimension((w/2), (h/4)));
        
        sampleTable = new JTable( new VarTableModel(vdat.returnSample(outTable.getSelectedRow()),
            vdat.returnSampleNames() ));
        sampleTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        sampleScroller = new JScrollPane(sampleTable,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sampleScroller.setPreferredSize(new Dimension((w/2), 80));
        sampleTable.setDefaultRenderer(Object.class, new SampleScoreRenderer());
        initColSizes(sampleTable, (VarTableModel)sampleTable.getModel());
        
        //Filters
        JPanel filtPane = new JPanel();
        filtPane.setLayout(new BoxLayout(filtPane, BoxLayout.Y_AXIS));
        //filtPane.setBorder(BorderFactory.createLineBorder(Color.black));
        JPanel includePane = new JPanel();
        includePane.setLayout(new BoxLayout(includePane, BoxLayout.Y_AXIS));
        includePane.setBorder(BorderFactory.createLineBorder(Color.black));
        includePane.add(new JLabel("Include:"));
        includePane.add(div);
        includePane.add(splice);
        includePane.add(nonsyn);
        includePane.add(stop);
        JPanel excludePane = new JPanel();
        excludePane.setLayout(new BoxLayout(excludePane, BoxLayout.Y_AXIS));
        excludePane.setBorder(BorderFactory.createLineBorder(Color.black));
        excludePane.add(new JLabel("Exclude:"));
        excludePane.add(dbsnp);
        filtPane.add(includePane);
        filtPane.add(Box.createRigidArea(new Dimension(0,15)));
        filtPane.add(excludePane);
        filtPane.add(Box.createRigidArea(new Dimension(0,15)));
        filtPane.add(apply);

        //Stats (line count)
        JPanel stats = new JPanel();
        JLabel linesl = new JLabel("Number of Variant Positions: ");
        lines.setText(Integer.toString(outTable.getRowCount()));
        stats.add(linesl);
        stats.add(lines);


        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.setPreferredSize(new Dimension((w/2), (h/3)));
        tablePanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        tablePanel.add(dataScroller);
        tablePanel.add(Box.createRigidArea(new Dimension(0,15)));
        tablePanel.add(sampleScroller);
        tablePanel.add(stats);

        apply.addActionListener(this);
        check.addActionListener(this);
                
        pane.add(tablePanel, BorderLayout.CENTER);
        pane.add(filtPane, BorderLayout.LINE_END);
        pane.add(check, BorderLayout.PAGE_END);
        add(pane);
        pack();
        setVisible(true);

    }
    

    /* *************
    *   Handle Actions
    *  *************
    */

    public void actionPerformed(ActionEvent e) {
        Object es = e.getSource();

        if (es == apply) {
            
            sorter.setTableHeader(null);    //Must do this to avoid memory leak
            BitSet mask = new BitSet(cBox.length);
            
            for (int i = 0; i < cBox.length; i++) {
                if (cBox[i].isSelected()) {
                    //System.out.println(cb.getText());
                    mask.set(i);
                }
            }

            if (mask.cardinality() == 0) {
                vdat.resetOutput();
            }
            else {
                vdat.filterData(mask);
            }
            
            //if (dbsnp.isSelected()) {
            //    vdat.filterData();
            //}
            
            sorter = new TableSorter( new VarTableModel(vdat.returnData(),
                vdat.returnDataNames()));
            outTable.setModel(sorter);
            sorter.setTableHeader(outTable.getTableHeader());
            initColSizes(outTable, (VarTableModel)((TableSorter)outTable.getModel()).getTableModel() );
            lines.setText(Integer.toString(outTable.getRowCount()));
            outTable.requestFocusInWindow();
        }
        
        if (es == check) {
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

            sampleTable.setModel(new VarTableModel(vdat.returnSample(dataIndex),
                vdat.returnSampleNames()));
            
            outTable.setRowSelectionInterval(rowIndex,rowIndex);
            //lastRow = rowIndex;
            initColSizes(sampleTable, (VarTableModel)sampleTable.getModel());
            
            //System.out.println(outTable.getSelectedRow() + "\t" + rowIndex + "\t" + dataIndex);
        }
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

        for (int i = 0; i < largestInCol.length; i++) {
            col = table.getColumnModel().getColumn(i);

            comp = headerRenderer.getTableCellRendererComponent(
                table, col.getHeaderValue(), false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;

            comp = table.getDefaultRenderer(model.getColumnClass(i)).getTableCellRendererComponent(
                table, largestInCol[i], false, false, 0, i);
            cellWidth = comp.getPreferredSize().width;

            col.setPreferredWidth( (Math.max(headerWidth, cellWidth) + 25) );
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
                    v = new VarSifter("test_data.txt");
                }
                //System.out.println("Width: " + v.w + "\tHeight: " + v.h);
            }
        });
    }
}
