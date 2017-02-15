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

package com.heliosdecompiler.helios.gui.helper;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.lang.reflect.Field;

public class DialogHelper {
    public static void enableClosing(Alert alert) {
        try {
            Field dialogField = Dialog.class.getDeclaredField("dialog");
            dialogField.setAccessible(true);

            Object dialog = dialogField.get(alert);
            Field stageField = dialog.getClass().getDeclaredField("stage");
            stageField.setAccessible(true);

            Stage stage = (Stage) stageField.get(dialog);
            stage.setOnCloseRequest(null);
            stage.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
                if (keyEvent.getCode() == KeyCode.ESCAPE) {
                    ((Button) alert.getDialogPane().lookupButton(ButtonType.OK)).fire();
                }
            });
        } catch (Exception ex) {
            // no point
            ex.printStackTrace();
        }
    }
}
