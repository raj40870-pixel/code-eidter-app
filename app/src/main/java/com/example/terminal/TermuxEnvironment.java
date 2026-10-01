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
        setupDefaultScripts();
    }

    private void setupDefaultScripts() {
        try {
            File pkgFile = new File(binDir, "pkg");
            if (!pkgFile.exists() || pkgFile.length() == 0) {
                String pkgScript = "#!/bin/sh\n"
                    + "CMD=\"$1\"\n"
                    + "shift 1 2>/dev/null || true\n"
                    + "case \"$CMD\" in\n"
                    + "  install|i)\n"
                    + "    TARGET=\"$1\"\n"
                    + "    [ -z \"$TARGET\" ] && echo \"Usage: pkg install <package>\" && exit 1\n"
                    + "    case \"$TARGET\" in\n"
                    + "      node|nodejs|npm) TARGET=\"nodejs\" ;;\n"
                    + "      py|python|python3) TARGET=\"python\" ;;\n"
                    + "      clang|clang++|gcc|g++|c|cpp|c_cpp|make) TARGET=\"c_cpp\" ;;\n"
                    + "      java|jdk|openjdk|openjdk-17|openjdk-21) TARGET=\"java\" ;;\n"
                    + "      golang|go) TARGET=\"go\" ;;\n"
                    + "      rust|rustc|cargo) TARGET=\"rust\" ;;\n"
                    + "      kotlin|kotlinc) TARGET=\"kotlin\" ;;\n"
                    + "      mono|cs|csharp) TARGET=\"csharp\" ;;\n"
                    + "      php|php8) TARGET=\"php\" ;;\n"
                    + "      ruby|gem) TARGET=\"ruby\" ;;\n"
                    + "      lua|lua54) TARGET=\"lua\" ;;\n"
                    + "    esac\n"
                    + "    echo \"==> TermCode Package Manager: Installing $TARGET...\"\n"
                    + "    curl -sL https://raw.githubusercontent.com/raj40870-pixel/library/main/install.sh | sh -s \"$TARGET\"\n"
                    + "    ;;\n"
                    + "  search|list)\n"
                    + "    echo \"Available packages: python, nodejs, c_cpp, java, go, rust, kotlin, csharp, php, ruby, lua, all\"\n"
                    + "    ;;\n"
                    + "  *) \n"
                    + "    echo \"TermCode Package Manager\"\n"
                    + "    echo \"Usage: pkg install <package_name|all>\"\n"
                    + "    echo \"Example: pkg install nodejs\"\n"
                    + "    echo \"         pkg install python\"\n"
                    + "    echo \"         pkg install clang\"\n"
                    + "    echo \"         pkg install all\"\n"
                    + "    ;;\n"
                    + "esac\n";
                java.nio.file.Files.write(pkgFile.toPath(), pkgScript.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                pkgFile.setExecutable(true, false);
                pkgFile.setReadable(true, false);
            }

            File installFile = new File(binDir, "install");
            if (!installFile.exists() || installFile.length() == 0) {
                String instScript = "#!/bin/sh\n"
                    + "if [ \"$#\" -ge 1 ] && [ \"$1\" != \"-c\" ] && [ \"$1\" != \"-d\" ] && [ ! -f \"$1\" ]; then\n"
                    + "  pkg install \"$@\"\n"
                    + "  exit $?\n"
                    + "fi\n"
                    + "exec /system/bin/install \"$@\" 2>/dev/null || exit 1\n";
                java.nio.file.Files.write(installFile.toPath(), instScript.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                installFile.setExecutable(true, false);
                installFile.setReadable(true, false);
            }

            File aptFile = new File(binDir, "apt");
            if (!aptFile.exists() || aptFile.length() == 0) {
                String aptScript = "#!/bin/sh\npkg \"$@\"\n";
                java.nio.file.Files.write(aptFile.toPath(), aptScript.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                aptFile.setExecutable(true, false);
                aptFile.setReadable(true, false);
            }
        } catch (Exception ignored) {}
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

        File goDir = new File(libDir, "go");
        if (goDir.exists()) {
            env.put("GOROOT", goDir.getAbsolutePath());
        }
        File goPath = new File(homeDir, "go");
        if (!goPath.exists()) goPath.mkdirs();
        env.put("GOPATH", goPath.getAbsolutePath());
        File goCache = new File(homeDir, ".cache/go-build");
        if (!goCache.exists()) goCache.mkdirs();
        env.put("GOCACHE", goCache.getAbsolutePath());

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
            return "curl -sL https://raw.githubusercontent.com/raj40870-pixel/library/main/install.sh | sh -s c_cpp\n(Fallback: 'pkg install clang')";
        } else if ("Python".equalsIgnoreCase(language)) {
            return "curl -sL https://raw.githubusercontent.com/raj40870-pixel/library/main/install.sh | sh -s python\n(Fallback: 'pkg install python')";
        } else if ("Java".equalsIgnoreCase(language)) {
            return "curl -sL https://raw.githubusercontent.com/raj40870-pixel/library/main/install.sh | sh -s java\n(Fallback: 'pkg install openjdk-21')";
        } else if ("Node.js".equalsIgnoreCase(language)) {
            return "curl -sL https://raw.githubusercontent.com/raj40870-pixel/library/main/install.sh | sh -s nodejs\n(Fallback: 'pkg install nodejs')";
        }
        return "curl -sL https://raw.githubusercontent.com/raj40870-pixel/library/main/install.sh | sh -s " + language.toLowerCase() + "\n(Fallback: 'pkg install " + language.toLowerCase() + "')";
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
