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
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class PromptView {

    private final Stage mainStage;
    private String headerText = null;
    private String message = null;

    public PromptView(Stage mainStage) {
        this.mainStage = mainStage;
    }

    public PromptView withMessage(String message) {
        this.message = message;
        return this;
    }

    public boolean show() {
        Alert alert = get();
        alert.showAndWait();
        return alert.getResult() == ButtonType.YES;
    }

    private Alert get() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Question!");
        alert.getButtonTypes().clear();
        alert.getButtonTypes().add(ButtonType.YES);
        alert.getButtonTypes().add(ButtonType.NO);
        alert.setHeaderText(headerText);
        alert.setContentText(message);
        alert.initOwner(mainStage);
        return alert;
    }
}
