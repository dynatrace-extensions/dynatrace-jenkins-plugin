package com.moviri.plugins.ws;

import com.moviri.plugins.config.DynatraceConfiguration;
import io.jenkins.cli.shaded.org.slf4j.Logger;
import io.jenkins.cli.shaded.org.slf4j.LoggerFactory;
import lombok.SneakyThrows;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynatraceClientTest {

    private static class MockedDynatraceClient extends DynatraceClient {
        @SneakyThrows
        @Override
        protected CloseableHttpClient getDefaultHttpClient() {
            CloseableHttpClient client = mock();
            CloseableHttpResponse response = mock();
            HttpEntity entity = mock();
            doReturn(12345).when(response).getCode();
            doReturn(response).when(client).execute(any());
            doReturn(entity).when(response).getEntity();
            doReturn(new ByteArrayInputStream("myFakeEntityContent".getBytes())).when(entity).getContent();
            return client;
        }
    }

    @Test
    void testFormatUrl() {
        DynatraceConfiguration dynatraceConfiguration = mock();
        DynatraceClient dynatraceClient = spy();
        doReturn("https://myFakeTenant.com").when(dynatraceConfiguration).getTenant();
        doReturn(dynatraceConfiguration).when(dynatraceClient).getDynatraceConfiguration();
        assertEquals("https://myFakeTenant.com/myEndpoint", dynatraceClient.formatUrl("/myEndpoint"));

        doReturn("https://myFakeTenant.com/").when(dynatraceConfiguration).getTenant();
        assertEquals("https://myFakeTenant.com/myEndpoint", dynatraceClient.formatUrl("/myEndpoint"));
    }

    @Test
    void testFormatMetricLines() {
        MintMetric mintMetric1 = new MintMetric("key1", 1);
        MintMetric mintMetric2 = new MintMetric("key2", 2);
        DynatraceClient dynatraceClient = spy();

        assertTrue(dynatraceClient.formatMetricLines(List.of(mintMetric1, mintMetric2)).contains("\n"));
    }

    @Test
    void testPostMintMetric() {
        DynatraceConfiguration dynatraceConfiguration = mock();
        DynatraceClient dynatraceClient = spy(new MockedDynatraceClient());

        doReturn(dynatraceConfiguration).when(dynatraceClient).getDynatraceConfiguration();
        doReturn("https://myFakeTenant.com").when(dynatraceConfiguration).getTenant();
        doReturn("myFakeApiToken").when(dynatraceConfiguration).getApiToken();

        MintMetric mintMetric = new MintMetric("key1", 1);
        assertDoesNotThrow(() -> dynatraceClient.postMintMetrics(List.of(mintMetric)));
    }

    @Test
    void testPostEmptyMintMetrics() {
        DynatraceConfiguration dynatraceConfiguration = mock();
        DynatraceClient dynatraceClient = spy(new MockedDynatraceClient());

        doReturn(dynatraceConfiguration).when(dynatraceClient).getDynatraceConfiguration();
        doReturn("https://myFakeTenant.com").when(dynatraceConfiguration).getTenant();
        doReturn("myFakeApiToken").when(dynatraceConfiguration).getApiToken();

        assertDoesNotThrow(() -> dynatraceClient.postMintMetrics(List.of()));
    }

    @Test
    void testPostLogLines() {
        DynatraceConfiguration dynatraceConfiguration = mock();
        DynatraceClient dynatraceClient = spy(new MockedDynatraceClient());

        doReturn(dynatraceConfiguration).when(dynatraceClient).getDynatraceConfiguration();
        doReturn("https://myFakeTenant.com").when(dynatraceConfiguration).getTenant();
        doReturn("myFakeApiToken").when(dynatraceConfiguration).getApiToken();

        LogLine logLine = new LogLine("myFakeContent", "myFakeJob", "myFakeBuildId");
        assertDoesNotThrow(() -> dynatraceClient.postLogLines(List.of(logLine)));
    }

    @Test
    void testPostEmptyLogLines() {
        DynatraceConfiguration dynatraceConfiguration = mock();
        DynatraceClient dynatraceClient = spy(new MockedDynatraceClient());

        doReturn(dynatraceConfiguration).when(dynatraceClient).getDynatraceConfiguration();
        doReturn("https://myFakeTenant.com").when(dynatraceConfiguration).getTenant();
        doReturn("myFakeApiToken").when(dynatraceConfiguration).getApiToken();

        assertDoesNotThrow(() -> dynatraceClient.postLogLines(List.of()));
    }
}
