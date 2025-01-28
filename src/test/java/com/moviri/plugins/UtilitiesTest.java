package com.moviri.plugins;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilitiesTest {

    @Test
    void testUnnecessaryQuotes() {
        assertEquals("my-fake-value", Utilities.encloseInQuotes("my-fake-value"));
    }

    @Test
    void testNecessaryQuotes() {
        assertEquals("\"my fake value\"", Utilities.encloseInQuotes("my fake value"));
    }

    @Test
    void testNecessaryQuotesBlankSpace() {
        assertEquals("\" \"", Utilities.encloseInQuotes(" "));
    }

    @Test
    void testUnnecessaryQuotesEmptyValue() {
        assertEquals("", Utilities.encloseInQuotes(""));
    }
}