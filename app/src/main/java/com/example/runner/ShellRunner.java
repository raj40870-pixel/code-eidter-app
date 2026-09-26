package com.example.runner;

public class ShellRunner implements LanguageRunner {
    @Override
    public String getLanguageName() {
        return "Shell";
    }

    @Override
    public boolean isToolchainInstalled() {
        return true;
    }

    @Override
    public String getVersionCommand() {
        return "bash --version";
    }

    @Override
    public String getCompileCommand(String sourcePath, String outputPath) {
        return null; // Shell is interpreted
    }

    @Override
    public String getRunCommand(String sourcePath) {
        return "bash \"" + sourcePath + "\"";
    }
}
