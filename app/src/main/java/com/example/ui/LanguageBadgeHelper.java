package com.example.ui;

import android.graphics.Color;
import com.example.R;

public class LanguageBadgeHelper {

    public static class BadgeInfo {
        public final String label;
        public final int badgeColor;
        public final int textColor;

        public BadgeInfo(String label, int badgeColor, int textColor) {
            this.label = label;
            this.badgeColor = badgeColor;
            this.textColor = textColor;
        }
    }

    public static int getLogoDrawable(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return R.drawable.ic_file;
        }

        // Clean out any spaces (e.g. "class. java" -> "class.java", "index. html" -> "index.html")
        String lower = fileName.replaceAll("\\s+", "").toLowerCase();

        // 1. HTML5 (Official Orange Shield with 5)
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return R.drawable.ic_logo_html5;
        }

        // 2. JavaScript (Official Yellow Square with JS)
        if (lower.endsWith(".js") || lower.endsWith(".mjs") || lower.endsWith(".cjs") || lower.endsWith(".jsx")) {
            return R.drawable.ic_logo_javascript;
        }

        // 3. CSS3 (Official Blue Shield with 3)
        if (lower.endsWith(".css") || lower.endsWith(".scss") || lower.endsWith(".sass")) {
            return R.drawable.ic_logo_css3;
        }

        // 4. Python (Official Blue & Yellow snakes)
        if (lower.endsWith(".py") || lower.endsWith(".pyw")) {
            return R.drawable.ic_logo_python;
        }

        // 5. Java (Official Coffee cup with steam)
        if (lower.endsWith(".java") || lower.endsWith(".jar")) {
            return R.drawable.ic_logo_java;
        }

        // 6. C++ (Official Hexagon)
        if (lower.endsWith(".cpp") || lower.endsWith(".cc") || lower.endsWith(".cxx") || lower.endsWith(".hpp")) {
            return R.drawable.ic_logo_cpp;
        }

        // 7. C (Official Hexagon)
        if (lower.endsWith(".c") || lower.endsWith(".h")) {
            return R.drawable.ic_logo_c;
        }

        // 8. Rust (Official Gear)
        if (lower.endsWith(".rs")) {
            return R.drawable.ic_logo_rust;
        }

        // 9. JSON (Official Gold {})
        if (lower.endsWith(".json")) {
            return R.drawable.ic_logo_json;
        }

        // 10. Shell (Green >_)
        if (lower.endsWith(".sh") || lower.endsWith(".bash") || lower.endsWith(".zsh")) {
            return R.drawable.ic_logo_shell;
        }

        return R.drawable.ic_file;
    }

    public static BadgeInfo getBadge(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return new BadgeInfo("📄", Color.parseColor("#90CAF9"), Color.WHITE);
        }

        String lower = fileName.replaceAll("\\s+", "").toLowerCase();

        if (lower.endsWith(".js") || lower.endsWith(".mjs") || lower.endsWith(".cjs")) {
            return new BadgeInfo("JS", Color.parseColor("#F7DF1E"), Color.BLACK);
        }
        if (lower.endsWith(".ts")) {
            return new BadgeInfo("TS", Color.parseColor("#3178C6"), Color.WHITE);
        }
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return new BadgeInfo("5", Color.parseColor("#E44D26"), Color.WHITE);
        }
        if (lower.endsWith(".css")) {
            return new BadgeInfo("3", Color.parseColor("#264DE4"), Color.WHITE);
        }
        if (lower.endsWith(".java") || lower.endsWith(".jar")) {
            return new BadgeInfo("JAVA", Color.parseColor("#EA2D2E"), Color.WHITE);
        }
        if (lower.endsWith(".py") || lower.endsWith(".pyw")) {
            return new BadgeInfo("PY", Color.parseColor("#3572A5"), Color.WHITE);
        }
        if (lower.endsWith(".cpp") || lower.endsWith(".cc") || lower.endsWith(".cxx")) {
            return new BadgeInfo("C++", Color.parseColor("#00599C"), Color.WHITE);
        }
        if (lower.endsWith(".c") || lower.endsWith(".h")) {
            return new BadgeInfo("C", Color.parseColor("#004482"), Color.WHITE);
        }
        if (lower.endsWith(".rs")) {
            return new BadgeInfo("RS", Color.parseColor("#DEA584"), Color.BLACK);
        }
        if (lower.endsWith(".go")) {
            return new BadgeInfo("GO", Color.parseColor("#00ADD8"), Color.WHITE);
        }
        if (lower.endsWith(".json")) {
            return new BadgeInfo("{}", Color.parseColor("#CBCB41"), Color.BLACK);
        }
        if (lower.endsWith(".sh") || lower.endsWith(".bash")) {
            return new BadgeInfo(">_", Color.parseColor("#4CAF50"), Color.WHITE);
        }

        return new BadgeInfo("📄", Color.parseColor("#546E7A"), Color.WHITE);
    }
}
