import java.awt.*;
import javax.swing.*;
import java.util.Map;


/**
*   The SampleExporter class opens a dialog to determine what types of varaints should be exported, and allows
*   retrieval of results one line at a time (one per variant location) with those samples using the getNext() method.
*/
public class SampleExporter {

    final String title = "<html>Select variant type(s).<p>Samples with variants of these types will be exported.</html>";
    final String outTitle = "Select information to output.";

    private JCheckBox homRefCB = new JCheckBox("Homozygous Reference");
    private JCheckBox homVarCB = new JCheckBox("Homozygous Variant");
    private JCheckBox hetVarCB = new JCheckBox("Heterozygous Variant");
    private JCheckBox otherCB  = new JCheckBox("Other");
    private JCheckBox nameCB   = new JCheckBox("Sample Name", true);
    private JCheckBox genCB    = new JCheckBox("Genotype");
    private JCheckBox scoreCB  = new JCheckBox("Genotype Score");
    private JCheckBox covCB    = new JCheckBox("Genotype Coverage");
    private JCheckBox[] genOptions = {homRefCB, homVarCB, hetVarCB, otherCB};
    private JCheckBox[] outOptions = {nameCB, genCB, scoreCB, covCB};
    private JTextField genScoreField = new JTextField(10);
    private JTextField genScoreCovRatioField = new JTextField(10);

    private boolean toFilter = false; //Only filter if this is true (ie, OK was clicked)

    private VarData vdat;
    private String[] names;
    private AbstractMapper[] annotMapper;
    private AbstractMapper[] sampleMapper;
    private int[][] outData;
    private int chrIndex;
    private int lfIndex;
    private int rfIndex;
    private int refIndex;
    private int varIndex;
    private int mutTypeIndex;
    private int indelIndex;
    private int NA_Allele;
    private int lastLine = 0;

    /**
    *   Constructor assigns local fields needed from VarData object; will use currently filtered variant list!
    *   Then, opens dialog to determine which variant types are desired.
    *   @param inVdat VarData object
    */
    public SampleExporter(VarData inVDat) {
        this.vdat = inVDat;
        names = vdat.returnSampleNames();
        Map<String, Integer> dataTypeAt = vdat.returnDataTypeAt();
        annotMapper = vdat.returnAnnotMap();
        sampleMapper = vdat.returnSampleMap();
        outData = vdat.returnData();
        chrIndex = dataTypeAt.get("Chr");
        lfIndex = dataTypeAt.get("LeftFlank");
        rfIndex = dataTypeAt.get("RightFlank");
        refIndex = dataTypeAt.get("ref_allele");
        varIndex = dataTypeAt.get("var_allele");
        mutTypeIndex = dataTypeAt.get("muttype");
        indelIndex = annotMapper[mutTypeIndex].getIndexOf("INDEL");
        NA_Allele = sampleMapper[0].getIndexOf("NA");

        VarSifter.setJComponentSize(genScoreField);
        genScoreField.setText(Integer.toString(VarSifter.SCORE_THRESH));
        VarSifter.setJComponentSize(genScoreCovRatioField);
        genScoreCovRatioField.setText(Float.toString(VarSifter.SCORE_COV_THRESH));
        JPanel scorePane = new JPanel();
        scorePane.setLayout(new BoxLayout(scorePane, BoxLayout.X_AXIS));
        scorePane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scorePane.add( new JLabel("Genotype Score Threshold:  "));
        scorePane.add(genScoreField);
        JPanel scoreCovPane = new JPanel();
        scoreCovPane.setLayout(new BoxLayout(scoreCovPane, BoxLayout.X_AXIS));
        scoreCovPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scoreCovPane.add( new JLabel("Genotype Score / Coverage Ratio Threshold:  "));
        scoreCovPane.add(genScoreCovRatioField);
        JPanel filtPane = new JPanel();
        filtPane.setLayout(new BoxLayout(filtPane, BoxLayout.Y_AXIS));
        filtPane.add(scorePane);
        filtPane.add(scoreCovPane);

        Object[] params = {title, genOptions, new JSeparator(SwingConstants.HORIZONTAL), filtPane, 
                           new JSeparator(SwingConstants.HORIZONTAL), outTitle, outOptions};

        int opt = JOptionPane.showConfirmDialog(null, params, "Sample Export", JOptionPane.OK_CANCEL_OPTION,
                                                JOptionPane.QUESTION_MESSAGE);
        if (opt == JOptionPane.OK_OPTION) {
            toFilter = true;
        }
    }

    
    /**
    *   Returns a header indicating which types of variants were desired (which checkboxes were checked)
    *   @return A String comment with the checked boxes
    */
    public String getHeader() {
        StringBuilder out = new StringBuilder("#Samples with the following variant types:\t");
        for (JCheckBox cb : genOptions) {
            if (cb.isSelected()) {
                out.append(cb.getText() + "\t");
            }
        }
        out.deleteCharAt(out.length() - 1);
        out.append(VarSifter.newLine);
        out.append("#Score_threshold: " + genScoreField.getText() + " score_coverage: " + 
            genScoreCovRatioField.getText());
        return out.toString();
    }


