package com.example.runner;

public class PythonRunner implements LanguageRunner {
    @Override
    public String getLanguageName() {
        return "Python";
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
        return null; // Python is interpreted
    }

    @Override
    public String getRunCommand(String sourcePath) {
        return "python " + sourcePath;
    }
}
