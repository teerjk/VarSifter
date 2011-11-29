/**
*   This class stores static messages.
*/
public class VSMessages {


    final static String geneNotFoundError  = "It looks like you may have switched columns! "
        + "Switch them so the gene names are in the first column!";

    // Gov disclaimer/license
    final static String govWork = "PUBLIC DOMAIN NOTICE\n" +
    "National Human Genome Research Institute, National Institutes of Health\n" +
    "This software/database is \"United States Government Work\" under the terms of\n" +
    "the United States Copyright Act.  It was written as part of the authors'\n" +
    "official duties for the United States Government and thus cannot be copyrighted.\n" +
    "This software/database is freely available to the public for use without a copyright\n" +
    "notice.  Restrictions cannot be placed on its present or future use.\n\n" +
    "Although all reasonable efforts have been taken to ensure the accuracy and\n" +
    "reliability of the software and data, the National Human Genome Research Institute\n" +
    "(NHGRI) and the U.S. Government does not and cannot warrant the performance or results\n" +
    "that may be obtained by using this software or data.  NHGRI and the U.S. Government\n" +
    "disclaims all warranties as to performance, merchantability or fitness for any\n" +
    "particular purpose.\n\n" +
    "In any work or product derived from this material, proper attribution of the authors\n" +
    "as the source of the software or data should be made.";

    //This class uses TableSorter.java, a class to sort a JTable.
    //The two subsequent statements are required to be distributed with the 
    //source and binary re-distributions of the TableSorter class,
    final static String sunCopyright = "Copyright (c) 1995 - 2008 Sun Microsystems, Inc.  All rights reserved.";
    final static String sunDisclaimer = "Redistribution and use in source and binary forms, with or without\n" +
    "modification, are permitted provided that the following conditions\n" +
    "are met:\n" +
    "    \n" +
    "  - Redistributions of source code must retain the above copyright\n" +
    "    notice, this list of conditions and the following disclaimer.\n" +
    "       \n" +
    "  - Redistributions in binary form must reproduce the above copyright\n" +
    "    notice, this list of conditions and the following disclaimer in the\n" +
    "    documentation and/or other materials provided with the distribution.\n" +
    "           \n" +
    "  - Neither the name of Sun Microsystems nor the names of its\n" +
    "    contributors may be used to endorse or promote products derived\n" +
    "    from this software without specific prior written permission.\n\n" +
    "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS\n" +
    "IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,\n" +
    "THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR\n" +
    "PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR\n" +
    "CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,\n" +
    "EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,\n" +
    "PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR\n" +
    "PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF\n" +
    "LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING\n" +
    "NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS\n" +
    "SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.";

    final static String welcome = "<html><h2>Welcome to VarSifter!</h2><p>It looks like you haven't specified a " +
        "data file to load. (You can do so at the end of the command line used to start VarSifter.)<p>" +
        "The following steps will help you get started:<p><p>" +
        "1) Please click File->\"Open File\" and choose a data file to load (VCF or VS).<p>" +
        "2) If you chose a VCF file, you will be asked about its settings:<p>" +
        "&nbsp;&nbsp;a) If a field is multiallelic (meaning it can have a value for each alternate allele), " +
        "check the \"MultiAllele\" box for that field.<p>" +
        "&nbsp;&nbsp;b) If INFO entries in your file are sub delimited (more levels than just the comma),<p>" +
        "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
        "enter the delimiting character in the \"Sub-delimiter\" field, and press \"Enter\" or an arrow key.<p>" +
        "&nbsp;&nbsp;c) Click \"OK\".<p>" +
        "3) You will now be asked which annotation columns to load. If you want them all, click \"Ok\", " +
        "otherwise,<p>&nbsp;&nbsp;&nbsp;&nbsp;" +
        "uncheck those you do not wish to see.<p><p>" +
        "If you have questions about VarSifter, you can find documentation online, <p>" +
        "or by clicking Help->Documetation.<p>If you are having trouble, or see error messages, " +
        "please see Help->Troubleshooting.</html>";

}
