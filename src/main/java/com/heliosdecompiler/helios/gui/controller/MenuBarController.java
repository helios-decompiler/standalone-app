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
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.heliosdecompiler.helios.Message;
import com.heliosdecompiler.helios.Settings;
import com.heliosdecompiler.helios.controller.ProcessController;
import com.heliosdecompiler.helios.controller.files.OpenedFileController;
import com.heliosdecompiler.helios.controller.ui.UserInterfaceController;
import com.heliosdecompiler.helios.ui.MessageHandler;
import com.heliosdecompiler.helios.ui.views.file.FileFilter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.apache.commons.configuration2.Configuration;

import java.io.File;
import java.io.IOException;

public class MenuBarController extends NestedController<MainViewController> {

    @FXML
    private MenuBar root;

    @Inject
    private EventBus eventBus;

    @Inject
    private MessageHandler messageHandler;

    @Inject
    private Configuration configuration;

    @Inject
    private GuiceFXMLLoader loader;

    private PathEditorController pathEditorController;
    private TransformerSettingsController transformerSettingsController;

    @Inject
    private OpenedFileController openedFileController;

    @Inject
    private UserInterfaceController userInterfaceController;

    @Inject
    private ProcessController processController;

    @Inject
    @Named(value = "mainStage")
    private Stage stage;

    @FXML
    private void initialize() {
        stage.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.O) {
                onOpen();
            }
        });
        stage.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.N) {
                onReset();
            }
        });
        stage.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.F5) {
                getParentController().getFileTreeController().reload();
            }
        });

        try {
            GuiceFXMLLoader.Result result = loader.load(getClass().getResource("/views/pathEditor.fxml"));
            pathEditorController = result.getController();
        } catch (IOException ex) {
            messageHandler.handleException(Message.ERROR_UNKNOWN_ERROR.format(), ex);
        }
        try {
            GuiceFXMLLoader.Result result = loader.load(getClass().getResource("/views/transformerSettings.fxml"));
            transformerSettingsController = result.getController();
        } catch (IOException ex) {
            messageHandler.handleException(Message.ERROR_UNKNOWN_ERROR.format(), ex);
        }

        // for mac (and maybe linux once java supports it)
        // todo refactor into UIController
        root.setUseSystemMenuBar(true);
    }

    @FXML
    private void openTransformerSettings(ActionEvent event) {
        transformerSettingsController.open();
    }

    @FXML
    private void setPython2(ActionEvent event) {
        File original = new File(configuration.getString(Settings.PYTHON2_KEY, "."));

        File selectedFile = messageHandler.chooseFile()
                .withTitle(Message.GENERIC_SELECT_FILE.format("Python 2.x"))
                .withInitialDirectory(original.isFile() ? original.getParentFile() : null)
                .promptSingle();

        if (selectedFile != null) {
            configuration.setProperty(Settings.PYTHON2_KEY, selectedFile);
        }
    }

    @FXML
    private void selectPath(ActionEvent event) {
        pathEditorController.open();
    }

    @FXML
    private void onNewClicked(ActionEvent event) {
        onReset();
    }

    @FXML
    private void onOpenClicked(ActionEvent event) {
        onOpen();
    }

    public void onOpen() {
        File lastDir = new File(configuration.getString(Settings.LAST_DIR_KEY, "."));

        File selectedFile = messageHandler.chooseFile()
                .withTitle(Message.GENERIC_OPEN.format())
                .withExtensionFilter(new FileFilter(Message.FILETYPE_JAVA_ARCHIVE_CLASS_FILE.format(), "*.jar", "*.class"), true)
                .withInitialDirectory(lastDir)
                .promptSingle();

        if (selectedFile != null) {
            configuration.setProperty(Settings.LAST_DIR_KEY, selectedFile.getParent());

            openedFileController.openFile(selectedFile);
        }
    }

    public void onReset() {
        messageHandler.prompt(Message.PROMPT_RESET_WORKSPACE.format(), result -> {
            if (result) {
                openedFileController.clear();
                processController.clear();
                getParentController().getAllFilesViewerController().clear();
            }
        });
    }

    @FXML
    public void onAddToContextMenu(ActionEvent actionEvent) {
        userInterfaceController.registerInContextMenu();
    }
}
