package com.example.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Layout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;

import java.util.ArrayList;
import java.util.List;

public class CodeAutoCompleteManager {

    public static final int KIND_METHOD = 1;
    public static final int KIND_KEYWORD = 2;
    public static final int KIND_TYPE = 3;
    public static final int KIND_VARIABLE = 4;
    public static final int KIND_SNIPPET = 5;

    public static class SuggestionItem {
        public final String name;
        public final String insertText;
        public final String detail;
        public final int kind;

        public SuggestionItem(String name, String insertText, String detail, int kind) {
            this.name = name;
            this.insertText = insertText;
            this.detail = detail;
            this.kind = kind;
        }
    }

    public interface OnSuggestionSelectedListener {
        void onSuggestionSelected(String insertText);
    }

    private final LineNumberEditText editText;
    private final PopupWindow popupWindow;
    private final VSCodeSuggestionAdapter adapter;
    private final float density;
    private final OnSuggestionSelectedListener listener;

    public CodeAutoCompleteManager(LineNumberEditText editText, OnSuggestionSelectedListener listener) {
        this.editText = editText;
        this.listener = listener;
        Context context = editText.getContext();
        this.density = context.getResources().getDisplayMetrics().density;

        View contentView = LayoutInflater.from(context).inflate(R.layout.popup_autocomplete, null);
        RecyclerView rv = contentView.findViewById(R.id.rv_suggestions);
        rv.setLayoutManager(new LinearLayoutManager(context));

        adapter = new VSCodeSuggestionAdapter(item -> {
            if (this.listener != null) {
                this.listener.onSuggestionSelected(item.insertText);
            }
            dismiss();
        });
        rv.setAdapter(adapter);

        popupWindow = new PopupWindow(contentView, (int) (260 * density), ViewGroup.LayoutParams.WRAP_CONTENT, false);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
    }

