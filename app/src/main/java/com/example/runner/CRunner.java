package com.example.runner;

public class CRunner implements LanguageRunner {
    @Override
    public String getLanguageName() {
        return "C";
    }

    @Override
    public boolean isToolchainInstalled() {
        return true;
    }

    @Override
    public String getVersionCommand() {
        return "clang --version";
    }

    @Override
    public String getCompileCommand(String sourcePath, String outputPath) {
        return "mkdir -p \"$HOME/.bin_cache\" && clang \"" + sourcePath + "\" -o \"" + outputPath + "\" && chmod +x \"" + outputPath + "\"";
    }

    @Override
    public String getRunCommand(String outputPath) {
        String exec = (outputPath.startsWith("/") || outputPath.startsWith("$")) ? "\"" + outputPath + "\"" : "./\"" + outputPath + "\"";
        return "stdbuf -o0 -e0 " + exec + " 2>/dev/null || " + exec;
    }
}
