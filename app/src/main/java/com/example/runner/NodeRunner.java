package com.example.runner;

public class NodeRunner implements LanguageRunner {
    @Override
    public String getLanguageName() {
        return "JavaScript";
    }

    @Override
    public boolean isToolchainInstalled() {
        return true;
    }

    @Override
    public String getVersionCommand() {
        return "node -v";
    }

    @Override
    public String getCompileCommand(String sourcePath, String outputPath) {
        return null;
    }

    @Override
    public String getRunCommand(String sourcePath) {
        return "node \"" + sourcePath + "\"";
    }
}
