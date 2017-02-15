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

import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class ErrorPopupView {
    private final Stage mainStage;
    private String headerText = null;
    private String message = null;

    public ErrorPopupView(Stage mainStage) {
        this.mainStage = mainStage;
    }

    public ErrorPopupView withMessage(String message) {
        this.message = message;
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
        alert.setTitle("An error has occurred!");
        alert.setHeaderText(headerText);
        alert.setContentText(message);
        alert.initOwner(mainStage);
        return alert;
    }
}
