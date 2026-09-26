package com.example.runner;

public class LuaRunner implements LanguageRunner {
    @Override
    public String getLanguageName() {
        return "Lua";
    }

    @Override
    public boolean isToolchainInstalled() {
        return true;
    }

    @Override
    public String getVersionCommand() {
        return "lua -v";
    }

    @Override
    public String getCompileCommand(String sourcePath, String outputPath) {
        return null;
    }

    @Override
    public String getRunCommand(String sourcePath) {
        return "lua \"" + sourcePath + "\"";
    }
}
