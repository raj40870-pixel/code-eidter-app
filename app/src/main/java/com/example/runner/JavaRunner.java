package com.example.runner;

public class JavaRunner implements LanguageRunner {
    @Override
    public String getLanguageName() {
        return "Java";
    }

    @Override
    public boolean isToolchainInstalled() {
        return true;
    }

    @Override
    public String getVersionCommand() {
        return "javac -version";
    }

    @Override
    public String getCompileCommand(String sourcePath, String outputPath) {
        return "mkdir -p \"$HOME/.bin_cache\" && javac -d \"$HOME/.bin_cache\" \"" + sourcePath + "\"";
    }

    @Override
    public String getRunCommand(String sourcePath) {
        String name = sourcePath;
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf('/') + 1);
        }
        if (name.endsWith(".java")) {
            name = name.substring(0, name.length() - 5);
        } else if (name.endsWith(".out")) {
            name = name.substring(0, name.length() - 4);
        }
        return "java -cp \"$HOME/.bin_cache:.\" " + name;
    }
}
