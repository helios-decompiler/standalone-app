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
import com.heliosdecompiler.helios.handler.ExceptionHandler;
import javafx.embed.swing.SwingNode;
import javafx.scene.Node;
import org.fife.ui.hex.swing.HexEditor;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class HexEditorView extends EditorView {
    @Override
    public Node createView(OpenedFile file, String path) {
        final HexEditor editor = new HexEditor();
        try {
            editor.open(new ByteArrayInputStream(file.getContent(path)));
        } catch (IOException e1) {
            ExceptionHandler.handle(e1);
        }
        SwingNode swingNode = new SwingNode();
        swingNode.setContent(editor);
        return swingNode;
    }

    @Override
    public String getDisplayName() {
        return "Hex";
    }
}
