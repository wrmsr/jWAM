package com.wrmsr.jwam;

/**
 * Funções úteis.
 *
 * @author Bruno Simões e Pedro Guerreiro
 * @version 1.00
 * Warren's Abstract Machine
 */

public class Util {

    /**
     * Construtor.
     */
    public Util() {
    }

    /**
     * Dado um registo do tipo A11/X11/Y11 devolve eg. 11.
     *
     * @param name nome do registo.
     * @return Devolve o n�mero que sucede a letra do registo.
     */

    public static int getRegisterIndex(String name) {
        int length = name.length();
        int counter = 0;
        int index = 0;
        while (++counter < length) {
            index = index * 10 + (name.charAt(counter) - '0');
        }
        return index;
    }

    /**
     * Passa uma string para inteiro.
     *
     * @param s String a converter para inteiro.
     * @return Devolve o inteiro convertido da String.
     */

    public static int parseInt(String s)
            throws Exception {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            throw new Exception("Formato de n�mero invalido: " + s);
        }
    }

    /**
     * Dado um n�mero devolve o n�mero de digitos para o representar.
     *
     * @param n n�mero a analisar.
     * @return N�mero de digitos necess�rios para o representar.
     */

    public static int nrDigits(int n) {
        int t = 0;
        while (n > 1) {
            if (n >= 10) {
                t++;
            }
            n = n / 10;
        }
        return t + 1;
    }

    /**
     * Cria uma string com <i>n</i> espa�os.
     *
     * @param n n�mero de espa�os.
     * @return Devolve uma String com <i>n</i> espa�os.
     */

    public static String createSpaces(int n) {
        String s = "";
        for (int i = 0; i < n; i++) {
            s += " ";
        }
        return s;
    }

    /**
     * Calcula a aridade de um functor no seguinte formato: 'functor/aridade'
     *
     * @param functor functor/aridade.
     * @return Devolve a aridade do functor.
     */

    public static int arity(String functor)
            throws Exception {
        int i = functor.lastIndexOf("/");
        return Util.parseInt(functor.substring(i + 1).trim());
    }
}