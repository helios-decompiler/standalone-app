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
import com.google.inject.Injector;
import com.heliosdecompiler.helios.controller.configuration.IntegerSetting;
import com.heliosdecompiler.helios.controller.configuration.Setting;
import com.heliosdecompiler.helios.controller.configuration.Troolean;
import com.heliosdecompiler.helios.controller.transformers.BaseTransformerController;
import com.heliosdecompiler.helios.controller.transformers.TransformerController;
import com.heliosdecompiler.helios.gui.model.TreeNode;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Callback;

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
    private Injector injector;

    private Map<BaseTransformerController<?>, ListView<Setting<?, ?>>> transformerSettingPanes = new HashMap<>();

    private Map<Class<?>, ConfigurationNode<?>> configurationNodes = new HashMap<>();

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
                initializeTransformer(baseTransformerController, settingsNode);
            }
        });

        configurationNodes.put(String.class, new ConfigurationNode<String>() {
            @Override
            public <SettingObject> Region createNode0(Setting<String, SettingObject> setting, BaseTransformerController<SettingObject> controller) {
                TextArea textArea = new TextArea();
                textArea.setPrefRowCount(1);
                textArea.textProperty().addListener((observable, oldValue, newValue) -> controller.setSettingValue(setting, newValue));
                textArea.setText(controller.getSettingValue(setting));
                return textArea;
            }
        });
        configurationNodes.put(Boolean.class, new ConfigurationNode<Boolean>() {
            @Override
            public <SettingObject> Region createNode0(Setting<Boolean, SettingObject> setting, BaseTransformerController<SettingObject> controller) {
                CheckBox checkBox = new CheckBox();
                checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                    controller.setSettingValue(setting, newValue);
                });
                checkBox.setSelected(controller.getSettingValue(setting));
                return checkBox;
            }
        });
        configurationNodes.put(Integer.class, new ConfigurationNode<Integer>() {
            @Override
            public <SettingObject> Region createNode0(Setting<Integer, SettingObject> setting, BaseTransformerController<SettingObject> controller) {
                IntegerSetting<?> integerSetting = (IntegerSetting<?>) setting;
                Spinner<Integer> spinner = new Spinner<>();
                spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(integerSetting.getMin(), integerSetting.getMax(), integerSetting.getStart(), integerSetting.getStep()));
                spinner.setEditable(true);
                spinner.getValueFactory().valueProperty().addListener((observable, oldValue, newValue) -> {
                    controller.setSettingValue(setting, newValue);
                });
                spinner.getValueFactory().setValue(controller.getSettingValue(setting));
                return spinner;
            }
        });
        configurationNodes.put(Troolean.class, new ConfigurationNode<Troolean>() {
            @Override
            public <SettingObject> Region createNode0(Setting<Troolean, SettingObject> setting, BaseTransformerController<SettingObject> controller) {
                ComboBox<String> comboBox = new ComboBox<>();
                comboBox.getItems().addAll("Neither", "True", "False");
                comboBox.getSelectionModel().select("Neither");
                comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                    controller.setSettingValue(setting, Troolean.parseTroolean(newValue));
                });
                comboBox.getSelectionModel().select(controller.getSettingValue(setting).getName());
                return comboBox;
            }
        });
    }

    private <SettingObject> void initializeTransformer(BaseTransformerController<SettingObject> baseTransformerController, TreeItem<TreeNode> settingsNode) {
        TreeItem<TreeNode> transformerNode = new TreeItem<>(new TreeNode(settingsNode.getValue(), baseTransformerController.getDisplayName()));
        transformerNode.getValue().getMetadata().put("instance", baseTransformerController);
        settingsNode.getChildren().add(transformerNode);

        ListView<Setting<?, SettingObject>> pane = new ListView<>();
        pane.setCellFactory(new Callback<ListView<Setting<?, SettingObject>>, ListCell<Setting<?, SettingObject>>>() {
            @Override
            public ListCell<Setting<?, SettingObject>> call(ListView<Setting<?, SettingObject>> param) {
                return new ListCell<Setting<?, SettingObject>>() {
                    @Override
                    protected void updateItem(Setting<?, SettingObject> item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            HBox hbox = new HBox();

                            VBox vBox1 = new VBox();
                            vBox1.setAlignment(Pos.CENTER_LEFT);
                            Label desc = new Label();
                            desc.setText(item.getDesc());
                            desc.setWrapText(true);
                            desc.setTextAlignment(TextAlignment.LEFT);
                            desc.setPadding(new Insets(5, 5, 5, 5));
                            vBox1.getChildren().add(desc);
                            vBox1.prefWidthProperty().bind(vbox.widthProperty().multiply(0.55));
                            vBox1.maxWidthProperty().bind(vbox.widthProperty().multiply(0.55));

                            Pane region = new Pane();

                            VBox vBox2 = new VBox();
                            vBox2.setAlignment(Pos.CENTER_RIGHT);
                            ConfigurationNode<?> configurationNode = getConfigurationNode(item.getClazz());
                            Region node = configurationNode.createNode(((Setting<Object, SettingObject>) item), baseTransformerController);
                            if (node instanceof Labeled) {
                                ((Labeled) node).setAlignment(Pos.CENTER_RIGHT);
                            }
                            transformerNode.getValue().getMetadata().put(item.getId(), node);
                            vBox2.getChildren().add(node);
                            vBox2.prefWidthProperty().bind(vbox.widthProperty().multiply(0.4));
                            vBox2.maxWidthProperty().bind(vbox.widthProperty().multiply(0.4));

                            hbox.getChildren().addAll(vBox1, region, vBox2);
                            HBox.setHgrow(vBox1, Priority.ALWAYS);
                            HBox.setHgrow(region, Priority.SOMETIMES);
                            HBox.setHgrow(vBox2, Priority.ALWAYS);
                            setGraphic(hbox);
                        }
                    }
                };
            }
        });

        ListView<Setting<?, ?>> widen = (ListView<Setting<?, ?>>) ((Object) pane);
        transformerSettingPanes.put(baseTransformerController, widen);

        for (Setting<?, SettingObject> setting : baseTransformerController.getSettings()) {
            pane.getItems().add(setting);
        }
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
        TreeItem<TreeNode> selectedItem = this.treeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null)
            return;

        TreeNode selected = selectedItem.getValue();
        if (selected.getMetadata().containsKey("instance")) {
            BaseTransformerController<?> instance = (BaseTransformerController<?>) selected.getMetadata().get("instance");
            vbox.getChildren().clear();
            vbox.getChildren().add(transformerSettingPanes.get(instance));
            VBox.setVgrow(transformerSettingPanes.get(instance), Priority.ALWAYS);
        }
    }

    private ConfigurationNode<?> getConfigurationNode(Class<?> clazz) {
        return configurationNodes.getOrDefault(clazz, configurationNodes.get(String.class));
    }

    private abstract class ConfigurationNode<SettingType> {
        public <SettingObject> Region createNode(Setting<Object, SettingObject> setting, BaseTransformerController<SettingObject> controller) {
            return createNode0((Setting<SettingType, SettingObject>) setting, controller);
        }

        abstract <SettingObject> Region createNode0(Setting<SettingType, SettingObject> setting, BaseTransformerController<SettingObject> controller);
    }
}
