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

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.heliosdecompiler.helios.Message;
import com.heliosdecompiler.helios.Settings;
import com.heliosdecompiler.helios.api.events.PathUpdatedEvent;
import com.heliosdecompiler.helios.controller.PathController;
import com.heliosdecompiler.helios.gui.view.JavaFXFileChooserView;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.apache.commons.configuration2.Configuration;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PathEditorController {

    @FXML
    private HBox root;

    @FXML
    private ListView<String> list;

    @Inject
    private Configuration configuration;

    @Inject
    private EventBus eventBus;

    @Inject
    private PathController pathController;

    private Stage stage;
    private volatile boolean firstRun = true;

    @FXML
    private void initialize() {
        this.list.setCellFactory(TextFieldListCell.forListView());

        List<String> path = configuration.getList(String.class, Settings.PATH_KEY, Collections.emptyList());
        this.list.getItems().addAll(path);

        stage = new Stage();
        stage.setOnCloseRequest(event -> {
            event.consume();
            stage.hide();
            this.list.getItems().removeAll(Arrays.asList(null, ""));
            configuration.setProperty(Settings.PATH_KEY, this.list.getItems());
            eventBus.post(new PathUpdatedEvent());
            pathController.reload();
        });
        stage.setScene(new Scene(root));
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/res/icon.png")));
        stage.setTitle("Edit Path");
    }

    public void open() {
        stage.show();
        stage.requestFocus();
        if (firstRun) {
            firstRun = false;
            stage.setMinWidth(stage.getWidth());
            stage.setMinHeight(stage.getHeight());
        }
    }

    @FXML
    private void cancelEdit(ListView.EditEvent<String> event) {
        list.getItems().removeAll(Collections.singleton(""));
    }

    @FXML
    private void commitEdit(ListView.EditEvent<String> event) {
        list.getItems().set(event.getIndex(), event.getNewValue());
    }

    @FXML
    private void add(MouseEvent event) {
        if (list.getEditingIndex() != -1) {
            list.edit(-1);
            list.scrollTo(list.getItems().size() - 1);
            list.layout();
        }
        list.getItems().add("");
        list.scrollTo(list.getItems().size() - 1);
        list.layout();
        list.getSelectionModel().select(list.getItems().size() - 1);
        list.edit(list.getItems().size() - 1);
    }

    @FXML
    private void edit(MouseEvent event) {
        int sel = list.getSelectionModel().getSelectedIndex();
        if (sel != -1) {
            list.edit(sel);
        }
    }

    @FXML
    private void browse(MouseEvent event) {
        int sel = list.getSelectionModel().getSelectedIndex();
        if (sel != -1) {
            String item = list.getItems().get(sel);
            File file = new File(item);

            File newFile = new JavaFXFileChooserView(this.stage)
                    .withInitialFile(file)
                    .withTitle(Message.GENERIC_OPEN.format())
                    .promptSingle();

            if (newFile != null) {
                list.getItems().set(sel, newFile.getAbsolutePath());
            }
            this.list.requestFocus();
        }
    }

    @FXML
    private void delete(MouseEvent event) {
        int selected = list.getSelectionModel().getSelectedIndex();
        if (selected != -1) {
            list.getItems().remove(selected);
            if (selected >= list.getItems().size()) {
                selected = list.getItems().size() - 1;
            }
            list.getSelectionModel().select(selected);
            this.list.requestFocus();
        }
    }

    @FXML
    private void up(MouseEvent event) {
        int selected = list.getSelectionModel().getSelectedIndex();
        if (selected != -1) {
            if (selected > 0) {
                Collections.swap(list.getItems(), selected, selected - 1);
                this.list.getSelectionModel().select(selected - 1);
            } else {
                this.list.getSelectionModel().select(0);
            }
            this.list.requestFocus();
        }
    }

    @FXML
    private void down(MouseEvent event) {
        int selected = list.getSelectionModel().getSelectedIndex();
        if (selected != -1) {
            if (selected < list.getItems().size() - 1) {
                Collections.swap(list.getItems(), selected, selected + 1);
                this.list.getSelectionModel().select(selected + 1);
            } else {
                this.list.getSelectionModel().select(this.list.getItems().size() - 1);
            }
            this.list.requestFocus();
        }
    }
}
