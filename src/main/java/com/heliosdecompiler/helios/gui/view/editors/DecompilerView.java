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

import com.heliosdecompiler.helios.controller.files.OpenedFile;
import com.heliosdecompiler.helios.controller.transformers.decompilers.DecompilerController;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.input.ScrollEvent;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecompilerView extends EditorView {

    private DecompilerController<?> controller;

    public DecompilerView(DecompilerController<?> controller) {
        this.controller = controller;
    }

    @Override
    public boolean canSave() {
        return false;
    }

    @Override
    protected Node createView0(OpenedFile file, String path) {
        CodeArea codeArea = new CodeArea();

        codeArea.setStyle("-fx-font-size: 1em");
        codeArea.getProperties().put("fontSize", 1);

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea, (digits) -> "%1$" + digits + "d"));
        codeArea.replaceText("Decompiling... this may take a while");
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

        controller.decompile(file, path, (success, text) -> {
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
