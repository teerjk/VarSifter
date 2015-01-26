# Welcome to VarSifter!

-------------------

Running VarSifter:

1. Unzip the VarSifter_<version>.zip file (which you've probably already done)
   and enter the VarSifter_<version> directory.

2. OSX users: Double-click VarSifter_<version>.command file to run VarSifter.
   Windows users: Double-click VarSifter_<version>.bat file to run VarSifter.

3. The main window opens, with a message outlining steps to getting started.
   Basically, click "File" on the menu and then "Open File".  Choose your data
   file, and click "Open". 

    (If you wish to skip this step, the name of a data file can be added at the
   end of the "java -Xmx..." line in the .command or .bat file, or on the
   command line.)

4. If you have chosen a VCF file, you need to give VarSifter some hints about
   your data:
    
    a. If you have MultiAllelic fields (ie, mulitple comma-separated values, 
    one for each alternate allele), VarSifter will check 'AF' fields, 
    but additional such field need to be identified by the user:
    check any boxes for fields which VarSifter could not determine
    MultiAllele status. Note that these boxes are only usable when the field
    has a "Number" entry greater than 1.
    
    b. Some researchers further sub-delimit INFO fields in their VCF files. For
    example, an INFO field with gene names may have multiple entries for each
    alt allele, delimited by a '/'.  In this case, VarSifter needs to know about
    this. The delimiting character should be entered in the "Sub-delimiter"
    field. After entering the last character, you must either select a
    different box or hit "return" in order for your change to stick.

5. Finally, you can choose which annotation columns to load and display. By
   default, all columns are selected.  Uncheck any you do not wish to load,
   and click "Ok".

--------------------

Quick Trouble Shooting

1. Make sure Java is installed. If you want to do custom querying, you need to
   have the full Java Developement Kit (JDK) 1.6 installed, and it must be the
   default version of Java on the system.

2. You may need more memory, especially if nothing seems to be happening, or
   you see an error in the terminal/console/command window about "Out of Heap Space".
   This can be changed by editing the .command or .bat file:
     Change "-Xmx500M" to be something larger, like "-Xmx2G" (no quotes).
     Note that this needs to be lower than your total system memory.

3. See the online Documentation.
   "User Guide"

4. Run VarSifter from a command prompt, copy the error produced. Usually
   starts with "Exception in thread...". Check the online Documentations, 
   or send this error to the developer.

--------------------

Acknowledgments

This distribution of VarSifter includes the following external libraries:

JUNG (Java Universal Network Graph) - http://jung.sourceforge.net/
    JUNG is distributed without alteration, and uses the BSD license, found here:
    http://jung.sourceforge.net/license.txt
    and in the enclosed JUNG-bsd_license.txt

As part of JUNG, collections-generic, the LarvaLabs version of the 
Jakarta Collections Framework (Apache Commons Foundation), is included. 
    It is distributed here without alteration, and uses the Apache v2.0 license, enclosed as
    apache2_license.txt.
    Project is found here:
    http://larvalabs.com/collections/index.html

json-simple - http://code.google.com/p/json-simple/
    json-simple is distributed without alteration, and uses the Apache v2.0
    license, enclosed as apache2_license.txt

