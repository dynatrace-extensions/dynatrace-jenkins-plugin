package com.moviri.plugins.collector;

import com.moviri.plugins.Preload;
import com.moviri.plugins.Utilities;
import com.moviri.plugins.ws.MintMetric;
import hudson.FilePath;
import hudson.model.*;
import io.jenkins.cli.shaded.org.slf4j.Logger;
import io.jenkins.cli.shaded.org.slf4j.LoggerFactory;
import jenkins.model.Jenkins;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.moviri.plugins.collector.FilesystemUtils.calculateDirectorySize;

@Preload
public class FilesystemMetricCollector implements Collector<List<MintMetric>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilesystemMetricCollector.class);
    private static final String JENKINS_HOME = "JENKINS_HOME";
    private static final String DIRECTORY_SIZE = "jenkins.fs.directory.size";
    private static final String DIRECTORY_FILE_COUNT = "jenkins.fs.directory.file_count";
    private static final String FS_TOTAL_SIZE = "jenkins.fs.total";
    private static final String FS_USED_SIZE = "jenkins.fs.used";
    private static final String FS_FREE_SIZE = "jenkins.fs.free";
    private static final String JOB_SIZE = "jenkins.fs.job.size";
    private static final String JOB_FILE_COUNT = "jenkins.fs.job.file_count";

    @Override
    public List<MintMetric> collect() {
        LOGGER.info("Starting filesystem metric collector...");
        var metrics = Stream.of(
                this.collectFSMetrics(),
                this.collectLocalDirectoryMetrics(),
                this.collectJobMetrics()
        ).flatMap(Collection::stream).collect(Collectors.toList());

        var jenkins = getJenkins();
        // Iterate over all Jenkins nodes (not including the controller node)
        LOGGER.info("Got jenkins nodes: {}", jenkins.getNodes());
        for (Node jenkinsNode : jenkins.getNodes()) {
            metrics.addAll(this.collectRemoteDirectoryMetrics(jenkinsNode));
            metrics.addAll(this.collectRemoteFSMetrics(jenkinsNode));
        }

        return metrics;
    }

    /**
     * Collect metrics for nodes that are not the controller node.
     *
     * @return list of metrics
     */
    protected List<MintMetric> collectRemoteDirectoryMetrics(Node jenkinsNode) {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Collecting remote directory metrics...");
        var metrics = new ArrayList<MintMetric>();
        Map<FilePath, String> directories = new HashMap<>();
        var rootPath = jenkinsNode.getRootPath();
        if (rootPath == null) {
            LOGGER.warn("Could not get root path for Jenkins Node '" + jenkinsNode.getNodeName() + "'");
            return Collections.emptyList();
        }

        // Collect all the top level directories
        directories.put(rootPath, JENKINS_HOME);
        try {
            for (FilePath directory : rootPath.listDirectories()) {
                LOGGER.info("Remote Directory: " + directory.getName());
                directories.put(directory, JENKINS_HOME + "/" + directory.getName());
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error collecting remote metrics for '" + jenkinsNode.getNodeName() + "': " + e);
            return Collections.emptyList();
        }
        Map<String, String> commonDimensions = new HashMap<>();
        commonDimensions.put("node", jenkinsNode.getSelfLabel().getDisplayName());

        // Calculate and report metrics on all top level directories
        for (Map.Entry<FilePath, String> entry : directories.entrySet()) {
            Map<String, String> dimensions = new HashMap<>(commonDimensions);
            dimensions.put("path", Utilities.encloseInQuotes(entry.getValue()));

            try {
                var directorySize = entry.getKey().act(new DirectorySizeCallable());
                metrics.add(new MintMetric(DIRECTORY_SIZE, directorySize.getSize(), dimensions));
                metrics.add(new MintMetric(DIRECTORY_FILE_COUNT, directorySize.getCount(), dimensions));
            } catch (IOException | InterruptedException e) {
                LOGGER.error(e.toString());
            }
        }
        LOGGER.info("Added {} metric lines.", metrics.size());
        LOGGER.info("Collected remote directory metrics in {}ms", System.currentTimeMillis() - startTime);
        return metrics;
    }

    /**
     * Collect metrics for each directory in JENKINS_HOME
     *
     * @return list of metrics
     */
    protected List<MintMetric> collectLocalDirectoryMetrics() {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Collecting local directory metrics...");
        var jenkins = getJenkins();
        Map<File, String> directories = new HashMap<>();
        // Include the Jenkins root directory and the java temp dir in the metrics
        directories.put(jenkins.getRootDir(), "JENKINS_HOME");
        directories.put(new File(System.getProperty("java.io.tmpdir")), "TMPDIR");

        File[] jenkinsRootDirectories = jenkins.getRootDir().listFiles();
        if (jenkinsRootDirectories == null) {
            LOGGER.error("Jenkins root directory is empty.");
            return Collections.emptyList();
        }

        // Filter to only add directories to the `directories` map
        directories.putAll(
                Arrays.stream(jenkinsRootDirectories).filter(File::isDirectory)
                        .collect(Collectors.toMap(file -> file, file -> JENKINS_HOME + "/" + file.getName()))

        );

        Map<String, String> commonDimensions = new HashMap<>();
        commonDimensions.put("node", jenkins.getSelfLabel().getDisplayName());
        var metrics = new ArrayList<MintMetric>();
        for (Map.Entry<File, String> entry : directories.entrySet()) {
            Map<String, String> dimensions = new HashMap<>(commonDimensions);
            dimensions.put("path", Utilities.encloseInQuotes(entry.getValue()));

            try {
                var directorySize = calculateDirectorySize(entry.getKey());
                metrics.add(new MintMetric(DIRECTORY_SIZE, directorySize.getSize(), dimensions));
                metrics.add(new MintMetric(DIRECTORY_FILE_COUNT, directorySize.getCount(), dimensions));
            } catch (IOException e) {
                LOGGER.error(e.toString());
            }
        }
        LOGGER.info("Added {} metric lines.", metrics.size());
        LOGGER.info("Collected local directory metrics in {}ms", System.currentTimeMillis() - startTime);
        return metrics;
    }

    protected List<MintMetric> collectRemoteFSMetrics(Node jenkinsNode) {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Collecting remote filesystem metrics...");
        var metrics = new ArrayList<MintMetric>();
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("node", jenkinsNode.getSelfLabel().getDisplayName());
        var rootPath = jenkinsNode.getRootPath();
        if (rootPath == null) {
            LOGGER.warn("Could not get root path for Jenkins Node '" + jenkinsNode.getNodeName() + "'");
            return Collections.emptyList();
        }
        try {
            double totalDiskSpace = rootPath.getTotalDiskSpace();
            double freeDiskSpace = rootPath.getFreeDiskSpace();

            metrics.add(new MintMetric(FS_TOTAL_SIZE, totalDiskSpace, dimensions));
            metrics.add(new MintMetric(FS_FREE_SIZE, freeDiskSpace, dimensions));
            metrics.add(new MintMetric(FS_USED_SIZE, (totalDiskSpace - freeDiskSpace), dimensions));

        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error collecting remote FS metrics: " + e);
        }
        LOGGER.info("Added {} metric lines.", metrics.size());
        LOGGER.info("Collected remote filesystem metrics in {}ms", System.currentTimeMillis() - startTime);
        return metrics;
    }

    /**
     * Collect filesystem metrics for the entire disk
     *
     * @return list of metrics
     */
    protected List<MintMetric> collectFSMetrics() {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Collecting filesystem metrics...");
        var jenkins = getJenkins();
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("node", jenkins.getSelfLabel().getDisplayName());
        double totalDiskSpace = jenkins.getRootDir().getTotalSpace();
        double freeDiskSpace = jenkins.getRootDir().getFreeSpace();

        var metrics = new ArrayList<MintMetric>();
        metrics.add(new MintMetric(FS_TOTAL_SIZE, totalDiskSpace, dimensions));
        metrics.add(new MintMetric(FS_FREE_SIZE, freeDiskSpace, dimensions));
        metrics.add(new MintMetric(FS_USED_SIZE, (totalDiskSpace - freeDiskSpace), dimensions));
        LOGGER.info("Added {} metric lines.", metrics.size());
        LOGGER.info("Collected filesystem metrics in {}ms", System.currentTimeMillis() - startTime);
        return metrics;
    }

    /**
     * Collect disk metrics for all Jenkins jobs
     *
     * @return list of metrics
     */
    protected List<MintMetric> collectJobMetrics() {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Collecting job filesystem metrics...");
        var jenkins = getJenkins();
        var metrics = new ArrayList<MintMetric>();
        Map<String, String> commonDimensions = new HashMap<>();
        commonDimensions.put("node", jenkins.getSelfLabel().getDisplayName());

        for (Job<?, ?> job : jenkins.getAllItems(Job.class)) {
            if (job instanceof TopLevelItem) {
//                job.getBuilds().get(0).getDuration()
                Map<String, String> dimensions = new HashMap<>(commonDimensions);
                dimensions.put("job", Utilities.encloseInQuotes(job.getName()));
                dimensions.put("job_full_name", Utilities.encloseInQuotes(job.getFullName()));

                try {
                    var directorySize = calculateDirectorySize(job.getRootDir());
                    metrics.add(new MintMetric(JOB_SIZE, directorySize.getSize(), dimensions));
                    metrics.add(new MintMetric(JOB_FILE_COUNT, directorySize.getCount(), dimensions));
                } catch (IOException e) {
                    LOGGER.error(e.toString());
                }
            }
        }

        LOGGER.info("Added {} metric lines.", metrics.size());
        LOGGER.info("Collected job filesystem metrics in {}ms", System.currentTimeMillis() - startTime);
        return metrics;
    }

    protected Jenkins getJenkins() {
        return Jenkins.get();
    }

}
