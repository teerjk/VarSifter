import java.text.NumberFormat;
import javax.swing.*;
import javax.swing.table.*;

/**
*   Sets a larger precision for Float values, sets center alignment
*   @author Jamie K. Teer
*/
public class FloatScoreRenderer extends DefaultTableCellRenderer {
    
    private NumberFormat f;

    /**
    *   Constructor
    *
    *   @param precision The number of decimal places to display
    */
    public FloatScoreRenderer(int precision) { 
        super(); 
        f = NumberFormat.getNumberInstance();
        f.setMaximumFractionDigits(precision);
        f.setMinimumFractionDigits(precision);
    
    }

    /**
    *   Sets text of cell with correct precision
    *
    *   @param val The stored value of the cell
    */
    @Override
    public void setValue(Object val) {
        float nv = ((Float)val).floatValue();
        setText(f.format(nv));
        setHorizontalAlignment(SwingConstants.CENTER);
    }
}
