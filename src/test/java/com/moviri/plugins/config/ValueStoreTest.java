package com.moviri.plugins.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ValueStoreTest {

    @Test
    void testGetEmptyBuildId() {
        ValueStore valueStore = spy(ValueStore.createForTesting());
        assertEquals(0, valueStore.getLastBuildId("myFakePipeline"));
    }

    @Test
    void testSetBuildId() {
        ValueStore valueStore = spy(ValueStore.createForTesting());
        doNothing().when(valueStore).save();
        assertEquals(0, valueStore.getLastBuildId("myFakePipeline"));
        valueStore.setLastBuildId("myFakePipeline", 12345);
        assertEquals(12345, valueStore.getLastBuildId("myFakePipeline"));
    }

    @Test
    void testConfigure() {
        ValueStore valueStore = ValueStore.createForTesting();
        assertTrue(valueStore.configure(mock(), mock()));
    }
}