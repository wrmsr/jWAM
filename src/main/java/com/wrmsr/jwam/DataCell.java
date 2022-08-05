package com.wrmsr.jwam;

/**
 * Representação de uma célula de memória.
 *
 * @author Bruno Simões
 * @author Pedro Guerreiro
 * @version 1.00
 */

public class DataCell
        extends Symbol {

    /**
     * Conteúdo da variável no caso de REF...
     */
    private int value;

    /**
     * Conteúdo da variável no caso de CON, FUN...
     */
    private String data;

    /**
     * UNB, REF, CON, LIS, STR...
     */
    private int tag;

    /**
     * Label associado a esta célula.
     */
    private String label = " ";

    /**
     * Constroi uma célula apartir de um TAG e uma referência.
     *
     * @param tag   TAG.
     * @param value referência.
     */

    public DataCell(int tag, int value) {
        this.set(tag, value);
    }

    /**
     * Cria uma célula contendo uma referência.
     *
     * @param value é suposto ser um endereço.
     */

    public DataCell(int value) {
        this.set(REF, value);
    }

    /**
     * Cria uma célula com determinado TAG, eg. CON, FUN, dados: e.g halt e REF.
     *
     * @param tag   TAG.
     * @param value informação em String.
     * @param addr  referência.
     */

    public DataCell(int tag, String value, int addr) {
        this.tag = tag;
        this.data = value;
        this.value = addr;
    }

    /**
     * Define um novo TAG e REF.
     *
     * @param tag   novo TAG.
     * @param value nova REF.
     */

    public void set(int tag, int value) {
        this.tag = tag;
        this.value = value;
    }

    /**
     * Associa um label a esta célula.
     *
     * @param l label a associar.
     */

    public void setLabel(String l) {
        label = l;
    }

    /**
     * Obtem o label associado à célula.
     *
     * @return Devolve o label associado à célula.
     */

    public String label() {
        return label;
    }

    /**
     * Compara dois labels.
     *
     * @param label label a comparar com o desta célula.
     * @return Devolve true se forem iguais.
     */

    public boolean compareToLabel(String label) {
        return this.label.equals(label);
    }

    /**
     * Obtem uma referência se for esse o conteúdo desta célula, else -1.
     *
     * @return Devolve a referencia se for esse o conteúdo desta célula, else -1.
     */

    public int getValue() {
        return (tag == REF || tag == CON) ? value : -1;
    }

    /**
     * Define dados nesta célula.
     *
     * @param data dados a inserir na célula.
     */

    public void setStringValue(String data) {
        this.data = data;
    }

    /**
     * Obtem a constante ou o functor se existirem.
     *
     * @return Devolve a constante ou o functor se existirem nesta célula.
     */

    public String getStringValue() {
        return (tag == CON || tag == FUN) ? data : "";
    }

    /**
     * Obtem o TAG desta célula.
     *
     * @return Devolve o TAG desta célula.
     */

    public int getTag() {
        return tag;
    }

    /**
     * Compara dois TAGS.
     *
     * @param tag TAG a comparar com o desta célula.
     */

    public boolean equals(int tag) {
        return this.tag == tag;
    }

    /**
     * Copia a informação de uma célula para esta.
     *
     * @param dc célula a copiar.
     */

    public void copyFrom(DataCell dc) {
        if (dc == null) {
            System.out.println("trying to copy a null datacell");
            return;
        }

        this.tag = dc.getTag();

        if (dc.equals(REF)) {
            this.value = dc.getValue();
        } else {
            this.data = dc.getStringValue();
        }
    }

    /**
     * Compara dois functores.
     *
     * @param f functor a comparar com o desta célula.
     * @return Devolve true se forem iguais.
     */

    public boolean compareFunctor(String f) {
        return (equals(FUN) && data.equals(f));
    }

    /**
     * Apresenta a celula numa representação textual.
     *
     * @return Devolve uma String que representa a celula.
     */

    public String toString() {
        if (equals(FUN)) {
            return "[ FUN | " + data + " ]";
        } else if (equals(CON)) {
            return "[ CON | " + data + " ]";
        } else if (equals(STR)) {
            return "[ STR | " + value + " ]";
        } else if (equals(REF)) {
            return "[ REF | " + value + " ]";
        } else if (equals(LIS)) {
            return "[ LIS | " + value + " ]";
        } else {
            return "[ _ | " + value + " ]";
        }
    }
}
