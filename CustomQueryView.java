import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.awt.event.*;
import java.util.regex.*;

import java.util.Collection;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ChainedTransformer;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import edu.uci.ics.jung.visualization.renderers.VertexLabelAsShapeRenderer;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.PickingGraphMousePlugin;


/**
*   Creates a window to handle GUI for custom query string generation
*     This class will generate an if-statement for sifting data
*   @author Jamie K. Teer
*/
public class CustomQueryView extends JPanel implements ActionListener, ListSelectionListener {
    String[] sampleNames;
    String[] annotNames;
    AbstractMapper[] annotMap;
    AbstractMapper currentMap;
    private VarData vdat;
    private VarSifter gui;
    Map<String, Integer> dataTypeAt;
    int annoSize;
    DelegateForest<CustomVertex, Integer> graph;

    private int lastQueryIndex = 0;
    private Integer edgeCount = 0;
    final static int SAMPLE = 0;
    final static int FIXED_SAMPLE = 1;
    final static int ACTION = 2;
    final static int ANNOT_ACTION = 3;
    final static int ANNOT_COMP = 4;
    final static int LOGICAL = 5;
    final static int ANNOT_LIST = 6;
    final static int ANNOT_VAL = 7;

    private final static Pattern compPat = Pattern.compile("[<>=&]|get|isH");

    private boolean isAnnotQuery = false;

    String[] fixedSamples = { "Homozygous Reference",
                              "Homozygous Variant",
                              "Homozygous",
                              "Heterozygous",
                              "NA"
                            };
    private JList sampleList;
    private JList fixedSampleList = new JList(fixedSamples);
    private JList annotList;
    private JList stringAnnotList = new JList();
    
    private JLabel sSampleLabel = new JLabel("Samples:");
    private JLabel sGenLabel = new JLabel("Genotypes:");
    private JLabel sActionLabel = new JLabel("Sample Actions:");

    private JLabel aAnnoLabel = new JLabel("Annotations:");
    private JLabel aValueLabel = new JLabel("Annotation Values:");
    private JLabel aActionLabel = new JLabel("Annotation Actions:");
    private JLabel aNumActionLabel = new JLabel("Annot. Numeric Actions:");

    private JLabel logicLabel = new JLabel("Logical connectors:");

    private JButton exactMatch = new JButton("Exactly Matches");
    private JButton noMatch = new JButton("Does Not Match");
    private JButton[] actionButtons = {exactMatch, noMatch};

    private JButton annotExactMatch = new JButton("Exactly Matches");
    private JButton annotNoMatch = new JButton("Does Not Match");
    private JButton[] annotActionButtons = {annotExactMatch, annotNoMatch};

    private JButton eqButton = new JButton("Equals");
    private JButton gtButton = new JButton("Greater Than");
    private JButton ltButton = new JButton("Less Than");
    private JButton[] annotCompButtons = {eqButton, gtButton, ltButton};

    private JButton applyAnnotComp = new JButton("Apply Number");
    private JButton applyStringPattern = new JButton("Apply Search Text");

    private JButton andButton = new JButton("AND");
    private JButton orButton = new JButton("OR");
    private JButton xorButton = new JButton("XOR");
    private JButton[] logicButtons = {andButton, orButton, xorButton};

    private JButton finalizeQuery = new JButton("Finalize Query");
    private JButton reset = new JButton("Reset Current Query");
    private JButton delete = new JButton("Delete Selected");
    private JButton qSave = new JButton("Save Query");
    private JButton qLoad = new JButton("Load Query");
    private JButton clear = new JButton("Clear All");
    private JComboBox modeBox;
    
    private JTextField outText = new JTextField();
    private JTextArea outMessage = new JTextArea("Select a Sample or Annotation");
    private JTextField inAnnotNumber = new JTextField();
    private JTextField inStringPattern = new JTextField();
    private StringBuilder query;
    private StringBuilder vertexLabel;
    private int vertexLabelCount = 1;   //Use this to know where we are in the query assembly process
    private String outGroup;
    private List<BitSet> bitSetList = new ArrayList<BitSet>();
    
    private TreeLayout<CustomVertex,Integer> layout;
    private VisualizationViewer<CustomVertex,Integer> vv;
    

