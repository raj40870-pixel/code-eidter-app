package com.example.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

import java.util.LinkedList;

public class LineNumberEditText extends AppCompatEditText {

    private final Paint lineNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gutterBgPaint = new Paint();
    private final Paint dividerPaint = new Paint();
    private final Rect rect = new Rect();

    private int gutterWidth;
    private int digitCount = 2;
    private final float density;

    // Undo / Redo history
    private final LinkedList<String> undoStack = new LinkedList<>();
    private final LinkedList<String> redoStack = new LinkedList<>();
    private boolean isUndoRedoOperation = false;
    private static final int MAX_HISTORY = 50;

    public LineNumberEditText(@NonNull Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        init();
    }

    public LineNumberEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        init();
    }

    public LineNumberEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = context.getResources().getDisplayMetrics().density;
        init();
    }

    private void init() {
        setTypeface(Typeface.MONOSPACE);
        setBackgroundColor(Color.parseColor("#181818"));
        setTextColor(Color.parseColor("#E6E6E6"));
        setTextSize(14.5f);

        lineNumberPaint.setColor(Color.parseColor("#666666"));
        lineNumberPaint.setTextSize(getTextSize() * 0.85f);
        lineNumberPaint.setTypeface(Typeface.MONOSPACE);
        lineNumberPaint.setTextAlign(Paint.Align.RIGHT);

        // Blends seamlessly like the reference screenshot
        gutterBgPaint.setColor(Color.parseColor("#181818"));
        dividerPaint.setColor(Color.parseColor("#262626"));
        dividerPaint.setStrokeWidth(1.2f * density);

        gutterWidth = (int) (38 * density);
        applyEditorPadding();

        addTextChangedListener(new TextWatcher() {
            private String beforeText;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isUndoRedoOperation) {
                    beforeText = s.toString();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isUndoRedoOperation && beforeText != null) {
                    undoStack.addLast(beforeText);
                    if (undoStack.size() > MAX_HISTORY) {
                        undoStack.removeFirst();
                    }
                    redoStack.clear();
                }
                updateGutterWidth();
            }
        });
    }

    private void applyEditorPadding() {
        int leftPad = gutterWidth + (int) (14 * density);
        int topPad = (int) (12 * density);
        int rightPad = (int) (16 * density);
        int botPad = (int) (16 * density);
        setPadding(leftPad, topPad, rightPad, botPad);
    }

    private void updateGutterWidth() {
        int lines = Math.max(1, getLineCount());
        int digits = String.valueOf(lines).length();
        if (digits < 2) digits = 2;

        float charWidth = lineNumberPaint.measureText("9");
        int newGutterWidth = (int) (charWidth * digits + (18 * density));
        if (newGutterWidth < (int) (38 * density)) {
            newGutterWidth = (int) (38 * density);
        }

        if (newGutterWidth != gutterWidth) {
            gutterWidth = newGutterWidth;
            digitCount = digits;
            applyEditorPadding();
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Layout layout = getLayout();
        if (layout != null) {
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            int viewHeight = getHeight();

            // 1. Draw seamless gutter background
            canvas.drawRect(scrollX, scrollY, scrollX + gutterWidth, scrollY + viewHeight, gutterBgPaint);

            // 2. Draw subtle vertical divider
            canvas.drawLine(scrollX + gutterWidth, scrollY, scrollX + gutterWidth, scrollY + viewHeight, dividerPaint);

            // 3. Draw line numbers right aligned
            canvas.getClipBounds(rect);
            int lineCount = getLineCount();
            int firstLine = layout.getLineForVertical(rect.top);
            int lastLine = layout.getLineForVertical(rect.bottom);

            CharSequence text = getText();
            int logicalLineNumber = 1;

            if (text != null) {
                int firstLineStart = layout.getLineStart(firstLine);
                for (int i = 0; i < firstLineStart && i < text.length(); i++) {
                    if (text.charAt(i) == '\n') {
                        logicalLineNumber++;
                    }
                }
            }

            float xPos = scrollX + gutterWidth - (8 * density);

            for (int i = firstLine; i <= lastLine && i < lineCount; i++) {
                int lineStart = layout.getLineStart(i);
                boolean isLogicalLine = (i == 0 || (text != null && lineStart > 0 && text.charAt(lineStart - 1) == '\n'));

                if (isLogicalLine) {
                    int baseline = getLineBounds(i, null);
                    canvas.drawText(String.valueOf(logicalLineNumber), xPos, baseline, lineNumberPaint);
                    logicalLineNumber++;
                }
            }
        }
        super.onDraw(canvas);
    }

    public void insertSymbol(String symbol) {
        int start = Math.max(0, getSelectionStart());
        int end = Math.max(0, getSelectionEnd());
        Editable editable = getText();
        if (editable == null) return;

        if (symbol.equals("Tab")) {
            editable.replace(start, end, "    ");
            setSelection(start + 4);
            return;
        }

        // Pair insertion or wrapping
        if (start != end) {
            String selected = editable.subSequence(start, end).toString();
            if (symbol.equals("{}") || symbol.equals("{")) {
                editable.replace(start, end, "{\n    " + selected + "\n}");
            } else if (symbol.equals("()") || symbol.equals("(")) {
                editable.replace(start, end, "(" + selected + ")");
            } else if (symbol.equals("[]") || symbol.equals("[")) {
                editable.replace(start, end, "[" + selected + "]");
            } else if (symbol.equals("\"\"") || symbol.equals("\"")) {
                editable.replace(start, end, "\"" + selected + "\"");
            } else if (symbol.equals("''") || symbol.equals("'")) {
                editable.replace(start, end, "'" + selected + "'");
            } else {
                editable.replace(start, end, symbol);
            }
        } else {
            if (symbol.equals("{}")) {
                editable.insert(start, "{}");
                setSelection(start + 1);
            } else if (symbol.equals("()")) {
                editable.insert(start, "()");
                setSelection(start + 1);
            } else if (symbol.equals("[]")) {
                editable.insert(start, "[]");
                setSelection(start + 1);
            } else if (symbol.equals("\"\"")) {
                editable.insert(start, "\"\"");
                setSelection(start + 1);
            } else if (symbol.equals("''")) {
                editable.insert(start, "''");
                setSelection(start + 1);
            } else {
                editable.insert(start, symbol);
                setSelection(start + symbol.length());
            }
        }
    }

    public void completeKeyword(String keyword) {
        int cursor = Math.max(0, getSelectionStart());
        Editable editable = getText();
        if (editable == null) return;

        // Find the start of the current word being typed before cursor
        int wordStart = cursor;
        while (wordStart > 0) {
            char c = editable.charAt(wordStart - 1);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '#' || c == '!') {
                wordStart--;
            } else {
                break;
            }
        }

        String toInsert = keyword;
        if (keyword.equals("include") && wordStart > 0 && editable.charAt(wordStart - 1) == '#') {
            toInsert = "include ";
        } else if (keyword.equals("include") && (wordStart == cursor || !editable.subSequence(wordStart, cursor).toString().startsWith("#"))) {
            toInsert = "#include ";
        } else {
            toInsert = keyword + " ";
        }

        editable.replace(wordStart, cursor, toInsert);
        setSelection(wordStart + toInsert.length());
    }

    public String getCurrentWordPrefix() {
        int cursor = Math.max(0, getSelectionStart());
        Editable editable = getText();
        if (editable == null || cursor == 0) return "";

        int wordStart = cursor;
        while (wordStart > 0) {
            char c = editable.charAt(wordStart - 1);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '#' || c == '!') {
                wordStart--;
            } else {
                break;
            }
        }
        return editable.subSequence(wordStart, cursor).toString();
    }

    public void insertKeyword(String keyword) {
        completeKeyword(keyword);
    }

    public void pasteText(String text) {
        if (text == null || text.isEmpty()) return;
        int start = Math.max(0, getSelectionStart());
        int end = Math.max(0, getSelectionEnd());
        Editable editable = getText();
        if (editable == null) return;
        editable.replace(start, end, text);
        setSelection(start + text.length());
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        if (canUndo()) {
            isUndoRedoOperation = true;
            String current = getText() != null ? getText().toString() : "";
            redoStack.addLast(current);
            String previous = undoStack.removeLast();
            setText(previous);
            setSelection(Math.min(previous.length(), getSelectionStart()));
            isUndoRedoOperation = false;
        }
    }

    public void redo() {
        if (canRedo()) {
            isUndoRedoOperation = true;
            String current = getText() != null ? getText().toString() : "";
            undoStack.addLast(current);
            String next = redoStack.removeLast();
            setText(next);
            setSelection(Math.min(next.length(), getSelectionStart()));
            isUndoRedoOperation = false;
        }
    }
}