    public void update(String prefix, String language) {
        if (prefix == null || prefix.trim().isEmpty()) {
            dismiss();
            return;
        }

        List<SuggestionItem> matches = getMatches(prefix.trim(), language);
        if (matches.isEmpty() || (matches.size() == 1 && matches.get(0).name.equalsIgnoreCase(prefix.trim()))) {
            dismiss();
            return;
        }

        if (matches.size() > 15) {
            matches = matches.subList(0, 15);
        }

        adapter.setItems(matches);

        Layout layout = editText.getLayout();
        int cursor = editText.getSelectionStart();
        if (layout == null || cursor < 0) {
            dismiss();
            return;
        }

        int line = layout.getLineForOffset(cursor);
        int x = (int) layout.getPrimaryHorizontal(cursor) + editText.getPaddingLeft() - editText.getScrollX();
        int y = layout.getLineBottom(line) + editText.getPaddingTop() - editText.getScrollY();

        int[] location = new int[2];
        editText.getLocationOnScreen(location);
        int screenX = location[0] + x;
        int screenY = location[1] + y + (int) (4 * density);

        int screenWidth = editText.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = editText.getResources().getDisplayMetrics().heightPixels;
        int popupWidth = (int) (260 * density);

        if (screenX + popupWidth > screenWidth - (int) (10 * density)) {
            screenX = screenWidth - popupWidth - (int) (10 * density);
        }
        if (screenX < (int) (10 * density)) {
            screenX = (int) (10 * density);
        }

        int maxPopupHeight = (int) (180 * density);
        if (screenY + maxPopupHeight > screenHeight - (int) (80 * density)) {
            int lineTop = layout.getLineTop(line) + editText.getPaddingTop() - editText.getScrollY();
            screenY = location[1] + lineTop - maxPopupHeight;
            if (screenY < 0) screenY = (int) (10 * density);
        }

        if (popupWindow.isShowing()) {
            popupWindow.update(screenX, screenY, popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        } else {
            popupWindow.setWidth(popupWidth);
            popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            popupWindow.showAtLocation(editText, Gravity.NO_GRAVITY, screenX, screenY);
        }
    }

    public void dismiss() {
        if (popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    public boolean isShowing() {
        return popupWindow.isShowing();
    }

    private List<SuggestionItem> getMatches(String prefix, String language) {
        List<SuggestionItem> allItems = getSuggestionsForLanguage(language);
        String cleanPrefix = prefix.toLowerCase().replace("#", "");
        List<SuggestionItem> list = new ArrayList<>();

        for (SuggestionItem item : allItems) {
            String lowerName = item.name.toLowerCase().replace("#", "");
            if (lowerName.startsWith(cleanPrefix) && !item.name.equalsIgnoreCase(prefix)) {
                list.add(item);
            }
        }
        return list;
    }

    private List<SuggestionItem> getSuggestionsForLanguage(String language) {
        List<SuggestionItem> items = new ArrayList<>();
        if (language == null) language = "C++";
        language = language.toLowerCase();

        if (language.contains("python") || language.endsWith(".py")) {
            items.add(new SuggestionItem("print", "print", "(*values) -> None", KIND_METHOD));
            items.add(new SuggestionItem("def", "def", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("class", "class", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("import", "import", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("from", "from", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("return", "return", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("if", "if", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("elif", "elif", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("else", "else", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("for", "for", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("while", "while", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("in", "in", "operator", KIND_KEYWORD));
            items.add(new SuggestionItem("try", "try", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("except", "except", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("finally", "finally", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("raise", "raise", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("with", "with", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("as", "as", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("lambda", "lambda", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("pass", "pass", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("break", "break", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("continue", "continue", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("True", "True", "bool", KIND_KEYWORD));
            items.add(new SuggestionItem("False", "False", "bool", KIND_KEYWORD));
            items.add(new SuggestionItem("None", "None", "NoneType", KIND_KEYWORD));
            items.add(new SuggestionItem("self", "self", "parameter", KIND_VARIABLE));
            items.add(new SuggestionItem("range", "range", "(stop) -> range", KIND_METHOD));
            items.add(new SuggestionItem("len", "len", "(obj) -> int", KIND_METHOD));
            items.add(new SuggestionItem("input", "input", "(prompt) -> str", KIND_METHOD));
            items.add(new SuggestionItem("str", "str", "class", KIND_TYPE));
            items.add(new SuggestionItem("int", "int", "class", KIND_TYPE));
            items.add(new SuggestionItem("list", "list", "class", KIND_TYPE));
            items.add(new SuggestionItem("dict", "dict", "class", KIND_TYPE));
            items.add(new SuggestionItem("append", "append", "list.append(item)", KIND_METHOD));
            items.add(new SuggestionItem("pop", "pop", "list.pop([index])", KIND_METHOD));
        } else if (language.contains("rust") || language.endsWith(".rs")) {
            items.add(new SuggestionItem("println!", "println!", "macro", KIND_METHOD));
            items.add(new SuggestionItem("print!", "print!", "macro", KIND_METHOD));
            items.add(new SuggestionItem("eprintln!", "eprintln!", "macro", KIND_METHOD));
            items.add(new SuggestionItem("fn", "fn", "keyword", KIND_KEYWORD));
            items.add(new SuggestionItem("let", "let", "keyword", KIND_KEYWORD));
            items.add(new SuggestionItem("mut", "mut", "keyword", KIND_KEYWORD));
            items.add(new SuggestionItem("struct", "struct", "type", KIND_TYPE));
            items.add(new SuggestionItem("enum", "enum", "type", KIND_TYPE));
            items.add(new SuggestionItem("use", "use", "keyword", KIND_KEYWORD));
            items.add(new SuggestionItem("pub", "pub", "visibility", KIND_KEYWORD));
            items.add(new SuggestionItem("mod", "mod", "keyword", KIND_KEYWORD));
            items.add(new SuggestionItem("trait", "trait", "keyword", KIND_KEYWORD));
            items.add(new SuggestionItem("impl", "impl", "keyword", KIND_KEYWORD));
            items.add(new SuggestionItem("match", "match", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("push", "push", "Vec::push(&mut self, val)", KIND_METHOD));
            items.add(new SuggestionItem("pop", "pop", "Vec::pop(&mut self)", KIND_METHOD));
            items.add(new SuggestionItem("panic!", "panic!", "macro", KIND_METHOD));
            items.add(new SuggestionItem("unwrap", "unwrap", "Result::unwrap(self)", KIND_METHOD));
            items.add(new SuggestionItem("String", "String", "struct", KIND_TYPE));
            items.add(new SuggestionItem("Vec", "Vec", "struct", KIND_TYPE));
            items.add(new SuggestionItem("Option", "Option", "enum", KIND_TYPE));
            items.add(new SuggestionItem("Result", "Result", "enum", KIND_TYPE));
            items.add(new SuggestionItem("Some", "Some", "Option::Some", KIND_KEYWORD));
            items.add(new SuggestionItem("None", "None", "Option::None", KIND_KEYWORD));
            items.add(new SuggestionItem("Ok", "Ok", "Result::Ok", KIND_KEYWORD));
            items.add(new SuggestionItem("Err", "Err", "Result::Err", KIND_KEYWORD));
        } else if (language.contains("js") || language.contains("javascript") || language.endsWith(".js") || language.endsWith(".ts")) {
            items.add(new SuggestionItem("console.log", "console.log", "(...data: any[]): void", KIND_METHOD));
            items.add(new SuggestionItem("function", "function", "keyword", KIND_KEYWORD));
            items.add(new SuggestionItem("const", "const", "keyword", KIND_KEYWORD));
            items.add(new SuggestionItem("let", "let", "keyword", KIND_KEYWORD));
            items.add(new SuggestionItem("var", "var", "keyword", KIND_KEYWORD));
            items.add(new SuggestionItem("return", "return", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("push", "push", "(...items): number", KIND_METHOD));
            items.add(new SuggestionItem("pop", "pop", "(): any", KIND_METHOD));
            items.add(new SuggestionItem("map", "map", "(callbackfn): any[]", KIND_METHOD));
            items.add(new SuggestionItem("filter", "filter", "(predicate): any[]", KIND_METHOD));
            items.add(new SuggestionItem("forEach", "forEach", "(callbackfn): void", KIND_METHOD));
            items.add(new SuggestionItem("Promise", "Promise", "class", KIND_TYPE));
            items.add(new SuggestionItem("async", "async", "modifier", KIND_KEYWORD));
            items.add(new SuggestionItem("await", "await", "expression", KIND_KEYWORD));
            items.add(new SuggestionItem("require", "require", "(id: string): any", KIND_METHOD));
            items.add(new SuggestionItem("module.exports", "module.exports", "object", KIND_VARIABLE));
        } else {
            // C / C++ Default
            items.add(new SuggestionItem("printf", "printf", "int printf(const char *format, ...)", KIND_METHOD));
            items.add(new SuggestionItem("scanf", "scanf", "int scanf(const char *format, ...)", KIND_METHOD));
            items.add(new SuggestionItem("push_back", "push_back", "void vector::push_back(const T&)", KIND_METHOD));
            items.add(new SuggestionItem("pop_back", "pop_back", "void vector::pop_back()", KIND_METHOD));
            items.add(new SuggestionItem("pair", "pair", "struct std::pair<T1, T2>", KIND_TYPE));
            items.add(new SuggestionItem("pow", "pow", "double pow(double, double)", KIND_METHOD));
            items.add(new SuggestionItem("puts", "puts", "int puts(const char*)", KIND_METHOD));
            items.add(new SuggestionItem("public", "public:", "access specifier", KIND_KEYWORD));
            items.add(new SuggestionItem("private", "private:", "access specifier", KIND_KEYWORD));
            items.add(new SuggestionItem("protected", "protected:", "access specifier", KIND_KEYWORD));
            items.add(new SuggestionItem("cout", "cout", "std::ostream (iostream)", KIND_VARIABLE));
            items.add(new SuggestionItem("cin", "cin", "std::istream (iostream)", KIND_VARIABLE));
            items.add(new SuggestionItem("endl", "endl", "std::endl manipulator", KIND_METHOD));
            items.add(new SuggestionItem("include", "#include ", "preprocessor directive", KIND_KEYWORD));
            items.add(new SuggestionItem("int", "int", "primitive type", KIND_TYPE));
            items.add(new SuggestionItem("char", "char", "primitive type", KIND_TYPE));
            items.add(new SuggestionItem("float", "float", "primitive type", KIND_TYPE));
            items.add(new SuggestionItem("double", "double", "primitive type", KIND_TYPE));
            items.add(new SuggestionItem("bool", "bool", "primitive type", KIND_TYPE));
            items.add(new SuggestionItem("void", "void", "primitive type", KIND_TYPE));
            items.add(new SuggestionItem("string", "string", "class std::string", KIND_TYPE));
            items.add(new SuggestionItem("vector", "vector", "class std::vector<T>", KIND_TYPE));
            items.add(new SuggestionItem("map", "map", "class std::map<K, V>", KIND_TYPE));
            items.add(new SuggestionItem("set", "set", "class std::set<T>", KIND_TYPE));
            items.add(new SuggestionItem("return", "return", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("if", "if", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("else", "else", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("for", "for", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("while", "while", "statement", KIND_KEYWORD));
            items.add(new SuggestionItem("class", "class", "keyword", KIND_KEYWORD));
            items.add(new SuggestionItem("struct", "struct", "keyword", KIND_KEYWORD));
            items.add(new SuggestionItem("nullptr", "nullptr", "pointer literal", KIND_KEYWORD));
            items.add(new SuggestionItem("malloc", "malloc", "void* malloc(size_t)", KIND_METHOD));
            items.add(new SuggestionItem("free", "free", "void free(void*)", KIND_METHOD));
            items.add(new SuggestionItem("strlen", "strlen", "size_t strlen(const char*)", KIND_METHOD));
            items.add(new SuggestionItem("sort", "sort", "std::sort(first, last)", KIND_METHOD));
            items.add(new SuggestionItem("size", "size", "size_type size() const", KIND_METHOD));
            items.add(new SuggestionItem("empty", "empty", "bool empty() const", KIND_METHOD));
            items.add(new SuggestionItem("clear", "clear", "void clear()", KIND_METHOD));
        }

        return items;
    }

    private static class VSCodeSuggestionAdapter extends RecyclerView.Adapter<VSCodeSuggestionAdapter.ViewHolder> {
        private final List<SuggestionItem> items = new ArrayList<>();
        private final OnItemClickListener clickListener;

        interface OnItemClickListener {
            void onItemClick(SuggestionItem item);
        }

        public VSCodeSuggestionAdapter(OnItemClickListener clickListener) {
            this.clickListener = clickListener;
        }

        public void setItems(List<SuggestionItem> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_suggestion, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SuggestionItem item = items.get(position);
            holder.tvText.setText(item.name);
            holder.tvDetail.setText(item.detail);

            // VS Code Symbol Icons & Theme Colors
            switch (item.kind) {
                case KIND_METHOD:
                    holder.tvBadge.setText("⬡");
                    holder.tvBadge.setTextColor(Color.parseColor("#B180D7")); // Purple
                    break;
                case KIND_KEYWORD:
                    holder.tvBadge.setText("k");
                    holder.tvBadge.setTextColor(Color.parseColor("#75BEFF")); // Blue
                    break;
                case KIND_TYPE:
                    holder.tvBadge.setText("C");
                    holder.tvBadge.setTextColor(Color.parseColor("#EE9D28")); // Yellow/Orange
                    break;
                case KIND_VARIABLE:
                    holder.tvBadge.setText("x");
                    holder.tvBadge.setTextColor(Color.parseColor("#4EC9B0")); // Teal
                    break;
                case KIND_SNIPPET:
                default:
                    holder.tvBadge.setText("⧉");
                    holder.tvBadge.setTextColor(Color.parseColor("#89D185")); // Green
                    break;
            }

            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onItemClick(item);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvBadge;
            final TextView tvText;
            final TextView tvDetail;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvBadge = itemView.findViewById(R.id.tv_badge);
                tvText = itemView.findViewById(R.id.tv_suggestion_text);
                tvDetail = itemView.findViewById(R.id.tv_detail);
            }
        }
    }
}
