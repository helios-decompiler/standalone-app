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

package com.heliosdecompiler.helios.gui.controller;

import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TabPane;

public class FileViewerController {

    @Inject
    private AllFilesViewerController allFilesViewerController;

    @FXML
    private TabPane root;

    @FXML
    public void initialize() {
        root.setOnContextMenuRequested(event -> {
            ContextMenu menu = allFilesViewerController.doOpenMenu(root);
            menu.show(root.getScene().getWindow(), event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }
}
