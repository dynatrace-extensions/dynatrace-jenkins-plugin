package com.moviri.plugins.collector;

import com.moviri.plugins.Preload;
import com.moviri.plugins.config.ValueStore;
import com.moviri.plugins.ws.LogLine;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.security.ACLContext;
import io.jenkins.cli.shaded.org.slf4j.Logger;
import io.jenkins.cli.shaded.org.slf4j.LoggerFactory;
import jenkins.model.Jenkins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

// https://github.com/LarrysGIT/Extract-Jenkins-Raw-Log
@Preload
public class JobLogCollector implements Collector<List<LogLine>> {
    public static final Logger LOGGER = LoggerFactory.getLogger(JobLogCollector.class);
    private static final String PREAMBLE_STR = "\u001B\\[8mha:";
    private static final String POSTAMBLE_STR = "\u001B\\[0m";

    @Override
    public List<LogLine> collect() {
        LOGGER.info("Collecting logs...");
        try (ACLContext ctx = ACL.as2(ACL.SYSTEM2)) {
            var jenkins = getJenkins();
            var logLines = new ArrayList<LogLine>();
            jobLoop:
            for (Job<?, ?> job : jenkins.getAllItems(Job.class)) {
                var pipelineFullName = job.getFullName();
                var pipelineRootDir = job.getRootDir().toPath();
                int currentBuildNumber = getValueStore().getLastBuildId(pipelineFullName);
                int nextBuildNumber;
                try {
                    nextBuildNumber = this.getNextBuildNumber(pipelineRootDir);
                } catch (Exception e) {
                    LOGGER.warn("Could not read nextBuildNumber for " + pipelineFullName + " (likely never triggered). Skipping.");
                    continue;
                }

                var buildDirectory = pipelineRootDir.resolve("builds");
                if (currentBuildNumber >= nextBuildNumber) {
                    LOGGER.info("Skipping " + pipelineFullName + " since there are no new logs after build id '" + currentBuildNumber + "'");
                    continue;
                }
                while (currentBuildNumber < nextBuildNumber) {
                    var build = job.getBuild(String.valueOf(currentBuildNumber));
                    if (build == null) {
                        LOGGER.info("Build '" + currentBuildNumber + "' on " + pipelineFullName + " no longer exists (skipped).");
                        currentBuildNumber++;
                        continue;
                    }

                    if (build.isBuilding()) {
                        LOGGER.info("Build '" + currentBuildNumber + "' on " + pipelineFullName + " is not completed yet.");
                        continue jobLoop;
                    }

                    LOGGER.info("Ingesting logs for '" + currentBuildNumber + "' on " + pipelineFullName);
                    var buildLogPath = buildDirectory.resolve(String.valueOf(currentBuildNumber)).resolve("log");
                    try {
                        Scanner scanner = createScanner(buildLogPath.toFile());
                        StringBuilder sb = new StringBuilder();
                        while (scanner.hasNextLine()) {
                            var nextLine = this.trimCompressedBytes(scanner.nextLine());
                            sb.append(nextLine).append("\n");
                        }
                        String content = sb.toString();
                        LogLine.Status status = LogLine.Status.INFO;
                        if (content.contains("Finished: FAILURE")) {
                            status = LogLine.Status.ERROR;
                        }
                        var duration = build.getDuration();
                        Map<String, String> dimensions = new HashMap<>();
                        dimensions.put("jenkins.build_duration_ms", String.valueOf(duration));
                        logLines.add(new LogLine(sb.toString(), pipelineFullName, String.valueOf(currentBuildNumber), status, dimensions));
                        scanner.close();
                    } catch (FileNotFoundException e) {
                        LOGGER.error("File not found: " + e);
                    }
                    currentBuildNumber++;
                }
                getValueStore().setLastBuildId(pipelineFullName, nextBuildNumber);
            }
            return logLines;
        }
    }

    protected ValueStore getValueStore() {
        return ValueStore.get();
    }

    protected Scanner createScanner(File file) throws FileNotFoundException {
        return new Scanner(file);
    }

    protected Jenkins getJenkins() {
        return Jenkins.get();
    }

    protected String trimCompressedBytes(String line) {
        StringBuilder sb = new StringBuilder();
        String[] pieces = line.split(PREAMBLE_STR);
        sb.append(pieces[0]);
        if (pieces.length > 1) {
            pieces = pieces[1].split(POSTAMBLE_STR);
            if (pieces.length > 1) {
                sb.append(pieces[1]);
            }
        }
        return sb.toString();
    }

    protected int getNextBuildNumber(Path jobRootDir) {
        var nextBuildNumberPath = jobRootDir.resolve("nextBuildNumber");
        try {
            String content = Files.readString(nextBuildNumberPath, StandardCharsets.UTF_8);
            return Integer.parseInt(content.trim());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
