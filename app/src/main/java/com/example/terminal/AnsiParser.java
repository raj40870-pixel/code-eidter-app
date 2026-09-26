package com.example.terminal;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnsiParser {

    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[([0-9;]*)m");

    public static CharSequence parse(String text) {
        if (text == null) return "";
        SpannableStringBuilder ssb = new SpannableStringBuilder();

        Matcher matcher = ANSI_PATTERN.matcher(text);
        int lastEnd = 0;
        int currentColor = Color.parseColor("#CCCCCC");
        boolean isBold = false;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            // Append text before code
            if (start > lastEnd) {
                String segment = text.substring(lastEnd, start);
                int segStart = ssb.length();
                ssb.append(segment);
                ssb.setSpan(new ForegroundColorSpan(currentColor), segStart, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (isBold) {
                    ssb.setSpan(new StyleSpan(Typeface.BOLD), segStart, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            // Parse ANSI codes
            String codes = matcher.group(1);
            if (codes != null && !codes.isEmpty()) {
                String[] parts = codes.split(";");
                for (String codeStr : parts) {
                    try {
                        int code = Integer.parseInt(codeStr);
                        switch (code) {
                            case 0: // Reset
                                currentColor = Color.parseColor("#CCCCCC");
                                isBold = false;
                                break;
                            case 1: // Bold
                                isBold = true;
                                break;
                            case 30: // Black
                                currentColor = Color.parseColor("#444444");
                                break;
                            case 31: // Red (Errors)
                                currentColor = Color.parseColor("#F44336");
                                break;
                            case 32: // Green (Success)
                                currentColor = Color.parseColor("#4CAF50");
                                break;
                            case 33: // Yellow (Warnings)
                                currentColor = Color.parseColor("#FFEB3B");
                                break;
                            case 34: // Blue
                                currentColor = Color.parseColor("#2196F3");
                                break;
                            case 35: // Magenta
                                currentColor = Color.parseColor("#AB47BC");
                                break;
                            case 36: // Cyan
                                currentColor = Color.parseColor("#00E5FF");
                                break;
                            case 37: // White
                                currentColor = Color.parseColor("#FFFFFF");
                                break;
                            case 90: // Bright Black (Gray)
                                currentColor = Color.parseColor("#888888");
                                break;
                            case 91: // Bright Red
                                currentColor = Color.parseColor("#FF5252");
                                break;
                            case 92: // Bright Green
                                currentColor = Color.parseColor("#69F0AE");
                                break;
                            case 93: // Bright Yellow
                                currentColor = Color.parseColor("#FFFF00");
                                break;
                            case 94: // Bright Blue
                                currentColor = Color.parseColor("#40C4FF");
                                break;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                // \033[m is reset
                currentColor = Color.parseColor("#CCCCCC");
                isBold = false;
            }

            lastEnd = end;
        }

        // Remaining text
        if (lastEnd < text.length()) {
            String segment = text.substring(lastEnd);
            int segStart = ssb.length();
            ssb.append(segment);
            ssb.setSpan(new ForegroundColorSpan(currentColor), segStart, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (isBold) {
                ssb.setSpan(new StyleSpan(Typeface.BOLD), segStart, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return ssb;
    }
}
