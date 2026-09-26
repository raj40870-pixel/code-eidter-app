package com.example.runner;

import java.util.HashMap;
import java.util.Map;

public class RunManager {
    private final Map<String, LanguageRunner> runners = new HashMap<>();

    public RunManager() {
        CppRunner cppRunner = new CppRunner();
        CRunner cRunner = new CRunner();
        PythonRunner pyRunner = new PythonRunner();
        JavaRunner javaRunner = new JavaRunner();
        ShellRunner shellRunner = new ShellRunner();
        RustRunner rustRunner = new RustRunner();
        NodeRunner nodeRunner = new NodeRunner();
        GoRunner goRunner = new GoRunner();
        RubyRunner rubyRunner = new RubyRunner();
        PhpRunner phpRunner = new PhpRunner();
        LuaRunner luaRunner = new LuaRunner();
        HtmlRunner htmlRunner = new HtmlRunner();
        KotlinRunner kotlinRunner = new KotlinRunner();
        CSharpRunner cSharpRunner = new CSharpRunner();

        // C++
        runners.put("c++", cppRunner);
        runners.put("cpp", cppRunner);
        runners.put("cc", cppRunner);
        runners.put("cxx", cppRunner);

        // C
        runners.put("c", cRunner);
        runners.put("h", cRunner);

        // Python
        runners.put("python", pyRunner);
        runners.put("py", pyRunner);

        // Java
        runners.put("java", javaRunner);

        // Rust
        runners.put("rust", rustRunner);
        runners.put("rs", rustRunner);

        // JavaScript / Node.js
        runners.put("javascript", nodeRunner);
        runners.put("js", nodeRunner);
        runners.put("ts", nodeRunner);

        // Go
        runners.put("go", goRunner);
        runners.put("golang", goRunner);

        // Ruby
        runners.put("ruby", rubyRunner);
        runners.put("rb", rubyRunner);

        // PHP
        runners.put("php", phpRunner);

        // Lua
        runners.put("lua", luaRunner);

        // Kotlin
        runners.put("kotlin", kotlinRunner);
        runners.put("kt", kotlinRunner);

        // C#
        runners.put("c#", cSharpRunner);
        runners.put("cs", cSharpRunner);

        // HTML & Web
        runners.put("html", htmlRunner);
        runners.put("htm", htmlRunner);

        // Shell
        runners.put("shell", shellRunner);
        runners.put("sh", shellRunner);
        runners.put("bash", shellRunner);
    }

    public LanguageRunner getRunner(String languageOrExt) {
        if (languageOrExt == null) return runners.get("cpp");
        String key = languageOrExt.toLowerCase();
        if (key.startsWith(".")) {
            key = key.substring(1);
        }
        LanguageRunner runner = runners.get(key);
        if (runner != null) return runner;
        return runners.get("cpp");
    }

    public LanguageRunner getRunnerForFile(String fileName) {
        if (fileName == null) return runners.get("cpp");
        int dot = fileName.lastIndexOf('.');
        if (dot != -1) {
            String ext = fileName.substring(dot + 1).toLowerCase();
            LanguageRunner runner = runners.get(ext);
            if (runner != null) return runner;
        }
        return runners.get("cpp");
    }
}
