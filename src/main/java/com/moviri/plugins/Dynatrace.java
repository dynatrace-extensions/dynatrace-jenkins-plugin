package com.moviri.plugins;

import com.moviri.plugins.collector.ExecutorCollector;
import com.moviri.plugins.collector.FilesystemMetricCollector;
import com.moviri.plugins.collector.JobLogCollector;
import com.moviri.plugins.config.DynatraceConfiguration;
import com.moviri.plugins.ws.DynatraceClient;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.logging.LogRecorder;
import hudson.model.PeriodicWork;
import io.jenkins.cli.shaded.org.slf4j.Logger;
import io.jenkins.cli.shaded.org.slf4j.LoggerFactory;
import jenkins.model.Jenkins;
import org.reflections.Reflections;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

@Extension
public class Dynatrace extends PeriodicWork {

    private static final Logger DT_LOGGER = LoggerFactory.getLogger(Dynatrace.class);
    private static final String VERSION = "1.0.4";
    private static final long RECURRENCE_PERIOD = TimeUnit.MINUTES.toMillis(1);
    public static final String LOG_PACKAGE_NAME = "com.moviri.plugins";
    public static final String LOG_RECORDER_NAME = "Dynatrace logs";

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    public static void initialize() {
        setupLogRecorder();
    }

    /**
     * Sets up a log recorder inside of Jenkins that is used to separate out the plugin logs from other plugin logs.
     * The log recorder gets a list of all the loggers in this project, but the loggers must be initialized
     * before they're noticed by the LogManager.
     */
    private static void setupLogRecorder() {
        DT_LOGGER.info("Setting up Dynatrace log recorder.");

        preloadLoggingClasses();

        // Get all logger names and filter for the ones created by this plugin.
        // Ignore the DynatraceManagementLink logger since that would cause a logging loop
        var pluginLogs = Collections.list(LogManager.getLogManager().getLoggerNames()).stream()
                .filter(
                        logName -> logName.startsWith(LOG_PACKAGE_NAME)
                ).filter(logName -> !(LOG_PACKAGE_NAME + ".DynatraceManagementLink").equals(logName))
                .collect(Collectors.toList());


        List<LogRecorder> logRecorders = Jenkins.get().getLog().getRecorders();
        boolean dynatraceLogRecorderExists = logRecorders.stream().anyMatch(logRecorder -> LOG_RECORDER_NAME.equals(logRecorder.getDisplayName()));

        if (dynatraceLogRecorderExists) {
            try {
                Jenkins.get().getLog().getLogRecorder(LOG_RECORDER_NAME).delete();
            } catch (IOException e) {
                DT_LOGGER.error("Error removing log recorder: {}", e);
                throw new RuntimeException(e);
            }
        }

        // Create a new log recorder for this plugins logs.
        LogRecorder dynatraceRecorder = new LogRecorder(LOG_RECORDER_NAME);
        List<LogRecorder.Target> targets = new ArrayList<>();
        for (String pluginLogName : pluginLogs) {
            targets.add(new LogRecorder.Target(pluginLogName, Level.ALL));
        }
        dynatraceRecorder.setLoggers(targets);
        logRecorders.add(dynatraceRecorder);
        Jenkins.get().getLog().setRecorders(logRecorders);
    }

    /**
     * Initializes the static fields of a class that has the '@Preload' annotation.
     */
    private static void preloadLoggingClasses() {
        Reflections reflections = new Reflections(LOG_PACKAGE_NAME);
        Set<Class<?>> classesToPreload = reflections.getTypesAnnotatedWith(Preload.class);
        for (Class<?> clazz : classesToPreload) {
            DT_LOGGER.info("Preloading class: {}", clazz.getName());
            try {
                Class.forName(clazz.getName());
            } catch (ClassNotFoundException e) {
                DT_LOGGER.error("Could not find class " + clazz.getName() + " : " + e.toString());
            }
        }
    }

    @Override
    public long getRecurrencePeriod() {
        return RECURRENCE_PERIOD;
    }

    private void collectFSMetrics(DynatraceClient client) {
        if (DynatraceConfiguration.get().isFilesystemMetricsEnabled()) {
            DT_LOGGER.info("Collecting Filesystem Metrics");
            client.postMintMetrics(new FilesystemMetricCollector().collect());
        }
    }

    private void collectExecutorMetrics(DynatraceClient client) {
        if (DynatraceConfiguration.get().isExecutorMetricsEnabled()) {
            DT_LOGGER.info("Collecting Executor Metrics");
            client.postMintMetrics(new ExecutorCollector().collect());
        }
    }

    private void collectJobLogs(DynatraceClient client) {
        if (DynatraceConfiguration.get().isJobLogEnabled()) {
            DT_LOGGER.info("Collecting Job Logs");
            client.postLogLines(new JobLogCollector().collect());
        }
    }

    @Override
    protected void doRun() {
        DT_LOGGER.info("Running query method ({}) ({})", Jenkins.getVersion(), VERSION);
        Instant start = Instant.now();
        DynatraceConfiguration config = getDynatraceConfiguration();
        if (config == null) {
            DT_LOGGER.error("Config is null.");
            return;
        }

        try {
            DynatraceClient client = getDynatraceClient();
            CompletableFuture<Void> fsMetrics = CompletableFuture.runAsync(() -> collectFSMetrics(client));
            CompletableFuture<Void> executorMetrics = CompletableFuture.runAsync(() -> collectExecutorMetrics(client));
            CompletableFuture<Void> jobLogs = CompletableFuture.runAsync(() -> collectJobLogs(client));
            CompletableFuture.allOf(fsMetrics, executorMetrics, jobLogs).join();
        } catch (Exception e) {
            DT_LOGGER.error(e.toString());
            e.printStackTrace();
        }
        Instant end = Instant.now();
        DT_LOGGER.info("Finished collection in {}ms", Duration.between(start, end).toMillis());

    }

    protected DynatraceClient getDynatraceClient() {
        var config = getDynatraceConfiguration();
        DT_LOGGER.info("Using Proxy: {}", config.getProxyUrl());
        return new DynatraceClient();
    }

    protected DynatraceConfiguration getDynatraceConfiguration() {
        return DynatraceConfiguration.get();
    }
}
