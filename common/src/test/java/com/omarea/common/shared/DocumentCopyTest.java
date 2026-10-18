package com.omarea.common.shared;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class DocumentCopyTest {
    public static void main(String[] args) throws Exception {
        for (int size : new int[]{0, 1, 1023, 1024, 1025, 65536, 65537}) {
            byte[] original = new byte[size];
            for (int i = 0; i < size; i++) original[i] = (byte) (i % 251);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            assert DocumentCopy.copy(new ByteArrayInputStream(original), output, size) == size;
            assert Arrays.equals(original, output.toByteArray()) : "Incorrect length or final partial chunk";
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean failed = false;
        try { DocumentCopy.copy(new ByteArrayInputStream(new byte[10]), output, 9); }
        catch (IOException expected) { failed = true; }
        assert failed && output.size() <= 9;
        failed = false;
        Thread.currentThread().interrupt();
        try { DocumentCopy.copy(new ByteArrayInputStream(new byte[1]), output, 1); }
        catch (java.io.InterruptedIOException expected) { failed = true; }
        finally { Thread.interrupted(); }
        assert failed;
        System.out.println("Document exact-copy, empty/partial chunk, space and cancellation checks passed");
    }
}
