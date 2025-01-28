package com.moviri.plugins.collector;

import com.moviri.plugins.ws.MintMetric;
import hudson.model.Label;
import hudson.model.LoadStatistics;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExecutorCollectorTest {

    @Test
    void testCollect() {
        ExecutorCollector collector = spy();
        Jenkins jenkins = mock();
        doReturn(jenkins).when(collector).getJenkins();
        Label label = mock();
        doReturn("myFakeLabel").when(label).getDisplayName();
        Set<Label> labels = Set.of(label);
        doReturn(labels).when(jenkins).getLabels();
        LoadStatistics.LoadStatisticsSnapshot loadStatisticsSnapshot = mock();
        doReturn(1).when(loadStatisticsSnapshot).getQueueLength();
        doReturn(2).when(loadStatisticsSnapshot).getOnlineExecutors();
        doReturn(3).when(loadStatisticsSnapshot).getConnectingExecutors();
        doReturn(4).when(loadStatisticsSnapshot).getAvailableExecutors();
        doReturn(5).when(loadStatisticsSnapshot).getIdleExecutors();
        doReturn(6).when(loadStatisticsSnapshot).getBusyExecutors();
        doReturn(7).when(loadStatisticsSnapshot).getDefinedExecutors();
        doReturn(loadStatisticsSnapshot).when(collector).getLoadStatistics(label);

        List<MintMetric> collected = collector.collect();

        assertEquals(7, collected.size());
        for (int i = 0; i < collected.size(); i += 1) {
            MintMetric metric = collected.get(i);
            assertEquals(i + 1, metric.getValue());
            assertEquals(Map.of("label", "myFakeLabel"), metric.getDimensions());
        }
    }
}