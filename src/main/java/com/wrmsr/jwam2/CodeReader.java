/******************************************************************************
 * Warren's Abstract Machine  -  Implementation by Stefan Buettcher
 *
 * developed:   December 2001 until February 2002
 *
 * CodeReader.java contains the CodeReader class that transforms a WAM code
 * input file into a Program structure (cf. Program.java / Statement.java)
 ******************************************************************************/
package com.wrmsr.jwam2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CodeReader {
    public CodeReader() {
    }

    public static Program readProgram(String fileName) {
        BufferedReader b;
        try {
            b = new BufferedReader(new FileReader(fileName));
            Program p = new Program();
            Statement s;
            String str;
            do {
                str = b.readLine();
                if (str != null) {
                    int j;
                    str = str.trim();
                    if (str.length() == 0)
                        continue;
                    if ((str.charAt(0) == ';') || (str.charAt(0) == '#') || (str.charAt(0) == '%'))
                        continue;
                    String mark = "";
                    j = str.indexOf(":");
                    if (j > 0) {
                        mark = str.substring(0, j).trim();
                        str = str.substring(j + 1).trim();
                    }
                    String function = "";
                    j = str.indexOf(" ");
                    if (j > 0) {
                        function = str.substring(0, j).trim();
                        str = str.substring(j + 1).trim();
                        j = str.indexOf(" ");
                        if (j > 0)
                            s = new Statement(mark, function, str.substring(0, j).trim(), str.substring(j + 1).trim());
                        else
                            s = new Statement(mark, function, str);
                    } else
                        s = new Statement(mark, str, "");
                    p.addStatement(s);
                }
            } while (str != null);
            b.close();
            p.updateLabels();
            return p;
        } catch (IOException io) {
            return null;
        }
    }
}
