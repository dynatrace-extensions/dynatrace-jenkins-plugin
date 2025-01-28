package com.moviri.plugins.collector;

import com.moviri.plugins.Preload;
import com.moviri.plugins.Utilities;
import com.moviri.plugins.ws.MintMetric;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import io.jenkins.cli.shaded.org.slf4j.Logger;
import io.jenkins.cli.shaded.org.slf4j.LoggerFactory;
import jenkins.model.Jenkins;
import lombok.Getter;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;

@Preload
public class FilesystemMetricCollector implements Collector<List<MintMetric>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilesystemMetricCollector.class);
    private static final String DIRECTORY_SIZE = "jenkins.fs.directory.size";
    private static final String DIRECTORY_FILE_COUNT = "jenkins.fs.directory.file_count";
    private static final String FS_TOTAL_SIZE = "jenkins.fs.total";
    private static final String FS_USED_SIZE = "jenkins.fs.used";
    private static final String FS_FREE_SIZE = "jenkins.fs.free";
    private static final String JOB_SIZE = "jenkins.fs.job.size";
    private static final String JOB_FILE_COUNT = "jenkins.fs.job.file_count";

    @Getter
    protected static class DirectorySize {
        private final long size;
        private final long count;
        public DirectorySize(long size, long count) {
            this.size = size;
            this.count = count;
        }
    }

    @Override
    public List<MintMetric> collect() {
        return Stream.of(
                this.collectFSMetrics(),
                this.collectDirectoryMetrics(),
                this.collectJobMetrics()
        ).flatMap(Collection::stream).collect(Collectors.toList());
    }

    /**
     * Collect metrics for each directory in JENKINS_HOME
     * @return list of metrics
     */
    protected List<MintMetric> collectDirectoryMetrics() {
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
                        .collect(Collectors.toMap(file -> file, file -> "JENKINS_HOME/" + file.getName()))

        );

        var metrics = new ArrayList<MintMetric>();
        for (Map.Entry<File, String> entry : directories.entrySet()) {
            var directorySize = this.calculateDirectorySize(entry.getKey());
            var dimensions = Map.ofEntries(entry("path", Utilities.encloseInQuotes(entry.getValue())));
            metrics.add(new MintMetric(DIRECTORY_SIZE, directorySize.getSize(), dimensions));
            metrics.add(new MintMetric(DIRECTORY_FILE_COUNT, directorySize.getCount(), dimensions));
        }

        return metrics;
    }

    /**
     * Calculate the directory size by summing all files and subdirectories within it.
     *
     * @param directory The parent directory file object
     * @return The size of the directory and count of files.
     */
    protected DirectorySize calculateDirectorySize(File directory) {
        long size = 0;
        long count = 0;
        File[] files = directory.listFiles();
        if (files == null) {
            return new DirectorySize(0, 0);
        }

        for (File file : files) {
            if (file.isDirectory()) {
                var directorySize = this.calculateDirectorySize(file);
                size += directorySize.getSize();
                count += directorySize.getCount();
            } else {
                size += file.length();
                count++;
            }
        }
        return new DirectorySize(size, count);
    }

    /**
     * Collect filesystem metrics for the entire disk
     * @return list of metrics
     */
    protected List<MintMetric> collectFSMetrics() {
        var jenkins = getJenkins();
        double totalDiskSpace = jenkins.getRootDir().getTotalSpace();
        double freeDiskSpace = jenkins.getRootDir().getFreeSpace();

        var metrics = new ArrayList<MintMetric>();
        metrics.add(new MintMetric(FS_TOTAL_SIZE, totalDiskSpace));
        metrics.add(new MintMetric(FS_FREE_SIZE, freeDiskSpace));
        metrics.add(new MintMetric(FS_USED_SIZE, (totalDiskSpace - freeDiskSpace)));
        return metrics;
    }

    /**
     * Collect disk metrics for all Jenkins jobs
     * @return list of metrics
     */
    protected List<MintMetric> collectJobMetrics() {
        var jenkins = getJenkins();
        var metrics = new ArrayList<MintMetric>();
        for (Job<?, ?> job : jenkins.getAllItems(Job.class)) {
            if (job instanceof TopLevelItem) {
                var directorySize = this.calculateDirectorySize(job.getRootDir());
                var dimensions = Map.ofEntries(entry("job", Utilities.encloseInQuotes(job.getName())));
                metrics.add(new MintMetric(JOB_SIZE, directorySize.getSize(), dimensions));
                metrics.add(new MintMetric(JOB_FILE_COUNT, directorySize.getCount(), dimensions));
            }
        }

        return metrics;
    }

    protected Jenkins getJenkins() {
        return Jenkins.get();
    }

}
