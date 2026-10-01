package com.example.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.BuildConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AppUpdateManager {

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
                .setTitle("🎉 New Update Available (v" + versionName + ")")
                .setMessage(releaseNotes + "\n\nDo you want to download and install the update now?")
                .setPositiveButton("Download Update", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(activity, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Later", null)
                .show();
    }
}
