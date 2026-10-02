package com.example.terminal;

import android.content.Context;
import android.system.Os;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ToolchainInstaller {

    private static final String TAG = "ToolchainInstaller";
    private static final String GITHUB_RELEASE_BASE = "https://github.com/raj40870-pixel/library/releases/download/v1.3.0/";
    private static final String GITHUB_RELEASE_BASE_FALLBACK = "https://github.com/raj40870-pixel/library/releases/download/v1.0.0/";

    private static final Map<String, String> PACKAGE_TO_TARGET = new HashMap<>();
    private static final Map<String, String> TARGET_DISPLAY_NAMES = new HashMap<>();

    static {
        // C & C++
        PACKAGE_TO_TARGET.put("clang", "c_cpp");
        PACKAGE_TO_TARGET.put("clang++", "c_cpp");
        PACKAGE_TO_TARGET.put("gcc", "c_cpp");
        PACKAGE_TO_TARGET.put("g++", "c_cpp");
        PACKAGE_TO_TARGET.put("c", "c_cpp");
        PACKAGE_TO_TARGET.put("cpp", "c_cpp");
        PACKAGE_TO_TARGET.put("c_cpp", "c_cpp");
        PACKAGE_TO_TARGET.put("make", "c_cpp");

        // Python
        PACKAGE_TO_TARGET.put("python", "python");
        PACKAGE_TO_TARGET.put("python3", "python");
        PACKAGE_TO_TARGET.put("py", "python");

        // Node.js / JavaScript / TypeScript
        PACKAGE_TO_TARGET.put("node", "nodejs");
        PACKAGE_TO_TARGET.put("nodejs", "nodejs");
        PACKAGE_TO_TARGET.put("npm", "nodejs");
        PACKAGE_TO_TARGET.put("js", "nodejs");
        PACKAGE_TO_TARGET.put("ts", "nodejs");

        // Java
        PACKAGE_TO_TARGET.put("java", "java");
        PACKAGE_TO_TARGET.put("jdk", "java");
        PACKAGE_TO_TARGET.put("openjdk", "java");
        PACKAGE_TO_TARGET.put("openjdk-17", "java");
        PACKAGE_TO_TARGET.put("openjdk-21", "java");
        PACKAGE_TO_TARGET.put("javac", "java");

        // Go
        PACKAGE_TO_TARGET.put("go", "go");
        PACKAGE_TO_TARGET.put("golang", "go");

        // Rust
        PACKAGE_TO_TARGET.put("rust", "rust");
        PACKAGE_TO_TARGET.put("rustc", "rust");
        PACKAGE_TO_TARGET.put("cargo", "rust");

        // Kotlin
        PACKAGE_TO_TARGET.put("kotlin", "kotlin");
        PACKAGE_TO_TARGET.put("kotlinc", "kotlin");

        // C#
        PACKAGE_TO_TARGET.put("csharp", "csharp");
        PACKAGE_TO_TARGET.put("mono", "csharp");
        PACKAGE_TO_TARGET.put("cs", "csharp");
        PACKAGE_TO_TARGET.put("mcs", "csharp");

        // PHP
        PACKAGE_TO_TARGET.put("php", "php");
        PACKAGE_TO_TARGET.put("php8", "php");

        // Ruby
        PACKAGE_TO_TARGET.put("ruby", "ruby");
        PACKAGE_TO_TARGET.put("gem", "ruby");

        // Lua
        PACKAGE_TO_TARGET.put("lua", "lua");
        PACKAGE_TO_TARGET.put("lua54", "lua");

        // Display names
        TARGET_DISPLAY_NAMES.put("c_cpp", "Clang & GCC (C/C++)");
        TARGET_DISPLAY_NAMES.put("python", "Python 3.14 Runtime");
        TARGET_DISPLAY_NAMES.put("nodejs", "Node.js & NPM");
        TARGET_DISPLAY_NAMES.put("java", "OpenJDK 21 (Java)");
        TARGET_DISPLAY_NAMES.put("go", "Go (Golang)");
        TARGET_DISPLAY_NAMES.put("rust", "Rust & Cargo");
        TARGET_DISPLAY_NAMES.put("kotlin", "Kotlin Compiler");
        TARGET_DISPLAY_NAMES.put("csharp", "C# Mono Compiler");
        TARGET_DISPLAY_NAMES.put("php", "PHP Interpreter");
        TARGET_DISPLAY_NAMES.put("ruby", "Ruby Interpreter");
        TARGET_DISPLAY_NAMES.put("lua", "Lua Interpreter");
    }

    public interface InstallCallback {
        void onProgress(String status, int percentage);
        void onSuccess(String target, String displayName);
        void onError(String target, String error);
    }

    public static String normalizeTarget(String inputPkg) {
        if (inputPkg == null) return null;
        String key = inputPkg.trim().toLowerCase();
        if (PACKAGE_TO_TARGET.containsKey(key)) {
            return PACKAGE_TO_TARGET.get(key);
        }
        return key;
    }

    public static String getDisplayName(String target) {
        String norm = normalizeTarget(target);
        if (TARGET_DISPLAY_NAMES.containsKey(norm)) {
            return TARGET_DISPLAY_NAMES.get(norm);
        }
        return target;
    }

    public static boolean isToolchainInstalled(Context context, String pkgOrTarget) {
        String target = normalizeTarget(pkgOrTarget);
        File filesDir = context.getFilesDir();
        File usrBin = new File(filesDir, "usr/bin");

        if ("c_cpp".equals(target)) {
            return new File(usrBin, "clang").exists() || new File(usrBin, "gcc").exists();
        } else if ("python".equals(target)) {
            return new File(usrBin, "python").exists() || new File(usrBin, "python3").exists();
        } else if ("nodejs".equals(target)) {
            return new File(usrBin, "node").exists();
        } else if ("java".equals(target)) {
            return new File(usrBin, "javac").exists() || new File(usrBin, "java").exists();
        } else if ("go".equals(target)) {
            return new File(usrBin, "go").exists();
        } else if ("rust".equals(target)) {
            return new File(usrBin, "rustc").exists();
        } else if ("kotlin".equals(target)) {
            return new File(usrBin, "kotlinc").exists();
        } else if ("csharp".equals(target)) {
            return new File(usrBin, "mono").exists() || new File(usrBin, "mcs").exists();
        } else if ("php".equals(target)) {
            return new File(usrBin, "php").exists();
        } else if ("ruby".equals(target)) {
            return new File(usrBin, "ruby").exists();
        } else if ("lua".equals(target)) {
            return new File(usrBin, "lua").exists();
        }
        return false;
    }

    public static void install(Context context, String pkgOrTarget, InstallCallback callback) {
        final String target = normalizeTarget(pkgOrTarget);
        final String displayName = getDisplayName(target);

        new Thread(() -> {
            File cacheDir = context.getCacheDir();
            File tempZip = new File(cacheDir, target + ".zip");
            File filesDir = context.getFilesDir();
            File usrDir = new File(filesDir, "usr");

            if (!usrDir.exists()) {
                usrDir.mkdirs();
            }

            try {
                String downloadUrl = GITHUB_RELEASE_BASE + target + ".zip";
                Log.d(TAG, "Downloading toolchain from: " + downloadUrl);

                int maxRetries = 5;
                int attempt = 0;
                boolean downloadSuccess = false;
                long totalSize = -1;

                while (attempt < maxRetries && !downloadSuccess) {
                    attempt++;
                    HttpURLConnection conn = null;
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        long existingLength = (tempZip.exists()) ? tempZip.length() : 0;
                        if (existingLength > 0) {
                            callback.onProgress("Resuming download for " + displayName + "...", 5);
                        } else {
                            callback.onProgress("Connecting to GitHub CDN for " + displayName + "...", 5);
                        }

                        URL url = new URL(downloadUrl);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; CodeEditor)");
                        conn.setInstanceFollowRedirects(true);
                        conn.setConnectTimeout(30000);
                        conn.setReadTimeout(120000);

                        if (existingLength > 0) {
                            conn.setRequestProperty("Range", "bytes=" + existingLength + "-");
                        }

                        int responseCode = conn.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                            responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                            responseCode == 307 || responseCode == 308) {
                            String redirect = conn.getHeaderField("Location");
                            if (redirect != null) {
                                conn.disconnect();
                                url = new URL(redirect);
                                conn = (HttpURLConnection) url.openConnection();
                                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; CodeEditor)");
                                conn.setConnectTimeout(30000);
                                conn.setReadTimeout(120000);
                                if (existingLength > 0) {
                                    conn.setRequestProperty("Range", "bytes=" + existingLength + "-");
                                }
                                responseCode = conn.getResponseCode();
                            }
                        }

                        boolean append = false;
                        long totalDownloaded = 0;

                        if (responseCode == 206) {
                            append = true;
                            totalDownloaded = existingLength;
                            long lengthRemaining = conn.getContentLengthLong();
                            if (lengthRemaining > 0) {
                                totalSize = existingLength + lengthRemaining;
                            }
                        } else if (responseCode == HttpURLConnection.HTTP_OK) {
                            append = false;
                            totalDownloaded = 0;
                            totalSize = conn.getContentLengthLong();
                        } else if (responseCode == 416) {
                            if (existingLength > 0) {
                                downloadSuccess = true;
                                break;
                            } else {
                                throw new IOException("HTTP 416 for " + downloadUrl);
                            }
                        } else {
                            throw new IOException("Server returned HTTP " + responseCode + " for " + downloadUrl);
                        }

                        in = conn.getInputStream();
                        out = new FileOutputStream(tempZip, append);

                        byte[] buffer = new byte[32768];
                        int read;
                        int lastPercent = -1;

                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                            totalDownloaded += read;
                            if (totalSize > 0) {
                                int percent = 5 + (int) ((totalDownloaded * 60) / totalSize);
                                if (percent != lastPercent) {
                                    lastPercent = percent;
                                    long dlMB = totalDownloaded / (1024 * 1024);
                                    long totMB = totalSize / (1024 * 1024);
                                    callback.onProgress("Downloading " + target + ".zip (" + dlMB + "MB / " + totMB + "MB)...", percent);
                                }
                            }
                        }

                        out.flush();
                        downloadSuccess = true;

                    } catch (IOException e) {
                        Log.w(TAG, "Download attempt " + attempt + " failed: " + e.getMessage() + ". Retrying...");
                        if (attempt >= maxRetries) {
                            throw e;
                        }
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ignored) {}
                    } finally {
                        if (in != null) try { in.close(); } catch (Exception ignored) {}
                        if (out != null) try { out.close(); } catch (Exception ignored) {}
                        if (conn != null) try { conn.disconnect(); } catch (Exception ignored) {}
                    }
                }

                callback.onProgress("Extracting " + displayName + " into Linux environment...", 68);
                extractZip(tempZip, usrDir, callback);

                callback.onProgress("Configuring permissions and dynamic linker symlinks...", 92);
                processSymlinks(usrDir);
                makeExecutables(new File(usrDir, "bin"));
                makeExecutables(new File(usrDir, "libexec"));
                makeExecutables(new File(usrDir, "lib/go"));
                makeExecutables(new File(usrDir, "lib/rustlib"));

                if (tempZip.exists()) {
                    tempZip.delete();
                }

                callback.onProgress("Library is installed successfully!", 100);
                callback.onSuccess(target, displayName);

            } catch (Exception e) {
                Log.e(TAG, "Installation failed for " + target, e);
                if (tempZip.exists()) tempZip.delete();
                callback.onError(target, e.getMessage() != null ? e.getMessage() : "Unknown installation error");
            }
        }).start();
    }

    private static void extractZip(File zipFile, File targetDir, InstallCallback callback) throws IOException {
        // Clean up any legacy backslash files in usr/
        File[] rootFiles = targetDir.listFiles();
        if (rootFiles != null) {
            for (File f : rootFiles) {
                if (f.isFile() && f.getName().contains("\\")) {
                    f.delete();
                }
            }
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[32768];
            int count = 0;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                while (name.startsWith("./")) {
                    name = name.substring(2);
                }
                while (name.startsWith("/")) {
                    name = name.substring(1);
                }

                File destination = new File(targetDir, name);
                if (entry.isDirectory()) {
                    if (destination.exists() && !destination.isDirectory()) {
                        try { Os.remove(destination.getAbsolutePath()); } catch (Exception ignored) {}
                        destination.delete();
                    }
                    destination.mkdirs();
                } else {
                    File parent = destination.getParentFile();
                    if (parent != null) {
                        if (parent.exists() && !parent.isDirectory()) {
                            try { Os.remove(parent.getAbsolutePath()); } catch (Exception ignored) {}
                            parent.delete();
                        }
                        if (!parent.exists()) {
                            parent.mkdirs();
                        }
                    }

                    // Crucial: remove existing file or broken symlink before writing
                    try {
                        Os.remove(destination.getAbsolutePath());
                    } catch (Exception ignored) {}
                    destination.delete();

                    try (FileOutputStream fos = new FileOutputStream(destination)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
                count++;
                if (count % 150 == 0) {
                    // Extract phase covers 68% to 92%
                    int pct = 68 + Math.min(24, count / 150);
                    callback.onProgress("Extracting files (" + count + " files extracted)...", pct);
                }
            }
        }
    }

    private static void processSymlinks(File targetDir) {
        File symlinksFile = new File(targetDir, "SYMLINKS.txt");
        if (!symlinksFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(symlinksFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String linkRel = null;
                String target = null;

                if (line.contains(" -> ")) {
                    String[] parts = line.split(" -> ");
                    linkRel = parts[0].trim();
                    target = parts[1].trim();
                } else if (line.contains("←")) {
                    String[] parts = line.split("←");
                    target = parts[0].trim();
                    linkRel = parts[1].trim();
                } else if (line.contains("→")) {
                    String[] parts = line.split("→");
                    linkRel = parts[0].trim();
                    target = parts[1].trim();
                } else if (line.contains("<-")) {
                    String[] parts = line.split("<-");
                    target = parts[0].trim();
                    linkRel = parts[1].trim();
                }

                if (linkRel != null && target != null) {
                    linkRel = linkRel.replace('\\', '/');
                    target = target.replace('\\', '/');
                    while (linkRel.startsWith("./")) {
                        linkRel = linkRel.substring(2);
                    }
                    while (linkRel.startsWith("/")) {
                        linkRel = linkRel.substring(1);
                    }

                    File linkFile = new File(targetDir, linkRel);
                    if (linkFile.exists()) {
                        linkFile.delete();
                    }
                    File parent = linkFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    try {
                        Os.symlink(target, linkFile.getAbsolutePath());
                    } catch (Exception e) {
                        Log.w(TAG, "Symlink failed: " + linkFile + " -> " + target, e);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing symlinks", e);
        } finally {
            symlinksFile.delete();
        }
    }

    private static void makeExecutables(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                makeExecutables(f);
            } else {
                f.setExecutable(true, false);
                f.setReadable(true, false);
                try {
                    Os.chmod(f.getAbsolutePath(), 0755);
                } catch (Exception ignored) {}
            }
        }
    }
}
