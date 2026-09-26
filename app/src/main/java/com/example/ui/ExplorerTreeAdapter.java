package com.example.ui;

import android.content.Context;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExplorerTreeAdapter extends RecyclerView.Adapter<ExplorerTreeAdapter.NodeViewHolder> {

    public interface OnExplorerNodeListener {
        void onFileClick(FSNode node);
        void onFileLongClick(FSNode node);
        void onFileDelete(FSNode node);
        void onFolderClick(FSNode node, boolean isExpanded);
        void onFolderLongClick(FSNode node, View anchorView);
        void onFolderDelete(FSNode node);
    }

    public static class Node {
        public final FSNode fsNode;
        public final int depth;

        public Node(FSNode fsNode, int depth) {
            this.fsNode = fsNode;
            this.depth = depth;
        }
    }

    private final Context context;
    private FSNode rootNode;
    private final List<FSNode> externalRoots = new ArrayList<>();
    private final List<Node> visibleNodes = new ArrayList<>();
    private final Set<String> expandedKeys = new HashSet<>();
    private final OnExplorerNodeListener listener;
    private String currentSearchQuery = "";
    private String activeFileKey = null;

    public ExplorerTreeAdapter(Context context, OnExplorerNodeListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setActiveFileKey(String activeKey) {
        this.activeFileKey = activeKey;
        notifyDataSetChanged();
    }

    public void setRootDirectory(File rootDir) {
        if (rootDir != null) {
            this.rootNode = FSNode.fromFile(rootDir);
        } else {
            this.rootNode = null;
        }
        rebuildTree();
    }

    public void setRootFSNode(FSNode node) {
        this.rootNode = node;
        rebuildTree();
    }

    public void setSearchQuery(String query) {
        this.currentSearchQuery = query != null ? query.trim().toLowerCase() : "";
        rebuildTree();
    }

    public void toggleFolder(FSNode folder) {
        String key = folder.getKey();
        if (expandedKeys.contains(key)) {
            expandedKeys.remove(key);
        } else {
            expandedKeys.add(key);
        }
        rebuildTree();
    }

    public boolean isFolderExpanded(FSNode folder) {
        return expandedKeys.contains(folder.getKey());
    }

    public void addExternalFSNode(FSNode node) {
        if (node != null && node.isDirectory()) {
            if (!externalRoots.contains(node)) {
                externalRoots.add(node);
            }
            expandedKeys.add(node.getKey());
            rebuildTree();
        }
    }

    public void addExternalFolder(File folder) {
        if (folder != null) {
            addExternalFSNode(FSNode.fromFile(folder));
        }
    }

    public void removeExternalFSNode(FSNode node) {
        externalRoots.remove(node);
        expandedKeys.remove(node.getKey());
        rebuildTree();
    }

    public void clearExternalFolders() {
        externalRoots.clear();
        rebuildTree();
    }

    public void rebuildTree() {
        visibleNodes.clear();

        // 1. Local Project Root (e.g. CppStarter)
        if (rootNode != null) {
            List<FSNode> localChildren = rootNode.listChildren(context);
            buildSubTree(localChildren, 0);
        }

        // 2. External Folders (e.g. Flexbox from phone storage)
        for (FSNode extNode : externalRoots) {
            if (extNode != null) {
                boolean matchesSearch = currentSearchQuery.isEmpty() || extNode.getName().toLowerCase().contains(currentSearchQuery);
                if (matchesSearch || hasMatchingDescendant(extNode, currentSearchQuery)) {
                    visibleNodes.add(new Node(extNode, 0));
                    if (!currentSearchQuery.isEmpty() || expandedKeys.contains(extNode.getKey())) {
                        List<FSNode> extChildren = extNode.listChildren(context);
                        buildSubTree(extChildren, 1);
                    }
                }
            }
        }

        notifyDataSetChanged();
    }

    private void buildSubTree(List<FSNode> children, int depth) {
        if (children == null) return;

        for (FSNode child : children) {
            boolean matchesSearch = currentSearchQuery.isEmpty() || child.getName().toLowerCase().contains(currentSearchQuery);
            if (child.isDirectory()) {
                if (matchesSearch || hasMatchingDescendant(child, currentSearchQuery)) {
                    visibleNodes.add(new Node(child, depth));
                    if (!currentSearchQuery.isEmpty() || expandedKeys.contains(child.getKey())) {
                        List<FSNode> grandChildren = child.listChildren(context);
                        buildSubTree(grandChildren, depth + 1);
                    }
                }
            } else {
                if (matchesSearch) {
                    visibleNodes.add(new Node(child, depth));
                }
            }
        }
    }

    private boolean hasMatchingDescendant(FSNode dir, String query) {
        if (query.isEmpty()) return true;
        List<FSNode> children = dir.listChildren(context);
        for (FSNode c : children) {
            if (c.getName().toLowerCase().contains(query)) return true;
            if (c.isDirectory() && hasMatchingDescendant(c, query)) return true;
        }
        return false;
    }

    @NonNull
    @Override
    public NodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_explorer_node, parent, false);
        return new NodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NodeViewHolder holder, int position) {
        Node node = visibleNodes.get(position);
        boolean isActive = activeFileKey != null && activeFileKey.equals(node.fsNode.getKey());
        holder.bind(node, expandedKeys.contains(node.fsNode.getKey()), isActive, listener);
    }

    @Override
    public int getItemCount() {
        return visibleNodes.size();
    }

    static class NodeViewHolder extends RecyclerView.ViewHolder {
        final View indentView;
        final ImageView ivActiveIndicator;
        final ImageView ivChevron;
        final ImageView ivFolderIcon;
        final ImageView ivFileLogo;
        final TextView tvName;
        final ImageView ivDelete;

        public NodeViewHolder(@NonNull View itemView) {
            super(itemView);
            indentView = itemView.findViewById(R.id.view_indent);
            ivActiveIndicator = itemView.findViewById(R.id.iv_active_indicator);
            ivChevron = itemView.findViewById(R.id.iv_chevron);
            ivFolderIcon = itemView.findViewById(R.id.iv_folder_icon);
            ivFileLogo = itemView.findViewById(R.id.iv_file_logo);
            tvName = itemView.findViewById(R.id.tv_node_name);
            ivDelete = itemView.findViewById(R.id.iv_node_delete);
        }

        public void bind(Node node, boolean isExpanded, boolean isActive, OnExplorerNodeListener listener) {
            float density = itemView.getResources().getDisplayMetrics().density;

            // Set dynamic indentation
            ViewGroup.LayoutParams lp = indentView.getLayoutParams();
            lp.width = (int) (node.depth * 14 * density);
            indentView.setLayoutParams(lp);

            FSNode fsNode = node.fsNode;
            String name = fsNode.getName();
            tvName.setText(name);

            if (fsNode.isDirectory()) {
                // Folder Row: Keep folder appearance intact as user requested
                ivActiveIndicator.setVisibility(View.GONE);
                ivChevron.setVisibility(View.VISIBLE);
                ivChevron.setImageResource(isExpanded ? R.drawable.ic_expand_more : R.drawable.ic_chevron_right);
                ivFolderIcon.setVisibility(View.VISIBLE);
                ivFolderIcon.setImageResource(R.drawable.ic_folder);
                ivFolderIcon.setColorFilter(Color.parseColor("#E0A842")); // Folder Gold
                ivFileLogo.setVisibility(View.GONE);

                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onFolderClick(fsNode, !isExpanded);
                });

                itemView.setOnLongClickListener(v -> {
                    if (listener != null) listener.onFolderLongClick(fsNode, v);
                    return true;
                });

                ivDelete.setOnClickListener(v -> {
                    if (listener != null) listener.onFolderDelete(fsNode);
                });

            } else {
                // File Row: Show Active Indicator Arrow + Real Vector Logo + Red Trash Can Delete Button
                ivChevron.setVisibility(View.GONE);
                ivFolderIcon.setVisibility(View.GONE);
                ivFileLogo.setVisibility(View.VISIBLE);

                // Teal Active Indicator Arrow (▶) like Spck Editor
                if (isActive) {
                    ivActiveIndicator.setVisibility(View.VISIBLE);
                    tvName.setTextColor(Color.parseColor("#FFFFFF"));
                } else {
                    ivActiveIndicator.setVisibility(View.INVISIBLE);
                    tvName.setTextColor(Color.parseColor("#CCCCCC"));
                }

                // Official Real Language Vector Logo (HTML5 shield, JS square, CSS3, Python, Java, etc.)
                int logoRes = LanguageBadgeHelper.getLogoDrawable(name);
                ivFileLogo.setImageResource(logoRes);
                ivFileLogo.clearColorFilter(); // Show original vibrant logo colors

                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onFileClick(fsNode);
                });

                itemView.setOnLongClickListener(v -> {
                    if (listener != null) listener.onFileLongClick(fsNode);
                    return true;
                });

                ivDelete.setOnClickListener(v -> {
                    if (listener != null) listener.onFileDelete(fsNode);
                });
            }
        }
    }
}
