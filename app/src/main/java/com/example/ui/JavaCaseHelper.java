package com.example.ui;

import android.text.Editable;
import java.util.HashMap;
import java.util.Map;

/**
 * Intelligent Java Case-Sensitivity Auto-Correction Helper.
 * Corrects mobile soft-keyboard auto-capitalization of keywords (e.g. Public -> public, Class -> class)
 * and auto-fixes lowercase typing for standard Java classes (e.g. system -> System, string -> String, scanner -> Scanner).
 */
public class JavaCaseHelper {

    private static final Map<String, String> KEYWORD_MAP = new HashMap<>();
    private static final Map<String, String> CLASS_MAP = new HashMap<>();

    static {
        // Java language keywords
        String[] keywords = {
                "abstract", "assert", "boolean", "break", "byte", "case", "catch",
                "char", "class", "const", "continue", "default", "do", "double",
                "else", "enum", "extends", "final", "finally", "float", "for",
                "goto", "if", "implements", "import", "instanceof", "int",
                "interface", "long", "native", "new", "package", "private",
                "protected", "public", "record", "return", "short", "static",
                "strictfp", "super", "switch", "synchronized", "this", "throw",
                "throws", "transient", "try", "void", "volatile", "while",
                "true", "false", "null", "main"
        };

        for (String kw : keywords) {
            // Capitalized version (e.g., "Public", "Class", "Static")
            String cap = Character.toUpperCase(kw.charAt(0)) + (kw.length() > 1 ? kw.substring(1) : "");
            KEYWORD_MAP.put(cap, kw);
            // ALL-CAPS version (e.g., "PUBLIC", "CLASS", "STATIC")
            KEYWORD_MAP.put(kw.toUpperCase(), kw);
        }

        // Standard Java classes commonly typed in lowercase on mobile
        addClassMapping("system", "System");
        addClassMapping("string", "String");
        addClassMapping("scanner", "Scanner");
        addClassMapping("math", "Math");
        addClassMapping("integer", "Integer");
        addClassMapping("character", "Character");
        addClassMapping("object", "Object");
        addClassMapping("objects", "Objects");
        addClassMapping("arrays", "Arrays");
        addClassMapping("collections", "Collections");
        addClassMapping("collection", "Collection");
        addClassMapping("list", "List");
        addClassMapping("arraylist", "ArrayList");
        addClassMapping("linkedlist", "LinkedList");
        addClassMapping("map", "Map");
        addClassMapping("hashmap", "HashMap");
        addClassMapping("linkedhashmap", "LinkedHashMap");
        addClassMapping("treemap", "TreeMap");
        addClassMapping("set", "Set");
        addClassMapping("hashset", "HashSet");
        addClassMapping("treeset", "TreeSet");
        addClassMapping("queue", "Queue");
        addClassMapping("deque", "Deque");
        addClassMapping("stack", "Stack");
        addClassMapping("vector", "Vector");
        addClassMapping("iterator", "Iterator");
        addClassMapping("iterable", "Iterable");
        addClassMapping("comparator", "Comparator");
        addClassMapping("comparable", "Comparable");
        addClassMapping("exception", "Exception");
        addClassMapping("throwable", "Throwable");
        addClassMapping("error", "Error");
        addClassMapping("runtimeexception", "RuntimeException");
        addClassMapping("ioexception", "IOException");
        addClassMapping("nullpointerexception", "NullPointerException");
        addClassMapping("indexoutofboundsexception", "IndexOutOfBoundsException");
        addClassMapping("illegalargumentexception", "IllegalArgumentException");
        addClassMapping("thread", "Thread");
        addClassMapping("runnable", "Runnable");
        addClassMapping("stringbuilder", "StringBuilder");
        addClassMapping("stringbuffer", "StringBuffer");
        addClassMapping("file", "File");
        addClassMapping("inputstream", "InputStream");
        addClassMapping("outputstream", "OutputStream");
        addClassMapping("printstream", "PrintStream");
        addClassMapping("bufferedreader", "BufferedReader");
        addClassMapping("bufferedwriter", "BufferedWriter");
        addClassMapping("filereader", "FileReader");
        addClassMapping("filewriter", "FileWriter");
        addClassMapping("printwriter", "PrintWriter");
        addClassMapping("optional", "Optional");
        addClassMapping("stream", "Stream");
        addClassMapping("biginteger", "BigInteger");
        addClassMapping("bigdecimal", "BigDecimal");
        addClassMapping("date", "Date");
        addClassMapping("calendar", "Calendar");
        addClassMapping("localdate", "LocalDate");
        addClassMapping("localdatetime", "LocalDateTime");
        addClassMapping("localtime", "LocalTime");
        addClassMapping("override", "Override");
        addClassMapping("@override", "@Override");
    }

    private static void addClassMapping(String lower, String proper) {
        CLASS_MAP.put(lower.toLowerCase(), proper);
    }

