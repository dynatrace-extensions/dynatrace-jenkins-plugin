package com.moviri.plugins;

import hudson.Extension;
import hudson.logging.LogRecorder;
import hudson.model.ManagementLink;
import io.jenkins.cli.shaded.org.slf4j.Logger;
import io.jenkins.cli.shaded.org.slf4j.LoggerFactory;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Extension
public class DynatraceManagementLink extends ManagementLink {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynatraceManagementLink.class);

    @Override
    public String getIconFileName() {
        return "symbol-gear.svg";
    }

    @Override
    public String getDisplayName() {
        return "Dynatrace Plugin";
    }

    @Override
    public String getUrlName() {
        return "dynatrace-plugin";
    }

    @RequirePOST
    public void doRunAction(StaplerRequest req, StaplerResponse rsp) {
        String logFileContent = getLogs();

        String zipFileName = createZipFileName();

        // Write log content to zip file and then the zip file to a byte stream to return to the user
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            ZipEntry entry = new ZipEntry("dynatrace-plugin.log");
            zos.putNextEntry(entry);

            zos.write(logFileContent.getBytes());

            zos.closeEntry();
            zos.finish();

            byte[] zipData = baos.toByteArray();

            rsp.setContentType("application/zip");
            rsp.addHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");

            rsp.getOutputStream().write(zipData);
            rsp.getOutputStream().flush();
        } catch (IOException e) {
            LOGGER.error("Error downloading log archive: " + e.toString());
        }
    }

    protected String createZipFileName() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        return "LogArchive_" + timeStamp + ".zip";
    }

    protected String getLogs() {
        var logRecorder = getLogRecorder();

        // Flatten all the log records into a single string with newlines.
        return logRecorder.getLogRecords().stream().map(LogRecord::getMessage).collect(Collectors.joining("\n"));
    }

    protected LogRecorder getLogRecorder() {
        return Jenkins.get().getLog().getLogRecorder(Dynatrace.LOG_RECORDER_NAME);
    }
}
