package com.moviri.plugins.ws;

import com.moviri.plugins.config.DynatraceConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationHeaderTest {

    @Test
    void testIsSensitive() {
        AuthenticationHeader header = new AuthenticationHeader();
        assertTrue(header.isSensitive());
    }

    @Test
    void testGetName() {
        AuthenticationHeader header = new AuthenticationHeader();
        assertEquals("Authorization", header.getName());
    }

    @Test
    void testGetValue() {
        AuthenticationHeader header = spy();

        DynatraceConfiguration configuration = mock();
        doReturn("myFakeApiToken").when(configuration).getApiToken();
        doReturn(configuration).when(header).getDynatraceConfiguration();

        assertEquals("Api-Token myFakeApiToken", header.getValue());
    }
}