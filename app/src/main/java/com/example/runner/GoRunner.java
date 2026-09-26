package com.example.runner;

public class GoRunner implements LanguageRunner {
    @Override
    public String getLanguageName() {
        return "Go";
    }

    @Override
    public boolean isToolchainInstalled() {
        return true;
    }

    @Override
    public String getVersionCommand() {
        return "go version";
    }

    @Override
    public String getCompileCommand(String sourcePath, String outputPath) {
        return null;
    }

    @Override
    public String getRunCommand(String sourcePath) {
        return "go run \"" + sourcePath + "\"";
    }
}
