package com.example.runner;

public interface LanguageRunner {
    String getLanguageName();
    boolean isToolchainInstalled();
    String getVersionCommand();
    String getCompileCommand(String sourcePath, String outputPath);
    String getRunCommand(String outputPath);
}
