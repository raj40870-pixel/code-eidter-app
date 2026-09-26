package com.example.terminal;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LocalTerminalSession implements TerminalSession {

    private static final String TAG = "LocalTerminal";

    private Process activeProcess;
    private String defaultShell;
    private String defaultCwd;
    private Map<String, String> environment;
    private OutputListener outputListener;
    private volatile boolean isBusy = false;
    private String initError = null;

    public LocalTerminalSession(String shell, String cwd, Map<String, String> environment) {
        this.defaultShell = shell;
        this.defaultCwd = cwd;
        this.environment = environment;
    }

    public LocalTerminalSession(String shell, String cwd) {
        this(shell, cwd, null);
    }

    @Override
    public void setOutputListener(OutputListener listener) {
        this.outputListener = listener;
    }

    @Override
    public boolean isBusy() {
        return isBusy && activeProcess != null && isAlive(activeProcess);
    }

    private boolean isAlive(Process p) {
        try {
            p.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    public void restart(String newShell, String newCwd, Map<String, String> newEnv) {
        stop();
        this.defaultShell = newShell;
        this.defaultCwd = newCwd;
        this.environment = newEnv;
        this.initError = null;
    }

    @Override
    public synchronized void execute(String command, String workingDir, Runnable onComplete) {
        if (isBusy()) {
            if (outputListener != null) {
                outputListener.onOutput("\u001B[33m[Warning: A process is already running. Type input or tap STOP/CLEAR to terminate.]\u001B[0m\n");
            }
            if (onComplete != null) onComplete.run();
            return;
        }

        isBusy = true;

        new Thread(() -> {
            try {
                // Determine shell to use
                String shellPath = defaultShell;
                File bash = new File("/data/data/com.termux/files/usr/bin/bash");
                if (bash.exists() && bash.canExecute()) {
                    shellPath = bash.getAbsolutePath();
                } else if (shellPath == null || !new File(shellPath).exists()) {
                    shellPath = "/system/bin/sh";
                }

                // Determine working directory
                File workDir = null;
                if (workingDir != null && !workingDir.isEmpty()) {
                    workDir = new File(workingDir);
                }
                if (workDir == null || !workDir.exists() || !workDir.isDirectory()) {
                    if (defaultCwd != null && !defaultCwd.isEmpty()) {
                        workDir = new File(defaultCwd);
                    }
                }
                if (workDir == null || !workDir.exists()) {
                    workDir = new File("/data/data/com.termux/files/home");
                    if (!workDir.exists()) workDir.mkdirs();
                }

                ProcessBuilder pb = new ProcessBuilder(shellPath, "-c", command);
                if (workDir.exists()) {
                    pb.directory(workDir);
                }
                if (environment != null && !environment.isEmpty()) {
                    pb.environment().putAll(environment);
                }
                pb.redirectErrorStream(true);

                activeProcess = pb.start();

                // Live read stdout + stderr with immediate flushing
                try (InputStream in = activeProcess.getInputStream();
                     InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    char[] buf = new char[512];
                    int read;
                    while ((read = reader.read(buf)) != -1) {
                        String chunk = new String(buf, 0, read);
                        if (outputListener != null) {
                            outputListener.onOutput(chunk);
                        }
                    }
                }

                int exitCode = activeProcess.waitFor();
                if (outputListener != null) {
                    if (exitCode == 0) {
                        outputListener.onOutput("\n\u001B[32m[Process completed successfully (exit code 0)]\u001B[0m\n");
                    } else {
                        outputListener.onOutput("\n\u001B[33m[Process exited with code " + exitCode + "]\u001B[0m\n");
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Command execution error", e);
                initError = e.getMessage();
                if (outputListener != null) {
                    outputListener.onOutput("\n\u001B[31m[Execution Error: " + (e.getMessage() != null ? e.getMessage() : "Unknown error") + "]\u001B[0m\n");
                }
            } finally {
                isBusy = false;
                activeProcess = null;
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        }).start();
    }

    @Override
    public void write(String data) {
        if (data != null) {
            write(data.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void write(byte[] data) {
        if (activeProcess != null && isAlive(activeProcess) && data != null) {
            try {
                OutputStream os = activeProcess.getOutputStream();
                os.write(data);
                os.flush();
            } catch (IOException e) {
                Log.w(TAG, "Error writing to process stdin", e);
            }
        }
    }

    public void sendCtrlC() {
        stop();
        if (outputListener != null) {
            outputListener.onOutput("\n^C\n\u001B[31m[Process interrupted]\u001B[0m\n");
        }
    }

    @Override
    public void resize(int rows, int cols) {}

    @Override
    public void stop() {
        if (activeProcess != null) {
            try {
                activeProcess.destroyForcibly();
            } catch (Exception ignored) {}
            activeProcess = null;
        }
        isBusy = false;
    }

    @Override
    public InputStream getInputStream() {
        return activeProcess != null ? activeProcess.getInputStream() : null;
    }

    @Override
    public OutputStream getOutputStream() {
        return activeProcess != null ? activeProcess.getOutputStream() : null;
    }

    @Override
    public String getInitError() {
        return initError;
    }
}
