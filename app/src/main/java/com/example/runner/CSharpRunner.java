package com.example.runner;

public class CSharpRunner implements LanguageRunner {
    @Override
    public String getLanguageName() {
        return "C#";
    }

    @Override
    public boolean isToolchainInstalled() {
        return true;
    }

    @Override
    public String getVersionCommand() {
        return "mono --version";
    }

    @Override
    public String getCompileCommand(String sourcePath, String outputPath) {
        String base = sourcePath.contains(".") ? sourcePath.substring(0, sourcePath.lastIndexOf('.')) : sourcePath;
        if (base.contains("/")) {
            base = base.substring(base.lastIndexOf('/') + 1);
        }
        String exe = "$HOME/.bin_cache/" + base + ".exe";
        return "mkdir -p \"$HOME/.bin_cache\" && mcs \"" + sourcePath + "\" -out:\"" + exe + "\"";
    }

    @Override
    public String getRunCommand(String sourceOrOutputPath) {
        String base = sourceOrOutputPath.contains(".") ? sourceOrOutputPath.substring(0, sourceOrOutputPath.lastIndexOf('.')) : sourceOrOutputPath;
        if (base.contains("/")) {
            base = base.substring(base.lastIndexOf('/') + 1);
        }
        String exe = "$HOME/.bin_cache/" + base + ".exe";
        return "mono \"" + exe + "\"";
    }
}
