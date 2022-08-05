package com.wrmsr.jwam;

import java.util.Hashtable;

/**
 * Defini��o do simbolos usados no programa.
 *
 * @author Bruno Sim�es e Pedro Guerreiro
 * @version 1.00
 * Warren's Abstract Machine.
 */

public class Symbol {

    /**
     * Tamanho das instru��es. eg INSTRUCTIONS_SIZE[PUT_VARIABLE]
     */
    public static final int[] INSTRUCTIONS_SIZE = {
            3, 3, 3, 3, 2, 3,
            3, 3, 3, 2, 3,
            2, 2, 2, 2, 2,
            2, 2, 2, 2, 2,
            1, 1, 2, 2, 1,
            2, 2, 1, 2, 2, 2,
            5, 3, 3,
            1, 2, 2,
            1
    };

    /* PUT INSTRUCTIONS */

    /**
     * ID para a instru��o "put_variable".
     */
    public static final int PUT_VARIABLE = 0;
    /**
     * ID para a instru��o "put_value".
     */
    public static final int PUT_VALUE = 1;
    /**
     * ID para a instru��o "put_unsafe_value".
     */
    public static final int PUT_UNSAFE_VALUE = 2;
    /**
     * ID para a instru��o "put_structure".
     */
    public static final int PUT_STRUCTURE = 3;
    /**
     * ID para a instru��o "put_list".
     */
    public static final int PUT_LIST = 4;
    /**
     * ID para a instru��o "put_constant".
     */
    public static final int PUT_CONSTANT = 5;

    /* GET INSTRUCTIONS */

    /**
     * ID para a instru��o "get_variable".
     */
    public static final int GET_VARIABLE = 6;
    /**
     * ID para a instru��o "get_value".
     */
    public static final int GET_VALUE = 7;
    /**
     * ID para a instru��o "get_structure".
     */
    public static final int GET_STRUCTURE = 8;
    /**
     * ID para a instru��o "get_list".
     */
    public static final int GET_LIST = 9;
    /**
     * ID para a instru��o "get_constant".
     */
    public static final int GET_CONSTANT = 10;

    /* SET INSTRUCTIONS */

    /**
     * ID para a instru��o "set_variable".
     */
    public static final int SET_VARIABLE = 11;
    /**
     * ID para a instru��o "set_value".
     */
    public static final int SET_VALUE = 12;
    /**
     * ID para a instru��o "set_local_value".
     */
    public static final int SET_LOCAL_VALUE = 13;
    /**
     * ID para a instru��o "set_constant".
     */
    public static final int SET_CONSTANT = 14;
    /**
     * ID para a instru��o "set_void".
     */
    public static final int SET_VOID = 15;

    /* UNIFY INSTRUCTIONS */

    /**
     * ID para a instru��o "unify_variable".
     */
    public static final int UNIFY_VARIABLE = 16;
    /**
     * ID para a instru��o "unify_value".
     */
    public static final int UNIFY_VALUE = 17;
    /**
     * ID para a instru��o "unify_local_value".
     */
    public static final int UNIFY_LOCAL_VALUE = 18;
    /**
     * ID para a instru��o "unify_constant".
     */
    public static final int UNIFY_CONSTANT = 19;
    /**
     * ID para a instru��o "unify_void".
     */
    public static final int UNIFY_VOID = 20;

    /* CONTROL INSTRUCTIONS */

    /**
     * ID para a instru��o "allocate".
     */
    public static final int ALLOCATE = 21;
    /**
     * ID para a instru��o "deallocate".
     */
    public static final int DEALLOCATE = 22;
    /**
     * ID para a instru��o "call".
     */
    public static final int CALL = 23;
    /**
     * ID para a instru��o "execute".
     */
    public static final int EXECUTE = 24;
    /**
     * ID para a instru��o "proceed".
     */
    public static final int PROCEED = 25;

    /* CHOISE INSTRUCTIONS */

    /**
     * ID para a instru��o "try_me_else".
     */
    public static final int TRY_ME_ELSE = 26;
    /**
     * ID para a instru��o "retry_me_else".
     */
    public static final int RETRY_ME_ELSE = 27;
    /**
     * ID para a instru��o "trust_me".
     */
    public static final int TRUST_ME = 28;
    /**
     * ID para a instru��o "try".
     */
    public static final int TRY = 29;
    /**
     * ID para a instru��o "retry".
     */
    public static final int RETRY = 30;
    /**
     * ID para a instru��o "trust".
     */
    public static final int TRUST = 31;

    /* INDEXING INSTRUCTIONS */

    /**
     * ID para a instru��o "switch_on_term".
     */
    public static final int SWITCH_ON_TERM = 32;
    /**
     * ID para a instru��o "switch_on_constant".
     */
    public static final int SWITCH_ON_CONSTANT = 33;
    /**
     * ID para a instru��o "switch_on_structure".
     */
    public static final int SWITCH_ON_STRUCTURE = 34;

    /* CUT INSTRUCTIONS */

    /**
     * ID para a instru��o "neck_cut".
     */
    public static final int NECK_CUT = 35;
    /**
     * ID para a instru��o "get_level".
     */
    public static final int GET_LEVEL = 36;
    /**
     * ID para a instru��o "cut".
     */
    public static final int CUT = 37;

    /**
     * ID para a instru��o "halt".
     */
    public static final int HALT = 38;

    /**
     * Representa��o textual de todas as instru��es.
     */
    private final static String[] WAM_KEYWORDS = {
            "put_variable",
            "put_value",
            "put_unsafe_value",
            "put_structure",
            "put_list",
            "put_constant",
            "get_variable",
            "get_value",
            "get_structure",
            "get_list",
            "get_constant",
            "set_variable",
            "set_value",
            "set_local_value",
            "set_constant",
            "set_void",
            "unify_variable",
            "unify_value",
            "unify_local_value",
            "unify_constant",
            "unify_void",
            "allocate",
            "deallocate",
            "call",
            "execute",
            "proceed",
            "try_me_else",
            "retry_me_else",
            "trust_me",
            "try",
            "retry",
            "trust",
            "switch_on_term",
            "switch_on_constant",
            "switch_on_structure",
            "neck_cut",
            "get_level",
            "cut",
            "halt"
    };

    /**
     * Atribui��o as instru��es de um identificador.
     */
    public static final Hashtable<String, Integer> keys =
            new Hashtable<String, Integer>();

    static {
        for (int i = 0; i < WAM_KEYWORDS.length; i++) {
            keys.put(WAM_KEYWORDS[i], new Integer(i));
        }
    }

    /**
     * A vari�vel � uma unbound variable.
     */
    public static final int UNB = 0;

    /**
     * A vari�vel � uma refer�ncia.
     */
    public static final int REF = 1;

    /**
     * A vari�vel � uma constante.
     */
    public static final int CON = 2;

    /**
     * A vari�vel � uma lista.
     */
    public static final int LIS = 3;

    /**
     * A vari�vel � uma estrutura.
     */
    public static final int STR = 4;

    /**
     * A vari�vel � um functor.
     */
    public static final int FUN = 5;

    /**
     * Modo de leitura.
     */
    public static final int READ = 6;

    /**
     * Modo de escrita.
     */
    public static final int WRITE = 7;
}