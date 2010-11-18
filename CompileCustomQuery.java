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
    String className = "QueryModule";

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

        StringBuilder out = new StringBuilder(64);
        //System.out.println(customQuery);

        out.append( "import java.util.BitSet;\n" );
        out.append( "public class " ).append(className).append( " implements AbstractQueryModule {\n" );
        out.append( "  public BitSet executeCustomQuery(VarData vdat) {\n" );
        out.append( "    int[][] allData = vdat.dataDump();\n" );
        out.append( "    BitSet bs = new BitSet(allData.length);\n" );
        out.append( "    AbstractMapper[] annotMapper = vdat.returnAnnotMap();\n" );
        out.append( "    AbstractMapper[] sampleMapper = vdat.returnSampleMap();\n" );
        out.append( "    int typeIndex = vdat.returnDataTypeAt().get(\"type\");\n" );
        out.append( "    int refIndex = vdat.returnDataTypeAt().get(\"ref_allele\");\n" );
        out.append( "    int nonRefIndex = vdat.returnDataTypeAt().get(\"var_allele\");\n" );
        out.append( "    int divNC = annotMapper[typeIndex].getIndexOf(\"DIV-NC\");\n" );
        out.append( "    int divC  = annotMapper[typeIndex].getIndexOf(\"DIV-c\");\n" );
        out.append( "    int divFS  = annotMapper[typeIndex].getIndexOf(\"DIV-fs\");\n" );
        out.append( "    int NA_Allele = sampleMapper[0].getIndexOf(\"NA\");\n" );
        out.append( "    for (int i=0;i<allData.length;i++) {\n");
        out.append( "        int type = allData[i][typeIndex];\n");
        out.append( "        String homRefAllele = annotMapper[refIndex].getString(allData[i][refIndex]);\n");
        out.append( "        String homNonRefAllele = annotMapper[nonRefIndex].getString(allData[i][nonRefIndex]);\n");
        out.append( "        int homRefGen;\n");
        out.append( "        int homNonRefGen;\n");
        out.append( "        if (type == divNC || type == divC || type == divFS) {\n");
        out.append( "           homRefGen = sampleMapper[0].getIndexOf(homRefAllele + \":\" + homRefAllele);\n");
        out.append( "           homNonRefGen = sampleMapper[0].getIndexOf(homNonRefAllele + \":\" + homNonRefAllele);\n");
        out.append( "        }\n");
        out.append( "        else {\n");
        out.append( "           homRefGen = sampleMapper[0].getIndexOf(homRefAllele + homRefAllele);\n");
        out.append( "           homNonRefGen = sampleMapper[0].getIndexOf(homNonRefAllele + homNonRefAllele);\n");
        out.append( "        }\n");

        out.append( "        if " );
        out.append(              customQuery );
        out.append(                        " {\n" );
        
        out.append( "        bs.set(i);\n" );
        out.append( "      }\n" );
        out.append( "    }\n" );
        out.append( "    return bs;\n" );
        out.append( "  }\n" );
        out.append( "}\n" );

        //out.append( "  public static void main(String args[]) {" );
        //out.append( "    VarSifter.showError(\"Hello World! \" + args[0]);\n" );
        //out.append( "    for (String s:vdat.returnSampleNames()) {\n" );
        //out.append( "      System.out.println(s);\n" );
        //out.append( "    }\n" );

        JavaFileObject file = new JavaSourceFromString(className, out.toString());
        //System.out.println(out.toString());

        CompilationTask task = compiler.getTask(null,null,diagnostics,null,null,Arrays.asList(file));
        boolean success = task.call();

        for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
          System.out.println(diagnostic.getCode());
          System.out.println(diagnostic.getKind());
          System.out.println(diagnostic.getPosition());
          System.out.println(diagnostic.getStartPosition());
          System.out.println(diagnostic.getEndPosition());
          System.out.println(diagnostic.getSource());
          System.out.println(diagnostic.getMessage(null));
    
        }

        //System.out.println(System.getProperty("java.class.path"));

        if (success) {
            return success;
        }
        else {
            VarSifter.showError("Compile Failed! Check console output for details.");
            return success;
        }
    }


    //public void run(String in) {
    //    try {
    //        String[] args = {in};
    //        Class.forName(className).getDeclaredMethod("main", new Class[] {String[].class})
    //            .invoke(null, new Object[] {args});
    //    }
    //    catch (Exception e) {
    //        System.out.println("Error: " + e);
    //    }
    //}
    
    /**
    *   Uses CustomClassLoader to reload QueryModule, and execute the query.
    *   @param vdat VarData object containing desired data
    *   @return BitSet where bits corresponding to rows passing filter are set.
    */
    public BitSet run(VarData vdat) {
        try {

            ClassLoader parentCL = getClass().getClassLoader();
            //ClassLoader parentCL = Thread.currentThread().getContextClassLoader();
            CustomClassLoader ccl = new CustomClassLoader(parentCL, className);
            Class myObjectClass = ccl.loadClass(className);

            AbstractQueryModule aqm = (AbstractQueryModule) myObjectClass.newInstance();
            BitSet out = aqm.executeCustomQuery(vdat);
            File cf = new File(className + ".class");
            if (!cf.delete()) {
                VarSifter.showError("Couldn't delete " + className + ".class. You may want to delete it.");
            }
            return out;
        }
        catch (Exception e) {
            VarSifter.showError("Error running custom query: check console output for details.");
            System.out.println("Error: " + e.toString());
            for (StackTraceElement st:e.getStackTrace()) {
                System.out.println(st.getClassName());
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