    /**
    *   Returns the samples with previously selected genotype type(s) for the next variant line from the FILTERED vars.
    *   Returns null when nothing left to do (or nothing to do in first place.)
    *   Therefore, its best to call this function as if reading a file:
    *   SampleExporter se = new SampleExporter(Vardata v);
    *   String outLine;
    *   while ((outLine = se.getNextVariant()) != null) {
    *       //do stuff
    *   }
    *   @return A tab-delim String with location, semi-colon delimited info for each sample.
    */
    public String getNextVariant() {
        if (! toFilter || lastLine >= outData.length) {
            return null;
        }

        int scoreT;
        float scoreCovT;

        try {
            scoreT = Integer.parseInt(genScoreField.getText());
            scoreCovT = Float.parseFloat(genScoreCovRatioField.getText());
        }
        catch (NumberFormatException nfe) {
            VarSifter.showError("<html>You have entered an inappropriate number in Sample Export window!<p>" +
                      "Please use an integer for Genotype Score, and a floating point number for " +
                      "Score/Cov Ratio.</html");
            System.out.println(nfe);
            return null;
        }

        String refAllele = annotMapper[refIndex].getString(outData[lastLine][refIndex]);
        String varAllele = annotMapper[varIndex].getString(outData[lastLine][varIndex]);
        int homRefGen;
        int homVarGen;
        int muttype = outData[lastLine][mutTypeIndex];
        if (muttype == indelIndex || refAllele.length() > 1) {
            homRefGen = sampleMapper[0].getIndexOf(refAllele + ":" + refAllele);
            homVarGen = sampleMapper[0].getIndexOf(varAllele + ":" + varAllele);
        }
        else {
            homRefGen = sampleMapper[0].getIndexOf(refAllele + refAllele);
            homVarGen = sampleMapper[0].getIndexOf(varAllele + varAllele);
        }

        StringBuilder outTemp = new StringBuilder();
        int[][] sampDataLine = vdat.returnSample(lastLine);

        for (int ind : new int[]{chrIndex, lfIndex, rfIndex, refIndex, varIndex, mutTypeIndex}) {
            outTemp.append(annotMapper[ind].getString(outData[lastLine][ind]) + "\t");
        }

        for (int j=0; j<names.length; j++) {
            int sampGen = sampDataLine[j][1];

            //qual filters - procede on success ("continue" otherwise)
            if ((sampleMapper[1].getDataType() == VarData.INTEGER &&
                 sampDataLine[j][2] >= scoreT &&
                 ((float)sampDataLine[j][2] / sampDataLine[j][3]) >= scoreCovT) ||
                (sampleMapper[1].getDataType() == VarData.FLOAT &&
                 sampleMapper[1].getFloat(sampDataLine[j][2]) >= scoreT &&
                 (sampleMapper[1].getFloat(sampDataLine[j][2]) / sampDataLine[j][3]) >= scoreCovT)
               ) {
                //Proceed
            }
            else {
                continue;
            }

            //homnonref
            if (homRefCB.isSelected() && sampGen == homRefGen) {

                outTemp.append( fetchSampleInfo(j, sampDataLine) + "\t");
                continue;
            }

            //homvar
            if (homVarCB.isSelected() && sampGen == homVarGen) {

                outTemp.append( fetchSampleInfo(j, sampDataLine) + "\t");
                continue;
            }

            //hetvar and other
            if ((hetVarCB.isSelected() 
                    || otherCB.isSelected()) && sampGen != homRefGen && sampGen != homVarGen
                    && sampGen != NA_Allele) {

                boolean hasVar = false;
                String[] alleles;
                if (muttype == indelIndex || refAllele.length() > 1) {
                    alleles = sampleMapper[0].getString(sampGen).split(":");
                }
                else {
                    alleles = sampleMapper[0].getString(sampGen).split("");
                }

                for (String s : alleles) {
                    if (s.equals(varAllele)) {
                        hasVar = true;
                    }
                }
                if (hetVarCB.isSelected() && hasVar) {
                    outTemp.append( fetchSampleInfo(j, sampDataLine) + "\t");
                    continue;
                }
                else if (otherCB.isSelected() && !hasVar) {
                    outTemp.append( fetchSampleInfo(j, sampDataLine) + "\t");
                    continue;
                }
            }

        }

        lastLine++;
        outTemp.deleteCharAt(outTemp.length() - 1);
        return outTemp.toString();
    }


    /**
    *   Get sample info, return as semicolon delimited string
    *   @param sampIndex The sample index (0-based)
    *   @param sampLine The 2d array of sample output [sampIndex][info_type(sample_name, genotype, score, coverage)]
    */
    private String fetchSampleInfo(int sampIndex, int[][] sampLine) {
        StringBuilder outString = new StringBuilder();
        
        if (nameCB.isSelected()) {
            outString.append(names[sampIndex] + ";"); //Sample name!
        }
        for (int i=0; i<VarData.S_FIELDS; i++) {
            // Check whether to output; Add 1 to outOptions index (as name has been added to beginning)
            if (outOptions[i+1].isSelected()) { 
                // Add 1 to sampLine second dimension (sampIndex[][THIS]), as the name has been added to beginning.
                outString.append(sampleMapper[i].getString(sampLine[sampIndex][i+1]) + ";");
            }
        }

        if (outString.length() > 0) {
            outString.deleteCharAt(outString.length() - 1);
        }
        return outString.toString();

    }

}
