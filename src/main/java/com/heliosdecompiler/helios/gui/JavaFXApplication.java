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

import com.cathive.fx.guice.GuiceApplication;
import com.cathive.fx.guice.GuiceFXMLLoader;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.heliosdecompiler.helios.Helios;
import com.heliosdecompiler.helios.controller.UpdateController;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTask;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTaskHelper;
import com.heliosdecompiler.helios.gui.controller.JavaFXMessageHandler;
import com.heliosdecompiler.helios.ui.MessageHandler;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class JavaFXApplication extends GuiceApplication {
    static volatile Injector rootInjector;
    static volatile Runnable runAfter;
    private static AtomicReference<Stage> primaryStage = new AtomicReference<>();

    @Inject
    private GuiceFXMLLoader loader;

    @Inject
    private UpdateController updateController;

    static List<Module> getModules() {
        return Arrays.asList(
                binder -> binder.bind(new TypeLiteral<AtomicReference<Stage>>() {
                }).annotatedWith(Names.named("mainStage")).toInstance(primaryStage),
                binder -> binder.bind(MessageHandler.class).to(JavaFXMessageHandler.class)
        );
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // note: at this point everything in this class has been injected
            JavaFXApplication.primaryStage.set(primaryStage);
            primaryStage.setOnCloseRequest(event -> {
                Platform.exit();
                System.exit(0);
            });

            primaryStage.setTitle("Helios - v" + updateController.getVersion() + " | By samczsun");
            primaryStage.getIcons().add(new Image("/res/icon.png"));

            primaryStage.setScene(new Scene(loader.load(getClass().getResource("/views/main.fxml")).getRoot()));
            primaryStage.show();

            if (runAfter != null) {
                getInjector().getInstance(BackgroundTaskHelper.class).submit(new BackgroundTask("After", false, () -> {
                    runAfter.run();
                }));
            }
        } catch (Throwable t) {
            Helios.displayError(t);
            System.exit(1);
        }
    }

    @Override
    public void init(List<Module> list) throws Exception {
    }

    @Override
    protected Injector createInjector(Set<Module> modules) {
        return rootInjector.createChildInjector(modules);
    }
}
