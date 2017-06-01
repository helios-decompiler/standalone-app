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

package com.heliosdecompiler.helios.gui.view;

import com.heliosdecompiler.helios.Message;
import com.heliosdecompiler.helios.gui.helper.DialogHelper;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

public class LongMessagePopupView {

    private final Message message;
    private final Stage stage;

    private final String longMessage;

    public LongMessagePopupView(Stage stage, Message message, String longMessage) {
        this.stage = stage;
        this.message = message;
        this.longMessage = longMessage;
    }

    public void show() {
        get().show();
    }

    public void showAndWait() {
        get().showAndWait();
    }

    private Alert get() {
        Alert alert = new Alert(Alert.AlertType.ERROR);

        DialogHelper.enableClosing(alert);

        alert.getButtonTypes().clear();

        alert.getButtonTypes().add(ButtonType.OK);

        alert.setTitle(message.toString());
        alert.setHeaderText(null);
        alert.setContentText(message.name());
        alert.initOwner(stage);

        TextArea textArea = new TextArea(longMessage);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);

        alert.getDialogPane().setExpandableContent(expContent);

        return alert;
    }
}
