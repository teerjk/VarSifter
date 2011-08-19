import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;


/**
*   A class displaying a table dialog. User selections are returned as a HashMap.
*/
public class InputTableDialog {

    public static final String[] addedKeys      = { "MultiAllele",
                                             "Sub-delimiter"
                                           };
    //private static Object[] addedObjects = { new JCheckBox(),
    //                                         new String()
    //                                       };

    private Object[][] data;
    private String[] colNames = new String[0];
    private int pKeyIndex;  //The index of the primary key in colNames[] (and data[][x])
    private Map<String, Map<String, String>> inHash = new HashMap<String, Map<String, String>>();
    private List<String> iList; //Primary keys, the ID fields
    private String inFile = "";
    private final static String vcfConfigExt = ".vcf_config";

    /**
    *   Constructor using a hash of hashes.
    *   @param in Hash of hashes, ideally ID->{key->value}
    *   @param inFile Filename of VCF file
    */
    public InputTableDialog(Map<String, Map<String, String>> in, String inFile) {
        
        this.inFile = inFile;
        inHash = new HashMap<String, Map<String, String>>(in);
        data = new Object[in.size()][];
        int i = 0;
        iList = new ArrayList<String>(in.keySet());
        Collections.sort(iList);

        //Primary map
        for (String iKey : iList) {
            Map<String, String> tempMap = in.get(iKey);
            int colNum = tempMap.size() + addedKeys.length;
            data[i] = new Object[colNum];
            if (i == 0) {
                colNames = new String[colNum];
            }

            int j = 0;
            List<String> jList = new ArrayList<String>(tempMap.keySet()); 
            Collections.sort(jList);
            pKeyIndex = jList.indexOf("ID");
            
            //Secondary map
            for (String jKey : jList) {
                if (i == 0) {
                    colNames[j] = jKey;
                }
                data[i][j] = tempMap.get(jKey);
                j++;
            }

            //Add new key->value pairs
            if (i == 0) {
                for (int k=0; k<addedKeys.length; k++) {
                    colNames[j+k] = addedKeys[k];
                }
            }
            data[i][j] = new JCheckBox();
            data[i][j+1] = "";

            if (tempMap.containsKey("Number") && !tempMap.get("Number").equals(".")) {
                ((JCheckBox)data[i][j]).setEnabled(false);
            }
            if (tempMap.containsKey("ID") && tempMap.get("ID").equals("AF")) {
                ((JCheckBox)data[i][j]).setSelected(true);
            }
            i++;
        }
    }

