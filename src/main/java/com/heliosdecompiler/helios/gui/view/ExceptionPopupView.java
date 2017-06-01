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

import com.heliosdecompiler.helios.gui.helper.DialogHelper;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionPopupView {

    private final String message;
    private final Throwable cause;
    private final Stage stage;

    private boolean allowReport = true;

    public ExceptionPopupView(Stage stage, String message, Throwable cause) {
        this.stage = stage;
        this.message = message;
        this.cause = cause;
    }

    public ExceptionPopupView disallowSendErrorReport() {
        this.allowReport = false;
        return this;
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

        if (allowReport)
            alert.getButtonTypes().add(new ButtonType("Send Error Report", ButtonBar.ButtonData.HELP));

        alert.getButtonTypes().add(ButtonType.OK);

        alert.setTitle("An exception has occurred");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        cause.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);

        return alert;
    }

    public ExceptionPopupView setAllowReport(boolean allowReport) {
        this.allowReport = allowReport;
        return this;
    }
}
