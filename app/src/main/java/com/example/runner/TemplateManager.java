package com.example.runner;

import java.util.HashMap;
import java.util.Map;

public class TemplateManager {
    private static final Map<String, String> templates = new HashMap<>();

    static {
        templates.put("cpp",
                "#include <iostream>\n\n" +
                "int main() {\n" +
                "    std::cout << \"Hello from C++ in Code Editor!\" << std::endl;\n" +
                "    return 0;\n" +
                "}\n");

        templates.put("c",
                "#include <stdio.h>\n\n" +
                "int main() {\n" +
                "    printf(\"Hello from C in Code Editor!\\n\");\n" +
                "    return 0;\n" +
                "}\n");

        templates.put("py",
                "# Python 3 Script\n\n" +
                "def main():\n" +
                "    print(\"Hello from Python in Code Editor!\")\n\n" +
                "if __name__ == '__main__':\n" +
                "    main()\n");

        templates.put("rs",
                "fn main() {\n" +
                "    println!(\"Hello from Rust in Code Editor!\");\n" +
                "}\n");

        templates.put("html",
                "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>My Project</title>\n" +
                "    <link rel=\"stylesheet\" href=\"style.css\">\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>Hello from Code Editor!</h1>\n" +
                "    <p>Welcome to modern web development.</p>\n" +
                "    <script src=\"script.js\"></script>\n" +
                "</body>\n" +
                "</html>\n");

        templates.put("css",
                "/* CSS Stylesheet */\n" +
                "* {\n" +
                "    box-sizing: border-box;\n" +
                "    margin: 0;\n" +
                "    padding: 0;\n" +
                "}\n\n" +
                "body {\n" +
                "    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\n" +
                "    background-color: #1e1e1e;\n" +
                "    color: #f1f1f1;\n" +
                "    padding: 24px;\n" +
                "}\n\n" +
                "h1 {\n" +
                "    color: #4ec9b0;\n" +
                "}\n");

        templates.put("js",
                "// JavaScript\n" +
                "console.log(\"Hello from JavaScript in Code Editor!\");\n\n" +
                "function greet(name) {\n" +
                "    return `Welcome, ${name}!`;\n" +
                "}\n\n" +
                "console.log(greet(\"Developer\"));\n");

        templates.put("ts",
                "// TypeScript\n" +
                "const greeting: string = \"Hello from TypeScript in Code Editor!\";\n" +
                "console.log(greeting);\n\n" +
                "interface User {\n" +
                "    name: string;\n" +
                "    id: number;\n" +
                "}\n");

        templates.put("go",
                "package main\n\n" +
                "import \"fmt\"\n\n" +
                "func main() {\n" +
                "    fmt.Println(\"Hello from Go in Code Editor!\")\n" +
                "}\n");

        templates.put("json",
                "{\n" +
                "  \"name\": \"code-editor-project\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"description\": \"Project built in Code Editor\"\n" +
                "}\n");

        templates.put("php",
                "<?php\n\n" +
                "echo \"Hello from PHP in Code Editor!\\n\";\n");

        templates.put("rb",
                "# Ruby Script\n" +
                "puts \"Hello from Ruby in Code Editor!\"\n");

        templates.put("lua",
                "-- Lua Script\n" +
                "print(\"Hello from Lua in Code Editor!\")\n");

        templates.put("sh",
                "#!/bin/bash\n\n" +
                "echo \"Hello from Shell in Code Editor!\"\n" +
                "echo \"Working Directory: $(pwd)\"\n");

        templates.put("md",
                "# Project Title\n\n" +
                "A brief description of what this project does.\n\n" +
                "## Getting Started\n\n" +
                "- Run your code directly from the toolbar.\n");
    }

    public static String getTemplate(String fileNameOrLang) {
        if (fileNameOrLang == null || fileNameOrLang.isEmpty()) {
            return templates.get("cpp");
        }

        // Clean out any spaces that Android soft keyboard might have inserted (e.g. "class. java" -> "class.java")
        String raw = fileNameOrLang.replaceAll("\\s+", "");
        String clean = raw.toLowerCase();

        // 1. Check for Java with dynamic Class name matching file name!
        if (clean.endsWith(".java") || clean.equals("java")) {
            String className = "Main";
            if (raw.contains(".")) {
                String base = raw.substring(0, raw.lastIndexOf('.'));
                base = base.replaceAll("[^a-zA-Z0-9_]", "");
                if (!base.isEmpty() && Character.isJavaIdentifierStart(base.charAt(0))) {
                    // Capitalize first letter while preserving remaining casing
                    className = Character.toUpperCase(base.charAt(0)) + (base.length() > 1 ? base.substring(1) : "");
                }
            }
            if (className.equalsIgnoreCase("class") || className.equalsIgnoreCase("interface") || className.equalsIgnoreCase("enum")) {
                className = "MainClass";
            }
            return "public class " + className + " {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        System.out.println(\"Hello from Java (" + className + ") in Code Editor!\");\n" +
                    "    }\n" +
                    "}\n";
        }

        // 2. Check by extension
        int dotIdx = clean.lastIndexOf('.');
        String ext = dotIdx != -1 ? clean.substring(dotIdx + 1) : clean;

        if (ext.equals("cpp") || ext.equals("cc") || ext.equals("cxx") || ext.equals("c++")) return templates.get("cpp");
        if (ext.equals("c") || ext.equals("h")) return templates.get("c");
        if (ext.equals("py") || ext.equals("python")) return templates.get("py");
        if (ext.equals("rs") || ext.equals("rust")) return templates.get("rs");
        if (ext.equals("html") || ext.equals("htm")) return templates.get("html");
        if (ext.equals("css")) return templates.get("css");
        if (ext.equals("js") || ext.equals("mjs") || ext.equals("cjs") || ext.equals("javascript")) return templates.get("js");
        if (ext.equals("ts") || ext.equals("typescript")) return templates.get("ts");
        if (ext.equals("go") || ext.equals("golang")) return templates.get("go");
        if (ext.equals("json")) return templates.get("json");
        if (ext.equals("php")) return templates.get("php");
        if (ext.equals("rb") || ext.equals("ruby")) return templates.get("rb");
        if (ext.equals("lua")) return templates.get("lua");
        if (ext.equals("sh") || ext.equals("bash")) return templates.get("sh");
        if (ext.equals("md") || ext.equals("markdown")) return templates.get("md");

        return "// Start coding in " + fileNameOrLang + "\n\n";
    }
}
