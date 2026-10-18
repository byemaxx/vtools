package com.omarea.common.shared;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FilePathResolver {
    public String getPath(Context context, Uri uri) {
        if (uri == null) return null;
        if ("file".equalsIgnoreCase(uri.getScheme())) return uri.getPath();
        if (!"content".equalsIgnoreCase(uri.getScheme())) return null;
        File imported = null;
        try {
            if ("com.android.externalstorage.documents".equals(uri.getAuthority()) && DocumentsContract.isDocumentUri(context, uri)) {
                String[] id = DocumentsContract.getDocumentId(uri).split(":", 2);
                if (id.length == 2 && "primary".equalsIgnoreCase(id[0])) {
                    File root = Environment.getExternalStorageDirectory().getCanonicalFile();
                    File file = new File(root, id[1]).getCanonicalFile();
                    if (file.getPath().startsWith(root.getPath() + File.separator) && file.isFile() && file.canRead()) return file.getPath();
                }
            }
            File directory = getDocumentCacheDir(context);
            long budget = Math.max(0, directory.getUsableSpace() - 64L * 1024 * 1024);
            String name = "document";
            try (Cursor cursor = context.getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex);
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex) && cursor.getLong(sizeIndex) > budget) throw new IOException("Insufficient cache space");
                }
            }
            imported = generateFileName(name, directory);
            if (imported == null) throw new IOException("Cannot create import file");
            try (InputStream input = context.getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(imported)) {
                if (input == null) throw new IOException("Document provider returned no stream");
                DocumentCopy.copy(input, output, budget);
            }
            return imported.getAbsolutePath();
        } catch (Exception error) {
            if (imported != null && !imported.delete()) Log.w("DocumentImport", "Cannot remove failed import");
            Log.w("DocumentImport", "Unable to import selected document", error);
            return null;
        }
    }

    public static File getDocumentCacheDir(Context context) throws IOException {
        File directory = new File(context.getCacheDir(), "documents");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create document cache");
        return directory;
    }

    public static File generateFileName(String name, File directory) throws IOException {
        // A document provider controls DISPLAY_NAME: it must never select the destination directory.
        String safe = name == null ? "document" : new File(name.replace('\\', '/')).getName();
        if (safe.isEmpty() || safe.equals(".") || safe.equals("..")) safe = "document";
        int dot = safe.lastIndexOf('.');
        String suffix = dot > 0 ? safe.substring(dot) : "";
        if (suffix.length() > 32) suffix = "";
        return File.createTempFile("document-", suffix, directory);
    }
}
