package com.moviri.plugins;

import hudson.logging.LogRecorder;
import org.junit.jupiter.api.Test;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynatraceManagementLinkTest {

    @Test
    void testGetIconFileName() {
        DynatraceManagementLink dynatraceManagementLink = new DynatraceManagementLink();
        assertEquals("symbol-gear.svg", dynatraceManagementLink.getIconFileName());
    }

    @Test
    void testGetDisplayName() {
        DynatraceManagementLink dynatraceManagementLink = new DynatraceManagementLink();
        assertEquals("Dynatrace Plugin", dynatraceManagementLink.getDisplayName());
    }

    @Test
    void testGetUrlName() {
        DynatraceManagementLink dynatraceManagementLink = new DynatraceManagementLink();
        assertEquals("dynatrace-plugin", dynatraceManagementLink.getUrlName());
    }

    @Test
    void testGetFlattenLogs() {
        DynatraceManagementLink dynatraceManagementLink = spy();

        List<LogRecord> logRecordList = List.of(
                new LogRecord(Level.INFO, "myFirstFakeLog"),
                new LogRecord(Level.WARNING, "mySecondFakeLog"),
                new LogRecord(Level.SEVERE, "myThirdFakeLog")
        );

        LogRecorder logRecorder = mock();
        doReturn(logRecordList).when(logRecorder).getLogRecords();
        doReturn(logRecorder).when(dynatraceManagementLink).getLogRecorder();

        assertEquals("myFirstFakeLog\nmySecondFakeLog\nmyThirdFakeLog",
                dynatraceManagementLink.getLogs());
    }

    @Test
    void testCreateZipFileName() {
        DynatraceManagementLink dynatraceManagementLink = new DynatraceManagementLink();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm")
                .format(Calendar.getInstance().getTime());
        String filename = dynatraceManagementLink.createZipFileName();
        assertNotNull(filename);
        assertTrue(filename.startsWith("LogArchive_" + timeStamp));
        assertTrue(filename.endsWith(".zip"));
    }

    @Test
    void testDoRunAction() throws IOException {
        DynatraceManagementLink dynatraceManagementLink = spy();

        List<LogRecord> logRecordList = List.of(
                new LogRecord(Level.INFO, "myFirstFakeLog"),
                new LogRecord(Level.WARNING, "mySecondFakeLog"),
                new LogRecord(Level.SEVERE, "myThirdFakeLog")
        );

        LogRecorder logRecorder = mock();
        doReturn(logRecordList).when(logRecorder).getLogRecords();
        doReturn(logRecorder).when(dynatraceManagementLink).getLogRecorder();

        String zipFileName = "LogArchive_Fake.zip";
        doReturn(zipFileName).when(dynatraceManagementLink).createZipFileName();

        StaplerResponse rsp = mock();
        ServletOutputStream outputStream = mock();
        doReturn(outputStream).when(rsp).getOutputStream();
        dynatraceManagementLink.doRunAction(mock(), rsp);
        verify(rsp, times(1)).setContentType("application/zip");
        verify(rsp, times(1)).addHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");
        verify(rsp.getOutputStream(), times(1)).write(any());
        verify(rsp.getOutputStream(), times(1)).flush();
    }
}