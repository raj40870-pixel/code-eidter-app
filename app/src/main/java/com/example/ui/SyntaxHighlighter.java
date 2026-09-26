package com.example.ui;

import android.graphics.Color;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {

    // C/C++ patterns
    private static final Pattern CPP_KEYWORDS = Pattern.compile(
            "\\b(asm|auto|break|case|catch|class|const|const_cast|continue|default|delete|do|dynamic_cast|else|enum|explicit|export|extern|false|for|friend|goto|if|inline|mutable|namespace|new|noexcept|nullptr|operator|private|protected|public|register|reinterpret_cast|return|sizeof|static|static_assert|static_cast|struct|switch|template|this|thread_local|throw|true|try|typedef|typeid|typename|union|using|virtual|volatile|while)\\b");

    private static final Pattern CPP_TYPES = Pattern.compile(
            "\\b(int|double|float|long|boolean|bool|char|wchar_t|short|unsigned|signed|void|size_t|int8_t|int16_t|int32_t|int64_t|uint8_t|uint16_t|uint32_t|uint64_t|string|vector|map|unordered_map|set|unordered_set|pair|queue|stack|deque|list|array|std|cout|cin|endl|cerr|printf|scanf|malloc|free|memcpy|memset)\\b");

    private static final Pattern CPP_PREPROCESSOR = Pattern.compile("^\\s*#\\s*(include|define|undef|ifdef|ifndef|if|elif|else|endif|pragma|error|warning).*", Pattern.MULTILINE);

    // Python patterns
    private static final Pattern PY_KEYWORDS = Pattern.compile(
            "\\b(and|as|assert|async|await|break|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|pass|raise|return|try|while|with|yield|True|False|None)\\b");

    private static final Pattern PY_BUILTINS = Pattern.compile(
            "\\b(print|input|len|range|enumerate|zip|map|filter|int|str|float|list|dict|set|tuple|bool|open|super|self|type|isinstance|sum|min|max|abs)\\b");

    private static final Pattern PY_COMMENTS = Pattern.compile("#.*");

    // General patterns
    private static final Pattern STRINGS = Pattern.compile("\"(\\\\.|[^\"])*\"|'(\\\\.|[^'])*'");
    private static final Pattern CPP_COMMENTS = Pattern.compile("//.*|/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern NUMBERS = Pattern.compile("\\b(\\d+(\\.\\d+)?f?|0x[0-9a-fA-F]+)\\b");

    // Colors (VS Code Dark theme)
    private static final int COLOR_KEYWORD = Color.parseColor("#569CD6");
    private static final int COLOR_TYPE = Color.parseColor("#4EC9B0");
    private static final int COLOR_STRING = Color.parseColor("#CE9178");
    private static final int COLOR_COMMENT = Color.parseColor("#6A9955");
    private static final int COLOR_NUMBER = Color.parseColor("#B5CEA8");
    private static final int COLOR_PREPROCESSOR = Color.parseColor("#C586C0");

    public static void highlight(Editable s) {
        highlight(s, "C++");
    }

    public static void highlight(Editable s, String language) {
        if (s == null || s.length() == 0) return;

        // Clear existing spans
        ForegroundColorSpan[] spans = s.getSpans(0, s.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            s.removeSpan(span);
        }

        if (language == null) language = "C++";
        String langClean = language.replaceAll("\\s+", "").toLowerCase();

        if (langClean.equals("python") || langClean.endsWith(".py")) {
            applySpan(s, PY_KEYWORDS, COLOR_KEYWORD);
            applySpan(s, PY_BUILTINS, COLOR_TYPE);
            applySpan(s, NUMBERS, COLOR_NUMBER);
            applySpan(s, STRINGS, COLOR_STRING);
            applySpan(s, PY_COMMENTS, COLOR_COMMENT);
        } else {
            // Default: C / C++ / Java
            applySpan(s, CPP_KEYWORDS, COLOR_KEYWORD);
            applySpan(s, CPP_TYPES, COLOR_TYPE);
            applySpan(s, CPP_PREPROCESSOR, COLOR_PREPROCESSOR);
            applySpan(s, NUMBERS, COLOR_NUMBER);
            applySpan(s, STRINGS, COLOR_STRING);
            applySpan(s, CPP_COMMENTS, COLOR_COMMENT);
        }
    }

    private static void applySpan(Editable s, Pattern pattern, int color) {
        Matcher matcher = pattern.matcher(s);
        while (matcher.find()) {
            s.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
