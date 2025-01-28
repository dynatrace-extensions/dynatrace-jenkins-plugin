package com.moviri.plugins.ws;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogLineTest {

    @Test
    void testInfoStatus() {
        String type = "INFO";
        LogLine.Status status = LogLine.Status.valueOf(type);
        assertEquals(LogLine.Status.INFO, status);
        assertEquals("info", status.toString());
    }

    @Test
    void testWarnStatus() {
        String type = "WARN";
        LogLine.Status status = LogLine.Status.valueOf(type);
        assertEquals(LogLine.Status.WARN, status);
        assertEquals("warn", status.toString());
    }

    @Test
    void testErrorStatus() {
        String type = "ERROR";
        LogLine.Status status = LogLine.Status.valueOf(type);
        assertEquals(LogLine.Status.ERROR, status);
        assertEquals("error", status.toString());
    }

    @Test
    void testToMapWithFullConstructor() {
        LogLine logLine = new LogLine("myFakeContent", "myFakeJob", "myFakeBuildId", LogLine.Status.ERROR);
        Map<String, String> resultMap = Map.of(
                "content", "myFakeContent",
                "jenkins.job", "myFakeJob",
                "jenkins.build_id", "myFakeBuildId",
                "status", "error"
        );
        assertEquals(resultMap, logLine.toMap());
    }

    @Test
    void testToMapWithBasicConstructor() {
        LogLine logLine = new LogLine("myFakeContent", "myFakeJob", "myFakeBuildId");
        Map<String, String> resultMap = Map.of(
                "content", "myFakeContent",
                "jenkins.job", "myFakeJob",
                "jenkins.build_id", "myFakeBuildId",
                "status", "info"
        );
        assertEquals(resultMap, logLine.toMap());
    }
}