package com.example.runner;

public class RubyRunner implements LanguageRunner {
    @Override
    public String getLanguageName() {
        return "Ruby";
    }

    @Override
    public boolean isToolchainInstalled() {
        return true;
    }

    @Override
    public String getVersionCommand() {
        return "ruby -v";
    }

    @Override
    public String getCompileCommand(String sourcePath, String outputPath) {
        return null;
    }

    @Override
    public String getRunCommand(String sourcePath) {
        return "ruby \"" + sourcePath + "\"";
    }
}
