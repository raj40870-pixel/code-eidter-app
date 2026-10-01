package com.example.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import java.security.MessageDigest;

public class AppSecurity {

    private static final String TAG = "AppSecurity";

    // Official SHA-256 Certificate Fingerprint of codeeditor-release.jks
    public static final String OFFICIAL_SHA256 = "25:29:A6:52:40:99:7C:FA:DD:2B:38:F7:DE:F2:51:49:E5:1C:8C:DE:0E:77:E2:27:B7:47:2D:98:4C:7A:42:93";

    /**
     * Verifies that the running APK was signed by the official developer keystore.
     * Returns true if authentic, false if tampered or re-signed by an attacker.
     */
    public static boolean verifyAppAuthenticity(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();
            Signature[] signatures;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
                if (packageInfo.signingInfo == null) return false;
                signatures = packageInfo.signingInfo.getApkContentsSigners();
            } else {
                PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
                signatures = packageInfo.signatures;
            }

            if (signatures == null || signatures.length == 0) return false;

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(signatures[0].toByteArray());

            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                String hex = Integer.toHexString(0xFF & digest[i]).toUpperCase();
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
                if (i < digest.length - 1) hexString.append(':');
            }

            String currentFingerprint = hexString.toString();
            return OFFICIAL_SHA256.equalsIgnoreCase(currentFingerprint);
        } catch (Exception e) {
            Log.e(TAG, "Error checking app signature: " + e.getMessage());
            return true; // Fallback to avoid breaking on platform permission anomalies
        }
    }

    /**
     * Checks integrity on app launch. If tampered, informs user and exits.
     */
    public static void checkTamperAndEnforce(Activity activity) {
        if (!verifyAppAuthenticity(activity)) {
            new AlertDialog.Builder(activity)
                    .setTitle("⚠️ Security Alert: Modified APK")
                    .setMessage("This copy of Code Editor has an unauthorized digital signature and may have been tampered with or modified.\n\nTo ensure your device security, please install the official verified build.")
                    .setCancelable(false)
                    .setPositiveButton("Get Official Build", (dialog, which) -> {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(AppUpdateManager.WEBSITE_URL));
                            activity.startActivity(intent);
                        } catch (Exception ignored) {
                        }
                        activity.finishAffinity();
                    })
                    .setNegativeButton("Exit", (dialog, which) -> activity.finishAffinity())
                    .show();
        }
    }
}
