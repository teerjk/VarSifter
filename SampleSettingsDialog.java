import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/** 
*  Creates a window to handle GUI for sample settings (aff, norm, case, control)
*    In addition to checkboxes, can also import a file of sample_name - setting pairs.
*/
public class SampleSettingsDialog {

    public static final String sampleKey = ".NA";

    private static final String[] colNames  = { "Sample",
                                                "Aff/Norm Pair Group",
                                                "Affected",
                                                "Normal",
                                                "Case",
                                                "Control"
                                              };
    private static final int editableCols = 4;

    private static final String[] colNameKeys = { "",
                                                 "",
                                                 "aff",
                                                 "norm",
                                                 "case",
                                                 "control"
                                               };


    private Object[][] data;    // (samples, sample flags)
    private VarData vdat;
    private String[] sampleNames;
    private String[] sampleNamesOrig;
    private int[] affAt;
    private int[] normAt;
    private int[] caseAt;
    private int[] controlAt;
    private Set<Integer> affAtSet;
    private Set<Integer> normAtSet;
    private Set<Integer> caseAtSet;
    private Set<Integer> controlAtSet;

    private List<Set<Integer>> oldSettings;

    /** 
    *   Constructor
    *
    *   @param inVdat VarData Object
    */
    public SampleSettingsDialog(VarData inVdat) {
        this.vdat = inVdat;
        this.sampleNames = inVdat.sampleNames;
        this.sampleNamesOrig = inVdat.sampleNamesOrig;
        this.affAt = inVdat.affAt;
        this.normAt = inVdat.normAt;
        this.caseAt = inVdat.caseAt;
        this.controlAt = inVdat.controlAt;

        affAtSet = loadSet(affAt);
        normAtSet = loadSet(normAt);
        caseAtSet = loadSet(caseAt);
        controlAtSet = loadSet(controlAt);

        oldSettings = Arrays.asList( new HashSet<Integer>(),
                                     new HashSet<Integer>(),
                                     affAtSet,
                                     normAtSet,
                                     caseAtSet,
                                     controlAtSet
                                   );
        

        int colNum = colNames.length;
        data = new Object[sampleNames.length][colNames.length];
        int affNum = 1;
        int normNum = 1;

        for (int i=0; i < sampleNames.length; i++) {
            
            //Sample Name
            data[i][0] = sampleNames[i];

            //Aff/Norm Pair Group
            data[i][1] = 0; //Default - not aff or norm

            ////Set default checkbox status defining the type of sample
            for (int j=2; j<colNames.length; j++) {
                boolean isSet = oldSettings.get(j).contains(i);
                data[i][j] = (isSet) ? new JCheckBox("", true) : new JCheckBox();
                
                if (isSet) {
                    switch (j) {
                        case 2:
                            data[i][1] = affNum++;
                            break;
                        case 3:
                            data[i][1] = normNum++;
                            break;
                    }
                }
            }
        }

    }

