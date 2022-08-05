package com.wrmsr.jwam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 * Interface.
 *
 * @author Bruno Simoes e Pedro Guerreiro
 * @version 1.00 06/04/29
 * Warren's Abstract Machine
 */

public class Jwam
        extends Symbol {

    /**
     * Program version
     */
    private final static double VERSION = 1.0;

    /**
     * Apresenta informacao de debug ?
     */
    private int debug = 0;

    /**
     * Apresenta benchmark ?
     */
    private boolean benchmark = true;

    /**
     * Inst�ncia WAM
     */
    private Wam wam;

    public Jwam() {
        this.wam = new Wam();
    }

    /**
     * Altera o estado de um paramentro interno.
     *
     * @param variable parametro a alterar.
     * @param value    valor a atribuir ao parametro.
     */
    private void setInternalVariable(String variable, String value) {
        try {

            if (variable.compareToIgnoreCase("benchmark") == 0) {
                benchmark = (Integer.parseInt(value) == 1) ? true : false;
            }
            if (variable.compareToIgnoreCase("debug") == 0) {
                debug = Integer.parseInt(value);
            }
            wam.setDebug(debug);
            getInternalVariable(variable);
        } catch (Exception e) {
        }
    }

    /**
     * Apresenta o valor das variveis
     */
    private void displayInternalVariables() {
        getInternalVariable("benchmark");
        getInternalVariable("debug");
    }

    /**
     * Apresenta o valor de determinada variavel.
     *
     * @param variable nome da variavel a apresentar.
     */
    private void getInternalVariable(String variable) {
        if (variable.compareToIgnoreCase("benchmark") == 0) {
            write("Internal variable BENCHMARK = " + benchmark + "\n");
        } else if (variable.compareToIgnoreCase("debug") == 0) {
            write("Internal variable DEBUG = " + debug + "\n");
        } else {
            write("Unknown internal variable.\n");
        }
    }

    /**
     * Obtem uma string do standard input.
     *
     * @return Devolve a string obtida.
     */
    private String read() {
        try {
            return new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException io) {
            return "";
        }
    }

    /**
     * Apresenta uma string no output.
     *
     * @param s string a apresentar.
     */
    private void write(String s) {
        System.out.print(s);
    }

    /**
     * Apresenta o texto de ajuda.
     */
    private void showHelp() {
        write("Available commands:\n\n");
        write("demo                    Test demo\n");
        write("exit                    Terminates the WAM\n");
        write("help                    Displays this help\n");
        write("list                    Lists all the memory\n");
        write("reset                   Clear all memory\n");
        write("set [PARAM[=VALUE]]     Displays all internal parameters (\"set\") or lets\n");
        write("                        the user set a parameter's new value, respectively\n");
        write("labels                  Displays all labels that can be found in memory\n");
        write("consult filename        Compile code into memory\n\n");
        try {
            write("" + wam.code.getStatementCount() + " lines of code in memory.\n");
        } catch (InvalidInstruction e) {
            write("Invalid instructions found\n");
        }
    }

    /**
     * Executa as instru��es dadas no input pelo utilizador.
     */
    private void console() {
        write("\nWarren's Abstract Machine\n");
        write("Copyright (C) 2005-2006 Bruno Simoes & Pedro Guerreiro\n");
        write("Type \"help\" to get some help.\n");

        String s;
        do {
            write("\n");
            write("| ?- ");
            s = read();
        }
        while ((s != null) && (runQuery(s)));

        write("halting..\n");
    }

    public boolean runQuery(String cmd) {

        cmd = cmd.trim();
        String[] cmds = cmd.split("\\.");

        for (String statement : cmds) {
            if (statement.isEmpty()) {
                continue;
            }

            if (statement.equals("exit")) {
                return false;
            }

            if (statement.equals("help")) {
                showHelp();
                return true;
            }

            if (statement.equals("set")) {
                displayInternalVariables();
                return true;
            }

            if (statement.equals("labels")) {
                wam.getAvaliableLabels();
                return true;
            }

            if (statement.equals("list")) {
                wam.getMemInformation();
                return true;
            }

            /** apaga a mem�ria */
            if (statement.equals("reset")) {
                write("Cleaning Memory ... Ok\n");
                wam.clear();
                return true;
            }

            if (statement.startsWith("consult")) {

                long ms = System.currentTimeMillis();

                StringTokenizer st = new StringTokenizer(statement, ",\n\t \\");
                int tokens = st.countTokens();
                if (tokens != 2) {
                    return false;
                }
                st.nextToken();
                String filename = st.nextToken();
                System.out.println(filename);
                String cprogram = "";//compiler.parse(filename);
                if (cprogram == null) {
                    return false;
                }

                try {
                    //wam.loadSource(cprogram);
                } catch (Exception e) {
                    System.out.println("Exception: " + e.getMessage());
                    wam.failAndExit("[program terminated]");
                }

                if (benchmark) {
                    write("\nTotal time elapsed: " + (System.currentTimeMillis() - ms) + " ms.\n");
                }

                return true;
            }

            if ((statement.length() > 4) && (statement.substring(0, 4).compareTo("set ") == 0)) {
                statement = statement.substring(4);
                int i = statement.indexOf(' ');
                while (i >= 0) {
                    statement = statement.substring(0, i) + statement.substring(i + 1);
                    i = statement.indexOf(' ');
                }
                i = statement.indexOf('=');
                if (i >= 0) {
                    String variable = statement.substring(0, i);
                    String value = statement.substring(i + 1);
                    setInternalVariable(variable, value);
                } else {
                    getInternalVariable(statement);
                }
                return true;
            }

            /** executa uma demo. */
            if (statement.equals("demo")) {
                teste();
            }
        }

        /** Executa a WAM */
        //String q = compiler.ss(s);
        //if(q == null) return false;

        try {
            if (wam.code.getStatementCount() == 0) {
                System.out.println("Nao existem instrucoes na memoria.");
                return true;
            }
        } catch (InvalidInstruction e) {
            write("Invalid instructions found\n");
            return false;
        }

        String answer = "";
        boolean failed = false;
        long time = 0L, ms, ms2;

        while (true) {

            if (!failed) {
                ms = System.currentTimeMillis();
                wam.setDebug(debug);
                failed = wam.run();
                ms2 = System.currentTimeMillis();
                time += ms2 - ms;
            }

            if (failed) {
                write("\nno\n");
                break;
            } else {
                wam.printARGS();
                write("\n\n? ");
                answer = read();
                if (answer.equals(";")) {
                    if (wam.stack.moreChoicePoints()) {
                        failed = !wam.backtrack();
                    } else {
                        write("\nno\n");
                        break;
                    }
                } else {
                    if (!failed) {
                        write("\nyes\n");
                        break;
                    }
                    failed = true;
                }
            }
        }
        if (benchmark) {
            write("\nTotal time elapsed: " + time + " ms.\n");
        }

        return true;
    }

    public void teste() {
        /** nao e necesario, mas repete o codigo ... e necesario verificar se o label existe. */
        wam.clear();
        String test1 =
                "male1/1:	try_me_else male2/1\n" +
                        "			get_constant rui, A0\n" +
                        "			proceed\n" +
                        "male2/1:	try_me_else male3/1\n" +
                        "			get_constant bruno, A0\n" +
                        "			proceed\n" +
                        "male3/1:	try_me_else male4/1\n" +
                        "			get_constant joao, A0\n" +
                        "			proceed\n" +
                        "male4/1:	trust_me\n" +
                        "			get_constant pedro, A0\n" +
                        "			proceed\n";

        String query =
                "query$	set_variable A0" +
                        "		call male1/1";

        try {
            wam.loadSource(test1);
            wam.loadSource(query);
        } catch (Exception e) {
            e.printStackTrace();

            System.out.println("Exception: " + e.getMessage());
            wam.failAndExit("[program terminated]");
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            Jwam m = new Jwam();
            m.console();
            System.exit(1);
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h") || args[i].equals("--help")) {
                System.out.println("\nJProlog [-v|--version] [-h|--help]");
            } else if (args[i].equals("-v") || args[i].equals("--version")) {
                System.out.println("\nJProlog v" + VERSION);
            }
        }
    }
}