    /**
    *   Prepare and draw dialog box
    */
    public Map<String, Map<String, String>> runDialog() {
        File f = new File(inFile + vcfConfigExt);
        String cMessage = "VCF Config found. Should it be used to parse VCF file?";
        
        if (f.exists() 
            && JOptionPane.showConfirmDialog(null, cMessage, null, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) 
               == JOptionPane.YES_OPTION) {

            String cLine = "";
            try {
                BufferedReader cReader = new BufferedReader(new FileReader(f));
                while ((cLine = cReader.readLine()) != null) {
                    String[] l = cLine.split("\t");
                    String[] newKeys = Arrays.copyOfRange(l, 1, l.length);
                    for (String k : newKeys) {
                        String[] pair = k.split("=", 2);
                        inHash.get(l[0]).put(pair[0], pair[1]);
                    }
                }
                cReader.close();
            }
            catch (IOException ioe) {
                VarSifter.showError("Error Parsing VCF config: " + f.getAbsolutePath() + " " + ioe.toString());
            }
        }
        else {

            JOptionPane oPane = new JOptionPane();
            JTable mapTable = getTable(oPane);
            oPane.setInputValue(mapTable.getModel());
            JScrollPane s = new JScrollPane(mapTable); 
                                            //ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                            //ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
            oPane.setMessage(s);
            oPane.setOptionType(JOptionPane.DEFAULT_OPTION);

            JDialog d = oPane.createDialog(null, "File Settings");
            d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            d.setResizable(true);
            d.setVisible(true);
            DialogTableModel dtm = (DialogTableModel)oPane.getInputValue();
            d.dispose();
            
            //Handle data, return HashMap
            int colCount = dtm.getColumnCount();
            for (int i=0; i<dtm.getRowCount(); i++) {
                Map<String, String> tempMap = inHash.get( (String)dtm.getValueAt(i,pKeyIndex) );
                tempMap.put("MultiAllele",  Boolean.toString(((JCheckBox)dtm.getValueAt(i,colCount-2)).isSelected()) );
                tempMap.put("Sub-delimiter", (String)dtm.getValueAt(i,colCount-1) );
            }

            int saveOpt = JOptionPane.showConfirmDialog(null, "Save config for this VCF file?", "Config", 
                                                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            if (saveOpt == JOptionPane.YES_OPTION) {
                saveVCFConfig();
            }
            else {
                System.out.println("VCF config NOT saved");
            }
        }

        return inHash;
    }

    
    /**
    *    Initiate JTable to place in dialog
    *    @param opt JOptionPane to store data in upon change
    *    @return The JTable with data and listener
    */
    private JTable getTable(final JOptionPane opt) {
        JTable t = new JTable(new DialogTableModel(data, colNames));
        t.setDefaultRenderer(JCheckBox.class, new JCheckBoxRenderer());
        t.setDefaultEditor(JCheckBox.class, new JCheckBoxCellEditor());
        t.getModel().addTableModelListener(new TableModelListener() {
                                        public void tableChanged(TableModelEvent e) {
                                            opt.setInputValue((DialogTableModel)e.getSource());
                                            return;
                                        }
        });
        return t;
    }

    /**
    *   Save VCF config to a text file using inHash
    */
    private void saveVCFConfig() {
        String outFileName = inFile + vcfConfigExt;
        File outFile = new File(outFileName);
        int ovwResult = JOptionPane.YES_OPTION;

        if (outFile.exists()) {
            ovwResult = JOptionPane.showConfirmDialog(null, outFileName + " already exists.  " +
                "Do you want to overwrite it?", "Overwrite Warning",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        }

        if (ovwResult == JOptionPane.YES_OPTION) {
            try {
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));
                for (String idKey : iList) {
                    pw.print(idKey);
                    for (String aKey : addedKeys) {
                        pw.print("\t" + aKey + "=" + inHash.get(idKey).get(aKey));
                    }
                    pw.print(VarSifter.newLine);
                }
                pw.close();
                if (pw.checkError()) {
                    VarSifter.showError("Error Detected writing VCF config file! File NOT saved!");
                }
                else {
                    System.out.println("VCF config written: " + outFileName);
                }
            }
            catch (IOException ioe) {
                JOptionPane.showMessageDialog(null, "File write error: " + ioe.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else {
            System.out.println(outFileName + " not overwritten!");
        }
    }


    public static void main(String[] args) {
        String[] col = {"ID", "Number", "Description"};
        String[][] tags = { {"A", ".", "single"}, {"B", ".", "multi"}, {"C", "1", "dual"}, {"AF", ".", "Allele Freq"} };
        Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();

        for (int i=0; i<tags.length; i++) {
            Map<String, String> tMap = new HashMap<String, String>();

            for (int j=0;j<tags[i].length;j++) {
                tMap.put(col[j], tags[i][j]);
            }
            map.put(tags[i][0], tMap);
        }

        InputTableDialog h = new InputTableDialog(map, null);
        map = h.runDialog();
        List<String> list = new ArrayList<String>(map.keySet());
        for (String key : list) {
            Map<String, String> temp = (Map<String, String>)map.get(key);
            System.out.println(key + " " + temp.get("Description") + " " + temp.get("Number") + " " + temp.get("MultiAllele") + " " + temp.get("Sub-delimiter"));
        }
        System.exit(0);
    }
}
