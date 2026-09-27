package com.example.terminal;

import android.content.Context;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TermuxEnvironment {

    private final Context context;
    private final File filesDir;
    private final File usrDir;
    private final File binDir;
    private final File libDir;
    private final File homeDir;
    private final File tmpDir;
    private final File projectsDir;

    public TermuxEnvironment(Context context) {
        this.context = context;
        this.filesDir = context.getFilesDir();
        this.usrDir = new File(filesDir, "usr");
        this.binDir = new File(usrDir, "bin");
        this.libDir = new File(usrDir, "lib");
        this.homeDir = new File(filesDir, "home");
        this.tmpDir = new File(usrDir, "tmp");
        this.projectsDir = new File(homeDir, "projects");

        setupDirectories();
    }

    private void setupDirectories() {
        if (!usrDir.exists()) usrDir.mkdirs();
        if (!binDir.exists()) binDir.mkdirs();
        if (!libDir.exists()) libDir.mkdirs();
        if (!homeDir.exists()) homeDir.mkdirs();
        if (!tmpDir.exists()) tmpDir.mkdirs();
        if (!projectsDir.exists()) projectsDir.mkdirs();
    }

    public Map<String, String> getEnvironment() {
        Map<String, String> env = new HashMap<>();
        String prefix = usrDir.getAbsolutePath();
        String home = homeDir.getAbsolutePath();
        String bin = binDir.getAbsolutePath();
        String lib = libDir.getAbsolutePath();
        String tmp = tmpDir.getAbsolutePath();

        env.put("PREFIX", prefix);
        env.put("HOME", home);
        env.put("PATH", bin + ":" + bin + "/applets:/system/bin:/system/xbin");
        env.put("LD_LIBRARY_PATH", lib);
        env.put("TMPDIR", tmp);
        env.put("TERMUX_APP_PACKAGE_MANAGER", "apt");
        env.put("USER", "u0_a100");
        env.put("LOGNAME", "u0_a100");
        env.put("TERM", "xterm-256color");
        env.put("LANG", "en_US.UTF-8");
        env.put("COLORTERM", "truecolor");

        File termuxExec = new File(libDir, "libtermux-exec.so");
        if (termuxExec.exists()) {
            env.put("LD_PRELOAD", termuxExec.getAbsolutePath());
        }

        File bashFile = new File(binDir, "bash");
        if (bashFile.exists()) {
            env.put("SHELL", bashFile.getAbsolutePath());
        } else {
            env.put("SHELL", "/system/bin/sh");
        }

        return env;
    }

    public String getDefaultShell() {
        File bashFile = new File(binDir, "bash");
        if (bashFile.exists()) {
            bashFile.setExecutable(true, false);
            return bashFile.getAbsolutePath();
        }
        File shFile = new File(binDir, "sh");
        if (shFile.exists()) {
            shFile.setExecutable(true, false);
            return shFile.getAbsolutePath();
        }
        return "/system/bin/sh";
    }

    public String getHomePath() {
        return homeDir.getAbsolutePath();
    }

    public String getProjectsPath() {
        return projectsDir.getAbsolutePath();
    }

    public boolean isBinaryAvailable(String binaryName) {
        File bin = new File(binDir, binaryName);
        if (bin.exists()) return true;
        File sysBin = new File("/system/bin", binaryName);
        return sysBin.exists();
    }

    public String getInstallHelp(String language) {
        if ("C++".equalsIgnoreCase(language) || "C".equalsIgnoreCase(language)) {
            return "pkg install clang\n(Or 'apt install clang')";
        } else if ("Python".equalsIgnoreCase(language)) {
            return "pkg install python";
        } else if ("Java".equalsIgnoreCase(language)) {
            return "pkg install openjdk-17";
        } else if ("Node.js".equalsIgnoreCase(language)) {
            return "pkg install nodejs";
        }
        return "pkg install " + language.toLowerCase();
    }

    public void clearAptLocks() {
        File[] dirs = {
            new File(usrDir, "var/lib/dpkg"),
            new File(usrDir, "var/lib/apt/lists"),
            new File(usrDir, "var/cache/apt/archives")
        };
        for (File d : dirs) {
            if (d.exists() && d.isDirectory()) {
                File[] files = d.listFiles((dir, name) -> name.startsWith("lock"));
                if (files != null) {
                    for (File f : files) {
                        try {
                            f.delete();
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        File[] locks = {
            new File(usrDir, "var/lib/dpkg/lock-frontend"),
            new File(usrDir, "var/lib/dpkg/lock"),
            new File(usrDir, "var/lib/apt/lists/lock"),
            new File(usrDir, "var/cache/apt/archives/lock")
        };
        for (File f : locks) {
            try {
                if (f.exists()) {
                    f.delete();
                }
            } catch (Exception ignored) {}
        }
    }
}
