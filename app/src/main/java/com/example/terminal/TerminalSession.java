package com.example.terminal;

import java.io.InputStream;
import java.io.OutputStream;

public interface TerminalSession {

    interface OutputListener {
        void onOutput(String text);
    }

    void write(String data);
    void write(byte[] data);
    void resize(int rows, int cols);
    void stop();
    boolean isBusy();
    void setOutputListener(OutputListener listener);
    void execute(String command, String workingDir, Runnable onComplete);
    InputStream getInputStream();
    OutputStream getOutputStream();
    String getInitError();
}
