package com.moviri.plugins.collector;

import com.moviri.plugins.ws.MintMetric;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FilesystemMetricCollectorTest {

    @Test
    void testCollect() {
        FilesystemMetricCollector collector = spy();

        MintMetric metric = mock();
        doReturn(List.of(metric)).when(collector).collectFSMetrics();
        doReturn(List.of(metric)).when(collector).collectDirectoryMetrics();
        doReturn(List.of(metric)).when(collector).collectJobMetrics();

        assertEquals(3, collector.collect().size());
    }

    @Test
    void testCollectFSMetrics() {
        FilesystemMetricCollector collector = spy();

        Jenkins jenkins = mock();
        doReturn(jenkins).when(collector).getJenkins();

        File rootDir = mock();
        doReturn(rootDir).when(jenkins).getRootDir();

        doReturn(10L).when(rootDir).getTotalSpace();
        doReturn(9L).when(rootDir).getFreeSpace();

        List<MintMetric> collected = collector.collectFSMetrics();
        assertEquals(3, collected.size());
        assertEquals(10, collected.get(0).getValue());
        assertEquals(9, collected.get(1).getValue());
        assertEquals(1, collected.get(2).getValue());
    }

    @Test
    void testCollectJobMetrics() {
        FilesystemMetricCollector collector = spy();

        Jenkins jenkins = mock();
        doReturn(jenkins).when(collector).getJenkins();

        Job<?, ?> job = mock(Job.class, withSettings().extraInterfaces(TopLevelItem.class));
        Job<?, ?> job_ = mock();
        doReturn(List.of(job, job_)).when(jenkins).getAllItems(Job.class);

        File rootDir = mock();
        doReturn(rootDir).when(job).getRootDir();

        doReturn("myFakeJobName").when(job).getName();

        FilesystemMetricCollector.DirectorySize directorySize = mock();
        doReturn(directorySize).when(collector).calculateDirectorySize(any());
        doReturn(1L).when(directorySize).getSize();
        doReturn(2L).when(directorySize).getCount();

        List<MintMetric> collected = collector.collectJobMetrics();
        assertEquals(2, collected.size());
        for (int i = 0; i < collected.size(); i += 1) {
            MintMetric metric = collected.get(i);
            assertEquals(i + 1, metric.getValue());
            assertEquals(Map.of("job", "myFakeJobName"), metric.getDimensions());
        }
    }

    @Test
    void testCollectDirectoryMetrics() {
        FilesystemMetricCollector collector = spy();

        Jenkins jenkins = mock();
        doReturn(jenkins).when(collector).getJenkins();

        File rootDir = mock();
        doReturn(rootDir).when(jenkins).getRootDir();

        File dir1 = mock();
        doReturn(true).when(dir1).isDirectory();
        doReturn("shouldBeThere1").when(dir1).getName();

        File dir2 = mock();
        doReturn(true).when(dir2).isDirectory();
        doReturn("shouldBeThere2").when(dir2).getName();

        File f1 = mock();
        doReturn(false).when(f1).isDirectory();
        doReturn("shouldNotBeThere1").when(f1).getName();

        File dir3 = mock();
        doReturn(true).when(dir3).isDirectory();
        doReturn("shouldNotBeThere2").when(dir3).getName();
        doReturn(new File[] {dir3}).when(dir1).listFiles();

        File[] directories = new File[] {dir1, dir2, f1};

        doReturn(directories).when(rootDir).listFiles();

        FilesystemMetricCollector.DirectorySize directorySize = mock();
        doReturn(directorySize).when(collector).calculateDirectorySize(any());
        doReturn(1L).when(directorySize).getSize();
        doReturn(2L).when(directorySize).getCount();

        List<MintMetric> collected = collector.collectDirectoryMetrics();

        assertEquals(8, collected.size());
        for (int i = 0; i < collected.size(); i += 1) {
            MintMetric metric = collected.get(i);
            assertEquals(1, metric.getDimensions().size());
            assertTrue(metric.getDimensions().containsKey("path"));
            assertFalse(metric.getDimensions().get("path").contains("shouldNotBeThere"));
            if (i % 2 == 0) {
                assertEquals(1, metric.getValue());
            } else {
                assertEquals(2, metric.getValue());
            }
        }
    }

    @Test
    void testCollectEmptyDirectoryMetrics() {
        FilesystemMetricCollector collector = spy();

        Jenkins jenkins = mock();
        doReturn(jenkins).when(collector).getJenkins();

        File file = mock();
        doReturn(file).when(jenkins).getRootDir();

        assertEquals(Collections.emptyList(), collector.collectDirectoryMetrics());
    }

    @Test
    void testCalculateDirectorySize() {
        FilesystemMetricCollector collector = new FilesystemMetricCollector();

        File f1 = mock();
        doReturn(1000L).when(f1).length();

        File f2 = mock();
        doReturn(1500L).when(f2).length();

        File[] files1 = new File[] {f1, f2};

        File dir1 = mock();
        doReturn(true).when(dir1).isDirectory();
        doReturn(files1).when(dir1).listFiles();

        File[] files2 = new File[] {dir1, f1};

        File dir2 = mock();
        doReturn(true).when(dir2).isDirectory();
        doReturn(files2).when(dir2).listFiles();

        var directorySize = collector.calculateDirectorySize(dir2);

        assertEquals(3500, directorySize.getSize());
        assertEquals(3, directorySize.getCount());
    }

    @Test
    void testCalculateEmptyDirectorySize() {
        FilesystemMetricCollector collector = new FilesystemMetricCollector();

        File f1 = mock();

        var directorySize = collector.calculateDirectorySize(f1);

        assertEquals(0, directorySize.getSize());
        assertEquals(0, directorySize.getCount());
    }
}