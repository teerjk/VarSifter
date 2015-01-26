## Troubleshooting 

(Taken from [[User Guide]])

1. **VarSifter will not start. I see error messages about "class" or "loading".**

    If you've copied and pasted the start command from the internet or E-mail, try typing it by hand. Sometimes, the minus sign is interpreted as a weird character that command lines do not recognize.
If you're using a Windows machine, Internet explorer may be "helpful" and rename the .jar file as .zip. This is bad. Either use Firefox to "Save As" the file (making sure it is not changing the filetype) or rename the file. To rename the file in Windows correctly, you either must be showing file extensions, or you have to use the "ren" command on the command prompt.
If you are using a helper file, make sure the VarSifter version in the helper file matches the version you want to be using (the latest version in your directory.)

2. **VarSifter will not start. I see error messages about "heap space" or "memory".**

    VarSifter is not being started with enough memory. Increase the memory requested in the -Xmx command. If using a helper file, change this in the helper file. Note that this value must be less than the total system memory in your machine, and you may have to close other programs to be able to use this memory.
You may be using a 32-bit Java virtual machine. In 64-bit **Mac OSX**, open "Java Preferences" in Applications->Utilities. Make sure that the topmost java version is 64-bit. Note that any 32-bit operating system (includes Windows XP) will not be able to run a 64-bit Java, and will be limited in how much memory can be used!
    
3. **VarSifter gives errors about Custom Querying, but I have the full Java 1.6 JDK installed.**

    Although the full Java Development Kit may be installed, your system may not be recognizing it. Ensure the 1.6 JDK is the system default. This is probably defined in your Java settings, which tend to be system specific. Alternately, find the correct java binary file, and point your scripts at the full path to that file. On OS X ~10.6, that may be in: /System/Library/Frameworks/JavaVM.framework/Versions/.
This can be especially problematic on Windows machines. Here is a workaround, courtesy of David Adams:

    *  Install java JDK
*  Append JDK bin directory to system PATH environmental variable
*  Go into JDK bin directory and make a copy of the java (java.exe if you have extensions visible) file and name the copy javasdk (javasdk.exe)
*  Start VarSifter at command line with "java -Xmx1000M -jar VarSifter\<version\>.jar"
*  Alternatively, one can remove all Java versions, and then install only the Java JDK 1.6. 

4. **VarSifter can't find/open a file, even though I could see it in the File select window.**

    This may be related to a Java and Windows issue where Java cannot properly see spaces or special characters in directory names. Try removing spaces from directory names with VarSifter and your data files. Also, try putting VarSifter and your data files directly on the hard drive, as I've seen issues with network mounted user directories and the "Desktop" location.