    /**
     * Finds the replacement for a given token based on the trigger character.
     * Returns null if no replacement is needed.
     */
    public static String getReplacement(String token, char triggerChar) {
        if (token == null || token.isEmpty()) return null;

        // Special handling for primitive types followed by dot (e.g. int.parseInt -> Integer.parseInt)
        if (triggerChar == '.') {
            if (token.equals("int")) return "Integer";
            if (token.equals("char")) return "Character";
            if (token.equals("double")) return "Double";
            if (token.equals("float")) return "Float";
            if (token.equals("long")) return "Long";
            if (token.equals("boolean")) return "Boolean";
            if (token.equals("byte")) return "Byte";
            if (token.equals("short")) return "Short";
        }

        // Check if token matches capitalized keyword
        if (KEYWORD_MAP.containsKey(token)) {
            // Keep wrapper classes capitalized if followed by dot (e.g. Boolean.parseBoolean)
            if (triggerChar == '.' && (token.equals("Boolean") || token.equals("Byte") ||
                    token.equals("Short") || token.equals("Long") || token.equals("Float") || token.equals("Double"))) {
                return null;
            }
            return KEYWORD_MAP.get(token);
        }

        // Check if token matches standard classes
        String lower = token.toLowerCase();
        if (CLASS_MAP.containsKey(lower)) {
            String proper = CLASS_MAP.get(lower);
            if (!token.equals(proper)) {
                return proper;
            }
        }

        return null;
    }

    /**
     * Checks if the position inside text is inside a double-quoted string literal or comments.
     */
    public static boolean isInsideStringOrComment(CharSequence s, int position) {
        if (s == null || position <= 0 || position > s.length()) return false;

        // Find line start
        int lineStart = position;
        while (lineStart > 0 && s.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }

        boolean inString = false;
        boolean inChar = false;
        for (int i = lineStart; i < position; i++) {
            char ch = s.charAt(i);
            if (ch == '\\') {
                i++; // Skip escaped char
                continue;
            }
            if (ch == '"' && !inChar) {
                inString = !inString;
            } else if (ch == '\'' && !inString) {
                inChar = !inChar;
            } else if (!inString && !inChar && ch == '/' && i + 1 < position && s.charAt(i + 1) == '/') {
                return true; // Inside single-line comment //
            }
        }

        if (inString || inChar) return true;

        // Block comment check /* ... */
        int lastBlockStart = -1;
        int lastBlockEnd = -1;
        for (int i = 0; i < position - 1; i++) {
            if (s.charAt(i) == '/' && s.charAt(i + 1) == '*') {
                lastBlockStart = i;
            } else if (s.charAt(i) == '*' && s.charAt(i + 1) == '/') {
                lastBlockEnd = i;
            }
        }
        return lastBlockStart != -1 && (lastBlockEnd == -1 || lastBlockStart > lastBlockEnd);
    }

    /**
     * Scans an entire document and fixes all Java case-sensitivity mistakes outside strings & comments.
     */
    public static void fixAllJavaCases(Editable ed) {
        if (ed == null || ed.length() == 0) return;

        boolean inString = false;
        boolean inChar = false;
        boolean inBlockComment = false;

        int len = ed.length();
        int tokenStart = -1;

        for (int i = 0; i < len; i++) {
            char ch = ed.charAt(i);

            // Handle comments and strings
            if (!inString && !inChar && !inBlockComment && ch == '/' && i + 1 < len) {
                if (ed.charAt(i + 1) == '/') {
                    while (i < len && ed.charAt(i) != '\n') {
                        i++;
                    }
                    tokenStart = -1;
                    continue;
                } else if (ed.charAt(i + 1) == '*') {
                    inBlockComment = true;
                    i++;
                    tokenStart = -1;
                    continue;
                }
            }

            if (inBlockComment) {
                if (ch == '*' && i + 1 < len && ed.charAt(i + 1) == '/') {
                    inBlockComment = false;
                    i++;
                }
                tokenStart = -1;
                continue;
            }

            if (ch == '\\' && (inString || inChar)) {
                i++; // Skip escape
                continue;
            }

            if (ch == '"' && !inChar) {
                inString = !inString;
                tokenStart = -1;
                continue;
            }

            if (ch == '\'' && !inString) {
                inChar = !inChar;
                tokenStart = -1;
                continue;
            }

            if (inString || inChar) {
                tokenStart = -1;
                continue;
            }

            // Outside string/comment: parse tokens
            boolean isTokenChar = Character.isLetterOrDigit(ch) || ch == '_' || ch == '@';
            if (isTokenChar) {
                if (tokenStart == -1) {
                    tokenStart = i;
                }
            } else {
                if (tokenStart != -1) {
                    int tokenEnd = i;
                    String token = ed.subSequence(tokenStart, tokenEnd).toString();
                    char trigger = ch;
                    String replacement = getReplacement(token, trigger);
                    if (replacement != null && !replacement.equals(token)) {
                        ed.replace(tokenStart, tokenEnd, replacement);
                        int diff = replacement.length() - token.length();
                        i += diff;
                        len = ed.length();
                    }
                    tokenStart = -1;
                }
            }
        }

        // Check trailing token at EOF
        if (tokenStart != -1 && !inString && !inChar && !inBlockComment) {
            String token = ed.subSequence(tokenStart, len).toString();
            String replacement = getReplacement(token, ' ');
            if (replacement != null && !replacement.equals(token)) {
                ed.replace(tokenStart, len, replacement);
            }
        }
    }
}
