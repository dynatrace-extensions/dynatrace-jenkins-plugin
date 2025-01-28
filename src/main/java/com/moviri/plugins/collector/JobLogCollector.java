package com.moviri.plugins.collector;

import com.moviri.plugins.Preload;
import com.moviri.plugins.config.ValueStore;
import com.moviri.plugins.ws.LogLine;
import hudson.model.Job;
import io.jenkins.cli.shaded.org.slf4j.Logger;
import io.jenkins.cli.shaded.org.slf4j.LoggerFactory;
import jenkins.model.Jenkins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// https://github.com/LarrysGIT/Extract-Jenkins-Raw-Log
@Preload
public class JobLogCollector implements Collector<List<LogLine>> {
    public static final Logger LOGGER = LoggerFactory.getLogger(JobLogCollector.class);
    private static final String PREAMBLE_STR = "\u001B\\[8mha:";
    private static final String POSTAMBLE_STR = "\u001B\\[0m";

    @Override
    public List<LogLine> collect() {
        LOGGER.info("Collecting logs...");
        var jenkins = getJenkins();
        var logLines = new ArrayList<LogLine>();
        for (Job<?, ?> job : jenkins.getAllItems(Job.class)) {
            var pipelineDisplayName = job.getDisplayName();
            var pipelineRootDir = job.getRootDir().toPath();
            int currentBuildNumber = getValueStore().getLastBuildId(pipelineDisplayName);
            int nextBuildNumber = this.getNextBuildNumber(pipelineRootDir);

            var buildDirectory = pipelineRootDir.resolve("builds");
            if (currentBuildNumber >= nextBuildNumber) {
                LOGGER.info("Skipping " + pipelineDisplayName + " since there are no new logs after build id '" + currentBuildNumber + "'");
                continue;
            }
            while (currentBuildNumber < nextBuildNumber) {
                LOGGER.info("Ingesting logs for '" + currentBuildNumber + "'");
                var buildLogPath = buildDirectory.resolve(String.valueOf(currentBuildNumber++)).resolve("log");
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
                    logLines.add(new LogLine(sb.toString(), pipelineDisplayName, String.valueOf(currentBuildNumber), status));
                    scanner.close();
                } catch (FileNotFoundException e) {
                    LOGGER.error("File not found: " + e);
                }
            }
            getValueStore().setLastBuildId(pipelineDisplayName, nextBuildNumber);
        }
        return logLines;
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
            LOGGER.info("read next build number: " + content);
            return Integer.parseInt(content.trim());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
