package com.wrmsr.jwam;

/**
 * Excep��o numa instru��o invalida.
 *
 * @author Bruno Sim�es e Pedro Guerreiro
 * @version 1.00
 * Warren's Abstract Machine
 */

public class InvalidInstruction
        extends Exception {

    private static final long serialVersionUID = -2344059694921443437L;

    /**
     * Construtor.
     */
    public InvalidInstruction() {
        super();
    }

    /**
     * Cria uma excep��o com dada string.
     *
     * @param s descri��o da excep��o.
     */

    public InvalidInstruction(String s) {
        super(s);
    }
}