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

package com.heliosdecompiler.helios.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class JavaFXSplashScreen extends Application {
    public static final CompletableFuture<Void> FINISHED_STARTUP_FUTURE = new CompletableFuture<>();
    public static final AtomicReference<JavaFXSplashScreen> INSTANCE = new AtomicReference<>();

    private Stage stage;
    private Label progressLabel;

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("Helios");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/res/icon.png")));

        Image logo = new Image(getClass().getResourceAsStream("/res/helios.png"));

        progressLabel = new Label("Starting...");

        VBox logoBox = new VBox();
        logoBox.getChildren().add(new ImageView(logo));
        logoBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(logoBox, Priority.ALWAYS);

        VBox textBox = new VBox();
        textBox.setAlignment(Pos.BOTTOM_CENTER);
        textBox.getChildren().add(progressLabel);
        VBox.setVgrow(textBox, Priority.NEVER);

        VBox mainVBox = new VBox(2);
        mainVBox.getChildren().add(logoBox);
        mainVBox.getChildren().add(textBox);

        BorderPane root = new BorderPane(mainVBox);
        BorderPane.setMargin(mainVBox, new Insets(10, 50, 10, 50));

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        primaryStage.show();

        // Center stage
        Rectangle2D rect = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(rect.getWidth() / 2 - (primaryStage.getWidth() / 2));
        primaryStage.setY(rect.getHeight() / 2 - (primaryStage.getHeight() / 2));

        INSTANCE.set(this);
        FINISHED_STARTUP_FUTURE.complete(null);
    }

    public void update(String message) {
        Platform.runLater(() -> progressLabel.setText(message));
    }

    public void hide() {
        stage.hide();
    }
}
