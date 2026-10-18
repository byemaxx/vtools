package com.omarea.utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CrashLogWriterTest {
    public static void main(String[] args) throws Exception {
        File directory = Files.createTempDirectory("scene-crash-test").toFile();
        File file = new File(directory, "private/error.log");
        try {
            CrashLogWriter.write(file, Thread.currentThread(), new NullPointerException());
            CrashLogWriter.write(file, Thread.currentThread(), new IllegalStateException("中文测试"));
            String log = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            assert log.contains("NullPointerException");
            assert log.contains("IllegalStateException: 中文测试");
            assert log.contains("CrashLogWriterTest.main");
            assert log.contains(Thread.currentThread().getName());
            System.out.println("Private crash log, null-message, UTF-8 and append checks passed");
        } finally {
            file.delete();
            file.getParentFile().delete();
            directory.delete();
        }
    }
}
