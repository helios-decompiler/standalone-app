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
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTask;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTaskHelper;
import com.heliosdecompiler.helios.gui.model.Message;
import com.heliosdecompiler.helios.ui.MessageHandler;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;


public class BackgroundTaskController {

    @FXML
    private ListView<HBox> root;

    @Inject
    private GuiceFXMLLoader loader;

    @Inject
    private MessageHandler messageHandler;

    private Stage stage;

    @Inject
    private BackgroundTaskHelper backgroundTaskHelper;

    private BiMap<BackgroundTask, HBox> nodes = HashBiMap.create();
    private volatile boolean firstRun = true;

    @FXML
    private void initialize() {
        stage = new Stage();
        stage.setScene(new Scene(root));
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/res/icon.png")));
        stage.setTitle("Helios - Background Tasks");
        stage.setOnCloseRequest(e -> {
            e.consume();
            stage.hide();
        });

        backgroundTaskHelper.visibleRunningInstances().addListener(new ListChangeListener<BackgroundTask>() {
            @Override
            public void onChanged(Change<? extends BackgroundTask> c) {
                if (c.next()) {
                    for (BackgroundTask task : c.getAddedSubList()) {
                        try {
                            GuiceFXMLLoader.Result result = loader.load(getClass().getResource("/views/backgroundTaskEntry.fxml"));
                            BackgroundTaskEntryController controller = result.getController();
                            controller.setParentController(BackgroundTaskController.this);
                            controller.setName(task.getDisplayName());
                            HBox root = result.getRoot();
                            nodes.put(task, root);

                            Platform.runLater(() -> {
                                BackgroundTaskController.this.root.getItems().add(root);
                                BackgroundTaskController.this.root.requestLayout();
                            });
                        } catch (IOException e) {
                            messageHandler.handleException(Message.UNKNOWN_ERROR, e);
                        }
                    }
                }

                for (BackgroundTask task : c.getRemoved()) {
                    HBox hbox = nodes.remove(task);
                    if (hbox != null) {
                        Platform.runLater(() -> {
                            root.getItems().remove(hbox);
                            BackgroundTaskController.this.root.requestLayout();
                        });
                    }
                }
            }
        });
    }

    public void cancel(HBox root) {
        BackgroundTask task = nodes.inverse().get(root);
        task.cancel();
    }

    public void show() {
        stage.show();
        stage.requestFocus();
        if (firstRun) {
            firstRun = false;
            stage.setMinWidth(stage.getWidth());
            stage.setMinHeight(stage.getHeight());
        }
    }
}
