package com.omarea.common.shared;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

final class DocumentCopy {
    static long copy(InputStream input, OutputStream output, long limit) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Import cancelled");
            if (count > limit - total) throw new IOException("Not enough space to import document");
            output.write(buffer, 0, count);
            total += count;
        }
        return total;
    }
}
