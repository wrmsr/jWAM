package com.wrmsr.jwam;

//class KeyValue helps implementing mappings "A=B" (key/value pairs)
public class KeyValue {
    public String key;
    public String stringValue;
    public int intValue;

    // create a new pair with key k and String value v
    public KeyValue(String k, String v) {
        key = k;
        stringValue = v;
        intValue = -12345;
    } // end of KeyValue.KeyValue(String, String)

    // create a new pair with key k and int value v
    public KeyValue(String k, int v) {
        key = k;
        intValue = v;
        stringValue = "";
    } // end of KeyValue.KeyValue(String, int)

    // in order to display the mapping on the screen (for debug purposes only)
    public String toString() {
        if (stringValue.length() == 0)
            return "[" + key + "=" + intValue + "]";
        else
            return "[" + key + "=" + stringValue + "]";
    } // end of KeyValue.toString()

}
