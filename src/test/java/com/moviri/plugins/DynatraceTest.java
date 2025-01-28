package com.moviri.plugins;

import com.moviri.plugins.collector.ExecutorCollector;
import com.moviri.plugins.collector.FilesystemMetricCollector;
import com.moviri.plugins.collector.JobLogCollector;
import com.moviri.plugins.config.DynatraceConfiguration;
import com.moviri.plugins.ws.DynatraceClient;
import com.moviri.plugins.ws.LogLine;
import com.moviri.plugins.ws.MintMetric;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynatraceTest {

    @Test
    void testLogPackageName() {
        assertEquals("com.moviri.plugins", Dynatrace.LOG_PACKAGE_NAME);
    }

    @Test
    void testLogRecorderName() {
        assertEquals("Dynatrace logs", Dynatrace.LOG_RECORDER_NAME);
    }

    @Test
    void testRecurrencePeriod() {
        Dynatrace dynatrace = new Dynatrace();
        assertEquals(TimeUnit.MINUTES.toMillis(1), dynatrace.getRecurrencePeriod());
    }

    @Test
    void testDoRunEmptyConfiguration() {
        Dynatrace dynatrace = spy();
        doReturn(null).when(dynatrace).getDynatraceConfiguration();

        assertDoesNotThrow(dynatrace::doRun);
        verify(dynatrace, times(0)).getDynatraceClient();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testDoRun() throws IOException {
        initialize();
        Dynatrace dynatrace = spy();
        DynatraceConfiguration configuration = mock();
        DynatraceClient client = mock();

        doReturn(client).when(dynatrace).getDynatraceClient();
        doReturn(configuration).when(dynatrace).getDynatraceConfiguration();

        assertDoesNotThrow(dynatrace::doRun);

        MintMetric fileSystemMetric = new MintMetric("myFakeFileSystemMetricKey", 1);
        MintMetric executorMetric = new MintMetric("myFakeExecutorMetricKey", 2);
        LogLine jobLogMetric = new LogLine("myFakeLogContent", "myFakeLogJob", "myFakeBuildId");

        ArgumentCaptor<List<MintMetric>> metricsCaptor = ArgumentCaptor.forClass(List.class);
        verify(client, times(2)).postMintMetrics(metricsCaptor.capture());
        List<List<MintMetric>> capturedMetrics = metricsCaptor.getAllValues();
        assertEquals(2, capturedMetrics.size());
        assertEquals(fileSystemMetric.getKey(), capturedMetrics.get(0).get(0).getKey());
        assertEquals(fileSystemMetric.getValue(), capturedMetrics.get(0).get(0).getValue());
        assertEquals(executorMetric.getKey(), capturedMetrics.get(1).get(0).getKey());
        assertEquals(executorMetric.getValue(), capturedMetrics.get(1).get(0).getValue());

        ArgumentCaptor<List<LogLine>> logCaptor = ArgumentCaptor.forClass(List.class);
        verify(client, times(1)).postLogLines(logCaptor.capture());
        List<List<LogLine>> capturedLogs = logCaptor.getAllValues();
        assertEquals(1, capturedLogs.size());
        assertEquals(jobLogMetric.toMap(), capturedLogs.get(0).get(0).toMap());
    }

    void initialize() {
        FilesystemMetricCollector filesystemMetricCollector = mock();
        ExecutorCollector executorCollector = mock();
        JobLogCollector jobLogCollector = mock();

        MintMetric fileSystemMetric = new MintMetric("myFakeFileSystemMetricKey", 1);
        MintMetric executorMetric = new MintMetric("myFakeExecutorMetricKey", 2);
        LogLine jobLogMetric = new LogLine("myFakeLogContent", "myFakeLogJob", "myFakeBuildId");

        doReturn(List.of(fileSystemMetric)).when(filesystemMetricCollector).collect();
        doReturn(List.of(executorMetric)).when(executorCollector).collect();
        doReturn(List.of(jobLogMetric)).when(jobLogCollector).collect();

        Dynatrace.metricCollectors.add(filesystemMetricCollector);
        Dynatrace.metricCollectors.add(executorCollector);
        Dynatrace.logCollectors.add(jobLogCollector);
    }
}