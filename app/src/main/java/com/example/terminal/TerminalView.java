package com.example.terminal;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TerminalView extends ScrollView {

    private TextView textView;
    private TerminalSession session;
    private final SpannableStringBuilder buffer = new SpannableStringBuilder();
    private static final int MAX_BUFFER_LENGTH = 30000;

    public TerminalView(Context context) {
        super(context);
        init();
    }

    public TerminalView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TerminalView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textView = new TextView(getContext());
        textView.setBackgroundColor(Color.parseColor("#121212"));
        textView.setTextColor(Color.parseColor("#CCCCCC"));
        textView.setTypeface(Typeface.MONOSPACE);
        textView.setTextSize(12);
        textView.setPadding(24, 24, 24, 24);
        textView.setTextIsSelectable(true);
        addView(textView);
        setBackgroundColor(Color.parseColor("#121212"));
        setVerticalScrollBarEnabled(true);
    }

    public void attachSession(TerminalSession session) {
        this.session = session;
        if (session != null) {
            session.setOutputListener(text -> post(() -> appendOutput(text)));
        }
    }

    private void startReading() {
        if (session == null) return;
        InputStream is = session.getInputStream();
        if (is == null) return;

        new Thread(() -> {
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                char[] cbuf = new char[2048];
                int read;
                while ((read = reader.read(cbuf)) != -1) {
                    final String chunk = new String(cbuf, 0, read);
                    post(() -> appendOutput(chunk));
                }
            } catch (Exception e) {
                post(() -> appendOutput("\n\u001B[31m[Session Closed]\u001B[0m\n"));
            }
        }).start();
    }

    public void appendOutput(String text) {
        CharSequence formatted = AnsiParser.parse(text);
        buffer.append(formatted);
        if (buffer.length() > MAX_BUFFER_LENGTH) {
            buffer.delete(0, buffer.length() - MAX_BUFFER_LENGTH);
        }
        textView.setText(buffer);
        post(() -> fullScroll(FOCUS_DOWN));
    }

    public void input(String text) {
        if (session != null) {
            session.write(text);
        }
    }

    public void clear() {
        buffer.clear();
        textView.setText("");
    }

    public TerminalSession getSession() {
        return session;
    }
}
