import java.io.*;
import java.util.regex.*;

public class VarData {
    
    int H_LINES  = 1;   //Number of header lines
    int S_FIELDS = 3;   //Number of columns for each sample

    String[][] data;
    String[] dataNames;
    String[][][] samples;
    String[] sampleNames;

     /*    
    *    Constructor reads in the file specified by full path in String inFile.
    *    It first reads through the file just to count lines for first dimension
    *     of data[][][].  Then, it reads again to fill in the array.  First dimension
    *    (i) is rows, second (j) is columns separated by tab (\t), third is mutations,
    *    separated by a semicolon (;).
    */

    public VarData(String inFile) {
        String line = new String();
        int lineCount = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            while ((line = br.readLine()) != null) {
                lineCount++;
            }
            data = new String[lineCount - H_LINES][];
            samples = new String[lineCount - H_LINES][][];
            br.close();
            lineCount = 0;
        }
        catch (IOException ioe) {
            System.out.println(ioe);
            System.exit(1);
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            boolean first = true;
            
            while ((line = br.readLine()) != null) {
                String temp[] = line.split("\t", -1);
                String sampleTemp = "";
                String dataTemp = "";
                
                //Handle the Header
                if (first) {
                    
                    Pattern pat = Pattern.compile("NA");
                    int sampleCount = 0;
                    
                    for ( String title : temp ) {
                        if ((pat.matcher(title)).find()) {
                            //System.out.println(title + "\t");
                            if (sampleCount % S_FIELDS == 0) {
                                sampleTemp += (title + "\t");
                            }
                            sampleCount++;
                            
                        }
                        else {
                            dataTemp += (title + "\t");
                        }
                    }
                    
                    sampleNames = sampleTemp.split("\t");
                    dataNames = dataTemp.split("\t");

                    //System.out.println(sampleNames.length + "\t" + dataNames.length);
                    //for (int i = 0; i < samples.length; i++) {
                    //    samples[i] = new String[sampleNames.length][S_FIELDS];
                    //}

                    
                    //for ( String x : dataNames ) {
                    //    System.out.println(x);
                    //}
                    
                    first = false;
                    continue;
                }
                
                //Fill data array (annotations)
                data[lineCount] = new String[dataNames.length];
                System.arraycopy(temp, 0, data[lineCount], 0, dataNames.length);
                
                //Fill samples array (genotypes)
                samples[lineCount] = new String[sampleNames.length][S_FIELDS];
                
                //System.out.println(temp.length);
                for (int i = 0; i < sampleNames.length; i++) {
                    System.arraycopy(temp, (dataNames.length + (i * S_FIELDS)), samples[lineCount][i], 0, 3);
                }
                
                lineCount++;
            }
        }
        catch (IOException ioe) {
            System.out.println(ioe);
            System.exit(1);
        }

                
    }


    public static void main(String args[]) {
        String input = "test_data";
        if (args.length > 0) {
            input = args[0];
        }
        VarData vdat = new VarData(input);

        //for (int i = 0; i < vdat.data.length; i++) {
        //    System.out.print((i+1) + "\t");
        //    for (int j = 0; j < vdat.data[i].length; j++) {
        //        System.out.print(vdat.data[i][j] + "\t");
        //    }
        //    System.out.println();
        //}

        for (int i = 0; i < vdat.samples.length; i++) {
            if (!vdat.samples[i][0][0].equals(vdat.samples[i][1][0]) && (!vdat.samples[i][0][0].equals("NA") 
                && !vdat.samples[i][1][0].equals("NA"))) {

                StringBuilder out = new StringBuilder();
                for (String s : vdat.data[i]) {
                    out.append(s + "\t");
                }
                for (String[] s : vdat.samples[i]) {
                    out.append((String)s[0] + "\t");
                }
                out.delete((out.length()-1), (out.length()));
                System.out.println(out.toString());
            }
        }

    }
}
