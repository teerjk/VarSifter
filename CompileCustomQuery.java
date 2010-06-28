import java.net.URI;
import java.util.Arrays;
import java.util.BitSet;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;

public class CompileCustomQuery {
    String className = "QueryModule";

    public CompileCustomQuery() {
    }
    public boolean compileCustom() {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        StringBuilder out = new StringBuilder(64);

        out.append( "import java.util.BitSet;\n" );
        out.append( "public class " ).append(className).append( " {\n" );
        out.append( "  public static BitSet executeCustomQuery(VarData vdat) {\n" );
        out.append( "    String[][] allData = vdat.dataDump();\n" );
        out.append( "    BitSet bs = new BitSet(allData.length);\n" );
        out.append( "    for (int i=1;i<allData.length;i++) {\n");
        out.append( "      if (allData[i][vdat.returnDataTypeAt().get(\"type\")].equals(\"Stop\")) {\n" );
        out.append( "        bs.set(i-1);\n" );
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
            VarSifter.showError("Compile Failed!");
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
    
    public BitSet run(VarData vdat) {
        try {
            BitSet out = (BitSet)Class.forName(className).getDeclaredMethod("executeCustomQuery", new Class[] {VarData.class})
                .invoke(null, new Object[] {vdat});
            return out;
        }
        catch (Exception e) {
            System.out.println("Error: " + e.toString());
            for (StackTraceElement st:e.getStackTrace()) {
                System.out.println(st.getClassName());
            }
            return null;
        }
    }
}

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
