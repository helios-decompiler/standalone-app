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

import com.google.inject.Injector;
import com.google.inject.Module;
import com.heliosdecompiler.helios.ui.GuiLauncher;
import javafx.application.Application;

import java.util.List;

public class JavaFXGuiLauncher extends GuiLauncher {
    @Override
    public void start(Injector rootInjector, Runnable afterGui) {
        JavaFXApplication.rootInjector = rootInjector;
        JavaFXApplication.runAfter = afterGui;
        new Thread(() -> Application.launch(JavaFXApplication.class), "JavaFX GUI Launcher").start();
    }

    @Override
    public List<Module> getModules() {
        return JavaFXApplication.getModules();
    }
}
