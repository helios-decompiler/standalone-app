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
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.ScrollEvent;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class TextView extends EditorView {
    @Override
    protected Node createView0(OpenedFile file, String path) {
        TextArea textArea = new TextArea();
        textArea.setWrapText(true);

        textArea.setStyle("-fx-font-size: 1em");
        textArea.getProperties().put("fontSize", 1);

        textArea.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isShortcutDown()) {
                if (e.getDeltaY() > 0) {
                    int size = (int) textArea.getProperties().get("fontSize") + 1;
                    textArea.setStyle("-fx-font-size: " + size + "em");
                    textArea.getProperties().put("fontSize", size);
                } else {
                    int size = (int) textArea.getProperties().get("fontSize") - 1;
                    if (size > 0) {
                        textArea.setStyle("-fx-font-size: " + size + "em");
                        textArea.getProperties().put("fontSize", size);
                    }
                }
                e.consume();
            }
        });

        textArea.setText(new String(file.getContent(path), StandardCharsets.UTF_8));

        ContextMenu newContextMenu = new ContextMenu();
        MenuItem save = new MenuItem("Save");
        save.setOnAction(e -> {
            save(textArea).whenComplete((res, err) -> {
                if (err != null) {
                    err.printStackTrace();
                } else {
                    file.putContent(path, res);
                }
            });
        });
        newContextMenu.getItems().add(save);
        textArea.setContextMenu(newContextMenu);

        return textArea;
    }

    @Override
    public CompletableFuture<byte[]> save(Node node) {
        if (!(node instanceof TextArea)) {
            return CompletableFuture.completedFuture(new byte[0]);
        }
        return CompletableFuture.completedFuture(((TextArea) node).getText().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String getDisplayName() {
        return "Text";
    }
}
