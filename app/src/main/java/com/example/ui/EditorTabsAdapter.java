package com.example.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.R;

import java.util.ArrayList;
import java.util.List;

public class EditorTabsAdapter extends RecyclerView.Adapter<EditorTabsAdapter.TabViewHolder> {

    public interface OnTabActionListener {
        void onTabClick(FSNode node);
        void onTabClose(FSNode node, int position);
    }

    private final List<FSNode> openTabs = new ArrayList<>();
    private FSNode activeNode = null;
    private final OnTabActionListener listener;

    public EditorTabsAdapter(OnTabActionListener listener) {
        this.listener = listener;
    }

    public void openTab(FSNode node) {
        if (node == null || node.isDirectory()) return;

        int existingIndex = -1;
        for (int i = 0; i < openTabs.size(); i++) {
            if (openTabs.get(i).getKey().equals(node.getKey())) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex == -1) {
            openTabs.add(node);
            activeNode = node;
            notifyDataSetChanged();
        } else {
            activeNode = openTabs.get(existingIndex);
            notifyDataSetChanged();
        }
    }

    public void setActiveNode(FSNode node) {
        this.activeNode = node;
        notifyDataSetChanged();
    }

    public FSNode getActiveNode() {
        return activeNode;
    }

    public List<FSNode> getOpenTabs() {
        return openTabs;
    }

    public FSNode closeTab(FSNode node) {
        int index = -1;
        for (int i = 0; i < openTabs.size(); i++) {
            if (openTabs.get(i).getKey().equals(node.getKey())) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            openTabs.remove(index);
            if (activeNode != null && activeNode.getKey().equals(node.getKey())) {
                if (!openTabs.isEmpty()) {
                    int nextIndex = Math.min(index, openTabs.size() - 1);
                    activeNode = openTabs.get(nextIndex);
                } else {
                    activeNode = null;
                }
            }
            notifyDataSetChanged();
            return activeNode;
        }
        return activeNode;
    }

    public void clearTabs() {
        openTabs.clear();
        activeNode = null;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_editor_tab, parent, false);
        return new TabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        FSNode node = openTabs.get(position);
        boolean isActive = activeNode != null && activeNode.getKey().equals(node.getKey());
        holder.bind(node, isActive, listener);
    }

    @Override
    public int getItemCount() {
        return openTabs.size();
    }

    static class TabViewHolder extends RecyclerView.ViewHolder {
        final View root;
        final ImageView ivLogo;
        final TextView tvTitle;
        final ImageView btnClose;

        public TabViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.layout_tab_root);
            ivLogo = itemView.findViewById(R.id.iv_tab_logo);
            tvTitle = itemView.findViewById(R.id.tv_tab_title);
            btnClose = itemView.findViewById(R.id.btn_tab_close);
        }

        public void bind(FSNode node, boolean isActive, OnTabActionListener listener) {
            tvTitle.setText(node.getName());

            // Set official vector logo (HTML5, JS, CSS3, Python, Java, etc.)
            int logoRes = LanguageBadgeHelper.getLogoDrawable(node.getName());
            ivLogo.setImageResource(logoRes);

            // Visual Styling: Active vs Inactive Tab
            if (isActive) {
                root.setBackgroundColor(Color.parseColor("#1E1E1E")); // Dark Active Tab
                tvTitle.setTextColor(Color.parseColor("#FFFFFF"));
                tvTitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                btnClose.setColorFilter(Color.parseColor("#CCCCCC"));
            } else {
                root.setBackgroundColor(Color.parseColor("#141414")); // Darker Inactive Tab
                tvTitle.setTextColor(Color.parseColor("#888888"));
                tvTitle.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
                btnClose.setColorFilter(Color.parseColor("#555555"));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onTabClick(node);
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onTabClose(node, getAdapterPosition());
                return true;
            });

            btnClose.setOnClickListener(v -> {
                if (listener != null) listener.onTabClose(node, getAdapterPosition());
            });
        }
    }
}
