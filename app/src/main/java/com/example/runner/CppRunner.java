package com.example.runner;

public class CppRunner implements LanguageRunner {
    @Override
    public String getLanguageName() {
        return "C++";
    }

    @Override
    public boolean isToolchainInstalled() {
        return true; 
    }

    @Override
    public String getVersionCommand() {
        return "clang++ --version";
    }

    @Override
    public String getCompileCommand(String sourcePath, String outputPath) {
        return "clang++ -std=c++17 \"" + sourcePath + "\" -o \"" + outputPath + "\"";
    }

    @Override
    public String getRunCommand(String outputPath) {
        return "stdbuf -o0 -e0 ./\"" + outputPath + "\" 2>/dev/null || ./\"" + outputPath + "\"";
    }
}
