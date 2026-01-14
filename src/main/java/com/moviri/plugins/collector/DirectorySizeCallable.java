package com.moviri.plugins.collector;

import com.moviri.plugins.collector.FilesystemUtils.DirectorySize;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

import java.io.File;
import java.io.IOException;

import static com.moviri.plugins.collector.FilesystemUtils.calculateDirectorySize;

public class DirectorySizeCallable extends MasterToSlaveFileCallable<DirectorySize> {
    @Override
    public DirectorySize invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        return calculateDirectorySize(f);
    }
}
