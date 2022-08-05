/******************************************************************************
 * Warren's Abstract Machine  -  Implementation by Stefan Buettcher
 *
 * developed:   December 2001 until February 2002
 *
 * Program.java contains the WAM program management class Program. A Program
 * consists of an array of Statements (cf. Statement.java).
 ******************************************************************************/
package com.wrmsr.jwam;

import java.util.TreeMap;
import java.util.Vector;

// Program class manages WAM programs, consisting of list (vector) of statements
public class Program {
    public static final int callWrite = -10;
    public static final int callWriteLn = -11;
    public static final int callNewLine = -12;
    public static final int callConsult = -13;
    public static final int callReconsult = -14;
    public static final int callLoad = -15;
    public static final int callAssert = -16;
    public static final int callRetractOne = -17;
    public static final int callRetractAll = -18;
    public static final int callIsInteger = -19;
    public static final int callIsAtom = -20;
    public static final int callIsBound = -21;
    public static final int callReadLn = -22;
    public static final int callCall = -23;

    private final Vector statements;
    public TreeMap labels;
    public Wam owner;

    public Program() {
        statements = new Vector();
        owner = null;
        labels = new TreeMap();
    }

    public Program(Wam anOwner) {
        statements = new Vector();
        owner = anOwner;
        labels = new TreeMap();
    }

    public void addProgram(Program p) {
        if (p == null) return;
        int cnt = p.getStatementCount();
        boolean canAdd = true;
        for (int i = 0; i < cnt; i++) {
            Statement s = p.getStatement(i);
            String label = s.getLabel();
            if (label.length() > 0) {
                if (labels.containsKey(label)) {
                    canAdd = false;
                    owner.writeLn("Error: Multiple occurrence of label \"" + label + "\". Use reconsult.");
                } else {
                    labels.put(label, new Integer(statements.size()));
                    canAdd = true;
                }
            }
            if (canAdd) addStatement(s);
        }
    }

    public void addStatement(Statement s) {
        statements.addElement(s);
    }

    public void addStatementAtPosition(Statement s, int position) {
        statements.insertElementAt(s, position);
    }

    public int getStatementCount() {
        return statements.size();
    }

    public Statement getStatement(int i) {
        return (Statement) statements.elementAt(i);
    }

    public int deleteFromLine(int lineNumber) {
        int result = 1;
        if (lineNumber >= 0) {
            statements.removeElementAt(lineNumber);
            while ((statements.size() > lineNumber) && (((Statement) statements.elementAt(lineNumber)).getLabel().length() == 0)) {
                result++;
                statements.removeElementAt(lineNumber);
            }
            updateLabels();
        }
        return result;
    }

    public int deleteFrom(String label) {
        return deleteFromLine(getLabelIndex(label));
    }

    public int getLastClauseOf(String procedureName) {
        int line = getLabelIndex(procedureName);
        if (line >= 0) {
            boolean finished = false;
            Statement s;
            do {
                s = getStatement(line);
                if ((s.operator == Statement.opTryMeElse) || (s.operator == Statement.opRetryMeElse)) line = s.jump;
                else finished = true;
            } while (!finished);
        }
        return line;
    }

    public int getLastClauseButOneOf(String procedureName) {
        int result = -1;
        int line = getLabelIndex(procedureName);
        if (line >= 0) {
            boolean finished = false;
            Statement s;
            do {
                s = getStatement(line);
                if ((s.operator == Statement.opTryMeElse) || (s.operator == Statement.opRetryMeElse)) {
                    result = line;
                    line = s.jump;
                } else {
                    finished = true;
                }
            } while (!finished);
        }
        return result;
    }

