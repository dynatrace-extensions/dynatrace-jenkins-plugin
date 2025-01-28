package com.moviri.plugins;

public class Utilities {
    public static String encloseInQuotes(String value) {
        if (value.contains(" ")) {
            return "\"" + value + "\"";
        }
        return value;
    }
}
