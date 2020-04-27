#  Compiling from source

1. Download the source in your desired working directory:

    `git clone https://github.com/teerjk/VarSifter.git`
    
2. Create a folder in working directory called 'jung'

3. Download jung jar files from http://jung.sourceforge.net/
   VarSifter has been tested with jung2-2_0_1.zip.
   Unzip jung zipfile into 'jung' directory.
   NOTE: Make sure the jung/ directory contains the .jar files, not another directory!
4. Download json-simple jar file from http://code.google.com/p/json-simple/
    VarSifter has been tested with 1.1.1.
    Add json-simple jarfile into 'jung' directory (I should probably rename
    this...).

5. If you have 'make' installed, simply run make.

If you do not have make installed, refer to the makefile for commands to use to build VarSifter.
The Makefile contains commands used to compile VarSifter, build a jarfile, and create Javadoc
documentation.

