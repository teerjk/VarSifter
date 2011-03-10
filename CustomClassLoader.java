import java.io.*;
import java.net.*;

//Learned about this from http://tutorials.jenkov.com/java-reflection/dynamic-class-loading-reloading.html

/**
*   A class to allow reloading of a dynamic class
*   @author Jamie K. Teer
*/
public class CustomClassLoader extends ClassLoader {
    private String fullClassName;
    private String baseClassName;

    /**
    *   Initiate a new CustomClassLoader
    *   @param parent   The ClassLoader that loaded the object that called this.
    *   @param inClassName  The full path and name of the class to load. Eg. /home/me/Test.class
    */
    public CustomClassLoader(ClassLoader parent, String inClassName) {
        super(parent);
        fullClassName = inClassName;
        File f = new File(fullClassName);
        baseClassName = f.getName();
        baseClassName = baseClassName.substring(0, baseClassName.lastIndexOf('.'));
        //System.out.println(fullClassName + " " + baseClassName);
    }

    /**
    *   (Re)load a class
    *   @param name The name of the class to load.  If different from the class given to the constructor, 
    *                                               this class will be passed to the parent loader.
    *   @return The loaded class.
    *   @throws ClassNotFoundException
    */
    @Override
    public Class loadClass(String name) throws ClassNotFoundException {
        if (!baseClassName.equals(name)) {
            return super.loadClass(name);
        }

        try {
            String url = "file:" + fullClassName;
            URL myURL = new URL(url);
            URLConnection connection = myURL.openConnection();
            InputStream in = connection.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int data = in.read();

            while (data != -1) {
                buffer.write(data);
                data = in.read();
            }
            in.close();

            byte[] classData = buffer.toByteArray();

            return defineClass(name, classData, 0, classData.length);

        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return null;
    }

    /*  Used this to test current working directory
    public static void main (String args[]) {
        CustomClassLoader c = new CustomClassLoader(CustomClassLoader.class.getClassLoader(), "test");
        try {
            c.loadClass("test");
        }
        catch (ClassNotFoundException e) {
        }
    }
    */
}


