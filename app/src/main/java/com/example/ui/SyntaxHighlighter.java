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

    public static class Theme {
        public final String name;
        public final int bgColor;
        public final int textColor;
        public final int gutterColor;
        public final int dividerColor;
        public final int lineNumberColor;
        public final int keywordColor;
        public final int typeColor;
        public final int stringColor;
        public final int commentColor;
        public final int numberColor;
        public final int preprocessorColor;

        public Theme(String name, String bg, String text, String gutter, String div, String lineNum,
                     String kw, String type, String str, String comment, String num, String prep) {
            this.name = name;
            this.bgColor = Color.parseColor(bg);
            this.textColor = Color.parseColor(text);
            this.gutterColor = Color.parseColor(gutter);
            this.dividerColor = Color.parseColor(div);
            this.lineNumberColor = Color.parseColor(lineNum);
            this.keywordColor = Color.parseColor(kw);
            this.typeColor = Color.parseColor(type);
            this.stringColor = Color.parseColor(str);
            this.commentColor = Color.parseColor(comment);
            this.numberColor = Color.parseColor(num);
            this.preprocessorColor = Color.parseColor(prep);
        }
    }

    public static final Theme THEME_VS_CODE = new Theme(
            "VS Code Dark", "#181818", "#E6E6E6", "#181818", "#262626", "#666666",
            "#569CD6", "#4EC9B0", "#CE9178", "#6A9955", "#B5CEA8", "#C586C0"
    );

    public static final Theme THEME_DRACULA = new Theme(
            "Dracula", "#282A36", "#F8F8F2", "#21222C", "#44475A", "#6272A4",
            "#FF79C6", "#8BE9FD", "#F1FA8C", "#6272A4", "#BD93F9", "#FF79C6"
    );

    public static final Theme THEME_MONOKAI = new Theme(
            "Monokai Pro", "#272822", "#F8F8F2", "#1E1F1C", "#3E3D32", "#75715E",
            "#F92672", "#66D9EF", "#E6DB74", "#75715E", "#AE81FF", "#FD971F"
    );

    public static final Theme THEME_ONE_DARK = new Theme(
            "One Dark Pro", "#282C34", "#ABB2BF", "#21252B", "#3E4451", "#5C6370",
            "#C678DD", "#E5C07B", "#98C379", "#5C6370", "#D19A66", "#C678DD"
    );

    public static final Theme THEME_MATRIX = new Theme(
            "Matrix Neon", "#0D1117", "#00FF66", "#070A0E", "#1F3826", "#008833",
            "#00FFCC", "#33FF33", "#66FF66", "#008833", "#00E5FF", "#39FF14"
    );

    private static Theme activeTheme = THEME_VS_CODE;

    public static void setActiveTheme(Theme theme) {
        if (theme != null) activeTheme = theme;
    }

    public static Theme getActiveTheme() {
        return activeTheme;
    }

    public static Theme getThemeByName(String name) {
        if (name == null) return THEME_VS_CODE;
        if (name.contains("Dracula")) return THEME_DRACULA;
        if (name.contains("Monokai")) return THEME_MONOKAI;
        if (name.contains("One Dark")) return THEME_ONE_DARK;
        if (name.contains("Matrix")) return THEME_MATRIX;
        return THEME_VS_CODE;
    }

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
            applySpan(s, PY_KEYWORDS, activeTheme.keywordColor);
            applySpan(s, PY_BUILTINS, activeTheme.typeColor);
            applySpan(s, NUMBERS, activeTheme.numberColor);
            applySpan(s, STRINGS, activeTheme.stringColor);
            applySpan(s, PY_COMMENTS, activeTheme.commentColor);
        } else {
            // Default: C / C++ / Java
            applySpan(s, CPP_KEYWORDS, activeTheme.keywordColor);
            applySpan(s, CPP_TYPES, activeTheme.typeColor);
            applySpan(s, CPP_PREPROCESSOR, activeTheme.preprocessorColor);
            applySpan(s, NUMBERS, activeTheme.numberColor);
            applySpan(s, STRINGS, activeTheme.stringColor);
            applySpan(s, CPP_COMMENTS, activeTheme.commentColor);
        }
    }

    private static void applySpan(Editable s, Pattern pattern, int color) {
        Matcher matcher = pattern.matcher(s);
        while (matcher.find()) {
            s.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
