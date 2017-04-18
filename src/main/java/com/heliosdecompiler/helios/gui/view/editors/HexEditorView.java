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
import com.heliosdecompiler.helios.gui.model.CommonError;
import com.heliosdecompiler.helios.gui.model.Message;
import com.heliosdecompiler.hexeditor.HexArea;
import com.heliosdecompiler.transformerapi.common.krakatau.KrakatauException;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import java.util.concurrent.CompletableFuture;

public class HexEditorView extends EditorView {
    @Override
    protected Node createView0(OpenedFile file, String path) {
        HexArea editor = new HexArea();
        editor.setContent(file.getContent(path));

        ContextMenu newContextMenu = new ContextMenu();
        MenuItem save = new MenuItem("Save");
        save.setOnAction(e -> {
            save(editor).whenComplete((res, err) -> {
                if (err != null) {
                    err.printStackTrace();
                } else {
                    file.putContent(path, res);
                }
            });
        });
        newContextMenu.getItems().add(save);
        editor.setContextMenu(newContextMenu);

        return editor;
    }

    @Override
    public CompletableFuture<byte[]> save(Node node) {
        if (!(node instanceof HexArea)) {
            return CompletableFuture.completedFuture(new byte[0]);
        }
        return CompletableFuture.completedFuture(((HexArea) node).toByteArray());
    }

    @Override
    public String getDisplayName() {
        return "Hex";
    }
}
