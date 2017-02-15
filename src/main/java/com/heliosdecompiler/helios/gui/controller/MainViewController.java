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

import javafx.fxml.FXML;

public class MainViewController {
    @FXML
    private MenuBarController menuBarController;

    @FXML
    private FileTreeController fileTreeController;

    @FXML
    private AllFilesViewerController allFilesViewerController;

    @FXML
    private StatusBarController statusBarController;

    @FXML
    public void initialize() {
        menuBarController.setParentController(this);
        fileTreeController.setParentController(this);
        allFilesViewerController.setParentController(this);
        statusBarController.setParentController(this);
    }

    public FileTreeController getFileTreeController() {
        return fileTreeController;
    }

    public MenuBarController getMenuBarController() {
        return menuBarController;
    }

    public AllFilesViewerController getAllFilesViewerController() {
        return allFilesViewerController;
    }

    public StatusBarController getStatusBarController() {
        return statusBarController;
    }
}
