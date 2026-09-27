package com.example.terminal;

import android.content.Context;
import android.os.Build;
import android.system.Os;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TermuxBootstrapInstaller {

    private static final String TAG = "TermuxInstaller";

    public interface InstallCallback {
        void onProgress(String status, int percentage);
        void onSuccess();
        void onError(String error);
    }

    public static boolean isInstalled(Context context) {
        checkAndMigrateIfExtractedInRoot(context);
        File filesDir = context.getFilesDir();
        File usrBin = new File(filesDir, "usr/bin");
        File bash = new File(usrBin, "bash");
        File apt = new File(usrBin, "apt");
        File pkg = new File(usrBin, "pkg");
        return (bash.exists() && bash.canExecute()) || (apt.exists()) || (pkg.exists());
    }

    public static void checkAndMigrateIfExtractedInRoot(Context context) {
        File filesDir = context.getFilesDir();
        File rootBin = new File(filesDir, "bin");
        File usrDir = new File(filesDir, "usr");
        File usrBin = new File(usrDir, "bin");

        // If 3500+ files were previously extracted directly into files/ (bin/ exists but usr/bin does not)
        if (rootBin.exists() && !usrBin.exists()) {
            Log.i(TAG, "Auto-migrating previously downloaded packages from files/ to files/usr/...");
            if (!usrDir.exists()) usrDir.mkdirs();

            String[] dirsToMove = {"bin", "lib", "etc", "share", "var"};
            for (String dirName : dirsToMove) {
                File src = new File(filesDir, dirName);
                File dest = new File(usrDir, dirName);
                if (src.exists() && !dest.exists()) {
                    boolean moved = src.renameTo(dest);
                    Log.i(TAG, "Moved " + dirName + " to usr/" + dirName + ": " + moved);
                }
            }

            File srcSymlinks = new File(filesDir, "SYMLINKS.txt");
            File destSymlinks = new File(usrDir, "SYMLINKS.txt");
            if (srcSymlinks.exists() && !destSymlinks.exists()) {
                srcSymlinks.renameTo(destSymlinks);
            }

            processSymlinks(usrDir);
            makeExecutables(new File(usrDir, "bin"));
            makeExecutables(new File(usrDir, "libexec"));
            makeExecutables(new File(usrDir, "lib/apt/methods"));
            setupEnvironmentFiles(filesDir);
            Log.i(TAG, "Auto-migration completed successfully!");
        }
    }

    public static String getArch() {
        for (String abi : Build.SUPPORTED_ABIS) {
            if (abi.contains("arm64")) return "aarch64";
            if (abi.contains("v7a") || abi.contains("armeabi")) return "arm";
            if (abi.contains("x86_64")) return "x86_64";
            if (abi.contains("x86")) return "i686";
        }
        return "aarch64";
    }

    public static String getBootstrapUrl(String arch) {
        return "https://github.com/termux/termux-packages/releases/download/bootstrap-2026.09.20-r1%2Bapt.android-7/bootstrap-" + arch + ".zip";
    }

    public static void install(Context context, InstallCallback callback) {
        new Thread(() -> {
            File tempZip = new File(context.getCacheDir(), "bootstrap.zip");
            try {
                String arch = getArch();
                String downloadUrl = getBootstrapUrl(arch);

                callback.onProgress("Connecting to Termux Linux repository...", 5);
                Log.d(TAG, "Downloading bootstrap from: " + downloadUrl);

                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; CodeEditor)");
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(30000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == 307 || responseCode == 308) {
                    String redirect = conn.getHeaderField("Location");
                    if (redirect != null) {
                        conn.disconnect();
                        url = new URL(redirect);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; CodeEditor)");
                        conn.setConnectTimeout(20000);
                        conn.setReadTimeout(30000);
                    }
                }

                int totalSize = conn.getContentLength();
                InputStream in = conn.getInputStream();
                OutputStream out = new FileOutputStream(tempZip);

                byte[] buffer = new byte[8192];
                int read;
                long totalDownloaded = 0;
                int lastPercent = -1;

                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    totalDownloaded += read;
                    if (totalSize > 0) {
                        int percent = (int) ((totalDownloaded * 60) / totalSize);
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            callback.onProgress("Downloading core Linux packages (" + (totalDownloaded / (1024 * 1024)) + "MB / " + (totalSize / (1024 * 1024)) + "MB)...", 5 + percent);
                        }
                    }
                }

                in.close();
                out.flush();
                out.close();
                conn.disconnect();

                callback.onProgress("Unpacking Linux environment...", 70);
                File filesDir = context.getFilesDir();
                File usrDir = new File(filesDir, "usr");
                if (!usrDir.exists()) usrDir.mkdirs();
                unzip(tempZip, usrDir, callback);

                callback.onProgress("Configuring permissions and symlinks...", 90);
                processSymlinks(usrDir);
                makeExecutables(new File(usrDir, "bin"));
                makeExecutables(new File(usrDir, "libexec"));
                makeExecutables(new File(usrDir, "lib/apt/methods"));

                setupEnvironmentFiles(filesDir);

                if (tempZip.exists()) {
                    tempZip.delete();
                }

                callback.onProgress("Linux environment ready!", 100);
                callback.onSuccess();

            } catch (Exception e) {
                Log.e(TAG, "Bootstrap install failed", e);
                if (tempZip.exists()) tempZip.delete();
                callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown installation error");
            }
        }).start();
    }

    private static void unzip(File zipFile, File targetDir, InstallCallback callback) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            int count = 0;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith("./")) {
                    name = name.substring(2);
                }

                File destination = new File(targetDir, name);
                if (entry.isDirectory()) {
                    destination.mkdirs();
                } else {
                    File parent = destination.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    try (FileOutputStream fos = new FileOutputStream(destination)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
                count++;
                if (count % 200 == 0) {
                    callback.onProgress("Extracting Linux tools (" + count + " files)...", 70 + Math.min(18, count / 200));
                }
            }
        }
    }

    private static void processSymlinks(File targetDir) {
        File symlinksFile = new File(targetDir, "SYMLINKS.txt");
        if (!symlinksFile.exists()) {
            symlinksFile = new File(targetDir, "usr/SYMLINKS.txt");
        }
        if (!symlinksFile.exists() && targetDir.getParentFile() != null) {
            symlinksFile = new File(targetDir.getParentFile(), "SYMLINKS.txt");
        }
        if (!symlinksFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(symlinksFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Termux format: target←symlink_path
                String[] parts = line.split("←");
                if (parts.length != 2) {
                    parts = line.split("→");
                }
                if (parts.length != 2) {
                    parts = line.split("<-");
                }
                if (parts.length == 2) {
                    String target = parts[0].trim();
                    String symlinkRel = parts[1].trim();
                    if (symlinkRel.startsWith("./")) {
                        symlinkRel = symlinkRel.substring(2);
                    }

                    File linkFile = new File(targetDir, symlinkRel);
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

    private static void setupEnvironmentFiles(File filesDir) {
        File homeDir = new File(filesDir, "home");
        if (!homeDir.exists()) homeDir.mkdirs();

        File projectsDir = new File(homeDir, "projects");
        if (!projectsDir.exists()) projectsDir.mkdirs();

        File etcDir = new File(filesDir, "usr/etc");
        if (!etcDir.exists()) etcDir.mkdirs();

        File aptDir = new File(etcDir, "apt");
        if (!aptDir.exists()) aptDir.mkdirs();

        // Setup sources.list pointing to official Termux mirror
        File sourcesList = new File(aptDir, "sources.list");
        if (!sourcesList.exists()) {
            try (FileWriter fw = new FileWriter(sourcesList)) {
                fw.write("deb https://packages.termux.dev/apt/termux-main stable main\n");
            } catch (Exception ignored) {}
        }

        File usrDir = new File(filesDir, "usr");
        // Setup .bashrc
        File bashrc = new File(homeDir, ".bashrc");
        if (!bashrc.exists()) {
            try (FileWriter fw = new FileWriter(bashrc)) {
                fw.write("# Code Editor Environment\n");
                fw.write("export PS1='\\[\\033[01;32m\\]code-editor\\[\\033[00m\\]:\\[\\033[01;34m\\]\\w\\[\\033[00m\\]\\$ '\n");
                fw.write("export PREFIX=" + usrDir.getAbsolutePath() + "\n");
                fw.write("export PATH=$PREFIX/bin:$PATH\n");
                fw.write("alias ll='ls -la'\n");
                fw.write("alias clear='printf \"\\033[2J\\033[H\"'\n");
            } catch (Exception ignored) {}
        }
    }
}
