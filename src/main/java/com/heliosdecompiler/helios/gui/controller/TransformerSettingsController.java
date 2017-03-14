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
import com.google.inject.Injector;
import com.heliosdecompiler.helios.controller.configuration.Setting;
import com.heliosdecompiler.helios.controller.transformers.BaseTransformerController;
import com.heliosdecompiler.helios.controller.transformers.TransformerController;
import com.heliosdecompiler.helios.gui.model.TreeNode;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.commons.configuration2.Configuration;

import java.util.HashMap;
import java.util.Map;

public class TransformerSettingsController {
    @FXML
    private HBox root;

    @FXML
    private TreeView<TreeNode> treeView;

    @FXML
    private VBox vbox;

    private TreeItem<TreeNode> rootNode;

    @Inject
    private TransformerController transformerController;

    @Inject
    private Configuration configuration;

    @Inject
    private EventBus eventBus;

    @Inject
    private Injector injector;

    private Map<BaseTransformerController<?>, ListView<Setting<?, ?>>> transformerSettingPanes = new HashMap<>();

    private Stage stage;
    private volatile boolean firstRun = true;

    @FXML
    private void initialize() {
        stage = new Stage();
        stage.setOnCloseRequest(event -> {
            event.consume();
            stage.hide();
        });
        stage.setScene(new Scene(root));
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/res/icon.png")));
        stage.setTitle("Transformer Settings");

        rootNode = new TreeItem<>(new TreeNode("[root]"));
        treeView.setRoot(rootNode);
        treeView.setCellFactory(new TreeCellFactory<>());

        transformerController.getTransformerControllers().forEach((type, classes) ->
        {
            TreeItem<TreeNode> settingsNode = new TreeItem<>(new TreeNode(rootNode.getValue(), type.getDisplayName()));
            rootNode.getChildren().add(settingsNode);
            for (Class<?> clazz : classes) {
                BaseTransformerController<?> baseTransformerController = (BaseTransformerController<?>) injector.getInstance(clazz);

                TreeItem<TreeNode> transformerNode = new TreeItem<>(new TreeNode(settingsNode.getValue(), baseTransformerController.getDisplayName()));
                transformerNode.getValue().getMetadata().put("instance", baseTransformerController);
                settingsNode.getChildren().add(transformerNode);

                ListView<Setting<?, ?>> pane = new ListView<>();
                pane.setCellFactory(new Callback<ListView<Setting<?, ?>>, ListCell<Setting<?, ?>>>() {
                    @Override
                    public ListCell<Setting<?, ?>> call(ListView<Setting<?, ?>> param) {
                        ListCell<Setting<?, ?>> cell = new ListCell<Setting<?, ?>>() {
                            @Override
                            protected void updateItem(Setting<?, ?> item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null) {
                                    setText(item.getId());
                                } else {
                                    setText("");
                                }
                            }
                        };
                        return cell;
                    }
                });
                transformerSettingPanes.put(baseTransformerController, pane);

                for (Setting<?, ?> setting : baseTransformerController.getSettings()) {
                    pane.getItems().add(setting);
                }
            }
        });
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
    public void onClickTreeItem(MouseEvent mouseEvent) {
        TreeNode selected = this.treeView.getSelectionModel().getSelectedItem().getValue();
        if (selected.getMetadata().containsKey("instance")) {
            BaseTransformerController<?> instance = (BaseTransformerController<?>) selected.getMetadata().get("instance");
            vbox.getChildren().clear();
            vbox.getChildren().add(transformerSettingPanes.get(instance));
            VBox.setVgrow(transformerSettingPanes.get(instance), Priority.ALWAYS);
        }
    }
}
