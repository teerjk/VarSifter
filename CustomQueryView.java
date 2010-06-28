import java.awt.*;
import javax.swing.*;
import java.util.HashMap;
import java.awt.event.*;

/* ********************
*   A window to handle GUI for custom query string generation
*     -This class will generate an if-statement for sifting data
*  ********************
*/

public class CustomQueryView extends JPanel implements ActionListener {
    String[] sampleNames;
    HashMap<String, Integer> dataTypeAt;
    int annoSize;

    private int lastQueryIndex = 0;
    final int SAMPLE = 0;
    final int ACTION = 1;
    final int CONNECT = 2;

    JButton[] sampleButtons;
    private JButton exactMatch = new JButton("Exactly Matches");
    private JButton noMatch = new JButton("Does Not Match");
    private JButton apply = new JButton("Apply");
    private JButton clear = new JButton("Clear");
    private JButton andButton = new JButton("And");
    private JButton orButton = new JButton("Or");
    
    private JTextArea outText = new JTextArea();
    private StringBuilder query;


    //Initializer
    public CustomQueryView(VarData vdat) {
        sampleNames = vdat.returnSampleNames();
        dataTypeAt = vdat.returnDataTypeAt();
        annoSize = dataTypeAt.size();
        sampleButtons = new JButton[sampleNames.length];
        
        String out = new String("");
        for (int i=0; i<sampleNames.length; i++) {
            sampleButtons[i] = new JButton(sampleNames[i]);
        }

        initQuery();
        initTable();
    }


    //Initialize GUI
    private void initTable() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JScrollPane textScroll = new JScrollPane(outText,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        JPanel samplePane = new JPanel();
        samplePane.setLayout(new BoxLayout(samplePane, BoxLayout.Y_AXIS));
        samplePane.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(Color.black),
                                BorderFactory.createEmptyBorder(7,7,7,7)
                            ));
        for (int i = 0; i<sampleButtons.length; i++) {
            //sampleButtons[i].setName( ((Integer)i).toString() );
            samplePane.add(sampleButtons[i]);
            sampleButtons[i].addActionListener(this);
            if (i != (sampleButtons.length - 1) ) {
                samplePane.add(Box.createRigidArea(new Dimension(0,5)));
            }
        }

        JPanel actionPane = new JPanel();
        actionPane.setLayout(new BoxLayout(actionPane, BoxLayout.Y_AXIS));
        actionPane.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(Color.black),
                                BorderFactory.createEmptyBorder(7,7,7,7)
                             ));
        actionPane.add(exactMatch);
        actionPane.add(Box.createRigidArea(new Dimension(0,5)));
        actionPane.add(noMatch);

        JPanel connectPane = new JPanel();
        connectPane.setLayout(new BoxLayout(connectPane, BoxLayout.X_AXIS));
        connectPane.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(Color.black),
                                BorderFactory.createEmptyBorder(7,7,7,7)
                             ));
        connectPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        connectPane.add(andButton);
        connectPane.add(Box.createRigidArea(new Dimension(5,0)));
        connectPane.add(orButton);
        
        JPanel runPane = new JPanel();
        runPane.setLayout(new BoxLayout(runPane, BoxLayout.X_AXIS));
        runPane.setBorder(BorderFactory.createEmptyBorder(7,7,7,7));
        runPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        runPane.add(apply);
        runPane.add(Box.createRigidArea(new Dimension(5,0)));
        runPane.add(clear);

        exactMatch.addActionListener(this);
        noMatch.addActionListener(this);
        apply.addActionListener(this);
        clear.addActionListener(this);
        andButton.addActionListener(this);
        orButton.addActionListener(this);

        add(new JLabel("Samples:"));
        add(samplePane);
        add(Box.createRigidArea(new Dimension(0,5)));
        add(new JLabel("Actions:"));
        add(actionPane);
        add(Box.createRigidArea(new Dimension(0,5)));
        add(new JLabel("Logical:"));
        add(connectPane);
        add(Box.createRigidArea(new Dimension(0,5)));
        add(textScroll);
        add(Box.createRigidArea(new Dimension(0,5)));
        add(runPane);
    }


    public void actionPerformed(ActionEvent e) {
        Object es = e.getSource();
        //System.out.println(es);

        if (es == apply) {
            finalizeQuery();
        }
        else if (es == clear) {
            initQuery();
        }
        else if (es == exactMatch) {
            query.append(".equals(");
        }
        else {
            for (int i=0; i<sampleButtons.length; i++) {
                if (es == sampleButtons[i]) {
                    //System.out.println(sampleButtons[i].getText());
                    query.append( ((i * VarData.S_FIELDS) + annoSize) );
                    break;
                }
            }
        }

        outText.setText(query.toString());

    }

    private void initQuery() {
        query = new StringBuilder(64);
        query.append("if (");
    }

    private void finalizeQuery() {
        query.append(") {");
    }
}
