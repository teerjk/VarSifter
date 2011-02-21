import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.HashMap;
import java.util.Set;
import java.util.ArrayList;
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
    HashMap<String, Integer> dataTypeAt;
    int annoSize;
    DelegateForest<CustomVertex, Integer> graph;

    private int lastQueryIndex = 0;
    private Integer edgeCount = 0;
    final int SAMPLE = 0;
    final int ACTION = 1;
    final int ANNOT_ACTION = 2;
    final int ANNOT_COMP = 3;
    final int LOGICAL = 4;

    private final static Pattern compPat = Pattern.compile("[<>=&]");

    private boolean isAnnotQuery = false;

    String[] fixedSamples = { "Homozygous Reference",
                              "Homozygous Non-reference",
                              "NA"
                            };
    private JList sampleList;
    private JList fixedSampleList = new JList(fixedSamples);
    private JList annotList;
    private JList stringAnnotList = new JList();
    
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

    private JButton andButton = new JButton("And");
    private JButton orButton = new JButton("Or");
    private JButton[] logicButtons = {andButton, orButton};

    private JButton apply = new JButton("Apply Query");
    private JButton delete = new JButton("Delete Selected");
    private JButton clear = new JButton("Clear All");
    private JComboBox modeBox;
    
    private JTextField outText = new JTextField();
    private JTextField inAnnotNumber = new JTextField();
    private JTextField inStringPattern = new JTextField();
    private StringBuilder query;
    private StringBuilder vertexLabel;
    private int vertexLabelCount = 1;   //Use this to know where we are in the query assembly process
    private String outGroup;
    
    private TreeLayout<CustomVertex,Integer> layout;
    private VisualizationViewer<CustomVertex,Integer> vv;
    

    /**
    *   Initializer
    *
    *   @param inVdat VarData Object
    */
    public CustomQueryView(VarData inVdat) {
        vdat = inVdat;
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

        String out = new String("");
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
        
        JPanel runPane = new JPanel();
        runPane.setLayout(new BoxLayout(runPane, BoxLayout.X_AXIS));
        runPane.setBorder(BorderFactory.createEmptyBorder(7,7,7,7));
        runPane.add(apply);
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
        extraPane.add(outText);
        extraPane.add(Box.createVerticalGlue());

        exactMatch.addActionListener(this);
        noMatch.addActionListener(this);
        annotExactMatch.addActionListener(this);
        annotNoMatch.addActionListener(this);
        eqButton.addActionListener(this);
        gtButton.addActionListener(this);
        ltButton.addActionListener(this);
        applyAnnotComp.addActionListener(this);
        applyStringPattern.addActionListener(this);
        apply.addActionListener(this);
        delete.addActionListener(this);
        clear.addActionListener(this);
        andButton.addActionListener(this);
        orButton.addActionListener(this);

        sampleList.addListSelectionListener(this);
        fixedSampleList.addListSelectionListener(this);
        annotList.addListSelectionListener(this);
        stringAnnotList.addListSelectionListener(this);

        // Mask buttons
        enableButtons(new int[] {ACTION,ANNOT_ACTION,ANNOT_COMP,LOGICAL}, false);
        applyAnnotComp.setEnabled(false);
        stringAnnotList.setEnabled(false);
        inAnnotNumber.setEnabled(false);


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

        sampleControlPane.add(new JLabel("Samples:"));
        sampleControlPane.add(samplePane);
        sampleControlPane.add(Box.createRigidArea(new Dimension(0,5)));
        sampleControlPane.add(new JLabel("Genotypes:"));
        sampleControlPane.add(fixedPane);
        sampleControlPane.add(Box.createRigidArea(new Dimension(0,5)));
        sampleControlPane.add(new JLabel("Actions:"));
        sampleControlPane.add(actionPane);
        sampleControlPane.add(Box.createRigidArea(new Dimension(0,5)));
        sampleControlPane.add(new JLabel("Logical:"));
        sampleControlPane.add(connectPane);

        JPanel annotControlPane = new JPanel();
        annotControlPane.setLayout(new BoxLayout(annotControlPane, BoxLayout.Y_AXIS));
        JScrollPane annotControlScroller = new JScrollPane(annotControlPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                                   JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        annotControlScroller.setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                                    BorderFactory.createEmptyBorder(2,2,2,5)
                                    ));

        annotControlPane.add(new JLabel("Annotations:"));
        annotControlPane.add(annotPane);
        annotControlPane.add(Box.createRigidArea(new Dimension(0,5)));
        annotControlPane.add(new JLabel("Annotation Values:"));
        annotControlPane.add(stringAnnotPane);
        annotControlPane.add(Box.createRigidArea(new Dimension(0,5)));
        annotControlPane.add(new JLabel("Annotation Actions:"));
        annotControlPane.add(annotActionPane);
        annotControlPane.add(Box.createRigidArea(new Dimension(0,5)));
        annotControlPane.add(new JLabel("Annot. Numeric Actions:"));
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

        if (es == apply) {
            finalizeQuery();
        }
        else if (es == delete) {
            deletePicked();
        }
        else if (es == clear) {
            initQuery();
            graph = new DelegateForest<CustomVertex,Integer>();
        }
        else if (es == exactMatch) {
            buildQueryVertex("==","==");
        }
        else if (es == noMatch) {
            buildQueryVertex("!=","!=");
        }
        else if (es == annotExactMatch) {
            buildQueryVertex("equals","==");
        }
        else if (es == annotNoMatch) {
            buildQueryVertex("not equals","!=");
        }
        else if (es == eqButton) {
            buildQueryVertex("==","==");
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
            //get indices matching this regex
        }
        else if (es == andButton) {
            linkVertices("And", " && ");
        }
        else if (es == orButton) {
            linkVertices("Or", " || ");
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
                    case 1: //HomNonRef
                        buildQueryVertex(fixedSamples[1], "homNonRefGen");
                        break;
                    case 2: //NA
                        buildQueryVertex(fixedSamples[2], "NA_Allele");
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
                    fixedSampleList.setEnabled(toEnabled);
                    sampleList.setEnabled(toEnabled);
                    break;
                case ACTION:
                    group = actionButtons;
                    break;
                case ANNOT_ACTION:
                    group = annotActionButtons;
                    break;
                case ANNOT_COMP:
                    group = annotCompButtons;
                    break;
                case LOGICAL:
                    group = logicButtons;
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
                    enableButtons(new int[] {ACTION}, true);
                }
                enableButtons(new int[] {SAMPLE}, false);
                annotList.setEnabled(false);
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
                            stringAnnotList.setEnabled(true);
                            break;
                        case VarData.FLOAT:
                        case VarData.INTEGER:
                            inAnnotNumber.setEnabled(true);
                            applyAnnotComp.setEnabled(true);
                            break;
                    }
                    
                }
                else {
                    enableButtons(new int[] {ACTION}, false);
                    enableButtons(new int[] {SAMPLE}, true);
                }
                query.append(queryString);
                break;
            case 3:
                vertexLabel.append(labelString);
                query.append(queryString);
                if (isAnnotQuery) {
                    switch (currentMap.getDataType()) {
                        case VarData.MULTISTRING:
                            query.append(")) > 0");
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
                
                enableButtons(new int[] {LOGICAL, SAMPLE}, true);
                annotList.clearSelection();
                stringAnnotList.clearSelection();
                isAnnotQuery = false;
                annotList.setEnabled(true);
                stringAnnotList.setEnabled(false);
                applyAnnotComp.setEnabled(false);
                vertexLabelCount = 1;
                redrawGraph();
                initQuery();
                break;
        }
    }

    /**
    *   Use regex to get indices of matching entries, return final StringBuilder
    *   @param regex The regex to search with
    *   @param query The StringBuilder generated thus far
    *   @return StringBuilder to use for this vertex
    */
    private StringBuilder buildQueryFromRegex(String regex, StringBuilder query) {
        //Do stuff here
    }


    /**
    * Link vertices with "and/or"
    */
    private void linkVertices(String labelString, String queryString) {
        Set<CustomVertex> picked = vv.getPickedVertexState().getPicked();
        if (picked.size() >= 2) {
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
                Integer[] ceArray = new Integer[childEdges.size()];
                childEdges.toArray(ceArray);
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
            ArrayList<String> stringGroup = new ArrayList<String>();
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
            //if (!rootVertex.getQuery().contains("=")) {
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
            ArrayList<String> stringGroup = new ArrayList<String>();
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

}
