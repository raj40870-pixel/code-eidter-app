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
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
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

    // Horizontal Scrolling & Dragging
    private int touchSlop;
    private float downX;
    private float downY;
    private float lastDragX;
    private float lastDragY;
    private boolean isHorizontalDragging = false;
    private boolean isVerticalDragging = false;
    private int maxContentWidth = 0;
    private boolean isHorizontallyScrollingEnabled = true;

    // Pinch-to-zoom
    private ScaleGestureDetector scaleGestureDetector;
    private float currentFontSizeSp = 14.5f;
    private static final float MIN_FONT_SIZE_SP = 9.0f;
    private static final float MAX_FONT_SIZE_SP = 40.0f;
    private OnFontSizeChangeListener onFontSizeChangeListener;

    public interface OnFontSizeChangeListener {
        void onFontSizeChanged(float newSizeSp);
    }

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
        setFontSizeSp(14.5f);

        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        setHorizontallyScrolling(true);

        lineNumberPaint.setColor(Color.parseColor("#666666"));
        lineNumberPaint.setTextSize(getTextSize() * 0.85f);
        lineNumberPaint.setTypeface(Typeface.MONOSPACE);
        lineNumberPaint.setTextAlign(Paint.Align.RIGHT);

        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private float initialSpan;
            private float initialSize;

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                initialSpan = detector.getCurrentSpan();
                initialSize = currentFontSizeSp;
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float span = detector.getCurrentSpan();
                if (initialSpan > 0) {
                    float ratio = span / initialSpan;
                    float targetSize = initialSize * ratio;
                    if (targetSize < MIN_FONT_SIZE_SP) targetSize = MIN_FONT_SIZE_SP;
                    if (targetSize > MAX_FONT_SIZE_SP) targetSize = MAX_FONT_SIZE_SP;
                    if (Math.abs(targetSize - currentFontSizeSp) >= 0.25f) {
                        setFontSizeSp(targetSize);
                    }
                }
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                if (onFontSizeChangeListener != null) {
                    onFontSizeChangeListener.onFontSizeChanged(currentFontSizeSp);
                }
            }
        });

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
        int scrollX = getScrollX();
        int scrollY = getScrollY();
        int viewHeight = getHeight();
        int viewWidth = getWidth();

        // 1. Clip and draw editor text to the right of the gutter
        canvas.save();
        canvas.clipRect(scrollX + gutterWidth, scrollY, scrollX + viewWidth, scrollY + viewHeight);
        super.onDraw(canvas);
        canvas.restore();

        // 2. Draw seamless gutter background
        Layout layout = getLayout();
        if (layout != null) {
            canvas.drawRect(scrollX, scrollY, scrollX + gutterWidth, scrollY + viewHeight, gutterBgPaint);

            // 3. Draw subtle vertical divider
            canvas.drawLine(scrollX + gutterWidth, scrollY, scrollX + gutterWidth, scrollY + viewHeight, dividerPaint);

            // 4. Draw line numbers pinned to gutter
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

    public void setOnFontSizeChangeListener(OnFontSizeChangeListener listener) {
        this.onFontSizeChangeListener = listener;
    }

    public void setFontSizeSp(float sizeSp) {
        if (sizeSp < MIN_FONT_SIZE_SP) sizeSp = MIN_FONT_SIZE_SP;
        if (sizeSp > MAX_FONT_SIZE_SP) sizeSp = MAX_FONT_SIZE_SP;
        this.currentFontSizeSp = sizeSp;
        setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSizeSp);
    }

    public float getFontSizeSp() {
        return currentFontSizeSp;
    }

    @Override
    public void setTextSize(int unit, float size) {
        super.setTextSize(unit, size);
        if (lineNumberPaint != null) {
            lineNumberPaint.setTextSize(getTextSize() * 0.85f);
            updateGutterWidth();
            invalidate();
        }
    }

    @Override
    public void setTypeface(@Nullable Typeface tf) {
        super.setTypeface(tf);
        if (lineNumberPaint != null && tf != null) {
            lineNumberPaint.setTypeface(tf);
            updateGutterWidth();
            invalidate();
        }
    }

    @Override
    public void setTypeface(@Nullable Typeface tf, int style) {
        super.setTypeface(tf, style);
        if (lineNumberPaint != null && tf != null) {
            lineNumberPaint.setTypeface(tf);
            updateGutterWidth();
            invalidate();
        }
    }

    @Override
    public void setHorizontallyScrolling(boolean whether) {
        super.setHorizontallyScrolling(whether);
        this.isHorizontallyScrollingEnabled = whether;
        if (!whether) {
            scrollTo(0, getScrollY());
        }
    }

    public boolean isHorizontallyScrollingEnabled() {
        return isHorizontallyScrollingEnabled;
    }

    private void updateMaxContentWidth() {
        Layout layout = getLayout();
        if (layout == null) {
            maxContentWidth = 0;
            return;
        }
        float maxWidth = 0;
        int count = layout.getLineCount();
        for (int i = 0; i < count; i++) {
            float w = layout.getLineWidth(i);
            if (w > maxWidth) {
                maxWidth = w;
            }
        }
        maxContentWidth = (int) Math.ceil(maxWidth);
    }

    private int computeMaxScrollX() {
        int viewWidth = getWidth();
        if (viewWidth <= 0) return 0;
        int totalWidth = getCompoundPaddingLeft() + maxContentWidth + getCompoundPaddingRight();
        if (totalWidth <= viewWidth) return 0;
        return (totalWidth - viewWidth) + (int) (60 * density);
    }

    @Override
    protected int computeHorizontalScrollRange() {
        return Math.max(getWidth(), getCompoundPaddingLeft() + maxContentWidth + getCompoundPaddingRight());
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return getScrollX();
    }

    @Override
    protected int computeHorizontalScrollExtent() {
        return getWidth();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (scaleGestureDetector != null) {
            scaleGestureDetector.onTouchEvent(event);
        }

        if (event.getPointerCount() > 1) {
            isHorizontalDragging = false;
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            return true;
        }

        if (!isHorizontallyScrollingEnabled) {
            return super.onTouchEvent(event);
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                lastDragX = downX;
                lastDragY = downY;
                isHorizontalDragging = false;
                isVerticalDragging = false;
                updateMaxContentWidth();
                super.onTouchEvent(event);
                return true;

            case MotionEvent.ACTION_MOVE:
                float currentX = event.getX();
                float currentY = event.getY();
                float deltaX = currentX - downX;
                float deltaY = currentY - downY;

                if (!isHorizontalDragging && !isVerticalDragging) {
                    if (Math.abs(deltaX) > touchSlop && Math.abs(deltaX) > Math.abs(deltaY) * 1.1f) {
                        isHorizontalDragging = true;
                        if (getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                        }
                    } else if (Math.abs(deltaY) > touchSlop) {
                        isVerticalDragging = true;
                    }
                }

                if (isHorizontalDragging) {
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    float dx = lastDragX - currentX;
                    int maxScrollX = computeMaxScrollX();
                    int newScrollX = Math.max(0, Math.min(maxScrollX, getScrollX() + (int) dx));
                    scrollTo(newScrollX, getScrollY());
                    lastDragX = currentX;
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isHorizontalDragging) {
                    isHorizontalDragging = false;
                    return true;
                }
                break;
        }

        return super.onTouchEvent(event);
    }
}
