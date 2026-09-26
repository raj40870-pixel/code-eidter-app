package com.example.runner;

public class KotlinRunner implements LanguageRunner {
    @Override
    public String getLanguageName() {
        return "Kotlin";
    }

    @Override
    public boolean isToolchainInstalled() {
        return true;
    }

    @Override
    public String getVersionCommand() {
        return "kotlinc -version";
    }

    @Override
    public String getCompileCommand(String sourcePath, String outputPath) {
        return "kotlinc \"" + sourcePath + "\" -include-runtime -d app.jar";
    }

    @Override
    public String getRunCommand(String outputPath) {
        return "java -jar app.jar";
    }
}
