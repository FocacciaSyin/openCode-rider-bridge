package com.github.opencode.rider;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.SelectionEvent;
import com.intellij.openapi.editor.event.SelectionListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.Timer;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public final class OpencodeContextProjectService implements Disposable {
    private final Project project;
    private ClaudeIdeBridgeServer server;
    private ClaudeIdeLockFile lockFile;
    private Timer publishTimer;
    private boolean started;

    public OpencodeContextProjectService(Project project) {
        this.project = project;
    }

    public void start() {
        if (started) {
            return;
        }
        started = true;

        try {
            String basePath = project.getBasePath();
            if (basePath == null) {
                return;
            }

            int port = PortAllocator.findAvailablePort();
            String authToken = UUID.randomUUID().toString();
            server = new ClaudeIdeBridgeServer(port, authToken);
            server.start();
            lockFile = ClaudeIdeLockFile.create(
                Path.of(System.getProperty("user.home"), ".claude", "ide"),
                port,
                List.of(basePath),
                authToken
            );

            registerEditorListeners();
            scheduleSelectionPublish();
        } catch (Exception ignored) {
        }
    }

    private void registerEditorListeners() {
        EditorFactory.getInstance().getEventMulticaster().addSelectionListener(new SelectionListener() {
            @Override
            public void selectionChanged(@NotNull SelectionEvent event) {
                if (project.equals(event.getEditor().getProject())) {
                    scheduleSelectionPublish();
                }
            }
        }, this);

        EditorFactory.getInstance().getEventMulticaster().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent event) {
                if (project.equals(event.getEditor().getProject())) {
                    scheduleSelectionPublish();
                }
            }
        }, this);

        project.getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                scheduleSelectionPublish();
            }
        });
    }

    private void scheduleSelectionPublish() {
        if (publishTimer != null) {
            publishTimer.stop();
        }
        publishTimer = new Timer(150, event -> publishSelection());
        publishTimer.setRepeats(false);
        publishTimer.start();
    }

    private void publishSelection() {
        if (server == null) {
            return;
        }

        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return;
        }

        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (file == null) {
            return;
        }

        SelectionModel selection = editor.getSelectionModel();
        LogicalPosition start = selection.hasSelection()
            ? editor.offsetToLogicalPosition(selection.getSelectionStart())
            : editor.getCaretModel().getLogicalPosition();
        LogicalPosition end = selection.hasSelection()
            ? editor.offsetToLogicalPosition(selection.getSelectionEnd())
            : start;
        String text = selection.hasSelection() && selection.getSelectedText() != null ? selection.getSelectedText() : "";

        server.broadcastSelection(new SelectionPayload(file.getPath(), text, start.line, start.column, end.line, end.column));
    }

    @Override
    public void dispose() {
        if (publishTimer != null) {
            publishTimer.stop();
        }
        try {
            if (server != null) {
                server.stop(1000);
            }
        } catch (Exception ignored) {
        }
        try {
            if (lockFile != null) {
                lockFile.close();
            }
        } catch (Exception ignored) {
        }
    }
}
