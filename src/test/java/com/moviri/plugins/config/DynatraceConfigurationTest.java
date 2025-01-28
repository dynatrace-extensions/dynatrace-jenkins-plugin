package com.moviri.plugins.config;

import lombok.SneakyThrows;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.kohsuke.stapler.StaplerRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynatraceConfigurationTest {

    @SneakyThrows
    @Test
    void testConfigure() {
        StaplerRequest req = mock();
        JSONObject jsonObject = mock();
        DynatraceConfiguration dynatraceConfiguration = spy(DynatraceConfiguration.createForTesting());
        doNothing().when(dynatraceConfiguration).save();

        assertTrue(dynatraceConfiguration.configure(req, jsonObject));
        verify(req, times(1)).bindJSON(dynatraceConfiguration, jsonObject);
    }
}
