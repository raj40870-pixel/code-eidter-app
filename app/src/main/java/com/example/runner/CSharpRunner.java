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
        String exe = sourcePath.contains(".") ? sourcePath.substring(0, sourcePath.lastIndexOf('.')) + ".exe" : sourcePath + ".exe";
        return "mcs \"" + sourcePath + "\" -out:\"" + exe + "\"";
    }

    @Override
    public String getRunCommand(String sourceOrOutputPath) {
        String exe = sourceOrOutputPath.contains(".") ? sourceOrOutputPath.substring(0, sourceOrOutputPath.lastIndexOf('.')) + ".exe" : sourceOrOutputPath + ".exe";
        return "mono \"" + exe + "\"";
    }
}
