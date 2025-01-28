package com.moviri.plugins.collector;

import com.moviri.plugins.Preload;
import com.moviri.plugins.ws.MintMetric;
import hudson.model.Label;
import hudson.model.LoadStatistics;
import io.jenkins.cli.shaded.org.slf4j.Logger;
import io.jenkins.cli.shaded.org.slf4j.LoggerFactory;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

@Preload
public class ExecutorCollector implements Collector<List<MintMetric>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorCollector.class);
    private static final String EXECUTORS_QUEUE_LENGTH = "jenkins.executors.queue_length";
    private static final String EXECUTORS_ONLINE = "jenkins.executors.online";
    private static final String EXECUTORS_CONNECTING = "jenkins.executors.connecting";
    private static final String EXECUTORS_AVAILABLE = "jenkins.executors.available";
    private static final String EXECUTORS_IDLE = "jenkins.executors.idle";
    private static final String EXECUTORS_BUSY = "jenkins.executors.busy";
    private static final String EXECUTORS_DEFINED = "jenkins.executors.defined";

    @Override
    public List<MintMetric> collect() {
        LOGGER.info("Collecting Executor metrics");
        List<MintMetric> metrics = new ArrayList<>();
        for (Label l : getJenkins().getLabels()) {
            var dimensions = Map.ofEntries(entry("label", l.getDisplayName()));
            var loadStatistics = getLoadStatistics(l);

            metrics.add(new MintMetric(EXECUTORS_QUEUE_LENGTH, loadStatistics.getQueueLength(), dimensions));
            metrics.add(new MintMetric(EXECUTORS_ONLINE, loadStatistics.getOnlineExecutors(), dimensions));
            metrics.add(new MintMetric(EXECUTORS_CONNECTING, loadStatistics.getConnectingExecutors(), dimensions));
            metrics.add(new MintMetric(EXECUTORS_AVAILABLE, loadStatistics.getAvailableExecutors(), dimensions));
            metrics.add(new MintMetric(EXECUTORS_IDLE, loadStatistics.getIdleExecutors(), dimensions));
            metrics.add(new MintMetric(EXECUTORS_BUSY, loadStatistics.getBusyExecutors(), dimensions));
            metrics.add(new MintMetric(EXECUTORS_DEFINED, loadStatistics.getDefinedExecutors(), dimensions));
        }

        return metrics;
    }

    protected LoadStatistics.LoadStatisticsSnapshot getLoadStatistics(Label l) {
        return l.loadStatistics.computeSnapshot();
    }

    protected Jenkins getJenkins() {
        return Jenkins.get();
    }

}
