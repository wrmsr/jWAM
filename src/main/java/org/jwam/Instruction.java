package org.jwam;

import java.util.Vector;

/**
 * Representa��o de uma instru��o.
 *
 * @author Bruno Sim�es e Pedro Guerreiro
 * @version 1.00
 *          Warren's Abstract Machine
 */

public class Instruction
{

    /**
     * Lista de argumentos da instru��o.
     */
    Vector<String> args;

    /**
     * Operador da instru��o. eg. allocate.
     */
    String op;

    /**
     * Identifica o primeiro argumento a ser retirado.
     */
    int i = 0;

    /**
     * Construtor para uma instru��o.
     *
     * @param inst operador da instru��o.
     */

    public Instruction(String inst)
    {
        op = inst;
        args = new Vector<String>();
    }

    /**
     * Adiciona argumentos � instru��o.
     *
     * @param arg argumento a adicionar.
     */

    public void addNewArg(String arg)
    {
        args.add(arg);
    }

    /**
     * Coloca o getArg() a apontar para o primeiro argumento.
     */
    public void reset() { i = 0; }

    /**
     * Obtem o operador da instru��o.
     *
     * @return Devolve uma String com o operador da instru��o.
     */

    public String getOp()
    {
        return op;
    }

    /**
     * Obtem um argumento da instru��o e passa a apontar para o seguinte.
     *
     * @return Devolve uma String com o argumento.
     */

    public String getArg()
    {
        return args.get(i++);
    }

    /**
     * Cria uma String com a representa��o da instru��o.
     *
     * @return Devolve uma String com a representa��o da instru��o.
     */

    public String toString()
    {
        String s = op;
        if (args.size() != 0) {
            s += " " + args.get(i);
        }

        for (int i = 1; i < args.size(); i++) {
            s += ", " + args.get(i);
        }
        return s;
    }
}