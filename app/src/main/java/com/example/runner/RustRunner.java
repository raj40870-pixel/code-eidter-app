package com.example.runner;

public class RustRunner implements LanguageRunner {
    @Override
    public String getLanguageName() {
        return "Rust";
    }

    @Override
    public boolean isToolchainInstalled() {
        return true;
    }

    @Override
    public String getVersionCommand() {
        return "rustc --version";
    }

    @Override
    public String getCompileCommand(String sourcePath, String outputPath) {
        return "mkdir -p \"$HOME/.bin_cache\" && rustc \"" + sourcePath + "\" -o \"" + outputPath + "\" && chmod +x \"" + outputPath + "\"";
    }

    @Override
    public String getRunCommand(String outputPath) {
        String exec = (outputPath.startsWith("/") || outputPath.startsWith("$")) ? "\"" + outputPath + "\"" : "./\"" + outputPath + "\"";
        return exec;
    }
}
