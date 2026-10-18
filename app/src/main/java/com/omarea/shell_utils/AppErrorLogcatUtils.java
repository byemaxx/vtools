package com.omarea.shell_utils;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AppErrorLogcatUtils {
    public String catLogInfo(Context context) {
        File file = new File(context.getFilesDir(), "crash/error.log");
        if (!file.isFile()) return "No crash log recorded.";
        try (java.io.RandomAccessFile input = new java.io.RandomAccessFile(file, "r")) {
            long start = Math.max(0, input.length() - 256 * 1024);
            input.seek(start);
            byte[] tail = new byte[(int) (input.length() - start)];
            input.readFully(tail);
            return new String(tail, StandardCharsets.UTF_8);
        } catch (IOException error) {
            return "Unable to read crash log: " + error.getMessage();
        }
    }
}