    public void addClause(String label, Program code) {
        int line = getLastClauseOf(label);
        if (line >= 0) {  // there already exists such a label: add via try_me_else
            Statement s = getStatement(line);
            int index = s.getLabel().indexOf('~');
            try {
                int i;
                if (index > 0) {
                    i = Integer.parseInt(s.getLabel().substring(index + 1)) + 1;
                } else {
                    i = 2;
                }
                // update the just-compiled program
                String newLabel = label + "~" + i;
                code.getStatement(0).setLabel(newLabel);
                // update the previous clause: trust_me -> try_me_else
                s.setFunction("try_me_else");
                s.getArgs().setElementAt(newLabel, 0);
                s.arg1 = newLabel;
                s.setJump(statements.size());
                // update labels and program itself
                addProgram(code);
            } catch (Exception e) {
            }
        } else {  // first label of that kind: just add to code and update jumpings
            addProgram(code);
            updateLabels();
        }
    }

    public int getLabelIndex(String label) {
        for (int i = 0; i < statements.size(); i++) {
            if (label.compareTo(((Statement) statements.elementAt(i)).getLabel()) == 0) {
                return i;
            }
        }
        return -1;
    }

    // updateLabels converts String label names in call, try_me_else and retry_me_else statements
    // to integer values. internal predicates (e.g. write, consult) are transformed to negative line numbers
    public void updateLabels() {
        labels = new TreeMap();
        String label;
        int cnt = statements.size();
        for (int i = 0; i < cnt; i++) {
            Statement s = (Statement) statements.elementAt(i);
            label = s.getLabel();
            if (label.length() > 0) {
                labels.put(label, new Integer(i));
            }
        }

        for (int i = 0; i < cnt; i++) {
            Statement s = (Statement) statements.elementAt(i);
            if ((s.getFunction().compareTo("call") == 0) ||
                    (s.getFunction().compareTo("not_call") == 0) ||
                    (s.getFunction().compareTo("try_me_else") == 0) ||
                    (s.getFunction().compareTo("retry_me_else") == 0)
            ) {
                label = (String) (s.getArgs().elementAt(0));
                s.setJump(-1);
                if (labels.containsKey(label)) { // label is a user-defined predicate
                    s.setJump(((Integer) labels.get(label)).intValue());
                } else {  // label is undefined or a built-in predicate
                    if (label.compareTo("atomic") == 0) {
                        s.setJump(callIsAtom);
                    } else if (label.compareTo("integer") == 0) {
                        s.setJump(callIsInteger);
                    } else if (label.compareTo("bound") == 0) {
                        s.setJump(callIsBound);
                    } else if (label.compareTo("write") == 0) {
                        s.setJump(callWrite);
                    } else if (label.compareTo("writeln") == 0) {
                        s.setJump(callWriteLn);
                    } else if (label.compareTo("call") == 0) {
                        s.setJump(callCall);
                    } else if ((label.compareTo("nl") == 0) || (label.compareTo("newline") == 0)) {
                        s.setJump(callNewLine);
                    } else if (label.compareTo("consult") == 0) {
                        s.setJump(callConsult);
                    } else if (label.compareTo("reconsult") == 0) {
                        s.setJump(callReconsult);
                    } else if (label.compareTo("load") == 0) {
                        s.setJump(callLoad);
                    } else if ((label.compareTo("assert") == 0) || (label.compareTo("assertz") == 0)) {
                        s.setJump(callAssert);
                    } else if ((label.compareTo("retract") == 0) || (label.compareTo("retractone") == 0)) {
                        s.setJump(callRetractOne);
                    } else if (label.compareTo("retractall") == 0) {
                        s.setJump(callRetractAll);
                    } else if (label.compareTo("readln") == 0) {
                        s.setJump(callReadLn);
                    }
                }
            }
        }
    }

    public String toString() {
        String result = "";
        for (int i = 0; i < statements.size(); i++) {
            String line = "(";
            if (i < 1000) {
                line += "0";
            }
            if (i < 100) {
                line += "0";
            }
            if (i < 10) {
                line += "0";
            }
            line += i + ")  ";
            result += line + statements.elementAt(i).toString();
            if (i < statements.size() - 1) {
                result += "\n";
            }
        }
        return result;
    }

} 