    /**
    *   Initializer
    *
    *   @param inVdat VarData Object
    */
    public CustomQueryView(VarData inVdat, VarSifter inGui) {
        vdat = inVdat;
        gui = inGui;
        sampleNames = vdat.returnSampleNames();
        annotNames = vdat.returnDataNames();
        dataTypeAt = vdat.returnDataTypeAt();
        annoSize = dataTypeAt.size();
        annotMap = vdat.returnAnnotMap();

        graph = new DelegateForest<CustomVertex,Integer>();
        layout = new TreeLayout<CustomVertex,Integer>(graph);
        vv = new VisualizationViewer<CustomVertex,Integer>(layout);
        VertexLabelAsShapeRenderer<CustomVertex,Integer> vlasr = new 
            VertexLabelAsShapeRenderer<CustomVertex,Integer>(vv.getRenderContext());
        vv.getRenderContext().setVertexLabelTransformer(
            new ChainedTransformer<CustomVertex,String>(new Transformer[]{
                new ToStringLabeller<String>(),
                new Transformer<String,String>() {
                    public String transform(String input) {
                        return "<html><center>"+input;
                    }
                }
            }));
        vv.getRenderContext().setVertexShapeTransformer(vlasr);
        vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line());
        vv.getRenderer().setVertexLabelRenderer(vlasr);

        final DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();
        modeBox = graphMouse.getModeComboBox();
        modeBox.setMaximumSize(modeBox.getPreferredSize());
        modeBox.addItemListener(graphMouse.getModeListener());
        graphMouse.setMode(ModalGraphMouse.Mode.PICKING);

        vv.setGraphMouse(graphMouse);

        String out = "";
        sampleList = new JList(sampleNames);
        annotList = new JList(annotNames);

