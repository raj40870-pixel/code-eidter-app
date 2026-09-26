package com.example.runner;

public class HtmlRunner implements LanguageRunner {
    @Override
    public String getLanguageName() {
        return "HTML";
    }

    @Override
    public boolean isToolchainInstalled() {
        return true;
    }

    @Override
    public String getVersionCommand() {
        return "python --version";
    }

    @Override
    public String getCompileCommand(String sourcePath, String outputPath) {
        return null;
    }

    @Override
    public String getRunCommand(String sourcePath) {
        return "python -m http.server 8080";
    }
}