    /**
    *   Prepare and draw dialog box
    */
    public void runDialog() {

        JOptionPane oPane = new JOptionPane();
        JTable mapTable = getTable();
        JScrollPane s = new JScrollPane(mapTable);
        oPane.setMessage(s);
        oPane.setOptionType(JOptionPane.OK_CANCEL_OPTION);

        JDialog d = oPane.createDialog(null, "Sample Settings");
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.setResizable(true);
        d.setVisible(true);
        d.dispose();

        int settingOpt;
        Object oR = oPane.getValue();
        //System.out.println(oR);
        if (oR == null) {
            settingOpt = JOptionPane.CANCEL_OPTION;
        }
        else if (oR instanceof Integer) {
            settingOpt = (Integer)oR;
        }
        else {
            settingOpt = JOptionPane.CANCEL_OPTION;
        }


        //Handle input data
        if (settingOpt == JOptionPane.OK_OPTION) {
            DialogTableModel dtm = (DialogTableModel)mapTable.getModel();
            //System.out.println("OK: " + ((JCheckBox)dtm.getValueAt(0,2)).isSelected());
            
            if (! checkUserInput(dtm) ) {
                VarSifter.showError("<html>There is an error in your Sample Settings selections. <p>" +
                    "A sample can only be Affected or Normal, not both, and a sample can only be <p>" +
                    "Case or Control, not both.  Additionally, for every Affected sample, there must be <p>" +
                    "a Normal, and vice versa; they only work in pairs");
                return;
            }

            List<Integer> affAtList = new ArrayList<Integer>();
            List<Integer> normAtList = new ArrayList<Integer>();
            List<Integer> caseAtList = new ArrayList<Integer>();
            List<Integer> controlAtList = new ArrayList<Integer>();
            
            List<List<Integer>> newSettings = Arrays.asList( new ArrayList<Integer>(),
                                                             new ArrayList<Integer>(),
                                                             affAtList,
                                                             normAtList,
                                                             caseAtList,
                                                             controlAtList
                                                           );

            for (int i=0; i<dtm.getRowCount(); i++) {
                Integer iInt = Integer.valueOf(i);
                int origIndex = i * VarData.S_FIELDS;

                for (int j=2; j<colNames.length; j++) {
                    if ( ((JCheckBox)dtm.getValueAt(i,j)).isSelected() ) {
                        newSettings.get(j).add(iInt);
                        if (! oldSettings.get(j).contains(iInt) ) {
                            sampleNames[i] = sampleNames[i].replaceFirst(sampleKey, colNameKeys[j] + sampleKey);
                            for (int k=0; k<VarData.S_FIELDS; k++) {
                                sampleNamesOrig[origIndex + k] = 
                                    sampleNamesOrig[origIndex + k].replaceFirst(sampleKey, colNameKeys[j] + sampleKey);
                            }
                        }
                    }
                    else { 
                        if ( oldSettings.get(j).contains(iInt) ) {
                            sampleNames[i] = sampleNames[i].replaceFirst(colNameKeys[j], "");
                            for (int k=0; k<VarData.S_FIELDS; k++) {
                                sampleNamesOrig[origIndex + k] = 
                                    sampleNamesOrig[origIndex + k].replaceFirst(colNameKeys[j], "");
                            }
                        }
                    }
                }

            }

            //Loop through ArrayLists, regenerate vdat int arrays

            //Affected
            vdat.affAt = new int[affAtList.size()];
            loadIntArray(vdat.affAt, affAtList);

            //Normal
            vdat.normAt = new int[normAtList.size()];
            loadIntArray(vdat.normAt, normAtList);

            //Case
            vdat.caseAt = new int[caseAtList.size()];
            loadIntArray(vdat.caseAt, caseAtList);

            //Control
            vdat.controlAt = new int[controlAtList.size()];
            loadIntArray(vdat.controlAt, controlAtList);


        }
        else {
            System.out.println("Sample settings not changed!");
        }

    }

    /**
    *   Initiate JTable to place in dialog
    *   @return The JTable with data and listener
    */
    private JTable getTable() {
        JTable t = new JTable(new DialogTableModel(data, colNames, editableCols));
        t.setDefaultRenderer(JCheckBox.class, new JCheckBoxRenderer());
        t.setDefaultEditor(JCheckBox.class, new JCheckBoxCellEditor());
        return t;
    }

    
    /**
    *   Load an <Integer> HashSet using an array of ints
    *   @param intList Array of ints
    *   @return The array of ints converted to a HashSet of Integers.  Order will be lost.
    */
    private HashSet<Integer> loadSet(int[] intList) {
        HashSet<Integer> tempHS = new HashSet<Integer>(intList.length);
        for (int i : intList) {
            tempHS.add(i);
        }
        return tempHS;
    }

    /**
    *   Load an int array using an <Integer> List
    *   @param intList List of Integers
    *   @return Array of ints; order should be preserved.
    */
    private void loadIntArray(int[] intArray, List<Integer> intList) {
        if (intArray.length != intList.size()) {
            System.out.println("ERROR! Internal error in Sample Settings module when converting list to array");
            System.exit(1);
        }

        for (int i=0; i<intArray.length; i++) {
            intArray[i] = ((Integer)intList.get(i)).intValue();
        }

    }

    /**
    *   Ensure user input is appropriate for the sample settings.
    *   @param dtmTemp The DialogTableModel holding user input
    *   @return True is user input is ok, false otherwise.
    */
    private boolean checkUserInput(DialogTableModel dtmTemp) {
        int normCount = 0;
        int affCount = 0;
        for (int i=0; i<dtmTemp.getRowCount(); i++) {
            boolean[] checked = new boolean[colNames.length];
            for (int j=2; j<colNames.length; j++) {
                checked[j] = ((JCheckBox)dtmTemp.getValueAt(i,j)).isSelected();
                switch (j) {
                    case 2:
                        if (checked[2]) {
                            affCount++;
                        }
                        break;
                    case 3:
                        if (checked[3]) {
                            normCount++;
                        }
                        break;
                }
            }

            //Aff and Norm
            if (checked[2] && checked[3]) {
                System.out.println("Sample " + sampleNames[i] + " cannot be Affected and Normal!");
                return false;
            }
            
            //Case and Control
            if (checked[4] && checked[5]) {
                System.out.println("Sample " + sampleNames[i] + " cannot be Case and Control!");
                return false;
            }
        }    

        if (affCount != normCount) {
            System.out.println("Number of Affected (" + affCount + ") and Normal (" + normCount +
                ") samples must be the same!");
            return false;
        }

        //OK
        return true;

    }

}
