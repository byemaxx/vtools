package com.omarea.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public final class CrashLogWriter {
    public static synchronized void write(File file, Thread thread, Throwable error) throws IOException {
        File directory = file.getParentFile();
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create crash log directory");
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
            writer.println(new Date() + " [" + thread.getName() + "]");
            error.printStackTrace(writer);
            if (writer.checkError()) throw new IOException("Cannot save crash log");
        }
    }
}
