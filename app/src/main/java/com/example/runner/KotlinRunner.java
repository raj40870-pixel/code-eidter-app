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
        return "mkdir -p \"$HOME/.bin_cache\" && kotlinc \"" + sourcePath + "\" -include-runtime -d \"$HOME/.bin_cache/app.jar\"";
    }

    @Override
    public String getRunCommand(String outputPath) {
        return "java -jar \"$HOME/.bin_cache/app.jar\"";
    }
}
