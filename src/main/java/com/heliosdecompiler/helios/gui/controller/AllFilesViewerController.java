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

import com.cathive.fx.guice.GuiceFXMLLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.heliosdecompiler.helios.controller.files.OpenedFile;
import com.heliosdecompiler.helios.gui.controller.editors.EditorController;
import com.heliosdecompiler.helios.gui.model.FileTabProperties;
import com.heliosdecompiler.helios.gui.model.TreeNode;
import com.heliosdecompiler.helios.gui.view.editors.EditorView;
import com.heliosdecompiler.helios.gui.view.editors.StandardEditors;
import com.sun.javafx.scene.control.behavior.TabPaneBehavior;
import com.sun.javafx.scene.control.skin.TabPaneSkin;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class AllFilesViewerController extends NestedController<MainViewController> {

    @FXML
    private TabPane root;

    @Inject
    private GuiceFXMLLoader loader;

    @Inject
    private EditorController editorController;

    private Map<String, Tab> fileTabs = new HashMap<>();

    private boolean isMenuOpen = false;

    @Inject
    @Named(value = "mainStage")
    private Stage stage;

    @FXML
    public void initialize() {
        stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.T) {
                if (!isMenuOpen) {
                    openOpenNewTabMenu();
                }
            }
        });
        stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.W) {
                if (event.isShiftDown()) {
                    Tab fileTab = root.getSelectionModel().getSelectedItem();
                    if (fileTab != null) {
                        TabPane editorTabs = (TabPane) fileTab.getContent();
                        if (editorTabs.getSelectionModel().getSelectedItem() != null) {
                            TabPaneBehavior behavior = ((TabPaneSkin) editorTabs.getSkin()).getBehavior();
                            behavior.closeTab(editorTabs.getSelectionModel().getSelectedItem());
                        }
                    }
                } else {
                    Tab fileTab = root.getSelectionModel().getSelectedItem();
                    if (fileTab != null) {
                        TabPaneBehavior behavior = ((TabPaneSkin) root.getSkin()).getBehavior();
                        behavior.closeTab(fileTab);
                    }
                }
            }
        });
    }

    public void openOpenNewTabMenu() {
        if (root.getSelectionModel().getSelectedItem() != null) {
            Tab selectedFile = root.getSelectionModel().getSelectedItem();
            ContextMenu contextMenu = doOpenMenu((TabPane) selectedFile.getContent());
            Point p = MouseInfo.getPointerInfo().getLocation();
            contextMenu.show(root.getScene().getWindow(), p.x, p.y);
        }
    }

    public ContextMenu doOpenMenu(TabPane target) {
        isMenuOpen = true;
        ContextMenu contextMenu = new ContextMenu();
        for (EditorView ev : editorController.getRegisteredEditors()) {
            MenuItem item = new MenuItem(ev.getDisplayName());
            item.setOnAction(event -> {
                openNewEditor(target, ev);
            });
            contextMenu.getItems().add(item);
        }
        contextMenu.setOnHidden(event -> {
            isMenuOpen = false;
        });
        return contextMenu;
    }

    public boolean handleClick(TreeNode value) {
        if (value.testFlag(OpenedFile.IS_LEAF)) {
            constructFileTab(value);
            return true;
        }
        return false;
    }

    private void constructFileTab(TreeNode node) {
        String uniqueKey = generateKey(node);

        if (fileTabs.containsKey(uniqueKey)) {
            // Already opened, and you double clicked. Let's just bring the focus to it

            Tab pane = fileTabs.get(uniqueKey);
            root.getSelectionModel().select(pane);
            return;
        }

        OpenedFile file = (OpenedFile) node.getMetadata().get(OpenedFile.OPENED_FILE);

        EditorView defaultTransformer = StandardEditors.HEX;

//        String extension = node.getDisplayName();
//        if (extension.lastIndexOf('.') != -1) {
//            extension = extension.substring(extension.lastIndexOf('.') + 1, extension.length());
//        }
//        JsonValue star = Settings.FILETYPE_ASSOCIATIONS.get().asObject().get(".*");
//        for (JsonObject.Member member : Settings.FILETYPE_ASSOCIATIONS.get().asObject()) {
//            if (member.getName().equals(extension)) {
//                if (defaultTransformer == null) {
//                    defaultTransformer = Transformer.getById(member.getValue().asString());
//                }
//            }
//        }
//        if (defaultTransformer == null && star != null) {
//            defaultTransformer = Transformer.getById(star.asString());
//        }
//        if (defaultTransformer == null) {
//            defaultTransformer = Transformer.HEX;
//        }

        try {
            GuiceFXMLLoader.Result tabResult = loader.load(getClass().getResource("/views/fileViewer.fxml"));
            TabPane fileTabPane = tabResult.getRoot();
            fileTabPane.setUserData(new FileTabProperties(file, (String) node.getMetadata().get(OpenedFile.FULL_PATH_KEY)));
            Tab allFilesTab = new Tab(node.getDisplayName());
            allFilesTab.setContent(fileTabPane);
            allFilesTab.setOnClosed(event -> {
                fileTabs.remove(uniqueKey);
            });

            fileTabs.put(uniqueKey, allFilesTab);

            root.getTabs().add(allFilesTab);
            root.getSelectionModel().select(allFilesTab);

            openNewEditor(fileTabPane, defaultTransformer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openNewEditor(TabPane filePane, EditorView editor) {
        FileTabProperties properties = (FileTabProperties) filePane.getUserData();
        if (properties.getOpenedEditors().containsKey(editor.getDisplayName())) {
            Tab tab = properties.getOpenedEditors().get(editor.getDisplayName());
            filePane.getSelectionModel().select(tab);
            return;
        }
        Tab editorTab = new Tab(editor.getDisplayName());
        editorTab.setContent(editor.createView(properties.getFile(), properties.getPath()));
        editorTab.setOnClosed(event -> {
            properties.getOpenedEditors().remove(editor.getDisplayName());
        });
        filePane.getTabs().add(editorTab);
        filePane.getSelectionModel().select(editorTab);
        properties.getOpenedEditors().put(editor.getDisplayName(), editorTab);
    }

    private String generateKey(TreeNode node) {
        OpenedFile file = (OpenedFile) node.getMetadata().get(OpenedFile.OPENED_FILE);

        return file.getTarget().toString() + "\0" + node.getMetadata().get(OpenedFile.FULL_PATH_KEY); // should be unique cuz nothing can use \0 in filename (right?)
    }

    public void clear() {
        root.getTabs().clear();
        fileTabs.clear();
    }
}
