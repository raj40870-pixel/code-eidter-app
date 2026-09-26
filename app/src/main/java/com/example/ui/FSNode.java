package com.example.ui;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Universal filesystem node supporting both local java.io.File and
 * SAF DocumentFile (Storage Access Framework) with seamless 2-way read/write.
 */
public class FSNode {

    private final File file;
    private final DocumentFile docFile;
    private final Uri uri;
    private final String name;
    private final boolean isDirectory;

    public FSNode(File file) {
        this.file = file;
        this.docFile = null;
        this.uri = null;
        this.name = file != null ? file.getName() : "file";
        this.isDirectory = file != null && file.isDirectory();
    }

    public FSNode(File file, DocumentFile docFile, Uri uri, String name, boolean isDirectory) {
        this.file = file;
        this.docFile = docFile;
        this.uri = uri != null ? uri : (docFile != null ? docFile.getUri() : null);
        this.name = name != null && !name.isEmpty() ? name : (file != null ? file.getName() : "item");
        this.isDirectory = isDirectory;
    }

    public static FSNode fromFile(File file) {
        return new FSNode(file);
    }

    public static FSNode fromDocumentFile(DocumentFile docFile, File localDir) {
        String n = docFile.getName();
        File childFile = localDir != null ? new File(localDir, n != null ? n : "") : null;
        return new FSNode(childFile, docFile, docFile.getUri(), n, docFile.isDirectory());
    }

    public String getName() {
        return name;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public File getFile() {
        return file;
    }

    public DocumentFile getDocFile() {
        return docFile;
    }

    public Uri getUri() {
        return uri;
    }

    public String getKey() {
        if (file != null) {
            return file.getAbsolutePath();
        }
        if (uri != null) {
            return uri.toString();
        }
        return name;
    }

    /**
     * Lists child files and directories. First tries standard java.io.File;
     * if that returns null or empty (e.g. Scoped Storage restriction on Android 11+),
     * it falls back to DocumentFile which is guaranteed to work via tree URI permission.
     */
    public List<FSNode> listChildren(Context context) {
        List<FSNode> result = new ArrayList<>();

        // 1. Try raw java.io.File listing first
        if (file != null && file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                for (File f : files) {
                    if (f.getName().startsWith(".")) continue;
                    result.add(new FSNode(f));
                }
                sortNodes(result);
                return result;
            }
        }

        // 2. Fallback to DocumentFile listing (Storage Access Framework)
        DocumentFile targetDoc = docFile;
        if (targetDoc == null && uri != null && context != null) {
            try {
                targetDoc = DocumentFile.fromTreeUri(context, uri);
            } catch (Exception ignored) {}
        }

        if (targetDoc != null && targetDoc.exists() && targetDoc.isDirectory()) {
            DocumentFile[] children = targetDoc.listFiles();
            if (children != null) {
                for (DocumentFile child : children) {
                    String childName = child.getName();
                    if (childName == null || childName.startsWith(".")) continue;

                    File localChild = null;
                    if (file != null) {
                        localChild = new File(file, childName);
                    }
                    result.add(new FSNode(localChild, child, child.getUri(), childName, child.isDirectory()));
                }
                sortNodes(result);
                return result;
            }
        }

        return result;
    }

    private void sortNodes(List<FSNode> list) {
        Collections.sort(list, (a, b) -> {
            if (a.isDirectory != b.isDirectory) {
                return a.isDirectory ? -1 : 1; // Folders first
            }
            return a.name.compareToIgnoreCase(b.name);
        });
    }

    /**
     * Reads text content from file or content URI.
     */
    public String readContent(Context context) throws IOException {
        // Try local file first if readable
        if (file != null && file.exists() && file.canRead()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        }

        // Fallback to content URI stream
        if (uri != null && context != null) {
            try (InputStream is = context.getContentResolver().openInputStream(uri);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        }

        return "";
    }

    /**
     * Saves content to both physical file and content URI for seamless 2-way sync.
     */
    public boolean writeContent(Context context, String content) {
        boolean saved = false;

        // Save to physical file
        if (file != null) {
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (FileWriter fw = new FileWriter(file, false)) {
                    fw.write(content);
                    saved = true;
                }
            } catch (Exception ignored) {}
        }

        // Save to SAF DocumentFile URI
        if (uri != null && context != null) {
            try (OutputStream os = context.getContentResolver().openOutputStream(uri, "wt")) {
                if (os != null) {
                    os.write(content.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    saved = true;
                }
            } catch (Exception ignored) {}
        }

        return saved;
    }

    public boolean delete(Context context) {
        boolean deleted = false;
        if (file != null && file.exists()) {
            deleted = deleteRecursively(file);
        }
        if (docFile != null && docFile.exists()) {
            deleted = docFile.delete() || deleted;
        }
        return deleted;
    }

    private boolean deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        return f.delete();
    }

    public FSNode createChildFile(Context context, String displayName) {
        File childFile = null;
        if (file != null) {
            childFile = new File(file, displayName);
            try {
                childFile.createNewFile();
            } catch (Exception ignored) {}
        }

        DocumentFile childDoc = null;
        if (docFile != null && docFile.exists() && docFile.isDirectory()) {
            try {
                childDoc = docFile.createFile("text/plain", displayName);
            } catch (Exception ignored) {}
        }

        return new FSNode(
                childFile,
                childDoc,
                childDoc != null ? childDoc.getUri() : null,
                displayName,
                false
        );
    }

    public FSNode createChildFolder(Context context, String displayName) {
        File childFolder = null;
        if (file != null) {
            childFolder = new File(file, displayName);
            childFolder.mkdirs();
        }

        DocumentFile childDoc = null;
        if (docFile != null && docFile.exists() && docFile.isDirectory()) {
            try {
                childDoc = docFile.createDirectory(displayName);
            } catch (Exception ignored) {}
        }

        return new FSNode(
                childFolder,
                childDoc,
                childDoc != null ? childDoc.getUri() : null,
                displayName,
                true
        );
    }

    public static FSNode findFirstCodeFile(Context context, FSNode root) {
        if (root == null) return null;
        List<FSNode> children = root.listChildren(context);
        for (FSNode child : children) {
            if (!child.isDirectory()) {
                return child;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FSNode other = (FSNode) o;
        return getKey().equals(other.getKey());
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }
}
