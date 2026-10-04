package com.example;

import android.Manifest;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.data.Project;
import com.example.databinding.ActivityMainBinding;
import com.example.runner.LanguageRunner;
import com.example.runner.RunManager;
import com.example.runner.TemplateManager;
import com.example.terminal.LocalTerminalSession;
import com.example.terminal.TermuxEnvironment;
import com.example.ui.CodeAutoCompleteManager;
import com.example.ui.EditorTabsAdapter;
import com.example.ui.ExplorerTreeAdapter;
import com.example.ui.FSNode;
import com.example.ui.LanguageBadgeHelper;
import com.example.ui.SyntaxHighlighter;
import com.example.viewmodel.ProjectViewModel;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private DrawerLayout drawerLayout;
    private ProjectViewModel projectViewModel;
    private ExplorerTreeAdapter explorerAdapter;
    private EditorTabsAdapter editorTabsAdapter;

    private Project currentProject;
    private File currentFile;
    private FSNode currentFSNode;
    private boolean isUpdatingText = false;
    private boolean isProjectInitialLoadDone = false;

    private LocalTerminalSession terminalSession;
    private TermuxEnvironment termuxEnv;
    private RunManager runManager;
    private CodeAutoCompleteManager autoCompleteManager;
    private SharedPreferences prefs;

    private ActivityResultLauncher<Intent> folderPickerLauncher;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = binding.drawerLayout;
        prefs = getSharedPreferences("code_editor_prefs", MODE_PRIVATE);
        projectViewModel = new ViewModelProvider(this).get(ProjectViewModel.class);
        termuxEnv = new TermuxEnvironment(this);
        runManager = new RunManager();

        initActivityLaunchers();

        setupTerminal();
        setupEditor();
        setupEditorTabs();
        setupDrawerAndExplorers();
        setupToolbarActions();
        setupTwoTierBottomBar();
        com.example.util.AppSecurity.checkTamperAndEnforce(this);
        checkStoragePermissions();
        com.example.util.BinaryCacheManager.cleanupExpiredBinaries(this);
        com.example.util.AppUpdateManager.checkForUpdates(this, false);
    }

    private void checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ MANAGE_EXTERNAL_STORAGE is checked on demand or prompt
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1001
                );
            }
        }
    }

    private void setupTerminal() {
        termuxEnv.clearAptLocks();
        terminalSession = new LocalTerminalSession(
                termuxEnv.getDefaultShell(),
                termuxEnv.getHomePath(),
                termuxEnv.getEnvironment()
        );
        binding.terminalView.attachSession(terminalSession);

        if (com.example.terminal.TermuxBootstrapInstaller.isInstalled(this)) {
            binding.terminalView.appendOutput("\u001B[32m[Termux Linux Ready - bash 5.2]\u001B[0m\n");
            binding.terminalView.appendOutput("\u001B[36mTry: 'pkg install clang' or 'pkg install python'\u001B[0m\n");
        } else {
            binding.terminalView.appendOutput("\u001B[32m[Terminal Ready - Base Shell]\u001B[0m\n");
            binding.terminalView.appendOutput("\u001B[33m⚡ Type 'pkg setup' or tap Menu > 'Install Linux Environment' to enable Clang, Python, and pkg package manager.\u001B[0m\n");
        }

        binding.btnTermClear.setOnClickListener(v -> {
            termuxEnv.clearAptLocks();
            if (terminalSession != null) terminalSession.stop();
            binding.terminalView.clear();
        });
        binding.ivTerminalToggle.setOnClickListener(v -> binding.terminalSection.setVisibility(View.GONE));

        // Terminal input handling
        binding.btnTerminalSend.setOnClickListener(v -> sendTerminalInput());
        binding.etTerminalInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                sendTerminalInput();
                return true;
            }
            return false;
        });
    }

    private void sendTerminalInput() {
        String input = binding.etTerminalInput.getText().toString().trim();
        if (!input.isEmpty()) {
            if (terminalSession != null && terminalSession.isBusy()) {
                // Program is currently running and waiting for input (e.g. cin >> a in C++)
                binding.terminalView.appendOutput("\u001B[32m" + input + "\u001B[0m\n");
                terminalSession.write(input + "\n");
            } else {
                if (input.equalsIgnoreCase("pkg setup") || input.equalsIgnoreCase("setup-termux") || input.equalsIgnoreCase("install-linux")) {
                    showBootstrapInstallDialog();
                } else if (input.startsWith("pkg install") || input.startsWith("apt install") || input.startsWith("pkg i ")) {
                    String[] parts = input.split("\\s+");
                    String targetPkg = null;
                    for (int i = 2; i < parts.length; i++) {
                        if (!parts[i].startsWith("-")) {
                            targetPkg = parts[i];
                            break;
                        }
                    }
                    if (targetPkg != null && !targetPkg.isEmpty()) {
                        binding.terminalView.appendOutput("\u001B[32m$ " + input + "\u001B[0m\n");
                        showToolchainInstallDialog(targetPkg, null);
                    } else {
                        binding.terminalView.appendOutput("\u001B[33mUsage: pkg install <package_name>\u001B[0m\n");
                    }
                } else if (input.equalsIgnoreCase("clear")) {
                    binding.terminalView.clear();
                } else {
                    if (input.startsWith("pkg ") || input.startsWith("apt ") || input.startsWith("apt-get ") || input.startsWith("dpkg ")) {
                        termuxEnv.clearAptLocks();
                    }
                    binding.terminalView.appendOutput("\u001B[32m$ " + input + "\u001B[0m\n");
                    runTerminalCommand(input, null);
                }
            }
            binding.etTerminalInput.setText("");
        }
    }

    private void runTerminalCommand(String command, String workingDir) {
        if (command != null && (command.contains("pkg ") || command.contains("apt ") || command.contains("dpkg "))) {
            termuxEnv.clearAptLocks();
        }
        if (terminalSession != null) {
            terminalSession.execute(command, workingDir, null);
        }
    }

    private void setupToolbarActions() {
        binding.ivMenuToggle.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // PASTE button: reads clipboard and pastes into editor
        binding.btnToolbarPaste.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
                CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
                if (text != null && text.length() > 0) {
                    binding.codeEditor.pasteText(text.toString());
                    Toast.makeText(this, "Pasted from clipboard", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
            }
        });

        // RUN button: compiles and runs active file
        binding.btnToolbarRun.setOnClickListener(v -> runCode());

        // TERMINAL button: directly opens or closes terminal anytime
        binding.btnToggleTerminal.setOnClickListener(v -> toggleTerminalVisibility());

        // SAVE button: saves current file
        binding.btnSave.setOnClickListener(v -> {
            saveCurrentFile();
            String name = currentFSNode != null ? currentFSNode.getName() : (currentFile != null ? currentFile.getName() : "file");
            Toast.makeText(this, "Saved: " + name, Toast.LENGTH_SHORT).show();
        });
    }

    private void toggleTerminalVisibility() {
        boolean isVisible = binding.terminalSection.getVisibility() == View.VISIBLE;
        if (isVisible) {
            binding.terminalSection.setVisibility(View.GONE);
        } else {
            binding.terminalSection.setVisibility(View.VISIBLE);
            binding.terminalView.post(() -> binding.terminalView.fullScroll(View.FOCUS_DOWN));
        }
    }

    private void setupEditor() {
        float savedFontSize = prefs.getFloat("font_size", 14.5f);
        boolean savedWordWrap = prefs.getBoolean("word_wrap", false);
        String savedFontFamily = prefs.getString("font_family", "Monospace (Default)");
        String savedTheme = prefs.getString("editor_theme", "VS Code Dark");
        boolean savedAutoClose = prefs.getBoolean("auto_close", true);

        binding.codeEditor.setFontSizeSp(savedFontSize);
        binding.codeEditor.setTypeface(getTypefaceForFont(savedFontFamily));
        binding.codeEditor.setHorizontallyScrolling(!savedWordWrap);
        binding.codeEditor.setAutoCloseEnabled(savedAutoClose);

        com.example.ui.SyntaxHighlighter.Theme currentTheme = com.example.ui.SyntaxHighlighter.getThemeByName(savedTheme);
        com.example.ui.SyntaxHighlighter.setActiveTheme(currentTheme);
        binding.codeEditor.applyTheme(currentTheme);

        binding.codeEditor.setOnFontSizeChangeListener(newSizeSp -> {
            prefs.edit().putFloat("font_size", newSizeSp).apply();
        });

        // VS Code / Notepad++ style floating IntelliSense Auto-Complete
        autoCompleteManager = new CodeAutoCompleteManager(binding.codeEditor, suggestion -> {
            binding.codeEditor.completeKeyword(suggestion);
        });

        binding.codeEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdatingText) return;
                isUpdatingText = true;
                String lang = currentFSNode != null ? currentFSNode.getName() : (currentFile != null ? currentFile.getName() : "C++");
                SyntaxHighlighter.highlight(s, lang);
                isUpdatingText = false;

                // Show floating IntelliSense popup at cursor
                String prefix = binding.codeEditor.getCurrentWordPrefix();
                autoCompleteManager.update(prefix, lang);
            }
        });
    }

    private void setupEditorTabs() {
        editorTabsAdapter = new EditorTabsAdapter(new EditorTabsAdapter.OnTabActionListener() {
            @Override
            public void onTabClick(FSNode node) {
                loadFile(node, false);
            }

            @Override
            public void onTabClose(FSNode node, int position) {
                saveCurrentFile();
                FSNode newActive = editorTabsAdapter.closeTab(node);
                if (newActive != null) {
                    loadFile(newActive, false);
                } else {
                    currentFSNode = null;
                    currentFile = null;
                    isUpdatingText = true;
                    binding.codeEditor.setText("");
                    isUpdatingText = false;
                    binding.tvProjectTitle.setText("Code Editor");
                    binding.tvActiveFile.setText("No file open");
                    binding.tvActiveFile.setVisibility(View.VISIBLE);
                    binding.llKeywords.removeAllViews();
                    if (explorerAdapter != null) {
                        explorerAdapter.setActiveFileKey(null);
                    }
                }
            }
        });

        LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.rvEditorTabs.setLayoutManager(lm);
        binding.rvEditorTabs.setAdapter(editorTabsAdapter);
    }

    private void setupTwoTierBottomBar() {
        float density = getResources().getDisplayMetrics().density;

        // Tier 1: Reference Keywords
        binding.llKeywords.removeAllViews();
        String activeFileName = currentFSNode != null ? currentFSNode.getName() : (currentFile != null ? currentFile.getName() : "main.cpp");
        String[] keywords = getKeywordsForLanguage(activeFileName);

        for (String kw : keywords) {
            TextView tv = new TextView(this);
            tv.setText(kw);
            tv.setTextColor(Color.parseColor("#5B9DF5"));
            tv.setTextSize(13.5f);
            tv.setTypeface(Typeface.MONOSPACE);
            tv.setPadding((int) (10 * density), 0, (int) (10 * density), 0);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setBackgroundResource(android.R.drawable.list_selector_background);
            tv.setClickable(true);
            tv.setFocusable(true);
            tv.setOnClickListener(v -> binding.codeEditor.insertKeyword(kw));
            binding.llKeywords.addView(tv);
        }

        // Tier 2: Symbols
        binding.llSymbols.removeAllViews();
        String[] symbols = {
                "Tab", "{}", "()", "[]", "\"\"", "''", ";", "=>", "=", "\\",
                "&", ",", "+", "-", "!", "::", "->", "_", "*", "#", "%",
                "<", ">", "@", "?"
        };

        for (String sym : symbols) {
            TextView tv = new TextView(this);
            tv.setText(sym);
            tv.setTextColor(Color.parseColor("#ECECEC"));
            tv.setTextSize(14f);
            tv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            tv.setPadding((int) (12 * density), 0, (int) (12 * density), 0);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setBackgroundResource(android.R.drawable.list_selector_background);
            tv.setClickable(true);
            tv.setFocusable(true);
            tv.setOnClickListener(v -> binding.codeEditor.insertSymbol(sym));
            binding.llSymbols.addView(tv);
        }
    }

    private String[] getKeywordsForLanguage(String fileName) {
        String lower = fileName != null ? fileName.replaceAll("\\s+", "").toLowerCase() : "";
        if (lower.endsWith(".rs")) {
            return new String[]{"fn", "let", "mut", "struct", "enum", "use", "pub", "mod", "trait", "async", "match", "for", "while", "return", "println!"};
        } else if (lower.endsWith(".py")) {
            return new String[]{"def", "class", "import", "from", "return", "if", "elif", "else", "for", "while", "print", "in", "try", "except"};
        } else if (lower.endsWith(".java")) {
            return new String[]{"public", "class", "static", "void", "main", "return", "if", "else", "for", "while", "new", "System.out.println"};
        } else if (lower.endsWith(".js") || lower.endsWith(".ts")) {
            return new String[]{"const", "let", "var", "function", "return", "if", "else", "for", "while", "import", "export", "class", "console.log"};
        } else if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return new String[]{"<div>", "<span>", "<p>", "<h1>", "<script>", "<link>", "<style>", "class=", "id=", "src=", "href="};
        } else {
            // C / C++ Default
            return new String[]{"include", "class", "struct", "public", "private", "int", "void", "return", "if", "else", "for", "while", "std", "cout", "cin", "vector", "string"};
        }
    }

    private void setupDrawerAndExplorers() {
        explorerAdapter = new ExplorerTreeAdapter(this, new ExplorerTreeAdapter.OnExplorerNodeListener() {
            @Override
            public void onFileClick(FSNode node) {
                drawerLayout.closeDrawer(GravityCompat.START);
                loadFile(node, true);
            }

            @Override
            public void onFileLongClick(FSNode node) {
                showRenameFileDialog(node);
            }

            @Override
            public void onFileDelete(FSNode node) {
                showDeleteFileDialog(node);
            }

            @Override
            public void onFolderClick(FSNode node, boolean isExpanded) {
                explorerAdapter.toggleFolder(node);
            }

            @Override
            public void onFolderLongClick(FSNode node, View anchorView) {
                showFolderPopupMenu(node, anchorView);
            }

            @Override
            public void onFolderDelete(FSNode node) {
                showDeleteFolderDialog(node);
            }
        });

        binding.rvExplorer.setLayoutManager(new LinearLayoutManager(this));
        binding.rvExplorer.setAdapter(explorerAdapter);

        // Header Action: Search
        binding.btnExplorerSearch.setOnClickListener(v -> toggleSearchBar());
        binding.btnSearchClear.setOnClickListener(v -> closeSearchBar());
        binding.etExplorerSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                explorerAdapter.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Header Action: Add File / Folder
        binding.btnExplorerAdd.setOnClickListener(this::showAddDropdown);

        // Header Action: More Options (Three-Dot Menu)
        binding.btnExplorerMore.setOnClickListener(this::showMoreDropdown);

        // Load project
        projectViewModel.getAllProjects().observe(this, projects -> {
            if (isProjectInitialLoadDone) return;
            isProjectInitialLoadDone = true;

            // Check if user previously had a file open
            String lastFilePath = prefs.getString("last_active_file_path", null);
            if (lastFilePath != null) {
                File lastFile = new File(lastFilePath);
                if (lastFile.exists()) {
                    File parent = lastFile.getParentFile();
                    if (parent != null && parent.exists()) {
                        currentProject = new Project(parent.getName(), "Auto", parent.getAbsolutePath());
                        binding.tvProjectSubtitle.setText(parent.getName().toUpperCase());
                        explorerAdapter.setRootDirectory(parent);
                        loadFile(FSNode.fromFile(lastFile), true);
                        return;
                    }
                }
            }

            if (currentProject == null || currentProject.path.contains("Download") || !new File(currentProject.path).exists()) {
                resetToCppStarter();
            } else {
                updateAppTitle(currentProject.language);
                loadProject(currentProject);
            }
        });
    }

    private void initActivityLaunchers() {
        folderPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                getContentResolver().takePersistableUriPermission(
                                        uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                );
                            } catch (Exception ignored) {}

                            DocumentFile docFolder = null;
                            try {
                                docFolder = DocumentFile.fromTreeUri(this, uri);
                            } catch (Exception ignored) {}

                            File localFolder = resolveFileFromUri(uri);
                            String folderName = docFolder != null && docFolder.getName() != null ? docFolder.getName()
                                    : (localFolder != null ? localFolder.getName() : "Folder");

                            FSNode rootNode = new FSNode(localFolder, docFolder, uri, folderName, true);
                            explorerAdapter.addExternalFSNode(rootNode);

                            FSNode firstCode = FSNode.findFirstCodeFile(this, rootNode);
                            if (firstCode != null) {
                                loadFile(firstCode, true);
                            }
                            drawerLayout.closeDrawer(GravityCompat.START);
                            Toast.makeText(this, "Opened folder: " + folderName, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            handlePickedFileUri(uri);
                        }
                    }
                }
        );
    }

    private void toggleSearchBar() {
        if (binding.layoutSearchBar.getVisibility() == View.VISIBLE) {
            closeSearchBar();
        } else {
            binding.layoutSearchBar.setVisibility(View.VISIBLE);
            binding.etExplorerSearch.requestFocus();
        }
    }

    private void closeSearchBar() {
        binding.etExplorerSearch.setText("");
        binding.layoutSearchBar.setVisibility(View.GONE);
        explorerAdapter.setSearchQuery("");
    }

    private void showAddDropdown(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "📄 New File");
        popup.getMenu().add(0, 2, 1, "📁 New Folder");
        popup.getMenu().add(0, 3, 2, "📂 Open Folder (Device Storage)");
        popup.getMenu().add(0, 4, 3, "📄 Open File (Device Storage)");
        popup.setOnMenuItemClickListener(item -> {
            File baseDir = currentProject != null ? new File(currentProject.path) : new File(termuxEnv.getProjectsPath());
            if (item.getItemId() == 1) {
                showCreateFileDialog(FSNode.fromFile(baseDir));
            } else if (item.getItemId() == 2) {
                showCreateFolderDialog(FSNode.fromFile(baseDir));
            } else if (item.getItemId() == 3) {
                launchSystemFolderPicker();
            } else if (item.getItemId() == 4) {
                launchSystemFilePicker();
            }
            return true;
        });
        popup.show();
    }

    private void launchSystemFolderPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            new AlertDialog.Builder(this)
                    .setTitle("📁 Storage Access (Recommended)")
                    .setMessage("For direct execution in Termux and editing phone folders, consider allowing 'All files access'.\n\nTap 'Allow' to open settings, or 'Continue' to pick folder right away.")
                    .setPositiveButton("Allow", (d, w) -> {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        } catch (Exception e) {
                            try {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                startActivity(intent);
                            } catch (Exception ignored) {}
                        }
                    })
                    .setNegativeButton("Continue", (d, w) -> openFolderPickerIntent())
                    .show();
        } else {
            openFolderPickerIntent();
        }
    }

    private void openFolderPickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        try {
            folderPickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open system file picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void launchSystemFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            filePickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open system file picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File resolveFileFromUri(Uri treeUri) {
        if (treeUri == null) return null;
        String docId = null;
        try {
            docId = DocumentsContract.getTreeDocumentId(treeUri);
        } catch (Exception ignored) {}
        if (docId == null) {
            docId = treeUri.getPath();
        }
        if (docId == null) return null;

        try {
            docId = Uri.decode(docId);
        } catch (Exception ignored) {}

        if (docId.startsWith("raw:")) {
            File rawFile = new File(docId.substring(4));
            if (rawFile.exists()) return rawFile;
        }

        if (docId.contains("primary:")) {
            String relative = docId.substring(docId.indexOf("primary:") + 8);
            File f = new File(Environment.getExternalStorageDirectory(), relative);
            if (f.exists()) return f;
            File f2 = new File("/storage/emulated/0/", relative);
            if (f2.exists()) return f2;
            return f;
        } else if (docId.contains(":")) {
            String[] parts = docId.split(":");
            if (parts.length > 1) {
                String volume = parts[0];
                if (volume.contains("/")) {
                    volume = volume.substring(volume.lastIndexOf('/') + 1);
                }
                String relative = parts[1];
                File storage = new File("/storage/" + volume + "/" + relative);
                if (storage.exists()) return storage;
            }
        }
        return null;
    }

    private void handlePickedFileUri(Uri uri) {
        try {
            String fileName = "imported_file";
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }

            File targetDir = currentProject != null ? new File(currentProject.path) : new File(termuxEnv.getProjectsPath(), "CppStarter");
            if (!targetDir.exists()) targetDir.mkdirs();
            File targetFile = new File(targetDir, fileName);

            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new java.io.FileOutputStream(targetFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            explorerAdapter.rebuildTree();
            loadFile(FSNode.fromFile(targetFile), true);
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Opened " + fileName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error opening file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void resetToCppStarter() {
        File starterDir = new File(termuxEnv.getProjectsPath(), "CppStarter");
        if (!starterDir.exists()) {
            starterDir.mkdirs();
        }
        File mainCpp = new File(starterDir, "main.cpp");
        if (!mainCpp.exists()) {
            try (FileWriter writer = new FileWriter(mainCpp)) {
                writer.write(TemplateManager.getTemplate("main.cpp"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        currentProject = new Project("CppStarter", "C++", starterDir.getAbsolutePath());
        binding.tvProjectSubtitle.setText("CPPSTARTER");
        explorerAdapter.clearExternalFolders();
        explorerAdapter.setRootDirectory(starterDir);
        if (editorTabsAdapter != null) {
            editorTabsAdapter.clearTabs();
        }
        loadFile(FSNode.fromFile(mainCpp), true);
        drawerLayout.closeDrawer(GravityCompat.START);
        Toast.makeText(this, "Switched to CppStarter", Toast.LENGTH_SHORT).show();
    }

    private void showMoreDropdown(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        boolean isLinuxInstalled = com.example.terminal.TermuxBootstrapInstaller.isInstalled(this);
        popup.getMenu().add(0, 1, 0, ">_ Open Terminal");
        popup.getMenu().add(0, 2, 1, isLinuxInstalled ? "⚡ Reinstall Linux Environment" : "⚡ Install Linux Environment");
        popup.getMenu().add(0, 3, 2, "🎨 Editor Settings & Themes");
        popup.getMenu().add(0, 4, 3, "🛠️ Compiler Setup Guide");
        popup.getMenu().add(0, 5, 4, "📜 Open Source Licenses & Credits");
        popup.getMenu().add(0, 6, 5, "ℹ️ About Code Editor");
        popup.getMenu().add(0, 7, 6, "🔄 Restart");
        popup.getMenu().add(0, 8, 7, "🌐 Official Website");
        popup.getMenu().add(0, 10, 8, "🐙 Official GitHub Repository");
        popup.getMenu().add(0, 9, 9, "🔄 Check for Updates");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    drawerLayout.closeDrawer(GravityCompat.START);
                    binding.terminalSection.setVisibility(View.VISIBLE);
                    binding.terminalView.post(() -> binding.terminalView.fullScroll(View.FOCUS_DOWN));
                    break;
                case 2:
                    showBootstrapInstallDialog();
                    break;
                case 3:
                    showEditorSettingsDialog();
                    break;
                case 4:
                    showCompilerGuideDialog();
                    break;
                case 5:
                    showLicensesDialog();
                    break;
                case 6:
                    showAboutDialog();
                    break;
                case 7:
                    showRestartConfirmDialog();
                    break;
                case 8:
                    com.example.util.AppUpdateManager.openWebsite(this);
                    break;
                case 10:
                    com.example.util.AppUpdateManager.openGitHubRepo(this);
                    break;
                case 9:
                    com.example.util.AppUpdateManager.checkForUpdates(this, true);
                    break;
            }
            return true;
        });
        popup.show();
    }

    private void showRestartConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Restart Environment")
                .setMessage("Are you sure you want to restart?\n\nAll temporary editor files and project data will be reset. Your installed compilers and Linux packages will remain safe and intact.")
                .setPositiveButton("Restart", (dialog, which) -> performRestart())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performRestart() {
        try {
            File starterDir = new File(termuxEnv.getProjectsPath(), "CppStarter");
            if (starterDir.exists()) {
                File[] files = starterDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        deleteRecursively(f);
                    }
                }
            } else {
                starterDir.mkdirs();
            }

            File mainCpp = new File(starterDir, "main.cpp");
            try (FileWriter writer = new FileWriter(mainCpp)) {
                writer.write(TemplateManager.getTemplate("main.cpp"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            currentProject = new Project("CppStarter", "C++", starterDir.getAbsolutePath());
            binding.tvProjectSubtitle.setText("CPPSTARTER");
            explorerAdapter.clearExternalFolders();
            explorerAdapter.setRootDirectory(starterDir);
            if (editorTabsAdapter != null) {
                editorTabsAdapter.clearTabs();
            }
            loadFile(FSNode.fromFile(mainCpp), true);
            drawerLayout.closeDrawer(GravityCompat.START);
            prefs.edit().remove("last_active_file_path").apply();
            Toast.makeText(this, "Environment restarted successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Restart error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteRecursively(File fileOrDir) {
        if (fileOrDir != null && fileOrDir.exists()) {
            if (fileOrDir.isDirectory()) {
                File[] children = fileOrDir.listFiles();
                if (children != null) {
                    for (File child : children) {
                        deleteRecursively(child);
                    }
                }
            }
            fileOrDir.delete();
        }
    }

    private void showBootstrapInstallDialog() {
        if (com.example.terminal.TermuxBootstrapInstaller.isInstalled(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("Termux Linux Environment")
                    .setMessage("The embedded Linux environment is installed and operational!\n\nYou can run packages and compilers in the terminal:\n• pkg install clang (C/C++)\n• pkg install python (Python 3)\n• pkg install nodejs (JavaScript)")
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Reinstall", (dialog, which) -> startBootstrapInstallation())
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("⚡ Install Termux Linux Environment")
                    .setMessage("Install the official embedded Linux package system (~31 MB)?\n\n• Installs bash, apt, and pkg package manager\n• Offline compilers: Clang (C/C++), Python, Node.js\n• Runs 100% inside this app without third-party apps")
                    .setPositiveButton("Install Now", (dialog, which) -> startBootstrapInstallation())
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void startBootstrapInstallation() {
        android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
        progress.setTitle("Installing Termux Linux");
        progress.setMessage("Connecting to repository...");
        progress.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progress.setMax(100);
        progress.setCancelable(false);
        progress.show();

        com.example.terminal.TermuxBootstrapInstaller.install(this, new com.example.terminal.TermuxBootstrapInstaller.InstallCallback() {
            @Override
            public void onProgress(String status, int percentage) {
                runOnUiThread(() -> {
                    progress.setMessage(status);
                    progress.setProgress(percentage);
                });
            }

            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    if (progress.isShowing()) progress.dismiss();
                    Toast.makeText(MainActivity.this, "Termux Linux environment installed successfully!", Toast.LENGTH_LONG).show();
                    termuxEnv.clearAptLocks();
                    if (terminalSession != null) {
                        terminalSession.restart(
                                termuxEnv.getDefaultShell(),
                                termuxEnv.getHomePath(),
                                termuxEnv.getEnvironment()
                        );
                    }
                    binding.terminalSection.setVisibility(View.VISIBLE);
                    binding.terminalView.appendOutput("\n\u001B[32m[Termux Linux Ready - " + termuxEnv.getDefaultShell() + "]\u001B[0m\n");
                    binding.terminalView.appendOutput("\u001B[36mTry: 'pkg install clang' or 'pkg install python'\u001B[0m\n");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (progress.isShowing()) progress.dismiss();
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Installation Failed")
                            .setMessage("Error during installation:\n" + error + "\n\nPlease check your internet connection and try again.")
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        });
    }

    private void showToolchainInstallDialog(String pkgOrTarget, Runnable onComplete) {
        String displayName = com.example.terminal.ToolchainInstaller.getDisplayName(pkgOrTarget);
        android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
        progress.setTitle("Installing " + displayName);
        progress.setMessage("Connecting to GitHub CDN...");
        progress.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progress.setMax(100);
        progress.setCancelable(false);
        progress.show();

        binding.terminalSection.setVisibility(View.VISIBLE);
        binding.terminalView.appendOutput("\n\u001B[36m==> TermCode Package Manager: Installing " + displayName + "...\u001B[0m\n");
        binding.terminalView.appendOutput("\u001B[33mConnecting to TermCode GitHub CDN...\u001B[0m\n");

        com.example.terminal.ToolchainInstaller.install(this, pkgOrTarget, new com.example.terminal.ToolchainInstaller.InstallCallback() {
            @Override
            public void onProgress(String status, int percentage) {
                runOnUiThread(() -> {
                    progress.setMessage(status);
                    progress.setProgress(percentage);
                });
            }

            @Override
            public void onSuccess(String target, String name) {
                runOnUiThread(() -> {
                    progress.setProgress(100);
                    progress.setMessage("Library is installed successfully!");
                    binding.terminalView.appendOutput("\u001B[32m✔ Library is installed successfully!\u001B[0m\n");
                    binding.terminalView.post(() -> binding.terminalView.fullScroll(View.FOCUS_DOWN));
                    Toast.makeText(MainActivity.this, "Library is installed successfully!", Toast.LENGTH_SHORT).show();

                    binding.terminalView.postDelayed(() -> {
                        if (progress.isShowing()) progress.dismiss();
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }, 650);
                });
            }

            @Override
            public void onError(String target, String error) {
                runOnUiThread(() -> {
                    if (progress.isShowing()) progress.dismiss();
                    binding.terminalView.appendOutput("\u001B[31m✘ Error installing " + displayName + ": " + error + "\u001B[0m\n");
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Installation Failed")
                            .setMessage("Failed to install " + displayName + ":\n" + error + "\n\nPlease check your internet connection and try again.")
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        });
    }

    private void showFolderPopupMenu(FSNode folder, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "📄 New File in " + folder.getName());
        popup.getMenu().add(0, 2, 1, "📁 New Folder in " + folder.getName());
        popup.getMenu().add(0, 3, 2, "🗑️ Remove / Delete Folder");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    showCreateFileDialog(folder);
                    break;
                case 2:
                    showCreateFolderDialog(folder);
                    break;
                case 3:
                    showDeleteFolderDialog(folder);
                    break;
            }
            return true;
        });
        popup.show();
    }

    private void showCreateFileDialog(FSNode parentNode) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_simple_input, null);
        TextView tvHint = view.findViewById(R.id.tv_dialog_hint);
        EditText etInput = view.findViewById(R.id.et_dialog_input);

        tvHint.setText("Create new file in: " + parentNode.getName());
        etInput.setHint("e.g. Main.java, index.html, script.py");

        new AlertDialog.Builder(this)
                .setTitle("New File")
                .setView(view)
                .setPositiveButton("Create", (dialog, which) -> {
                    String rawName = etInput.getText().toString().trim();
                    if (!rawName.isEmpty()) {
                        String name = rawName.replaceAll("\\s*\\.\\s*", ".");
                        if (name.toLowerCase().endsWith(".java")) {
                            String base = name.substring(0, name.length() - 5);
                            if (!base.isEmpty() && Character.isLowerCase(base.charAt(0))) {
                                name = Character.toUpperCase(base.charAt(0)) + (base.length() > 1 ? base.substring(1) : "") + ".java";
                            }
                        }
                        FSNode newFileNode = parentNode.createChildFile(this, name);
                        if (newFileNode != null) {
                            String template = TemplateManager.getTemplate(name);
                            newFileNode.writeContent(this, template);
                            explorerAdapter.rebuildTree();
                            loadFile(newFileNode, true);
                            Toast.makeText(this, "Created " + name, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Could not create file", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCreateFolderDialog(FSNode parentNode) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_simple_input, null);
        TextView tvHint = view.findViewById(R.id.tv_dialog_hint);
        EditText etInput = view.findViewById(R.id.et_dialog_input);

        tvHint.setText("Create folder in: " + parentNode.getName());
        etInput.setHint("Folder name (e.g. src, components)");

        new AlertDialog.Builder(this)
                .setTitle("New Folder")
                .setView(view)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etInput.getText().toString().trim();
                    if (!name.isEmpty()) {
                        FSNode newDirNode = parentNode.createChildFolder(this, name);
                        if (newDirNode != null) {
                            explorerAdapter.rebuildTree();
                            Toast.makeText(this, "Created folder: " + name, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Could not create folder", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRenameFileDialog(FSNode node) {
        if (node.getFile() == null) {
            Toast.makeText(this, "Rename not supported for cloud files", Toast.LENGTH_SHORT).show();
            return;
        }
        File file = node.getFile();
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_simple_input, null);
        TextView tvHint = view.findViewById(R.id.tv_dialog_hint);
        EditText etInput = view.findViewById(R.id.et_dialog_input);

        tvHint.setText("Rename file:");
        etInput.setText(file.getName());
        etInput.selectAll();

        new AlertDialog.Builder(this)
                .setTitle("Rename File")
                .setView(view)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = etInput.getText().toString().trim();
                    if (!newName.isEmpty() && !newName.equals(file.getName())) {
                        File dest = new File(file.getParentFile(), newName);
                        if (file.renameTo(dest)) {
                            if (editorTabsAdapter != null) {
                                editorTabsAdapter.closeTab(node);
                            }
                            FSNode renamedNode = FSNode.fromFile(dest);
                            if (currentFile != null && currentFile.getAbsolutePath().equals(file.getAbsolutePath())) {
                                currentFile = dest;
                                currentFSNode = renamedNode;
                                binding.tvActiveFile.setText(dest.getName());
                            }
                            explorerAdapter.rebuildTree();
                            loadFile(renamedNode, true);
                            Toast.makeText(this, "Renamed to " + newName, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteFileDialog(FSNode node) {
        new AlertDialog.Builder(this)
                .setTitle("Delete File")
                .setMessage("Are you sure you want to delete " + node.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (editorTabsAdapter != null) {
                        editorTabsAdapter.closeTab(node);
                    }
                    if (node.delete(this)) {
                        Toast.makeText(this, "Deleted " + node.getName(), Toast.LENGTH_SHORT).show();
                        explorerAdapter.rebuildTree();
                        if (currentFSNode != null && currentFSNode.getKey().equals(node.getKey())) {
                            FSNode next = editorTabsAdapter != null ? editorTabsAdapter.getActiveNode() : null;
                            if (next != null) {
                                loadFile(next, false);
                            } else {
                                resetToCppStarter();
                            }
                        }
                    } else {
                        Toast.makeText(this, "Could not delete file", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteFolderDialog(FSNode folder) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Folder")
                .setMessage("Are you sure you want to delete folder '" + folder.getName() + "' and its contents?")
                .setPositiveButton("Delete Permanently", (dialog, which) -> {
                    explorerAdapter.removeExternalFSNode(folder);
                    folder.delete(this);
                    explorerAdapter.rebuildTree();
                    Toast.makeText(this, "Folder deleted permanently", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Remove from Explorer", (dialog, which) -> {
                    explorerAdapter.removeExternalFSNode(folder);
                    explorerAdapter.rebuildTree();
                    Toast.makeText(this, "Folder removed from explorer", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static Typeface getTypefaceForFont(String fontName) {
        if (fontName == null) return Typeface.MONOSPACE;
        switch (fontName) {
            case "Roboto Mono (Clean)":
                return Typeface.create("sans-serif-monospace", Typeface.NORMAL);
            case "Monospace Bold":
                return Typeface.create("monospace", Typeface.BOLD);
            case "Condensed Pro":
                return Typeface.create("sans-serif-condensed", Typeface.NORMAL);
            case "Sans-Serif Modern":
                return Typeface.create("sans-serif", Typeface.NORMAL);
            case "Serif Editorial":
                return Typeface.create("serif", Typeface.NORMAL);
            case "Casual Style":
                return Typeface.create("casual", Typeface.NORMAL);
            case "Monospace (Default)":
            default:
                return Typeface.MONOSPACE;
        }
    }

    private void showEditorSettingsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_editor_settings, null);
        RadioGroup rgFontSize = view.findViewById(R.id.rg_font_size);
        Spinner spinnerFontFamily = view.findViewById(R.id.spinner_font_family);
        Spinner spinnerEditorTheme = view.findViewById(R.id.spinner_editor_theme);
        SwitchMaterial switchWordWrap = view.findViewById(R.id.switch_word_wrap);
        SwitchMaterial switchAutoClose = view.findViewById(R.id.switch_auto_close);

        float currentSize = prefs.getFloat("font_size", 14.5f);
        boolean currentWrap = prefs.getBoolean("word_wrap", false);
        String currentFont = prefs.getString("font_family", "Monospace (Default)");
        String currentThemeName = prefs.getString("editor_theme", "VS Code Dark");
        boolean currentAutoClose = prefs.getBoolean("auto_close", true);

        if (currentSize <= 12f) rgFontSize.check(R.id.rb_font_12);
        else if (currentSize <= 14f) rgFontSize.check(R.id.rb_font_14);
        else if (currentSize <= 16f) rgFontSize.check(R.id.rb_font_16);
        else if (currentSize <= 18f) rgFontSize.check(R.id.rb_font_18);
        else rgFontSize.check(R.id.rb_font_20);

        String[] fontOptions = new String[]{
                "Monospace (Default)",
                "Roboto Mono (Clean)",
                "Monospace Bold",
                "Condensed Pro",
                "Sans-Serif Modern",
                "Serif Editorial",
                "Casual Style"
        };

        ArrayAdapter<String> fontAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fontOptions) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(Color.WHITE);
                    ((TextView) v).setTextSize(13);
                    ((TextView) v).setTypeface(getTypefaceForFont(fontOptions[position]));
                }
                return v;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                v.setBackgroundColor(Color.parseColor("#262626"));
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(Color.WHITE);
                    ((TextView) v).setPadding(28, 28, 28, 28);
                    ((TextView) v).setTypeface(getTypefaceForFont(fontOptions[position]));
                }
                return v;
            }
        };
        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFontFamily.setAdapter(fontAdapter);

        for (int i = 0; i < fontOptions.length; i++) {
            if (fontOptions[i].equals(currentFont)) {
                spinnerFontFamily.setSelection(i);
                break;
            }
        }

        String[] themeOptions = new String[]{
                "VS Code Dark",
                "Dracula",
                "Monokai Pro",
                "One Dark Pro",
                "Matrix Neon"
        };

        ArrayAdapter<String> themeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, themeOptions) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(Color.WHITE);
                    ((TextView) v).setTextSize(13);
                }
                return v;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                v.setBackgroundColor(Color.parseColor("#262626"));
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(Color.WHITE);
                    ((TextView) v).setPadding(28, 28, 28, 28);
                }
                return v;
            }
        };
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEditorTheme.setAdapter(themeAdapter);

        for (int i = 0; i < themeOptions.length; i++) {
            if (themeOptions[i].equals(currentThemeName)) {
                spinnerEditorTheme.setSelection(i);
                break;
            }
        }

        switchWordWrap.setChecked(currentWrap);
        switchAutoClose.setChecked(currentAutoClose);

        new AlertDialog.Builder(this)
                .setTitle("⚙️ Editor Settings & Themes")
                .setView(view)
                .setPositiveButton("Done", (dialog, which) -> {
                    float newSize = 14.5f;
                    int checkedId = rgFontSize.getCheckedRadioButtonId();
                    if (checkedId == R.id.rb_font_12) newSize = 12f;
                    else if (checkedId == R.id.rb_font_14) newSize = 14f;
                    else if (checkedId == R.id.rb_font_16) newSize = 16f;
                    else if (checkedId == R.id.rb_font_18) newSize = 18f;
                    else if (checkedId == R.id.rb_font_20) newSize = 20f;

                    String selectedFont = (String) spinnerFontFamily.getSelectedItem();
                    String selectedTheme = (String) spinnerEditorTheme.getSelectedItem();
                    boolean newWrap = switchWordWrap.isChecked();
                    boolean newAutoClose = switchAutoClose.isChecked();

                    prefs.edit()
                            .putFloat("font_size", newSize)
                            .putString("font_family", selectedFont)
                            .putBoolean("word_wrap", newWrap)
                            .putString("editor_theme", selectedTheme)
                            .putBoolean("auto_close", newAutoClose)
                            .apply();

                    binding.codeEditor.setFontSizeSp(newSize);
                    binding.codeEditor.setTypeface(getTypefaceForFont(selectedFont));
                    binding.codeEditor.setHorizontallyScrolling(!newWrap);
                    binding.codeEditor.setAutoCloseEnabled(newAutoClose);

                    com.example.ui.SyntaxHighlighter.Theme newTheme = com.example.ui.SyntaxHighlighter.getThemeByName(selectedTheme);
                    com.example.ui.SyntaxHighlighter.setActiveTheme(newTheme);
                    binding.codeEditor.applyTheme(newTheme);

                    Toast.makeText(this, "Settings updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCompilerGuideDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_compiler_guide, null);
        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Open Terminal", (dialog, which) -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    binding.terminalSection.setVisibility(View.VISIBLE);
                    binding.terminalView.post(() -> binding.terminalView.fullScroll(View.FOCUS_DOWN));
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Code Editor v" + BuildConfig.VERSION_NAME)
                .setMessage("A universal, multi-language standalone mobile IDE powered by an embedded Linux userland.\n\n" +
                        "• Supported Languages & Compilers:\n" +
                        "  - C (main.c - Clang)\n" +
                        "  - C++ (main.cpp - Clang++)\n" +
                        "  - C# (Program.cs - Mono)\n" +
                        "  - Python (main.py - Python 3)\n" +
                        "  - Java (Main.java - OpenJDK 21)\n" +
                        "  - JavaScript (main.js - Node.js)\n" +
                        "  - TypeScript (main.ts - Node.js)\n" +
                        "  - Go (main.go - Golang Toolchain)\n" +
                        "  - Rust (main.rs - Rustc & Cargo)\n" +
                        "  - Kotlin (Main.kt - Kotlinc)\n" +
                        "  - PHP (index.php - PHP 8+)\n" +
                        "  - Ruby (main.rb - Ruby 3+)\n" +
                        "  - Lua (main.lua - Lua 5.4)\n" +
                        "  - HTML/CSS (index.html, style.css) with Live Browser Preview\n" +
                        "  - Full-Stack Node.js Projects (package.json, npm start)\n\n" +
                        "• Multi-File Horizontal Tab Bar (VS Code / Spck style)\n" +
                        "• Authentic Language Vector Logos & Badges\n" +
                        "• Zero-Config Automated Toolchain CDN Installer (0-100% Progress)\n" +
                        "• Built-in Linux Terminal with full 'pkg' package manager\n" +
                        "• Floating IntelliSense Auto-Complete & Syntax Highlighting\n" +
                        "• Two-Tier Coding Accessory Bar (Keywords & Symbols)")
                .setPositiveButton("OK", null)
                .setNeutralButton("📜 Licenses", (dialog, which) -> showLicensesDialog())
                .show();
    }

    private void showLicensesDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_licenses, null);
        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Close", null)
                .show();
    }

    private void updateAppTitle(String language) {
        if (language == null || language.isEmpty()) {
            binding.tvProjectTitle.setText("Code Editor - C++");
        } else {
            binding.tvProjectTitle.setText("Code Editor - " + language);
        }
    }

    private void loadProject(Project project) {
        File dir = new File(project.path);
        binding.tvProjectSubtitle.setText(project.name.toUpperCase());
        explorerAdapter.setRootDirectory(dir);

        // If a file is ALREADY active in editor and exists, do NOT override it!
        if (currentFSNode != null && currentFSNode.getFile() != null && currentFSNode.getFile().exists()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                if (f.isFile()) {
                    loadFile(FSNode.fromFile(f), true);
                    break;
                }
            }
        } else {
            File defaultFile = new File(dir, "main.cpp");
            try {
                if (defaultFile.createNewFile()) {
                    try (FileWriter writer = new FileWriter(defaultFile)) {
                        writer.write(TemplateManager.getTemplate("main.cpp"));
                    }
                }
                explorerAdapter.rebuildTree();
                loadFile(FSNode.fromFile(defaultFile), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadFile(File file) {
        if (file != null) {
            loadFile(FSNode.fromFile(file), true);
        }
    }

    private void loadFile(FSNode node) {
        loadFile(node, true);
    }

    private void loadFile(FSNode node, boolean addTab) {
        if (node == null || node.isDirectory()) return;

        // If clicking/loading the same file that is ALREADY loaded, just ensure tab is active
        if (currentFSNode != null && currentFSNode.getKey().equals(node.getKey())) {
            if (editorTabsAdapter != null) editorTabsAdapter.setActiveNode(node);
            return;
        }

        saveCurrentFile();
        currentFSNode = node;
        currentFile = node.getFile();
        if (currentFile != null) {
            prefs.edit().putString("last_active_file_path", currentFile.getAbsolutePath()).apply();
        }
        binding.tvActiveFile.setText(node.getName());
        binding.tvActiveFile.setVisibility(View.GONE);

        if (addTab && editorTabsAdapter != null) {
            editorTabsAdapter.openTab(node);
            int idx = editorTabsAdapter.getOpenTabs().indexOf(node);
            if (idx != -1) {
                binding.rvEditorTabs.smoothScrollToPosition(idx);
            }
        } else if (editorTabsAdapter != null) {
            editorTabsAdapter.setActiveNode(node);
        }

        if (explorerAdapter != null) {
            explorerAdapter.setActiveFileKey(node.getKey());
        }

        // Update Title for any recognized language
        String name = node.getName() != null ? node.getName().replaceAll("\\s+", "").toLowerCase() : "";
        if (name.endsWith(".rs")) updateAppTitle("Rust");
        else if (name.endsWith(".py")) updateAppTitle("Python");
        else if (name.endsWith(".java")) updateAppTitle("Java");
        else if (name.endsWith(".js") || name.endsWith(".ts")) updateAppTitle("JavaScript");
        else if (name.endsWith(".html") || name.endsWith(".htm")) updateAppTitle("HTML");
        else if (name.endsWith(".css")) updateAppTitle("CSS");
        else if (name.endsWith(".json")) updateAppTitle("JSON");
        else if (name.endsWith(".go")) updateAppTitle("Go");
        else if (name.endsWith(".rb")) updateAppTitle("Ruby");
        else if (name.endsWith(".php")) updateAppTitle("PHP");
        else if (name.endsWith(".lua")) updateAppTitle("Lua");
        else if (name.endsWith(".sh") || name.endsWith(".bash")) updateAppTitle("Shell");
        else if (name.endsWith(".cpp") || name.endsWith(".cc")) updateAppTitle("C++");
        else if (name.endsWith(".c") || name.endsWith(".h")) updateAppTitle("C");
        else updateAppTitle(node.getName());

        setupTwoTierBottomBar();

        String content = "";
        try {
            content = node.readContent(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        isUpdatingText = true;
        binding.codeEditor.setText(content);
        isUpdatingText = false;
        SyntaxHighlighter.highlight(binding.codeEditor.getText(), node.getName());
    }

    private void saveCurrentFile() {
        if (currentFSNode != null) {
            currentFSNode.writeContent(this, binding.codeEditor.getText().toString());
        } else if (currentFile != null) {
            try (FileWriter writer = new FileWriter(currentFile)) {
                writer.write(binding.codeEditor.getText().toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void runCode() {
        if (currentFSNode == null && currentFile == null) {
            Toast.makeText(this, "No file open to run", Toast.LENGTH_SHORT).show();
            return;
        }

        saveCurrentFile();

        // Expand terminal automatically
        binding.terminalSection.setVisibility(View.VISIBLE);

        String fileName = currentFSNode != null ? currentFSNode.getName() : currentFile.getName();
        File localFile = currentFSNode != null ? currentFSNode.getFile() : currentFile;

        if (localFile != null && localFile.exists() && localFile.getParentFile() != null) {
            String parentDir = localFile.getParentFile().getAbsolutePath();
            executeInTermux(parentDir, fileName, localFile);
        } else {
            // For cloud/SAF items without direct POSIX path, sync to Termux workspace
            File syncedDir = new File(termuxEnv.getProjectsPath(), "SyncedWorkspace");
            if (!syncedDir.exists()) syncedDir.mkdirs();
            File syncedFile = new File(syncedDir, fileName);
            try (FileWriter fw = new FileWriter(syncedFile)) {
                fw.write(binding.codeEditor.getText().toString());
            } catch (Exception ignored) {}

            // Also copy sibling files (style.css, script.js, images) if available from SAF parent
            if (currentFSNode != null && currentFSNode.getDocFile() != null && currentFSNode.getDocFile().getParentFile() != null) {
                DocumentFile parentDoc = currentFSNode.getDocFile().getParentFile();
                if (parentDoc != null && parentDoc.isDirectory()) {
                    for (DocumentFile sibling : parentDoc.listFiles()) {
                        if (sibling.isFile() && sibling.getName() != null && !sibling.getName().equals(fileName)) {
                            File destSibling = new File(syncedDir, sibling.getName());
                            try (InputStream in = getContentResolver().openInputStream(sibling.getUri());
                                 OutputStream out = new java.io.FileOutputStream(destSibling)) {
                                byte[] buf = new byte[4096];
                                int len;
                                while ((len = in.read(buf)) != -1) {
                                    out.write(buf, 0, len);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }

            executeInTermux(syncedDir.getAbsolutePath(), fileName, syncedFile);
        }
    }

    private String getMissingToolchainPackage(String langName, String fileName) {
        File usrBin = new File(getFilesDir(), "usr/bin");
        if ("C++".equalsIgnoreCase(langName) || "C".equalsIgnoreCase(langName)) {
            if (!new File(usrBin, "clang++").exists() && !new File(usrBin, "clang").exists()) {
                return "clang";
            }
        } else if ("Java".equalsIgnoreCase(langName)) {
            if (!new File(usrBin, "javac").exists()) {
                return "openjdk-21";
            }
        } else if ("Python".equalsIgnoreCase(langName)) {
            if (!new File(usrBin, "python").exists() && !new File(usrBin, "python3").exists()) {
                return "python";
            }
        } else if ("JavaScript".equalsIgnoreCase(langName) || "Node.js".equalsIgnoreCase(langName) || fileName.endsWith(".js") || fileName.endsWith(".ts")) {
            if (!new File(usrBin, "node").exists()) {
                return "nodejs";
            }
        } else if ("Go".equalsIgnoreCase(langName)) {
            if (!new File(usrBin, "go").exists()) {
                return "golang";
            }
        } else if ("Rust".equalsIgnoreCase(langName)) {
            if (!new File(usrBin, "rustc").exists()) {
                return "rust";
            }
        } else if ("Kotlin".equalsIgnoreCase(langName)) {
            if (!new File(usrBin, "kotlinc").exists()) {
                return "kotlin";
            }
        } else if ("C#".equalsIgnoreCase(langName)) {
            if (!new File(usrBin, "mono").exists() && !new File(usrBin, "mcs").exists()) {
                return "mono";
            }
        } else if ("PHP".equalsIgnoreCase(langName)) {
            if (!new File(usrBin, "php").exists()) {
                return "php";
            }
        } else if ("Ruby".equalsIgnoreCase(langName)) {
            if (!new File(usrBin, "ruby").exists()) {
                return "ruby";
            }
        } else if ("Lua".equalsIgnoreCase(langName)) {
            if (!new File(usrBin, "lua").exists() && !new File(usrBin, "lua54").exists()) {
                return "lua54";
            }
        }
        return null;
    }

    private String getToolchainDisplayName(String langName, String fileName) {
        if ("C++".equalsIgnoreCase(langName) || "C".equalsIgnoreCase(langName)) return "Clang Compiler (C/C++)";
        if ("Java".equalsIgnoreCase(langName)) return "OpenJDK 21 (Java)";
        if ("Python".equalsIgnoreCase(langName)) return "Python 3 Runtime";
        if ("JavaScript".equalsIgnoreCase(langName) || "Node.js".equalsIgnoreCase(langName) || fileName.endsWith(".js") || fileName.endsWith(".ts")) return "Node.js Runtime";
        if ("Go".equalsIgnoreCase(langName)) return "Go (Golang)";
        if ("Rust".equalsIgnoreCase(langName)) return "Rust Compiler (rustc)";
        if ("Kotlin".equalsIgnoreCase(langName)) return "Kotlin Compiler";
        if ("C#".equalsIgnoreCase(langName)) return "C# Mono Compiler";
        if ("PHP".equalsIgnoreCase(langName)) return "PHP Interpreter";
        if ("Ruby".equalsIgnoreCase(langName)) return "Ruby Interpreter";
        if ("Lua".equalsIgnoreCase(langName)) return "Lua Interpreter";
        return langName + " Toolchain";
    }

    private String getLibraryKey(String langName, String fileName) {
        if ("C++".equalsIgnoreCase(langName) || "C".equalsIgnoreCase(langName)) return "c_cpp";
        if ("Java".equalsIgnoreCase(langName)) return "java";
        if ("Python".equalsIgnoreCase(langName)) return "python";
        if ("JavaScript".equalsIgnoreCase(langName) || "Node.js".equalsIgnoreCase(langName) || fileName.endsWith(".js") || fileName.endsWith(".ts")) return "nodejs";
        if ("Go".equalsIgnoreCase(langName)) return "go";
        if ("Rust".equalsIgnoreCase(langName)) return "rust";
        if ("Kotlin".equalsIgnoreCase(langName)) return "kotlin";
        if ("C#".equalsIgnoreCase(langName)) return "csharp";
        if ("PHP".equalsIgnoreCase(langName)) return "php";
        if ("Ruby".equalsIgnoreCase(langName)) return "ruby";
        if ("Lua".equalsIgnoreCase(langName)) return "lua";
        return langName.toLowerCase();
    }

    private void executeInTermux(String workingDir, String fileName, File localFile) {
        LanguageRunner runner = runManager.getRunnerForFile(fileName);
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String binDir = "$HOME/.bin_cache";
        String outputBinary = binDir + "/" + baseName + ".out";
        String langName = runner.getLanguageName();

        // 0. Auto-clean expired binary artifacts (>24h)
        com.example.util.BinaryCacheManager.cleanupExpiredBinaries(this);

        // 1. Special handling for HTML / Web Server & Live In-App Preview
        if ("HTML".equalsIgnoreCase(langName) || fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            handleHtmlExecution(workingDir, fileName, localFile);
            return;
        }

        // 2. Special handling for Full-Stack Node.js Projects
        if (new File(workingDir, "package.json").exists()) {
            File usrBin = new File(getFilesDir(), "usr/bin");
            if (!new File(usrBin, "node").exists()) {
                termuxEnv.clearAptLocks();
                binding.terminalView.appendOutput("\n\u001B[33m⚠️ Node.js is required for full-stack projects, but is not installed.\u001B[0m\n");
                showToolchainInstallDialog("nodejs", () -> {
                    executeInTermux(workingDir, fileName, localFile);
                });
                return;
            }
            StringBuilder fsCmd = new StringBuilder();
            if (!new File(workingDir, "node_modules").exists()) {
                fsCmd.append("npm install && ");
            }
            fsCmd.append("npm start");
            binding.terminalView.appendOutput("\n\u001B[32m🚀 Running Full-Stack Project (npm start)...\u001B[0m\n");
            runTerminalCommand(fsCmd.toString(), workingDir);
            return;
        }

        // 3. Check if required compiler/runtime is installed; if missing, install from TermCode GitHub Releases CDN
        String missingPkg = getMissingToolchainPackage(langName, fileName);
        if (missingPkg != null) {
            termuxEnv.clearAptLocks();
            String toolName = getToolchainDisplayName(langName, fileName);
            String libKey = getLibraryKey(langName, fileName);
            binding.terminalView.appendOutput("\n\u001B[33m⚠️ " + toolName + " is not installed yet!\u001B[0m\n");
            showToolchainInstallDialog(libKey, () -> {
                executeInTermux(workingDir, fileName, localFile);
            });
            return;
        }

        String runFile = fileName;
        String runOutput = outputBinary;

        // 4. Smart Java class name synchronization
        if ("Java".equalsIgnoreCase(langName)) {
            String editorText = binding.codeEditor.getText().toString();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("public\\s+class\\s+([A-Za-z0-9_]+)").matcher(editorText);
            if (m.find()) {
                String publicClass = m.group(1);
                String expectedName = publicClass + ".java";
                if (!fileName.equals(expectedName)) {
                    File properFile = new File(workingDir, expectedName);
                    try {
                        java.nio.file.Files.write(properFile.toPath(), editorText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        runFile = expectedName;
                        runOutput = binDir + "/" + publicClass + ".out";
                        binding.terminalView.appendOutput("\u001B[33mℹ️ Java public class '" + publicClass + "' matched: syncing '" + expectedName + "'\u001B[0m\n");
                    } catch (Exception ignored) {}
                } else {
                    runOutput = binDir + "/" + publicClass + ".out";
                }
            }
        }

        // 5. Toolchain verified -> Compile and Execute directly
        StringBuilder cmd = new StringBuilder();
        if (runner.getCompileCommand(runFile, runOutput) != null) {
            cmd.append(runner.getCompileCommand(runFile, runOutput))
                    .append(" && ")
                    .append(runner.getRunCommand(runOutput));
        } else {
            cmd.append(runner.getRunCommand(runFile));
        }

        binding.terminalView.appendOutput("\u001B[36m⚙️ Compiling & Executing " + runFile + "...\u001B[0m\n");
        binding.terminalView.appendOutput("\u001B[32m$ " + cmd.toString() + "\u001B[0m\n");
        runTerminalCommand(cmd.toString(), workingDir);

        // Rebuild file tree after compilation (binary files like .out/.class are filtered out)
        binding.terminalView.postDelayed(() -> {
            if (explorerAdapter != null) {
                explorerAdapter.rebuildTree();
            }
        }, 1500);
    }

    private void handleHtmlExecution(String workingDir, String fileName, File localFile) {
        File usrBin = new File(getFilesDir(), "usr/bin");
        
        // 1. If Python is not installed, install it automatically via ToolchainInstaller from GitHub Releases CDN
        if (!new File(usrBin, "python").exists() && !new File(usrBin, "python3").exists()) {
            termuxEnv.clearAptLocks();
            binding.terminalView.appendOutput("\n\u001B[33m⚠️ Python is required to host the Local Web Server for HTML, CSS & JavaScript projects.\u001B[0m\n");
            showToolchainInstallDialog("python", () -> {
                handleHtmlExecution(workingDir, fileName, localFile);
            });
            return;
        }

        // 2. Python IS installed: start persistent background local server
        String startServerCmd = "pkill -9 -f 'http.server' 2>/dev/null || true; (nohup python -m http.server 8080 --bind 0.0.0.0 --directory \"" + workingDir + "\" </dev/null >/dev/null 2>&1 &)";
        binding.terminalView.appendOutput("\n\u001B[32m🌐 Localhost Web Server live at http://localhost:8080\u001B[0m\n");
        binding.terminalView.appendOutput("\u001B[36m📂 Serving folder: " + workingDir + "\u001B[0m\n");
        binding.terminalView.appendOutput("\u001B[32m$ " + startServerCmd + "\u001B[0m\n");
        runTerminalCommand(startServerCmd, workingDir);

        // 3. Open instant In-App Web Preview Dialog
        showWebPreviewDialog(workingDir, fileName, localFile);
    }

    private void showWebPreviewDialog(String workingDir, String fileName, File localFile) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_web_preview);

        TextView tvTitle = dialog.findViewById(R.id.tvPreviewTitle);
        TextView tvUrl = dialog.findViewById(R.id.tvPreviewUrl);
        ImageButton btnRefresh = dialog.findViewById(R.id.btnRefreshPreview);
        ImageButton btnClose = dialog.findViewById(R.id.btnClosePreview);
        ProgressBar progressWeb = dialog.findViewById(R.id.progressWeb);
        WebView webView = dialog.findViewById(R.id.webViewPreview);

        tvTitle.setText("Web Preview: " + fileName);
        String targetUrl = "http://localhost:8080/" + (fileName.equalsIgnoreCase("index.html") ? "" : fileName);
        tvUrl.setText(targetUrl);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressWeb.setVisibility(View.VISIBLE);
                    progressWeb.setProgress(newProgress);
                } else {
                    progressWeb.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                loadLocalHtmlDirectly(webView, workingDir, fileName, localFile);
            }
        });

        loadHtmlPreview(webView, workingDir, fileName, localFile, targetUrl);

        btnRefresh.setOnClickListener(v -> {
            loadHtmlPreview(webView, workingDir, fileName, localFile, targetUrl);
            Toast.makeText(this, "Refreshed Preview", Toast.LENGTH_SHORT).show();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(d -> webView.destroy());

        dialog.show();
    }

    private void loadHtmlPreview(WebView webView, String workingDir, String fileName, File localFile, String targetUrl) {
        if (localFile != null && localFile.exists()) {
            webView.loadUrl("file://" + localFile.getAbsolutePath());
        } else {
            String editorContent = binding.codeEditor.getText().toString();
            webView.loadDataWithBaseURL("file://" + workingDir + "/", editorContent, "text/html", "UTF-8", null);
        }
    }

    private void loadLocalHtmlDirectly(WebView webView, String workingDir, String fileName, File localFile) {
        if (localFile != null && localFile.exists()) {
            webView.loadUrl("file://" + localFile.getAbsolutePath());
        } else {
            String editorContent = binding.codeEditor.getText().toString();
            webView.loadDataWithBaseURL("file://" + workingDir + "/", editorContent, "text/html", "UTF-8", null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentFile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveCurrentFile();
        if (autoCompleteManager != null) {
            autoCompleteManager.dismiss();
        }
        if (terminalSession != null) {
            terminalSession.stop();
        }
    }

    @Override
    public void onBackPressed() {
        if (autoCompleteManager != null && autoCompleteManager.isShowing()) {
            autoCompleteManager.dismiss();
        } else if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (binding.terminalSection.getVisibility() == View.VISIBLE) {
            binding.terminalSection.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }
}
