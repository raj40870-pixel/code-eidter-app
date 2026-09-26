package com.example.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileItemAdapter extends RecyclerView.Adapter<FileItemAdapter.FileViewHolder> {

    private final List<File> fileList = new ArrayList<>();
    private final OnFileActionListener listener;

    public interface OnFileActionListener {
        void onFileClick(File file);
        void onFileDelete(File file);
    }

    public FileItemAdapter(OnFileActionListener listener) {
        this.listener = listener;
    }

    public void setFiles(List<File> files) {
        fileList.clear();
        if (files != null) {
            fileList.addAll(files);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        File file = fileList.get(position);
        holder.bind(file, listener);
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvName;
        private final ImageView ivDelete;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_file_icon);
            tvName = itemView.findViewById(R.id.tv_file_name);
            ivDelete = itemView.findViewById(R.id.iv_file_delete);
        }

        public void bind(File file, OnFileActionListener listener) {
            String name = file.getName();
            tvName.setText(name);

            // Color code based on extension
            if (name.endsWith(".cpp") || name.endsWith(".cc") || name.endsWith(".cxx")) {
                ivIcon.setColorFilter(Color.parseColor("#009688")); // Teal
            } else if (name.endsWith(".c") || name.endsWith(".h")) {
                ivIcon.setColorFilter(Color.parseColor("#00BCD4")); // Cyan
            } else if (name.endsWith(".py")) {
                ivIcon.setColorFilter(Color.parseColor("#FFCA28")); // Yellow/Python
            } else if (name.endsWith(".java")) {
                ivIcon.setColorFilter(Color.parseColor("#FF7043")); // Orange/Java
            } else if (name.endsWith(".sh")) {
                ivIcon.setColorFilter(Color.parseColor("#66BB6A")); // Green/Shell
            } else {
                ivIcon.setColorFilter(Color.parseColor("#90CAF9")); // Light blue
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onFileClick(file);
            });

            ivDelete.setOnClickListener(v -> {
                if (listener != null) listener.onFileDelete(file);
            });
        }
    }
}
