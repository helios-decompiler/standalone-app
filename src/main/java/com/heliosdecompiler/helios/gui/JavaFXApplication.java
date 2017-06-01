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
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.heliosdecompiler.helios.Helios;
import com.heliosdecompiler.helios.Message;
import com.heliosdecompiler.helios.controller.UpdateController;
import com.heliosdecompiler.helios.gui.controller.editors.DisassemblerViewFactory;
import com.heliosdecompiler.helios.gui.controller.editors.EditorController;
import com.heliosdecompiler.helios.gui.view.editors.DisassemblerView;
import com.heliosdecompiler.helios.ui.MessageHandler;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class JavaFXApplication extends GuiceApplication {
    public static final CompletableFuture<Void> FINISHED_STARTUP_FUTURE = new CompletableFuture<>();
    public static final CompletableFuture<Void> FINISHED_SHOWING_FUTURE = new CompletableFuture<>();
    public static final AtomicReference<JavaFXApplication> INSTANCE= new AtomicReference<>();

    /*public*/ static final AtomicReference<Injector> ROOT_INJECTOR = new AtomicReference<>();
    private static final AtomicReference<Stage> PRIMARY_STAGE = new AtomicReference<>();

    @Inject
    private GuiceFXMLLoader loader;

    @Inject
    private UpdateController updateController;

    public void show() {
        PRIMARY_STAGE.get().show();
        FINISHED_SHOWING_FUTURE.complete(null);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // note: at this point everything in this class has been injected
            JavaFXApplication.PRIMARY_STAGE.set(primaryStage);
            primaryStage.setOnCloseRequest(event -> {
                Platform.exit();
                System.exit(0);
            });

            primaryStage.setTitle("Helios - v" + updateController.getVersion() + " | By samczsun");
            primaryStage.getIcons().add(new Image("/res/icon.png"));

            primaryStage.setScene(new Scene(loader.load(getClass().getResource("/views/main.fxml")).getRoot()));

            // Special case
            getInjector().injectMembers(getInjector().getInstance(MessageHandler.class));

            INSTANCE.set(this);
            FINISHED_STARTUP_FUTURE.complete(null);
        } catch (Throwable t) {
            getInjector().getInstance(MessageHandler.class).handleException(Message.ERROR_UNKNOWN_ERROR.format(), t);
            System.exit(1);
        }
    }

    @Override
    public void init(List<Module> list) throws Exception {
        list.add(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Stage.class).annotatedWith(Names.named("mainStage")).toProvider(PRIMARY_STAGE::get);
                        bind(EditorController.class);
                    }
                }
        );
        list.add(new FactoryModuleBuilder()
                .implement(DisassemblerView.class, DisassemblerView.class)
                .build(DisassemblerViewFactory.class)
        );
    }

    @Override
    protected Injector createInjector(Set<Module> modules) {
        return ROOT_INJECTOR.get().createChildInjector(modules);
    }
}
