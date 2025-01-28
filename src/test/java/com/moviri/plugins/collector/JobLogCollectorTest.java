package com.moviri.plugins.collector;

import com.moviri.plugins.config.ValueStore;
import com.moviri.plugins.ws.LogLine;
import hudson.model.Job;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JobLogCollectorTest {

    private final JobLogCollector instance = new JobLogCollector();

    @Test
    void testTrimCompressedBytesWithPreambleAndPostamble() {
        String input = "SomeText\u001B[8mha:CompressedText\u001B[0mRemainingText";
        String expected = "SomeTextRemainingText";
        String result = instance.trimCompressedBytes(input);
        assertEquals(expected, result);
    }

    @Test
    void testTrimCompressedBytesWithoutPostamble() {
        String input = "SomeText\u001B[8mha:CompressedText";
        String expected = "SomeText";
        String result = instance.trimCompressedBytes(input);
        assertEquals(expected, result);
    }

    @Test
    void testTrimCompressedBytesWithoutPreamble() {
        String input = "SomeTextRemainingText";
        String expected = "SomeTextRemainingText";
        String result = instance.trimCompressedBytes(input);
        assertEquals(expected, result);
    }

    @Test
    void testTrimCompressedBytesEmptyString() {
        String input = "";
        String expected = "";
        String result = instance.trimCompressedBytes(input);
        assertEquals(expected, result);
    }

    @Test
    void testTrimCompressedBytesPreambleAtStart() {
        String input = "\u001B[8mha:CompressedText\u001B[0mRemainingText";
        String expected = "RemainingText";
        String result = instance.trimCompressedBytes(input);
        assertEquals(expected, result);
    }

    @Test
    void testTrimCompressedBytesPreambleWithoutPostambleAtStart() {
        String input = "\u001B[8mha:CompressedText";
        String expected = "";
        String result = instance.trimCompressedBytes(input);
        assertEquals(expected, result);
    }

    @Test
    void testTrimCompressedBytesPreambleAndPostambleOnly() {
        String input = "\u001B[8mha:\u001B[0m";
        String expected = "";
        String result = instance.trimCompressedBytes(input);
        assertEquals(expected, result);
    }

    @Test
    void testGetNextBuildNumberValidFile(@TempDir Path tempDir) throws IOException {
        Path jobRootDir = tempDir.resolve("job");
        Files.createDirectories(jobRootDir);

        Path nextBuildNumberPath = jobRootDir.resolve("nextBuildNumber");
        Files.writeString(nextBuildNumberPath, "42", StandardCharsets.UTF_8);

        int result = instance.getNextBuildNumber(jobRootDir);
        assertEquals(42, result);
    }

    @Test
    void testGetNextBuildNumberFileNotFound(@TempDir Path tempDir) throws IOException {
        Path jobRootDir = tempDir.resolve("job");
        Files.createDirectories(jobRootDir);

        Exception exception = assertThrows(RuntimeException.class, () ->
                instance.getNextBuildNumber(jobRootDir)
        );

        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void testGetNextBuildNumberInvalidContent(@TempDir Path tempDir) throws IOException {
        Path jobRootDir = tempDir.resolve("job");
        Files.createDirectories(jobRootDir);

        Path nextBuildNumberPath = jobRootDir.resolve("nextBuildNumber");
        Files.writeString(nextBuildNumberPath, "not-a-number", StandardCharsets.UTF_8);

        Exception exception = assertThrows(RuntimeException.class, () ->
                instance.getNextBuildNumber(jobRootDir)
        );

        assertInstanceOf(NumberFormatException.class, exception);
    }

    @Test
    void testGetNextBuildNumberEmptyFile(@TempDir Path tempDir) throws IOException {
        Path jobRootDir = tempDir.resolve("job");
        Files.createDirectories(jobRootDir);

        Path nextBuildNumberPath = jobRootDir.resolve("nextBuildNumber");
        Files.writeString(nextBuildNumberPath, "", StandardCharsets.UTF_8);

        Exception exception = assertThrows(RuntimeException.class, () ->
                instance.getNextBuildNumber(jobRootDir)
        );

        assertInstanceOf(NumberFormatException.class, exception);
    }

    @Test
    void testCollectWithValidBuildLogs() throws Exception {
        // Mock Jenkins and jobs
        Jenkins jenkins = mock();
        Job<?, ?> job = mock();

        doReturn(List.of(job)).when(jenkins).getAllItems(Job.class);

        // Mock job details
        String jobName = "TestPipeline";
        doReturn(jobName).when(job).getDisplayName();

        File mockRootDir = mock();
        doReturn(mockRootDir).when(job).getRootDir();

        Path mockPipelineRootDir = mock();
        doReturn(mockPipelineRootDir).when(mockRootDir).toPath();

        Path buildsDir = mock();
        doReturn(buildsDir).when(mockPipelineRootDir).resolve("builds");

        // Mock nextBuildNumber
        JobLogCollector collector = spy();
        doReturn(jenkins).when(collector).getJenkins();
        doReturn(3).when(collector).getNextBuildNumber(mockPipelineRootDir); // Next build to ingest

        // Mock ValueStore
        ValueStore valueStore = mock();
        doReturn(valueStore).when(collector).getValueStore();
        doReturn(1).when(valueStore).getLastBuildId(jobName);

        // Mock build logs
        File buildLogFile = mock();
        Path buildLogPath1 = mock();
        doReturn(buildLogPath1).when(buildsDir).resolve("1");
        doReturn(buildLogPath1).when(buildLogPath1).resolve("log");
        doReturn(buildLogFile).when(buildLogPath1).toFile();

        File buildLogFile2 = mock();
        Path buildLogPath2 = mock();
        doReturn(buildLogPath2).when(buildsDir).resolve("2");
        doReturn(buildLogPath2).when(buildLogPath2).resolve("log");
        doReturn(buildLogFile2).when(buildLogPath2).toFile();

        // Mock file content for logs
        Scanner scanner1 = mock();
        doReturn(true, false).when(scanner1).hasNextLine();
        doReturn("Started build...\nFinished: SUCCESS").when(scanner1).nextLine();
        doReturn(scanner1).when(collector).createScanner(buildLogFile);

        Scanner scanner2 = mock(Scanner.class);
        doReturn(true, false).when(scanner2).hasNextLine();
        doReturn("Started build...\nFinished: FAILURE").when(scanner2).nextLine();
        doReturn(scanner2).when(collector).createScanner(buildLogFile2);

        // Run the test
        List<LogLine> result = collector.collect();

        // Assertions
        assertEquals(2, result.size());

        LogLine log1 = result.get(0);
        assertEquals("Started build...\nFinished: SUCCESS\n", log1.getContent());
        assertEquals(LogLine.Status.INFO, log1.getStatus());

        LogLine log2 = result.get(1);
        assertEquals("Started build...\nFinished: FAILURE\n", log2.getContent());
        assertEquals(LogLine.Status.ERROR, log2.getStatus());

        // Verify interactions
        verify(valueStore).setLastBuildId(jobName, 3);
        verify(scanner1).close();
        verify(scanner2).close();
    }

    @Test
    void testCollectNoNewBuilds() {
        // Mock Jenkins and job setup
        Jenkins jenkins = mock();
        Job<?, ?> job = mock();
        doReturn(List.of(job)).when(jenkins).getAllItems(Job.class);

        String jobName = "NoNewBuildsPipeline";
        doReturn(jobName).when(job).getDisplayName();

        File mockRootDir = mock();
        doReturn(mockRootDir).when(job).getRootDir();

        Path mockPipelineRootDir = mock();
        doReturn(mockPipelineRootDir).when(mockRootDir).toPath();

        // Mock ValueStore and next build number
        JobLogCollector collector = spy();
        doReturn(jenkins).when(collector).getJenkins();
        doReturn(5).when(collector).getNextBuildNumber(mockPipelineRootDir);
        ValueStore valueStore = mock();
        doReturn(valueStore).when(collector).getValueStore();
        doReturn(5).when(valueStore).getLastBuildId(jobName);

        // Run the test
        List<LogLine> result = collector.collect();

        // Assertions
        assertTrue(result.isEmpty());
        verify(valueStore, never()).setLastBuildId(eq(jobName), anyInt());
    }
}
