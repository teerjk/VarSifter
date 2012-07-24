import java.net.URI;
import java.util.Arrays;
import java.util.BitSet;
import java.io.File;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;

/**
*   A class to dynamically compile a custom query object, load it, and execute the query method.
*   Requires Java 1.6 or higher (full JDK, not just JRE).
*   @author Jamie K. Teer
*/
public class CompileCustomQuery {
    final static String className = "VSQueryModule";
    final String fullClassName = System.getProperty("user.home") 
                               + System.getProperty("file.separator")
                               + className
                               + ".class";

    public CompileCustomQuery() {
    }

    /**
    *   Compile the QueryModule class
    *   @param customQuery The if statement (enclosed in parens) with which to search the data structure output
    *                      by VarData.dataDump();
    *   @return True if the compilation succeeds, false otherwise.
    */
    public boolean compileCustom(String customQuery) {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            VarSifter.showError("Error compiling custom query!  It is possible you are not using the full Java "
                + "Developement Kit (JDK).  Please ensure you have installed the JDK, not just the JRE.");
            return false;
        }
            

        if (customQuery.equals("")) {
            VarSifter.showError("No custom query string; did you click \"Apply\" in the Custom Query window?");
            return false;
        }


        StringBuilder out = new StringBuilder(64);
        //System.out.println(customQuery);

        //QueryModule code - wish Java had heredocs!!
        out.append( "import java.util.BitSet;\n" );
        out.append( "import java.util.regex.Pattern;\n" );
        out.append( "public class " ).append(className).append( " implements AbstractQueryModule {\n" );
        out.append( "  private static final Pattern hetPat = Pattern.compile(" );
        out.append(      "\"^([acgtACGT])(?!\\\\1)[acgtACGT]$|^([acgtnACGTN\'*]+):(?!\\\\2$)[acgtnACGTN\'*]+$\");\n ");
        out.append( "  private static final Pattern homPat = Pattern.compile(" );
        out.append(      "\"^([acgtnACGTN])\\\\1$|^([acgtnACGTN\'*]+):\\\\2$\");\n ");
        out.append( "  private int[][] allData;\n" );
        out.append( "  private int[][][] sampData;\n" );
        out.append( "  private AbstractMapper[] sampleMapper;\n" );
        out.append( "  private AbstractMapper[] annotMapper;\n" );
        out.append( "  private int mutTypeIndex;\n" );
        out.append( "  private int refIndex;\n" );
        out.append( "  private int nonRefIndex;\n" );
        out.append( "  private int indel;\n" );
        out.append( "  private int NA_Allele;\n" );
        out.append( "  private BitSet[] bitSets;\n" );
        out.append( "  private BitSet hetBS;\n" );
        out.append( "  private BitSet homBS;\n" );
        out.append( "  private int muttype; //Re-assigned for each data row\n" );
        out.append( "                                                           \n" );
        out.append( "  public ").append(className).append( "(VarData vdat) {\n" );
        out.append( "    allData = vdat.returnData();\n" );
        out.append( "    sampData = vdat.returnSamples();\n" );
        out.append( "    bitSets = vdat.getCustomBitSet();\n" );
        out.append( "    annotMapper = vdat.returnAnnotMap();\n" );
        out.append( "    sampleMapper = vdat.returnSampleMap();\n" );
        out.append( "    hetBS = sampleMapper[0].filterWithPattern(hetPat);\n" );
        out.append( "    homBS = sampleMapper[0].filterWithPattern(homPat);\n" );
        //out.append( "    System.out.println(\"Hom: \" + homBS.size() + \" \" + homBS.cardinality());\n" ); //TESTING
        //out.append( "    System.out.println(\"Het: \" + hetBS.size() + \" \" + hetBS.cardinality());\n" ); //TESTING
        out.append( "    mutTypeIndex = vdat.returnDataTypeAt().get(\"muttype\");\n" );
        out.append( "    refIndex = vdat.returnDataTypeAt().get(\"ref_allele\");\n" );
        out.append( "    nonRefIndex = vdat.returnDataTypeAt().get(\"var_allele\");\n" );
        out.append( "    indel = annotMapper[mutTypeIndex].getIndexOf(\"INDEL\");\n" );
        out.append( "    NA_Allele = sampleMapper[0].getIndexOf(\"NA\");\n" );
        out.append( "  }\n" );
        out.append( "  public BitSet executeCustomQuery() {\n" );
        out.append( "    BitSet bs = new BitSet(allData.length);\n" );
        out.append( "    for (int i=0;i<allData.length;i++) {\n");
        out.append( "      muttype = allData[i][mutTypeIndex];\n" );
        out.append( "      String homRefAllele = annotMapper[refIndex].getString(allData[i][refIndex]);\n");
        out.append( "      String homNonRefAllele = annotMapper[nonRefIndex].getString(allData[i][nonRefIndex]);\n");
        out.append( "      int homRefGen;\n");
        out.append( "      int homNonRefGen;\n");
        out.append( "      int hemiRefGen = sampleMapper[0].getIndexOf(homRefAllele);\n");
        out.append( "      int hemiVarGen = sampleMapper[0].getIndexOf(homNonRefAllele);\n");
        out.append( "      if (muttype == indel || homRefAllele.length() > 1) {\n");
        out.append( "        homRefGen = sampleMapper[0].getIndexOf(homRefAllele + \":\" + homRefAllele);\n");
        out.append( "        homNonRefGen = sampleMapper[0].getIndexOf(homNonRefAllele + \":\" + homNonRefAllele);\n");
        out.append( "      }\n");
        out.append( "      else {\n");
        out.append( "        homRefGen = sampleMapper[0].getIndexOf(homRefAllele + homRefAllele);\n");
        out.append( "        homNonRefGen = sampleMapper[0].getIndexOf(homNonRefAllele + homNonRefAllele);\n");
        out.append( "      }\n");

