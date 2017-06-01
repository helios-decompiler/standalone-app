/*
 * Copyright 2017 Sam Sun <github-contact@samczsun.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.heliosdecompiler.helios.gui.view.editors;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.heliosdecompiler.helios.Message;
import com.heliosdecompiler.helios.Settings;
import com.heliosdecompiler.helios.controller.ProcessController;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTask;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTaskHelper;
import com.heliosdecompiler.helios.controller.files.OpenedFile;
import com.heliosdecompiler.helios.controller.transformers.disassemblers.DisassemblerController;
import com.heliosdecompiler.helios.controller.transformers.disassemblers.KrakatauDisassemblerController;
import com.heliosdecompiler.helios.ui.MessageHandler;
import com.heliosdecompiler.transformerapi.StandardTransformers;
import com.heliosdecompiler.transformerapi.TransformationException;
import com.heliosdecompiler.transformerapi.TransformationResult;
import com.heliosdecompiler.transformerapi.assemblers.krakatau.KrakatauAssemblerSettings;
import com.heliosdecompiler.transformerapi.common.krakatau.KrakatauException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ScrollEvent;
import org.apache.commons.configuration2.Configuration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.File;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DisassemblerView extends EditorView {

    @Inject
    private Configuration configuration;

    @Inject
    private ProcessController processController;

    @Inject
    private BackgroundTaskHelper backgroundTaskHelper;

    @Inject
    private MessageHandler messageHandler;

    private DisassemblerController<?> controller;

    @Inject
    public DisassemblerView(
            @Assisted(value = "controller") DisassemblerController<?> controller
    ) {
        this.controller = controller;
        this.configuration = configuration;
        this.backgroundTaskHelper = backgroundTaskHelper;
        this.processController = processController;
    }

    @Override
    public boolean canSave() {
        return controller instanceof KrakatauDisassemblerController;
    }

    @Override
    public CompletableFuture<byte[]> save(Node node) {
        if (!(node instanceof CodeArea)) {
            return CompletableFuture.completedFuture(new byte[0]);
        }

        String assembledCode = ((CodeArea) node).getText();

        CompletableFuture<byte[]> future = new CompletableFuture<>();

        backgroundTaskHelper.submit(new BackgroundTask(Message.TASK_ASSEMBLE_FILE.format(node.getProperties().get("path").toString()), true, () -> {
            if (controller instanceof KrakatauDisassemblerController) {
                KrakatauAssemblerSettings settings = new KrakatauAssemblerSettings();
                settings.setPythonExecutable(new File(configuration.getString(Settings.PYTHON2_KEY)));
                settings.setProcessCreator(processController::launchProcess);

                try {
                    TransformationResult<byte[]> result = StandardTransformers.Assemblers.KRAKATAU.assemble(assembledCode, settings);
                    if (result.getTransformationData().size() == 1) {
                        future.complete(result.getTransformationData().values().iterator().next());
                    } else {
                        future.completeExceptionally(new KrakatauException(KrakatauException.Reason.UNKNOWN, result.getStdout(), result.getStderr()));
                    }
                } catch (TransformationException e) {
                    future.completeExceptionally(e);
                }
            } else {
                future.complete(new byte[0]);
            }
        }));

        return future;
    }

    @Override
    protected Node createView0(OpenedFile file, String path) {
        CodeArea codeArea = new CodeArea();

        if (controller instanceof KrakatauDisassemblerController) {
            ContextMenu contextMenu = new ContextMenu();

            MenuItem save = new MenuItem("Assemble");
            save.setOnAction(e -> {
                save(codeArea).whenComplete((res, err) -> {
                    if (err != null) {
                        if (err instanceof KrakatauException) {
                            StringBuilder message = new StringBuilder();
                            message.append("stdout:\r\n").append(((KrakatauException) err).getStdout())
                                    .append("\r\n\r\nstderr:\r\n").append(((KrakatauException) err).getStderr());

                            messageHandler.handleLongMessage(Message.ERROR_FAILED_TO_ASSEMBLE_KRAKATAU, message.toString());
                        } else {
                            messageHandler.handleException(Message.ERROR_UNKNOWN_ERROR.format(), err);
                        }
                    } else {
                        file.putContent(path, res);
                        messageHandler.handleMessage(Message.GENERIC_ASSEMBLED.format());
                    }
                });
            });

            contextMenu.getItems().add(save);
            codeArea.setContextMenu(contextMenu);
        }

        codeArea.setStyle("-fx-font-size: 1em");
        codeArea.getProperties().put("fontSize", 1);

        codeArea.setParagraphGraphicFactory(line -> {
            Node label = LineNumberFactory.get(codeArea, (digits) -> "%1$" + digits + "d").apply(line);
            label.styleProperty().bind(codeArea.styleProperty());
            return label;
        });
        codeArea.replaceText("Disassembling... this may take a while");
        codeArea.getUndoManager().forgetHistory();

        codeArea.richChanges()
                .filter(ch -> !ch.getInserted().equals(ch.getRemoved())) // XXX
                .successionEnds(Duration.ofMillis(500))
                .supplyTask(() -> computeHighlightingAsync(codeArea))
                .awaitLatest(codeArea.richChanges())
                .filterMap(t -> {
                    if (t.isSuccess()) {
                        return Optional.of(t.get());
                    } else {
                        t.getFailure().printStackTrace();
                        return Optional.empty();
                    }
                })
                .subscribe(f -> applyHighlighting(codeArea, f));
        codeArea.getStylesheets().add(getClass().getResource("/java-keywords.css").toExternalForm());

        codeArea.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isShortcutDown()) {
                if (e.getDeltaY() > 0) {
                    int size = (int) codeArea.getProperties().get("fontSize") + 1;
                    codeArea.setStyle("-fx-font-size: " + size + "em");
                    codeArea.getProperties().put("fontSize", size);
                } else {
                    int size = (int) codeArea.getProperties().get("fontSize") - 1;
                    if (size > 0) {
                        codeArea.setStyle("-fx-font-size: " + size + "em");
                        codeArea.getProperties().put("fontSize", size);
                    }
                }
                e.consume();
            }
        });

        controller.disassemble(file, path, (success, text) -> {
            Platform.runLater(() -> {
                codeArea.replaceText(text);
                codeArea.getUndoManager().forgetHistory();
            });
        });

        return new VirtualizedScrollPane<>(codeArea);
    }

    @Override
    public String getDisplayName() {
        return this.controller.getDisplayName();
    }


    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync(CodeArea codeArea) {
        String text = codeArea.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
            @Override
            protected StyleSpans<Collection<String>> call() throws Exception {
                return computeHighlighting(text);
            }
        };
        Executors.newSingleThreadExecutor().execute(task);
        return task;
    }

    private void applyHighlighting(CodeArea codeArea, StyleSpans<Collection<String>> highlighting) {
        codeArea.setStyleSpans(0, highlighting);
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while (matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                            matcher.group("PAREN") != null ? "paren" :
                                    matcher.group("BRACE") != null ? "brace" :
                                            matcher.group("BRACKET") != null ? "bracket" :
                                                    matcher.group("SEMICOLON") != null ? "semicolon" :
                                                            matcher.group("STRING") != null ? "string" :
                                                                    matcher.group("COMMENT") != null ? "comment" :
                                                                            null; /* never happens */
            assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }


    private static final String[] KEYWORDS = new String[]{
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );
}
