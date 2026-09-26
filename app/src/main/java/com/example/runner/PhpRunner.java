package com.example.runner;

public class PhpRunner implements LanguageRunner {
    @Override
    public String getLanguageName() {
        return "PHP";
    }

    @Override
    public boolean isToolchainInstalled() {
        return true;
    }

    @Override
    public String getVersionCommand() {
        return "php -v";
    }

    @Override
    public String getCompileCommand(String sourcePath, String outputPath) {
        return null;
    }

    @Override
    public String getRunCommand(String sourcePath) {
        return "php \"" + sourcePath + "\"";
    }
}
