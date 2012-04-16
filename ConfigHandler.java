import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;


/**
*   ConfigHandler parses a key=value config file, and stores the information
*/
public class ConfigHandler {
    
    protected final static Pattern comment = Pattern.compile("^#");

    private Map<String,String> Opt = new HashMap<String,String>();

    public ConfigHandler(String inFile) {
        
        try {
            String line;
            BufferedReader br = new BufferedReader(new FileReader(inFile));
            while (( line = br.readLine()) != null) {
                if (comment.matcher(line).find() || line.equals("")) {
                    continue;
                }

                String[] kv = line.split("=", 2);
                Opt.put(kv[0], kv[1]);
            }
            br.close();
        }
        catch (IOException ioe) {
            System.err.println("Error parsing config file! No configuration available for use.");
            System.err.println(ioe);
        }

    }


    /**
    *   Returns true if the provided key exists in the config file
    *   @param inKey The String to check for existence.
    */
    public boolean exists(String inKey) {
        return Opt.containsKey(inKey);
    }

    /**
    *   Return the String form of the value for the given key or null if key doesn't exist
    *   @param inKey The String key
    *   @return The value in String form
    */
    public String get(String inKey) {
        return Opt.get(inKey);
    }

    /**
    *   Return the Integer form of the value for a given key or null if key doesn't exist or isn't an integer
    *   @param inKey The String key
    *   @return The value in Integer form
    */
    public Integer getInteger(String inKey) {
        try {
            Integer v = new Integer(Opt.get(inKey));
            return v;
        }
        catch (NumberFormatException nfe) {
            System.err.println("Integer parsing error for key \"" + inKey + "\". Returning null.");
            return null;
        }
    }

    /**
    *   Return the Float form of the value for a given key or null if key doesn't exist or isn't an integer
    *   @param inKey The String key
    *   @return The value in Float form
    */
    public Float getFloat(String inKey) {
        try {
            Float v = new Float(Opt.get(inKey));
            return v;
        }
        catch (NumberFormatException nfe) {
            System.err.println("Float parsing error for key \"" + inKey + "\". Returning null.");
            return null;
        }
    }
}