        initQuery();
        initTable();
    }


    /**
    *   Initialize GUI
    */
    private void initTable() {
        this.setLayout(new BorderLayout());

        ToolTipManager.sharedInstance().setInitialDelay(1250);
        ToolTipManager.sharedInstance().setReshowDelay(0);
        ToolTipManager.sharedInstance().setDismissDelay(10000);


        /* *******
        *   Sample query setup
        *  *******
        */

        JPanel fixedPane = new JPanel();
        fixedPane.setLayout(new BoxLayout(fixedPane, BoxLayout.Y_AXIS));
        fixedPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        fixedSampleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane fSS = new JScrollPane(fixedSampleList);
        fSS.setMaximumSize(new Dimension(fSS.getMaximumSize().width, 60));
        fixedPane.add(fSS);
        fixedPane.setPreferredSize(new Dimension(fSS.getPreferredSize().width, 60));


        JPanel samplePane = new JPanel();
        samplePane.setLayout(new BoxLayout(samplePane, BoxLayout.Y_AXIS));
        samplePane.setAlignmentX(Component.LEFT_ALIGNMENT);

        sampleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        samplePane.add(new JScrollPane(sampleList));


        JPanel actionPane = new JPanel();
        actionPane.setLayout(new BoxLayout(actionPane, BoxLayout.Y_AXIS));
        actionPane.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(Color.black),
                                BorderFactory.createEmptyBorder(7,7,7,7)
                             ));
        actionPane.add(exactMatch);
        actionPane.add(Box.createRigidArea(new Dimension(0,5)));
        actionPane.add(noMatch);


        /* *******
        *   Annotation panel setup
        *  *******
        */

        JPanel annotPane = new JPanel();
        annotPane.setLayout(new BoxLayout(annotPane, BoxLayout.Y_AXIS));
        annotPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        annotPane.setPreferredSize(new Dimension(200, annotPane.getPreferredSize().height));
        annotList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane annotPaneScroller = new JScrollPane();
        annotPane.add(new JScrollPane(annotList));
        
        
        JPanel stringAnnotPane = new JPanel();
        stringAnnotPane.setLayout(new BoxLayout(stringAnnotPane, BoxLayout.Y_AXIS));
        stringAnnotPane.setPreferredSize(new Dimension(200, stringAnnotPane.getPreferredSize().height));
        stringAnnotPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        stringAnnotList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stringAnnotPane.add(new JScrollPane(stringAnnotList));


        JPanel annotActionPane = new JPanel();
        annotActionPane.setLayout(new BoxLayout(annotActionPane, BoxLayout.Y_AXIS));
        annotActionPane.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(Color.black),
                                BorderFactory.createEmptyBorder(7,7,7,7)
                                ));
        inStringPattern.setMaximumSize(new Dimension( inStringPattern.getMaximumSize().width, 
                                              inStringPattern.getMinimumSize().height));
        annotActionPane.add(annotExactMatch);
        annotActionPane.add(Box.createRigidArea(new Dimension(0,5)));
        annotActionPane.add(annotNoMatch);
        annotActionPane.add(Box.createRigidArea(new Dimension(0,5)));
        annotActionPane.add(inStringPattern);
        annotActionPane.add(Box.createRigidArea(new Dimension(0,5)));
        annotActionPane.add(applyStringPattern);


        JPanel annotCompPane = new JPanel();
        annotCompPane.setLayout(new BoxLayout(annotCompPane, BoxLayout.Y_AXIS));
        annotCompPane.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(Color.black),
                                BorderFactory.createEmptyBorder(7,7,7,7)
                            ));
        annotCompPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        inAnnotNumber.setMaximumSize(new Dimension( inAnnotNumber.getMaximumSize().width, 
                                              inAnnotNumber.getMinimumSize().height));
        inAnnotNumber.setAlignmentX(Component.LEFT_ALIGNMENT);
        annotCompPane.add(eqButton);
        annotCompPane.add(Box.createRigidArea(new Dimension(0,5)));
        annotCompPane.add(gtButton);
        annotCompPane.add(Box.createRigidArea(new Dimension(0,5)));
        annotCompPane.add(ltButton);
        annotCompPane.add(Box.createRigidArea(new Dimension(0,5)));
        annotCompPane.add(inAnnotNumber);
        annotCompPane.add(Box.createRigidArea(new Dimension(0,5)));
        annotCompPane.add(applyAnnotComp);



        /* ********
        *   Main Controls
        *  ********
        */

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
        connectPane.add(Box.createRigidArea(new Dimension(5,0)));
        connectPane.add(xorButton);
        
        JPanel runPane = new JPanel();
        runPane.setLayout(new BoxLayout(runPane, BoxLayout.X_AXIS));
        runPane.setBorder(BorderFactory.createEmptyBorder(7,7,7,7));
        runPane.add(finalizeQuery);
        runPane.add(Box.createRigidArea(new Dimension(5,0)));
        runPane.add(reset);
        runPane.add(Box.createRigidArea(new Dimension(5,0)));
        runPane.add(qSave);
        runPane.add(Box.createRigidArea(new Dimension(5,0)));
        runPane.add(qLoad);
        runPane.add(Box.createRigidArea(new Dimension(5,0)));
        runPane.add(delete);
        runPane.add(Box.createRigidArea(new Dimension(5,0)));
        runPane.add(clear);
        runPane.add(Box.createRigidArea(new Dimension(5,0)));

        JPanel extraPane = new JPanel();
        extraPane.setLayout(new BoxLayout(extraPane, BoxLayout.X_AXIS));
        extraPane.setBorder(BorderFactory.createEmptyBorder(7,7,7,7));
        extraPane.add(modeBox);
        extraPane.add(Box.createRigidArea(new Dimension(5,0)));
        outText.setMaximumSize(new Dimension( outText.getMaximumSize().width, 
                                              outText.getMinimumSize().height));
        //extraPane.add(outText);   //Debugging
        extraPane.add(outMessage);
        outMessage.setEditable(false);
        extraPane.add(Box.createVerticalGlue());

        //Icons
        finalizeQuery.setIcon(createImageIcon("images/boxes.png", "filter tree"));

        //Listeners
        exactMatch.addActionListener(this);
        noMatch.addActionListener(this);
        annotExactMatch.addActionListener(this);
        annotNoMatch.addActionListener(this);
        eqButton.addActionListener(this);
        gtButton.addActionListener(this);
        ltButton.addActionListener(this);
        applyAnnotComp.addActionListener(this);
        applyStringPattern.addActionListener(this);
        finalizeQuery.addActionListener(this);
        reset.addActionListener(this);
        qSave.addActionListener(this);
        qLoad.addActionListener(this);
        delete.addActionListener(this);
        clear.addActionListener(this);
        andButton.addActionListener(this);
        orButton.addActionListener(this);
        xorButton.addActionListener(this);

        sampleList.addListSelectionListener(this);
        fixedSampleList.addListSelectionListener(this);
        annotList.addListSelectionListener(this);
        stringAnnotList.addListSelectionListener(this);

        // Mask buttons
        enableButtons(new int[] {ACTION,ANNOT_ACTION,ANNOT_COMP,LOGICAL,ANNOT_VAL,FIXED_SAMPLE}, false);
        applyAnnotComp.setEnabled(false);
        applyStringPattern.setEnabled(false);
        //stringAnnotList.setEnabled(false);
        inAnnotNumber.setEnabled(false);

        //ToolTips
        finalizeQuery.setToolTipText("<html>Prepares the query logic for filtering.<p>"
            + "Must be clicked before filtering will work!");
        reset.setToolTipText("<html>Returns the current query to the intial state, without affecting query "
            + "boxes in the main window.<p>All selections used for the current query willl be lost.");
        qSave.setToolTipText("<html>Saves the current query for later use.<p>"
            + "Will only work for THIS data file!");
        qLoad.setToolTipText("<html>Loads a previously saved query.<p>"
            + "Can only load a query that was saved using THIS data file!");
        clear.setToolTipText("Deletes are queries and logical connections.");
        delete.setToolTipText("Deletes the selected query boxes.");
        modeBox.setToolTipText("<html>Defines box selection behavior:<p>"
            + "PICKING: Allows selection of boxes. Shift-click to select multiple boxes," 
            + "or click-drag to draw a selection box.<p>"
            + "TRANSFORMING: Click-drag the screen to move it around.");


        /* ********
        *   Layout the major sections
        *  ********
        */

        JPanel sampleControlPane = new JPanel();
        sampleControlPane.setLayout(new BoxLayout(sampleControlPane, BoxLayout.Y_AXIS));
        JScrollPane sampleControlScroller = new JScrollPane(sampleControlPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                                   JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sampleControlScroller.setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                                    BorderFactory.createEmptyBorder(2,5,2,2)
                                    ));

        sampleControlPane.add(sSampleLabel);
        sampleControlPane.add(samplePane);
        sampleControlPane.add(Box.createRigidArea(new Dimension(0,5)));
        sampleControlPane.add(sGenLabel);
        sampleControlPane.add(fixedPane);
        sampleControlPane.add(Box.createRigidArea(new Dimension(0,5)));
        sampleControlPane.add(sActionLabel);
        sampleControlPane.add(actionPane);
        sampleControlPane.add(Box.createRigidArea(new Dimension(0,5)));
        sampleControlPane.add(logicLabel);
        sampleControlPane.add(connectPane);

        JPanel annotControlPane = new JPanel();
        annotControlPane.setLayout(new BoxLayout(annotControlPane, BoxLayout.Y_AXIS));
        JScrollPane annotControlScroller = new JScrollPane(annotControlPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                                   JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        annotControlScroller.setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                                    BorderFactory.createEmptyBorder(2,2,2,5)
                                    ));

        annotControlPane.add(aAnnoLabel);
        annotControlPane.add(annotPane);
        annotControlPane.add(Box.createRigidArea(new Dimension(0,5)));
        annotControlPane.add(aValueLabel);
        annotControlPane.add(stringAnnotPane);
        annotControlPane.add(Box.createRigidArea(new Dimension(0,5)));
        annotControlPane.add(aActionLabel);
        annotControlPane.add(annotActionPane);
        annotControlPane.add(Box.createRigidArea(new Dimension(0,5)));
        annotControlPane.add(aNumActionLabel);
        annotControlPane.add(annotCompPane);

        JPanel mainControlPane = new JPanel();
        mainControlPane.setLayout(new BoxLayout(mainControlPane, BoxLayout.X_AXIS));
        JScrollPane mainControlScroller = new JScrollPane(mainControlPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                                           JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mainControlScroller.setBorder(null);
        mainControlPane.add(runPane);
        mainControlPane.add(extraPane);


        add(sampleControlScroller, BorderLayout.LINE_START);
        add(annotControlScroller, BorderLayout.LINE_END);
        add(mainControlScroller, BorderLayout.PAGE_END);
        final GraphZoomScrollPane zoomPane = new GraphZoomScrollPane(vv);
        zoomPane.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        add(zoomPane, BorderLayout.CENTER);
    }

    /**
    *   Handle Actions
    *
    *   @param e resulting from clicking a button
    */
    public void actionPerformed(ActionEvent e) {
        Object es = e.getSource();

        if (es == finalizeQuery) {
            finalizeQuery();
        }
        else if (es == reset) {
            resetQuery();
        }
        else if (es == qSave) {
            writeGraph();
        }
        else if (es == qLoad) {
            readGraph();
        }
        else if (es == delete) {
            deletePicked();
        }
        else if (es == clear) {
            bitSetList = new ArrayList<BitSet>();
            initQuery();
            graph = new DelegateForest<CustomVertex,Integer>();
        }
        else if (es == exactMatch) {
            buildQueryVertex("equals","==");
        }
        else if (es == noMatch) {
            buildQueryVertex("does not equal","!=");
        }
        else if (es == annotExactMatch) {
            buildQueryVertex("equals","==");
        }
        else if (es == annotNoMatch) {
            buildQueryVertex("does not equal","!=");
        }
        else if (es == eqButton) {
            buildQueryVertex("equals","==");
        }
        else if (es == gtButton) {
            buildQueryVertex(">", ">");
        }
        else if (es == ltButton) {
            buildQueryVertex("&lt", "<");
        }
        else if (es == applyAnnotComp) {
            String in = inAnnotNumber.getText();
            try {
                int i = Integer.parseInt(in);
                buildQueryVertex(in, in);
            }
            catch (NumberFormatException nfe) {
                try {
                    float f = Float.parseFloat(in);
                    buildQueryVertex(Float.toString(f), Float.toString(f) + "f");
                }
                catch (NumberFormatException n) {
                    System.out.println("Entry not a number! Please enter a number and try again!");
                    VarSifter.showError("Entry not a number! Please enter a number and try again!");
                }
            }
        }
        else if (es == applyStringPattern) {
            String in = inStringPattern.getText();
            if (buildQueryFromRegex(in)) {
                buildQueryVertex(in, "");
            }
        }
        else if (es == andButton) {
            linkVertices("AND", " && ");
        }
        else if (es == orButton) {
            linkVertices("OR", " || ");
        }
        else if (es == xorButton) {
            linkVertices("XOR", " ^ ");
        }
        redrawGraph();

    }


    /**
    *   Handle List Selections
    *   
    *   @param e Event from a list selection
    */
    public void valueChanged(ListSelectionEvent e) {
        Object es = e.getSource();

        if (e.getValueIsAdjusting() == false) {

            if (es == sampleList && sampleList.getSelectedIndex() >= 0) {
                if (vertexLabelCount == 1) {
                    isAnnotQuery = false;
                }
                int selIndex = sampleList.getSelectedIndex();
                String labelTemp = sampleNames[selIndex];
                labelTemp = labelTemp.replaceFirst("\\.NA$", "");
                String queryTemp = ("allData[i]["
                                    + (( selIndex * VarData.S_FIELDS) + annoSize) + "]");
                buildQueryVertex(labelTemp, queryTemp);
                sampleList.clearSelection();
            }
            else if (es == fixedSampleList && fixedSampleList.getSelectedIndex() >= 0) {
                int selIndex = fixedSampleList.getSelectedIndex();
                switch (selIndex) {
                    case 0: //HomRef
                        buildQueryVertex(fixedSamples[0], "homRefGen");
                        break;
                    case 1: //HomVariant (HomNonref)
                        buildQueryVertex(fixedSamples[1], "homNonRefGen");
                        break;
                    case 2: //Homozygous
                        buildQueryFromBitSet("isHom");
                        buildQueryVertex(fixedSamples[2], "");
                        break;
                    case 3: //Heterzygous
                        buildQueryFromBitSet("isHet");
                        buildQueryVertex(fixedSamples[3], "");
                        break;
                    case 4: //NA
                        buildQueryVertex(fixedSamples[4], "NA_Allele");
                        break;
                }
                fixedSampleList.clearSelection();
            }
            else if (es == annotList && annotList.getSelectedIndex() >= 0) {
                int selIndex = annotList.getSelectedIndex();
                if (vertexLabelCount == 1) {
                    isAnnotQuery = true;
                }
                String labelTemp = annotNames[selIndex];
                String queryTemp = ("allData[i][" + selIndex + "]");
                buildQueryVertex(labelTemp, queryTemp);

            }
            else if (es == stringAnnotList && stringAnnotList.getSelectedIndex() >= 0) {
                String selValue = (String)stringAnnotList.getSelectedValue();
                buildQueryVertex(selValue, Integer.toString(currentMap.getIndexOf(selValue)));
            }
        }
    }

    /**
    *   Enable buttons to allow proper query building
    */
    private void enableButtons(int[] buttonGroup, boolean toEnabled) {
        for (int bG: buttonGroup) {
            JButton[] group = null;
            switch (bG) {
                case SAMPLE:
                    sampleList.setEnabled(toEnabled);
                    sSampleLabel.setEnabled(toEnabled);
                    break;
                case FIXED_SAMPLE:
                    fixedSampleList.setEnabled(toEnabled);
                    sGenLabel.setEnabled(toEnabled);
                    break;
                case ACTION:
                    group = actionButtons;
                    sActionLabel.setEnabled(toEnabled);
                    break;
                case ANNOT_ACTION:
                    group = annotActionButtons;
                    aActionLabel.setEnabled(toEnabled);
                    break;
                case ANNOT_COMP:
                    group = annotCompButtons;
                    aNumActionLabel.setEnabled(toEnabled);
                    break;
                case LOGICAL:
                    group = logicButtons;
                    logicLabel.setEnabled(toEnabled);
                    break;
                case ANNOT_LIST:
                    annotList.setEnabled(toEnabled);
                    aAnnoLabel.setEnabled(toEnabled);
                    break;
                case ANNOT_VAL:
                    stringAnnotList.setEnabled(toEnabled);
                    aValueLabel.setEnabled(toEnabled);
                    break;
            }
            if (group != null) {
                for (JButton b: group) {
                    b.setEnabled(toEnabled);
                }
            }
        }
    }

    /**
    *   Build the query based on what stage of button clicking one is at
    */
    private void buildQueryVertex(String labelString, String queryString) {
        switch (vertexLabelCount) {
            case 1:
                initQuery();
                vertexLabel.append("<html>" + labelString + "<p>");
                query.append(queryString);
                vertexLabelCount++;
                outText.setText(vertexLabel.toString());
                if (isAnnotQuery) {
                    int selIndex = annotList.getSelectedIndex();
                    currentMap = annotMap[selIndex];
                    outMessage.setText("Next, please select an Annotation Action");
                    switch (currentMap.getDataType()) {
                        case VarData.MULTISTRING:
                        case VarData.STRING:
                            stringAnnotList.setListData(currentMap.getSortedEntries());
                            enableButtons(new int[] {ANNOT_ACTION}, true);
                            break;
                        case VarData.FLOAT:
                            query.insert(0, "annotMapper[" + selIndex + "].getFloat(");
                            query.append(")");
                        case VarData.INTEGER:
                            enableButtons(new int[] {ANNOT_COMP}, true);
                            break;
                            
                    }
                }
                else {
                    outMessage.setText("Next, please select a Sample Action");
                    enableButtons(new int[] {ACTION}, true);
                }
                enableButtons(new int[] {SAMPLE, ANNOT_LIST, LOGICAL}, false);
                //annotList.setEnabled(false);
                break;
            case 2:
                vertexLabel.append(labelString + "<p>");
                vertexLabelCount++;
                outText.setText(vertexLabel.toString());
                if (isAnnotQuery) {
                    enableButtons(new int[] {ANNOT_ACTION, ANNOT_COMP}, false);
                    switch (currentMap.getDataType()) {
                        case VarData.MULTISTRING:
                            if (queryString.equals("==")) {
                                queryString = " & ";
                            }
                            else if (queryString.equals("!=")) {
                                queryString = " & ";
                                query.insert(0,"~");
                            }
                            queryString += "(int)Math.pow(2,";
                            query.insert(0, "(");
                        case VarData.STRING:
                            applyStringPattern.setEnabled(true);
                            enableButtons(new int[] {ANNOT_VAL}, true);
                            outMessage.setText("Finally, select an Annotation Value, or enter Search Text" + 
                                VarSifter.newLine + "and click \"Apply Search Text\"");
                            //stringAnnotList.setEnabled(true);
                            break;
                        case VarData.FLOAT:
                        case VarData.INTEGER:
                            inAnnotNumber.setEnabled(true);
                            applyAnnotComp.setEnabled(true);
                            outMessage.setText("Finally, enter a Number and click \"Apply Number\"");
                            break;
                    }
                    
                }
                else {
                    outMessage.setText("Finally, select another Sample or a Genotype");
                    enableButtons(new int[] {ACTION}, false);
                    enableButtons(new int[] {FIXED_SAMPLE, SAMPLE}, true);
                }
                query.append(queryString);
                break;
            case 3:
                vertexLabel.append(labelString);
                query.append(queryString);
                if (isAnnotQuery) {
                    switch (currentMap.getDataType()) {
                        case VarData.MULTISTRING:
                            if (!query.toString().contains("~")) {
                                query.append(")) > 0");
                            }
                            else {
                                int aI = query.lastIndexOf("&");
                                String maskNum = query.substring(aI+2);
                                query.append("))==" + maskNum);
                                if (query.toString().contains("Math.pow")) {
                                    query.append(")");
                                }
                            }
                        case VarData.STRING:
                            stringAnnotList.setListData(new String[]{""});
                            break;
                        case VarData.FLOAT:
                        case VarData.INTEGER:
                            break;
                    }                    
                    currentMap = null;
                }
                query.append(")");
                query.insert(0, "(");
                graph.addVertex(new CustomVertex(vertexLabel.toString(), query.toString()));
                
                redrawGraph();
                resetQuery();
                break;
        }
    }
    

    /**
    *   Reset the query state to 1 (ready for a new query)
    */
    private void resetQuery() {
        enableButtons(new int[] {LOGICAL, SAMPLE, ANNOT_LIST}, true);
        enableButtons(new int[] {ACTION, ANNOT_ACTION, ANNOT_COMP, ANNOT_VAL, FIXED_SAMPLE}, false);
        annotList.clearSelection();
        stringAnnotList.clearSelection();
        stringAnnotList.setListData(new String[]{""});
        isAnnotQuery = false;
        outMessage.setText("Select a Sample or Annotation");
        //annotList.setEnabled(true);
        //stringAnnotList.setEnabled(false);
        applyAnnotComp.setEnabled(false);
        applyStringPattern.setEnabled(false);
        vertexLabelCount = 1;
        initQuery();
    }

    /**
    *   Use regex to get indices of matching entries, append to a list.
    *   Then, recreate query to interrogate BitSet in list.
    *   @param regex The regex to search with
    *   @return True on success.
    */
    private boolean buildQueryFromRegex(String regex) {
        int index = bitSetList.size();
        Pattern pat;
        StringBuilder tempQuery = new StringBuilder();
        try {
            pat = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }
        catch (PatternSyntaxException pse) {
            VarSifter.showError("Error parsing search term. Not Applying! Please try again.");
            System.out.println(pse.toString());
            return false;
        }

        switch (currentMap.getDataType()) {
            case VarData.MULTISTRING:
                int lastIndex = -1;
                int msBSMask = 0;
                int thisIndex;
                BitSet msBS = currentMap.filterWithPattern(pat);
                while ( (thisIndex = msBS.nextSetBit(lastIndex+1)) >= 0 ) {
                    msBSMask += (int)Math.pow(2,thisIndex);
                    lastIndex = thisIndex;
                }
                tempQuery.append("(");
                tempQuery.append(query.toString());
                tempQuery.delete(tempQuery.length() - 16, tempQuery.length());  //remove "(int)Math.pow(2,"
                tempQuery.append(msBSMask);
                break;
            case VarData.STRING:
                bitSetList.add(currentMap.filterWithPattern(pat));
                if (query.substring( query.length()-2 ).equals("!=")) {
                    tempQuery.append("!");
                }
                tempQuery.append("bitSets[" + index + "].get(");
                tempQuery.append(query.toString());
                tempQuery.delete(tempQuery.length()-2, tempQuery.length());
                tempQuery.append(")");
                break;
        }
        //System.out.println(tempQuery.toString()); //TESTING
        //System.out.println(bitSetList.get(index).cardinality()); //TESTING
        query = tempQuery;

        index++;
        return true;
    }


    /**
    *   Construct the query statement against a bitset compiled into the QueryModule class
    *    built by the CompileCustomQuery class.  Only works for Step 3 (vertexLabelCount == 3)!!
    *   @param bs The name of the BitSet in QueryModule to interrogate. Compile error if this variable not present.
    */
    private void buildQueryFromBitSet(String bs) {
        StringBuilder tempQuery = new StringBuilder();

        //For now, this is designed for Genotypes, so only String types are handled
        switch (VarData.STRING) {
            case VarData.STRING:
                if (query.substring( query.length()-2 ).equals("!=")) {
                    tempQuery.append("!");
                }
                tempQuery.append(bs + "(");
                tempQuery.append(query.toString());
                tempQuery.delete(tempQuery.length()-2, tempQuery.length());
                tempQuery.append(")");
                break;
            case VarData.MULTISTRING:
            case VarData.FLOAT:
            case VarData.INTEGER:
                //Unhandled for now - kill query
                VarSifter.showError("<html>Internal Error - Can only use buildQueryFromBitSet(String) with STRING data!"
                    + "<p>Current Query will be reset.  Please Contact developer.");
                resetQuery();
                break;
        }
        query = tempQuery;
    }


    /**
    * Link vertices with "and/or/xor"
    */
    private void linkVertices(String labelString, String queryString) {
        Set<CustomVertex> picked = vv.getPickedVertexState().getPicked();
        if (picked.size() > 2 && labelString.equals("XOR") ) {
            VarSifter.showError("<html>Linking multiple elements with XOR will probably NOT work correctly!<p>" 
                              + "For example, \"true XOR true XOR true\" will be true, not false!<p>");
        }
        else if (picked.size() >= 2) {
            CustomVertex tempCV  = new CustomVertex(labelString, queryString);
            graph.addVertex(tempCV);
            for (CustomVertex cv: picked) {
                graph.addEdge(edgeCount++, tempCV, cv);
            }
        }
        else {
            VarSifter.showError("Must pick two or more elements to connect!");
        }
    }

    /**
    *   Delete selected boxes
    */
    private void deletePicked() {
        Set<CustomVertex> picked = vv.getPickedVertexState().getPicked();
        if (picked.size() > 0) {
            for (CustomVertex cv: picked) {
                Collection<Integer> childEdges = graph.getChildEdges(cv);
                Integer[] ceArray = childEdges.toArray(new Integer[childEdges.size()]);
                for (Integer ce: ceArray) {
                    graph.removeEdge(ce, false);
                }
                graph.removeVertex(cv);
            }
        }
        else {
            VarSifter.showError("Must choose a box to delete!");
        }
    }

    /**
    *   Return the built query (the if-statement)
    *
    *   @return String containing the if statement constructed by the GUI
    */
    public String getQuery() {
        return outGroup;
    }


    /**
    *   Walk the tree, assembling the query parameters at each node
    */
    private void findLeavesAndWrite(CustomVertex rootVertex, Collection<String> parentStringGroup) {
        if (graph.getChildCount(rootVertex) > 0) {
            List<String> stringGroup = new ArrayList<String>();
            String outGroup = "(";
            for (CustomVertex cv : graph.getChildren(rootVertex)) {
                findLeavesAndWrite(cv, stringGroup);
            }
            for (Iterator<String> sgi = stringGroup.iterator(); sgi.hasNext();) {
                outGroup += sgi.next();
                if (sgi.hasNext()) {
                    outGroup += rootVertex.getQuery();
                }
            }
            outGroup += ")";
            parentStringGroup.add(outGroup);

        }
        else {
            parentStringGroup.add(rootVertex.getQuery());
            if (! compPat.matcher(rootVertex.getQuery()).find()) {
                VarSifter.showError("It looks like one of the bottom elements is not a comparison: this will probably not work!");
            }
        }
    }

    private void initQuery() {
        query = new StringBuilder(64);
        vertexLabel = new StringBuilder(64);
        outText.setText("");
    }

    private void finalizeQuery() {
        Collection<CustomVertex> roots = graph.getRoots();

        if (roots.size() == 1) {
            List<String> stringGroup = new ArrayList<String>();
            outGroup = "";
            findLeavesAndWrite(roots.iterator().next(), stringGroup);
            for (Iterator<String> sgi = stringGroup.iterator(); sgi.hasNext(); ) {
                outGroup += sgi.next();
                if (sgi.hasNext()) {
                    outGroup += roots.iterator().next();
                }
            }
            outText.setText(outGroup);
            vdat.setCustomQuery(outGroup);
            vdat.setCustomBitSet(bitSetList.toArray(new BitSet[bitSetList.size()]));
            if (gui != null) {
                gui.setCBoxChecked(gui.CUSTOM, true);
                gui.toFront();
            }
        }
        else {
            VarSifter.showError("The query is disconnected!  Please make sure all the parts are connected as one unit!");
        }
    }

    private void redrawGraph() {
        Collection<CustomVertex> cv = graph.getVertices();
        int maxWidth = 0;
        for (CustomVertex s: cv) {
            int width = vv.getRenderContext().getVertexShapeTransformer().transform(s).getBounds().width;
            if (width >= maxWidth) {
                maxWidth = width;
            }
        }
        vv.setGraphLayout(new TreeLayout<CustomVertex,Integer>(graph, (maxWidth + 5), 80));
        vv.repaint();
    }


    /**
    *   Use serialization to read graph object file
    */
    private void readGraph() {
        try {
            File df = new File(vdat.dataFile);
            File queryFile;
            JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
            fc.setDialogTitle("Open Query");
            int fcReturnVal = fc.showOpenDialog(this);
            if (fcReturnVal == JFileChooser.APPROVE_OPTION) {
                queryFile = fc.getSelectedFile();
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(queryFile));
                String queryOfFile = (String)ois.readObject();
                System.out.println("Read query for data " + queryOfFile);
                if ( queryOfFile.equals(df.getName()) ) {
                    JOptionPane.showMessageDialog(this, "<html>The loaded query appears to match this data file by name." 
                        + "<p>However, if the query didn't really come from this EXACT data file, you will get incorrect results!!!");
                    graph = (DelegateForest<CustomVertex,Integer>)ois.readObject();
                    redrawGraph();
                } 
                else {
                    VarSifter.showError("<html>It looks like the query you loaded was saved from a different data file: " +
                        queryOfFile + " <p>Refusing to load, as the query results will be incorrect.");
                }
                ois.close();
            }
            else {
                System.out.println("No file selected");
            }
        }
        catch (IOException ioe) {
            System.out.println(ioe.toString());
        }
        catch (ClassNotFoundException e) {
            System.out.println(e.toString());
        }
    }


    /**
    *   Use serialization to save graph
    */
    private void writeGraph() {
        try {
            File df = new File(vdat.dataFile);
            File queryFile;
            int ovwResult = JOptionPane.YES_OPTION;
            JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
            fc.setDialogTitle("Save Query As");
            int fcReturnVal = fc.showSaveDialog(this);
            if (fcReturnVal == JFileChooser.APPROVE_OPTION) {
                queryFile = fc.getSelectedFile();

                if (queryFile.exists()) {
                    ovwResult = JOptionPane.showConfirmDialog(null, queryFile.getAbsolutePath() + "already exists.  " +
                        "Do you want to overwrite it?", "Overwrite Warning", 
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                }

                if (ovwResult == JOptionPane.YES_OPTION) {
                    ObjectOutputStream ow = new ObjectOutputStream(new FileOutputStream(queryFile));
                    ow.writeObject(df.getName());
                    ow.writeObject(graph);
                    ow.close();
                    System.out.println("Saved query for data " + df.getName());
                }
                else {
                    System.out.println("Query file not written.");
                }
            }
            else {
                System.out.println("Query file not written.");
            }
        }
        catch (IOException ioe) {
            System.out.println(ioe.toString());
        }
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
            VarSifter.showError("Couldn't find icon path: " + path);
            return null;
        }
    }

}
