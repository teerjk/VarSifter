import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.event.*;

/* ****************
*   VarSifter is designed to read a flat file where a row contains a variant, and all sample info
*    and allow sorting, and filtering of the data. This class is a wrapper that handles the GUI
*    via Swing.
*  ****************
*/

public class VarSifter extends JFrame implements ListSelectionListener {
    
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

    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    int w = (int)dim.getWidth();
    int h = (int)dim.getHeight();


    // Test using object[][], titles[]
    VarData vdat;
    JTable outTable;
    JTable sampleTable;
    JScrollPane dataScroller;
    JScrollPane sampleScroller;
    ListSelectionModel lsm;
    JLabel lines = new JLabel();

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
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        vdat = new VarData(inFile);
        outTable = new JTable(vdat.returnData(), vdat.returnDataNames());
        outTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        outTable.setRowSelectionInterval(0,0);
        outTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        lsm = outTable.getSelectionModel();
        lsm.addListSelectionListener(this);
        //lsm.addListSelectionListener(new SharedListSelectionHandler());
        lines.setText(Integer.toString(outTable.getRowCount()));
        
        dataScroller = new JScrollPane(outTable,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dataScroller.setPreferredSize(new Dimension((w/2), (h/4)));
        
        sampleTable = new JTable( vdat.returnSample(outTable.getSelectedRow()), 
            vdat.returnSampleNames() );
        sampleTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        sampleScroller = new JScrollPane(sampleTable,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sampleScroller.setPreferredSize(new Dimension((w/2), 40));

        JPanel stats = new JPanel();
        JLabel linesl = new JLabel("Number of Variant Positions: ");
        stats.add(linesl);
        stats.add(lines);
        
                
        pane.add(dataScroller);
        pane.add(Box.createRigidArea(new Dimension(0,15)));
        pane.add(sampleScroller);
        pane.add(stats);
        add(pane);
        setVisible(true);

    }


    /* *************
    *   Handle List Actions
    *  *************
    */

    public void valueChanged(ListSelectionEvent e) {
        ListSelectionModel lsme = (ListSelectionModel)e.getSource();
        
        //May remove this test if its the only List Action
        if ( e.getSource() == lsm && lsme.getValueIsAdjusting() == false) {
            sampleTable.setModel(new DefaultTableModel(vdat.returnSample(outTable.getSelectedRow()),
                vdat.returnSampleNames()));
            
            //System.out.println(outTable.getSelectedRow());
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
