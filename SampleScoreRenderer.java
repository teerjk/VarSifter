import javax.swing.*;
import javax.swing.table.*;
import java.awt.Color;
import java.awt.Component;

/**
*   Sample Table display
*   Will lowlight rows with Genotype Score / Coverage ratios below a value
*   If ratio cannot be determined from data, uses 1.0f
*   @author Jamie K. Teer
*/
public class SampleScoreRenderer extends DefaultTableCellRenderer {
    private float cutoffRatio = 0.5f;

    /**
    *   Uses 0.5 as default Genotype Score / Coverage ratio cutoff
    */
    public SampleScoreRenderer() { 
        super(); 
    }

    /**
    *   Takes a float as input to use for lowlighting
    *   
    *   @param f Float defining Genotype Score / Coverage ratio cutoff
    */
    public SampleScoreRenderer(float f) {
        super();
        cutoffRatio = f;
    }


    /**
    *   Sets background to be gray if mpg score/coverage ratio < cutoffRatio
    *
    */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                    int row, int column) {
        Component comp = super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column);
        float ratio = 1.0f;

        if (table.getModel().getColumnCount() >= 4) {
            ratio = ((Number)table.getModel().getValueAt(row, 2)).floatValue() 
                / ((Number)table.getModel().getValueAt(row, 3)).floatValue() ;
        }
        
        if (ratio < cutoffRatio) {
            comp.setBackground(Color.darkGray);
            comp.setForeground(Color.red);
        }
        else {
            comp.setBackground(null);
            comp.setForeground(null);
        }
        return comp;
    }
}
