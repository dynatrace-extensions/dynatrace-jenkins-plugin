package com.moviri.plugins.collector;

import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

public class FilesystemUtils {

    @Getter
    public static class DirectorySize implements Serializable {

        private static final long serialVersionUID = 1L;
        private final long size;
        private final long count;

        public DirectorySize(long size, long count) {
            this.size = size;
            this.count = count;
        }
    }

    public static DirectorySize calculateDirectorySize(File directory) throws IOException {
        final AtomicLong size = new AtomicLong(0);
        final AtomicLong count = new AtomicLong(0);
        Path path = directory.toPath();

        if (Files.exists(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    size.addAndGet(attrs.size());
                    count.addAndGet(1);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return new DirectorySize(size.get(), count.get());
    }
}