package com.wrmsr.jwam;

/**
 * Excep��o devido a overflow na mem�ria.
 *
 * @author Bruno Sim�es e Pedro Guerreiro
 * @version 1.00
 * Warren's Abstract Machine
 */

public class MemoryOverflow
        extends Exception {

    private static final long serialVersionUID = 4993891090193862054L;

    /**
     * Construtor.
     */
    public MemoryOverflow() {
        super();
    }

    /**
     * Cria uma excep��o com dada string.
     *
     * @param s descri��o da excep��o.
     */

    public MemoryOverflow(String s) {
        super(s);
    }
}