        out.append( "      if " );
        out.append(            customQuery );
        out.append(                      " {\n" );
        
        out.append( "        bs.set(i);\n" );
        out.append( "      }\n" );
        out.append( "    }\n" );
        out.append( "    return bs;\n" );
        out.append( "  }\n" );
        out.append( "  private boolean isHet(int genoIndex) {\n" );
        out.append( "    if (hetBS.get(genoIndex)) {\n" );
        out.append( "      if (muttype == indel && !sampleMapper[0].getString(genoIndex).contains(\":\")) {\n" );
        out.append( "        return false;\n") ;
        out.append( "      }\n" );
        out.append( "      else { return true; }\n");
        out.append( "    }\n" );
        out.append( "    else { return false; }\n" );
        out.append( "  }\n" );
        out.append( "  private boolean isHom(int genoIndex) {\n" );
        out.append( "    if (homBS.get(genoIndex)) {\n" );
        out.append( "      if (muttype == indel && !sampleMapper[0].getString(genoIndex).contains(\":\")) {\n" );
        out.append( "        return false;\n") ;
        out.append( "      }\n" );
        out.append( "      else { return true; }\n");
        out.append( "    }\n" );
        out.append( "    else { return false; }\n" );
        out.append( "  }\n" );
        out.append( "}\n" );


        JavaFileObject file = new JavaSourceFromString(className, out.toString());
        //System.out.println(out.toString());  //DEBUG TESTING

        boolean success = false;
        try {
            File fTest = new File(fullClassName);
            if (fTest.exists()) {
                VarSifter.showError("<html>File " + fullClassName + " already exists!<p>Will NOT overwrite, "
                    + "so cannot apply filter.<p>You will have to manually remove the file!</html>");
            }
            else {
                final Iterable<String> opts = Arrays.asList("-d", System.getProperty("user.home"));
                CompilationTask task = compiler.getTask(null,null,diagnostics,opts,null,Arrays.asList(file));
                success = task.call();
            }
        }
        catch (NullPointerException npe) {
            System.out.println(npe.toString());
        }

        for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
          System.out.println(diagnostic.getCode());
          System.out.println(diagnostic.getKind());
          System.out.println(diagnostic.getPosition());
          System.out.println(diagnostic.getStartPosition());
          System.out.println(diagnostic.getEndPosition());
          System.out.println(diagnostic.getSource());
          System.out.println(diagnostic.getMessage(null));
    
        }
        //System.out.println(System.getProperty("java.class.path")); //TESTING

        if (success) {
            return success;
        }
        else {
            VarSifter.showError("Compile Failed! Check console output for details.");
            return success;
        }
    }

    
    /**
    *   Uses Reflection via CustomClassLoader to reload VSQueryModule, and execute the query.
    *   @param vdat VarData object containing desired data
    *   @return BitSet where bits corresponding to rows passing filter are set.
    */
    public BitSet run(VarData vdat) {
        try {

            ClassLoader parentCL = getClass().getClassLoader();
            CustomClassLoader ccl = new CustomClassLoader(parentCL, fullClassName);
            Class myObjectClass = ccl.loadClass(className);

            @SuppressWarnings("unchecked")
            AbstractQueryModule aqm = 
                (AbstractQueryModule) myObjectClass.getConstructor(new Class[]{VarData.class}).newInstance(new Object[]{vdat});
            BitSet out = aqm.executeCustomQuery();
            File cf = new File(fullClassName);
            if (!cf.delete()) {
                VarSifter.showError("Couldn't delete " + fullClassName + ". You may want to delete it.");
            }
            return out;
        }
        catch (Exception e) {
            VarSifter.showError("Error running custom query: check console output for details.");
            System.out.println("Error: " + e.toString());
            System.out.println("Cause: " + e.getCause().toString());
            for (StackTraceElement st:e.getStackTrace()) {
                System.out.println(st.toString());
            }
            return null;
        }
    }
}

/**
*   A class to write the source to memory.
*/
class JavaSourceFromString extends SimpleJavaFileObject {
    final String code;

    JavaSourceFromString(String name, String code) {
        super(URI.create("string:///" + name + Kind.SOURCE.extension), Kind.SOURCE);
        this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }
}
