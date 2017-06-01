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
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.heliosdecompiler.helios.Message;
import com.heliosdecompiler.helios.gui.view.*;
import com.heliosdecompiler.helios.ui.MessageHandler;
import com.heliosdecompiler.helios.ui.views.file.FileChooserView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.stage.Stage;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Singleton
public class JavaFXMessageHandler implements MessageHandler {

    @Inject(optional = true)
    @Named(value = "mainStage")
    private Stage stage;

    @FXML
    public void initialize() {
    }

    @Override
    public void handleError(Message.FormattedMessage message, Runnable after) {
        Platform.runLater(() -> {
            ErrorPopupView view = new ErrorPopupView(stage).withMessage(message.getText());
            if (after != null) {
                view.showAndWait();
                after.run();
            } else {
                view.show();
            }
        });
    }

    @Override
    public void handleMessage(Message.FormattedMessage message, Runnable after) {
        Platform.runLater(() -> {
            InfoPopupView view = new InfoPopupView(stage).withMessage(message.getText());
            if (after != null) {
                view.showAndWait();
                after.run();
            } else {
                view.show();
            }
        });
    }

    @Override
    public void prompt(Message.FormattedMessage message, Consumer<Boolean> consumer) {
        Platform.runLater(() -> {
            consumer.accept(new PromptView(stage).withMessage(message.getText()).show());
        });
    }

    @Override
    public FileChooserView chooseFile() {
        return new JavaFXFileChooserView(stage);
    }

    @Override
    public CompletableFuture<Void> handleLongMessage(Message shortMessage, String longMessage) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (Platform.isFxApplicationThread()) {
            new LongMessagePopupView(stage, shortMessage, longMessage).show();
            future.complete(null);
        } else {
            Platform.runLater(() -> {
                new LongMessagePopupView(stage, shortMessage, longMessage).show();
                future.complete(null);
            });
        }

        return future;
    }

    public void handleException(Message.FormattedMessage message, Throwable e) {
        handleException(message, e, true);
    }

    public void handleException(Message.FormattedMessage message, Throwable e, boolean allowReport) {
        e.printStackTrace();
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> {
                new ExceptionPopupView(stage, message.getText(), e).setAllowReport(allowReport).show();
            });
        } else {
            new ExceptionPopupView(stage, message.getText(), e).setAllowReport(allowReport).show();
        }
    }

    public void handleWarning(Message.FormattedMessage message, boolean wait) {
        new WarningPopupView(stage).withMessage(message.getText()).show();
    }
}
