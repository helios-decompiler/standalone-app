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

import com.heliosdecompiler.helios.controller.editors.decompilers.DecompilerController;
import com.heliosdecompiler.helios.controller.files.OpenedFile;
import javafx.embed.swing.SwingNode;
import javafx.scene.Node;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;

public class DecompilerView extends EditorView {

    private DecompilerController<?> controller;

    public DecompilerView(DecompilerController<?> controller) {
        this.controller = controller;
    }

    @Override
    public Node createView(OpenedFile file, String path) {
        RSyntaxTextArea area = new RSyntaxTextArea(); // todo migrate back to clickable
        area.getCaret().setSelectionVisible(true);
        SwingUtilities.invokeLater(() -> {
            area.setText("Decompiling... this may take a while");
            area.discardAllEdits();
        });

        RTextScrollPane scrollPane = new RTextScrollPane(area);
        scrollPane.setLineNumbersEnabled(true);
        scrollPane.setFoldIndicatorEnabled(true);

        SwingNode swingNode = new SwingNode();
        swingNode.setContent(scrollPane);

        controller.decompile(file, path, (success, text) -> {
            SwingUtilities.invokeLater(() -> {
                area.setText(text);
                area.discardAllEdits();
                if (success) {
                    area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                    area.setCodeFoldingEnabled(true);
                }
                area.setCaretPosition(0);
            });
        });
        return swingNode;
    }

    @Override
    public String getDisplayName() {
        return this.controller.getName();
    }
}
