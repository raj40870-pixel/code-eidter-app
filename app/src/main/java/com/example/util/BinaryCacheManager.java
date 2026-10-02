package com.example.util;

import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * Manages compiled output binaries and build artifacts:
 * 1. Directs output binaries into a dedicated $HOME/.bin_cache directory.
 * 2. Filters out binary/build artifacts from the project File Explorer tree.
 * 3. Automatically purges binaries and artifacts older than 24 hours (24-hour expiration policy).
 */
public class BinaryCacheManager {

    private static final String TAG = "BinaryCacheManager";
    public static final long EXPIRY_MS = 24 * 60 * 60 * 1000L; // 24 hours

    /**
     * Returns the dedicated binary cache directory inside Termux userland ($HOME/.bin_cache).
     * This directory has full native execution privileges (unlike external /sdcard).
     */
    public static File getBinaryCacheDir(Context context) {
        File homeDir = new File(context.getFilesDir(), "home");
        File binCacheDir = new File(homeDir, ".bin_cache");
        if (!binCacheDir.exists()) {
            binCacheDir.mkdirs();
        }
        binCacheDir.setReadable(true, false);
        binCacheDir.setWritable(true, false);
        binCacheDir.setExecutable(true, false);
        return binCacheDir;
    }

    /**
     * Determines whether a given file/folder name is a compiled binary or build artifact
     * that should NOT appear in the user's File Explorer.
     */
    public static boolean isBinaryOrArtifact(String name) {
        if (name == null || name.isEmpty()) return false;
        String lower = name.toLowerCase().trim();

        // Binary and compiler output formats
        if (lower.endsWith(".out")
                || lower.endsWith(".output")
                || lower.endsWith(".class")
                || lower.endsWith(".o")
                || lower.endsWith(".obj")
                || lower.endsWith(".exe")
                || lower.endsWith(".bin")
                || lower.endsWith(".dSYM")
                || lower.endsWith(".a")
                || lower.endsWith(".so")) {
            return true;
        }

        // Dedicated binary/build folders
        return lower.equals(".bin")
                || lower.equals("bin")
                || lower.equals(".bin_cache")
                || lower.equals("bin_cache")
                || lower.equals(".build")
                || lower.equals("build")
                || lower.equals("__pycache__");
    }

    /**
     * Automatically scans and purges binary files older than 24 hours.
     * Runs asynchronously on a background thread so UI is never blocked.
     */
    public static void cleanupExpiredBinaries(Context context) {
        if (context == null) return;
        final Context appContext = context.getApplicationContext();

        new Thread(() -> {
            try {
                long now = System.currentTimeMillis();

                // 1. Clean the dedicated $HOME/.bin_cache directory
                File binCacheDir = getBinaryCacheDir(appContext);
                if (binCacheDir.exists() && binCacheDir.isDirectory()) {
                    File[] files = binCacheDir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f.isFile() && (now - f.lastModified() > EXPIRY_MS)) {
                                boolean deleted = f.delete();
                                if (deleted) {
                                    Log.d(TAG, "Purged expired binary (>24h): " + f.getName());
                                }
                            }
                        }
                    }
                }

                // 2. Scan and clean any leftover legacy .out or .class files in projects directory
                File homeDir = new File(appContext.getFilesDir(), "home");
                File projectsDir = new File(homeDir, "projects");
                if (projectsDir.exists() && projectsDir.isDirectory()) {
                    cleanOldArtifactsRecursively(projectsDir, now);
                }

            } catch (Exception e) {
                Log.w(TAG, "Error cleaning expired binaries: " + e.getMessage());
            }
        }).start();
    }

    private static void cleanOldArtifactsRecursively(File dir, long now) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().equals(".git") && !f.getName().equals("node_modules")) {
                    cleanOldArtifactsRecursively(f, now);
                }
            } else if (f.isFile()) {
                if (isBinaryOrArtifact(f.getName())) {
                    // If it's an artifact older than 24 hours, delete it
                    if (now - f.lastModified() > EXPIRY_MS) {
                        f.delete();
                    }
                }
            }
        }
    }
}
