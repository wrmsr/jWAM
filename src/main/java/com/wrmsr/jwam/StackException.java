package com.wrmsr.jwam;

/**
 * Excep��es que ocorrem na stack.
 *
 * @author Bruno Sim�es e Pedro Guerreiro
 * @version 1.00
 * Warren's Abstract Machine
 */

public class StackException
        extends Exception {
    private static final long serialVersionUID = 8834448563464345382L;

    public StackException() {
        super();
    }

    /**
     * Cria uma excep��o com dada string.
     *
     * @param s descri��o da excep��o.
     */

    public StackException(String s) {
        super(s);
    }
}