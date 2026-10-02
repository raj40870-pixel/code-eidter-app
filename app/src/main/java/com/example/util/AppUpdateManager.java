package com.example.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.example.BuildConfig;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AppUpdateManager {

    private static final String TAG = "AppUpdateManager";
    public static final String WEBSITE_URL = "https://code-eidter-apk-website.vercel.app/";
    public static final String VERSION_API_URL = "https://code-eidter-apk-website.vercel.app/version.json";

    public static void openWebsite(Activity activity) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(WEBSITE_URL));
            activity.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(activity, "Could not open browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void checkForUpdates(Activity activity, boolean isManualCheck) {
        new Thread(() -> {
            try {
                URL url = new URL(VERSION_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    int serverVersionCode = json.optInt("versionCode", 1);
                    String serverVersionName = json.optString("versionName", "1.0.0");
                    String apkUrl = json.optString("apkUrl", WEBSITE_URL);
                    String releaseNotes = json.optString("releaseNotes", "New update available with bug fixes and improvements.");

                    int currentVersionCode = BuildConfig.VERSION_CODE;

                    activity.runOnUiThread(() -> {
                        if (serverVersionCode > currentVersionCode) {
                            showUpdateDialog(activity, serverVersionName, releaseNotes, apkUrl);
                        } else if (isManualCheck) {
                            new AlertDialog.Builder(activity)
                                    .setTitle("You're Up to Date!")
                                    .setMessage("You are running the latest version of Code Editor (v" + BuildConfig.VERSION_NAME + ").")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    });
                } else if (isManualCheck) {
                    activity.runOnUiThread(() ->
                            Toast.makeText(activity, "Check update failed (HTTP " + responseCode + ")", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                if (isManualCheck) {
                    activity.runOnUiThread(() ->
                            Toast.makeText(activity, "Could not check updates: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }).start();
    }

    private static void showUpdateDialog(Activity activity, String versionName, String releaseNotes, String apkUrl) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        new AlertDialog.Builder(activity)
                .setTitle("🚀 New Update Available (v" + versionName + ")")
                .setMessage(releaseNotes + "\n\nDo you want to download and install this update directly in the app?")
                .setPositiveButton("Update Now", (dialog, which) -> {
                    startInAppDownloadAndInstall(activity, apkUrl, versionName);
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private static void startInAppDownloadAndInstall(Activity activity, String apkUrl, String versionName) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        // Check if Android 8.0+ unknown sources permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                Intent permissionIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(permissionIntent);
                Toast.makeText(activity, "Please allow 'Install unknown apps' permission to update Code Editor directly.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle("Updating Code Editor");
        progressDialog.setMessage("Downloading v" + versionName + " directly...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                URL url = new URL(apkUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.connect();

                int fileLength = connection.getContentLength();
                File downloadDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (downloadDir == null) {
                    downloadDir = activity.getCacheDir();
                }
                if (!downloadDir.exists()) downloadDir.mkdirs();

                File targetApk = new File(downloadDir, "CodeEditor-v" + versionName + ".apk");
                if (targetApk.exists()) targetApk.delete();

                InputStream input = new BufferedInputStream(connection.getInputStream());
                OutputStream output = new FileOutputStream(targetApk);

                byte[] data = new byte[8192];
                long total = 0;
                int count;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    if (fileLength > 0) {
                        int progress = (int) (total * 100 / fileLength);
                        activity.runOnUiThread(() -> {
                            progressDialog.setProgress(progress);
                            progressDialog.setMessage("Downloading v" + versionName + " (" + progress + "%)...");
                        });
                    }
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

                activity.runOnUiThread(() -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        progressDialog.dismiss();
                    }
                    installApk(activity, targetApk);
                });

            } catch (Exception e) {
                Log.e(TAG, "Download error: " + e.getMessage(), e);
                activity.runOnUiThread(() -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(activity, "Update download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private static void installApk(Activity activity, File apkFile) {
        try {
            if (!apkFile.exists()) {
                Toast.makeText(activity, "APK file not found for install", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", apkFile);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                apkUri = Uri.fromFile(apkFile);
            }
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            activity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Install error: " + e.getMessage(), e);
            Toast.makeText(activity, "Could not launch installer: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
