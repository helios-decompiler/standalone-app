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
import com.heliosdecompiler.helios.Constants;
import com.heliosdecompiler.helios.Message;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTask;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTaskHelper;
import com.heliosdecompiler.helios.ui.MessageHandler;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import org.controlsfx.control.StatusBar;

import java.io.IOException;

public class StatusBarController extends NestedController<MainViewController> {

    @FXML
    private StatusBar root;

    @FXML
    private Label backgroundLabel;

    @FXML
    private ProgressBar memUsage;

    @Inject
    private BackgroundTaskHelper backgroundTaskHelper;

    @Inject
    private GuiceFXMLLoader loader;

    @Inject
    private MessageHandler messageHandler;

    private BackgroundTaskController backgroundTaskController;

    @FXML
    private void initialize() {
        backgroundTaskHelper.visibleRunningInstances().addListener((ListChangeListener<? super BackgroundTask>) c -> {
            int tasks = backgroundTaskHelper.getTasks(false).size();
            Platform.runLater(() -> {
                backgroundLabel.setText(tasks + " background task" + (tasks == 1 ? "" : "s"));
            });
        });

        new Thread(() -> {
            while (true) {
                int used = Constants.USED_MEMORY.get();
                int total = Constants.TOTAL_MEMORY.get();
                Platform.runLater(() -> {
                    memUsage.setProgress(used * 1.0 / total);
                    memUsage.getTooltip().setText(used + "MB/" + total + "MB");
                });
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
        }, "Status Bar Updater").start();

        try {
            backgroundTaskController = loader.load(getClass().getResource("/views/backgroundTasks.fxml")).getController();
        } catch (IOException e) {
            messageHandler.handleException(Message.ERROR_UNKNOWN_ERROR.format(), e);
        }
    }

    @FXML
    private void onClickBackgroundLabel(MouseEvent event) {
        backgroundTaskController.show();
    }
}
