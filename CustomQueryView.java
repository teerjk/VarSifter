import java.awt.*;
import javax.swing.*;
import java.util.HashMap;
import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.event.*;

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
//import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
//import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.PickingGraphMousePlugin;


/* ********************
*   A window to handle GUI for custom query string generation
*     -This class will generate an if-statement for sifting data
*  ********************
*/

public class CustomQueryView extends JPanel implements ActionListener {
    String[] sampleNames;
    private VarData vdat;
    HashMap<String, Integer> dataTypeAt;
    int annoSize;
    DelegateForest<CustomVertex, Integer> graph;

    private int lastQueryIndex = 0;
    private Integer edgeCount = 0;
    final int SAMPLE = 0;
    final int ACTION = 1;
    final int LOGICAL = 2;

    private JButton[] sampleButtons;
    
    private JButton exactMatch = new JButton("Exactly Matches");
    private JButton noMatch = new JButton("Does Not Match");
    private JButton[] actionButtons = {exactMatch, noMatch};

    private JButton andButton = new JButton("And");
    private JButton orButton = new JButton("Or");
    private JButton[] logicButtons = {andButton, orButton};

    private JButton apply = new JButton("Apply");
    private JButton delete = new JButton("Delete Selected");
    private JButton clear = new JButton("Clear All");
    
    private JTextField outText = new JTextField();
    private StringBuilder query;
    private StringBuilder vertexLabel;
    private int vertexLabelCount = 1;
    private String outGroup;
    
    private TreeLayout<CustomVertex,Integer> layout;
    private VisualizationViewer<CustomVertex,Integer> vv;
    

    //Initializer
    public CustomQueryView(VarData inVdat) {
        vdat = inVdat;
        sampleNames = vdat.returnSampleNames();
        dataTypeAt = vdat.returnDataTypeAt();
        annoSize = dataTypeAt.size();
        sampleButtons = new JButton[sampleNames.length];

        graph = new DelegateForest<CustomVertex,Integer>();
        layout = new TreeLayout<CustomVertex,Integer>(graph);
        //System.out.println(layout.getSize().toString());
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

        PluggableGraphMouse graphMouse = new PluggableGraphMouse();
        graphMouse.add(new PickingGraphMousePlugin());
        vv.setGraphMouse(graphMouse);

        //DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();
        //vv.setGraphMouse(graphMouse);
        //graphMouse.setMode(ModalGraphMouse.Mode.PICKING);

        //vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        //vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
        
        String out = new String("");
        for (int i=0; i<sampleNames.length; i++) {
            sampleButtons[i] = new JButton(sampleNames[i]);
        }

        initQuery();
        initTable();
    }


    //Initialize GUI
    private void initTable() {
        //this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setLayout(new BorderLayout());
        //JScrollPane textScroll = new JScrollPane(outText,
        //        ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
        //        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

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
        runPane.setLayout(new BoxLayout(runPane, BoxLayout.Y_AXIS));
        runPane.setBorder(BorderFactory.createEmptyBorder(7,7,7,7));
        runPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        runPane.add(apply);
        runPane.add(Box.createRigidArea(new Dimension(0,5)));
        runPane.add(delete);
        runPane.add(Box.createRigidArea(new Dimension(0,5)));
        runPane.add(clear);
        runPane.add(Box.createRigidArea(new Dimension(0,5)));
        outText.setMaximumSize(new Dimension( outText.getMaximumSize().width, 
                                              outText.getMinimumSize().height));
        runPane.add(outText);

        exactMatch.addActionListener(this);
        noMatch.addActionListener(this);
        apply.addActionListener(this);
        delete.addActionListener(this);
        clear.addActionListener(this);
        andButton.addActionListener(this);
        orButton.addActionListener(this);

        // Mask buttons
        enableButtons(new int[] {ACTION,LOGICAL}, false);
        
        JPanel controlPane = new JPanel();
        controlPane.setLayout(new BoxLayout(controlPane, BoxLayout.Y_AXIS));

        controlPane.add(new JLabel("Samples:"));
        controlPane.add(samplePane);
        controlPane.add(Box.createRigidArea(new Dimension(0,5)));
        controlPane.add(new JLabel("Actions:"));
        controlPane.add(actionPane);
        controlPane.add(Box.createRigidArea(new Dimension(0,5)));
        controlPane.add(new JLabel("Logical:"));
        controlPane.add(connectPane);
        controlPane.add(Box.createRigidArea(new Dimension(0,5)));
        controlPane.add(runPane);
        add(controlPane, BorderLayout.LINE_START);
        add(vv, BorderLayout.CENTER);
        //add(textScroll, BorderLayout.PAGE_END);
    }


    public void actionPerformed(ActionEvent e) {
        Object es = e.getSource();
        //System.out.println(es);

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
            buildQueryVertex("==",".equals(");
        }
        else if (es == noMatch) {
            query.insert(0,"!");
            buildQueryVertex("!=",".equals(");
        }
        else if (es == andButton) {
            linkVertices("And", " && ");
        }
        else if (es == orButton) {
            linkVertices("Or", " || ");
        }
        else {
            for (int i=0; i<sampleButtons.length; i++) {
                if (es == sampleButtons[i]) {
                    String labelTemp = sampleButtons[i].getText();
                    labelTemp = labelTemp.replaceFirst("\\.NA$", "");
                    String queryTemp = ("allData[i][" + ((i * VarData.S_FIELDS) + annoSize) + "]");
                    buildQueryVertex(labelTemp, queryTemp);
                    //graph.addVertex(sampleButtons[i].getText());
                    break;
                }
            }
        }

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

    private void enableButtons(int[] buttonGroup, boolean toEnabled) {
        for (int bG: buttonGroup) {
            switch (bG) {
                case SAMPLE: 
                    for (JButton b: sampleButtons) {
                        b.setEnabled(toEnabled);
                    }
                    break;
                case ACTION:
                    for (JButton b: actionButtons) {
                        b.setEnabled(toEnabled);
                    }
                    break;
                case LOGICAL:
                    for (JButton b: logicButtons) {
                        b.setEnabled(toEnabled);
                    }
                    break;
            }
        }
    }

    private void buildQueryVertex(String labelString, String queryString) {
        switch (vertexLabelCount) {
            case 1:
                initQuery();
                vertexLabel.append("<html>" + labelString + "<p>");
                query.append(queryString);
                enableButtons(new int[] {ACTION}, true);
                enableButtons(new int[] {SAMPLE}, false);
                vertexLabelCount++;
                outText.setText(vertexLabel.toString());
                break;
            case 2:
                vertexLabel.append(labelString + "<p>");
                query.append(queryString);
                enableButtons(new int[] {ACTION}, false);
                enableButtons(new int[] {SAMPLE}, true);
                vertexLabelCount++;
                outText.setText(vertexLabel.toString());
                break;
            case 3:
                vertexLabel.append(labelString);
                query.append(queryString + "))");
                query.insert(0, "(");
                graph.addVertex(new CustomVertex(vertexLabel.toString(), query.toString()));
                enableButtons(new int[] {LOGICAL}, true);
                vertexLabelCount = 1;
                initQuery();
                break;
        }
    }

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

    public String getQuery() {
        return outGroup;
    }

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
            if (!rootVertex.getQuery().contains("equals")) {
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
}
