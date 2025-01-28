package com.moviri.plugins;

import com.moviri.plugins.collector.Collector;
import com.moviri.plugins.collector.ExecutorCollector;
import com.moviri.plugins.collector.FilesystemMetricCollector;
import com.moviri.plugins.collector.JobLogCollector;
import com.moviri.plugins.config.DynatraceConfiguration;
import com.moviri.plugins.ws.DynatraceClient;
import com.moviri.plugins.ws.LogLine;
import com.moviri.plugins.ws.MintMetric;
import hudson.Extension;
import hudson.PluginWrapper;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.logging.LogRecorder;
import hudson.model.PeriodicWork;
import io.jenkins.cli.shaded.org.slf4j.Logger;
import io.jenkins.cli.shaded.org.slf4j.LoggerFactory;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.reflections.Reflections;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

@Extension
public class Dynatrace extends PeriodicWork {

    private static final Logger DT_LOGGER = LoggerFactory.getLogger(Dynatrace.class);
    protected static final List<Collector<List<MintMetric>>> metricCollectors = new ArrayList<>();
    protected static final List<Collector<List<LogLine>>> logCollectors = new ArrayList<>();

    private static final long RECURRENCE_PERIOD = TimeUnit.MINUTES.toMillis(1);
    public static final String LOG_PACKAGE_NAME = "com.moviri.plugins";
    public static final String LOG_RECORDER_NAME = "Dynatrace logs";

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    public static void initialize() {
        DT_LOGGER.info("initialize");
        metricCollectors.add(new FilesystemMetricCollector());
        metricCollectors.add(new ExecutorCollector());

        logCollectors.add(new JobLogCollector());
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
            return;
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

    @Override
    protected void doRun() {
        DT_LOGGER.info("Running query method");
        DynatraceConfiguration config = getDynatraceConfiguration();
        if (config == null) {
            DT_LOGGER.error("Config is null.");
            return;
        }

        try {
            DynatraceClient client = getDynatraceClient();
            for (Collector<List<MintMetric>> metricCollector : metricCollectors) {
                var mintLines = metricCollector.collect();
                client.postMintMetrics(mintLines);
            }

            for (Collector<List<LogLine>> logCollector : logCollectors) {
                var logLines = logCollector.collect();
                client.postLogLines(logLines);
            }
        } catch (Exception e) {
            DT_LOGGER.error(e.toString());
        }
    }

    protected DynatraceClient getDynatraceClient() {
        return new DynatraceClient();
    }

    protected DynatraceConfiguration getDynatraceConfiguration() {
        return DynatraceConfiguration.get();
    }
}
