package com.wrmsr.jwam;

/**
 * Excepções que ocorrem na Heap.
 *
 * @author Bruno Sim�es e Pedro Guerreiro
 * @version 1.00
 * Warren's Abstract Machine
 */

public class HeapException
        extends Exception {

    private static final long serialVersionUID = 400765544833692031L;

    /**
     * Construtor.
     */
    public HeapException() {
        super();
    }

    /**
     * Cria uma excep��o com dada string.
     *
     * @param s descri��o da excep��o.
     */

    public HeapException(String s) {
        super(s);
    }
}