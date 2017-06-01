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
import com.heliosdecompiler.helios.gui.controller.JavaFXMessageHandler;
import com.heliosdecompiler.helios.gui.view.ErrorPopupView;
import com.heliosdecompiler.helios.gui.view.ExceptionPopupView;
import com.heliosdecompiler.helios.ui.GraphicsProvider;
import com.heliosdecompiler.helios.ui.MessageHandler;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;

public class JavaFXGraphicsProvider extends GraphicsProvider {

    private JavaFXSplashScreen splashScreen;
    private JavaFXApplication application;

    @Override
    public void startSplash() {
        new Thread(() -> {
            Application.launch(JavaFXSplashScreen.class);
        }).start();
        JavaFXSplashScreen.FINISHED_STARTUP_FUTURE.join();
        splashScreen = JavaFXSplashScreen.INSTANCE.get();
    }

    @Override
    protected void setSplashMessage(String message) {
        splashScreen.update(message);
    }

    @Override
    public void prepare(Injector injector) {
        JavaFXApplication.ROOT_INJECTOR.set(injector);
        Platform.runLater(() -> {
            Application app = new JavaFXApplication();
            try {
                app.init();
                app.start(new Stage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        JavaFXApplication.FINISHED_STARTUP_FUTURE.join();
        application = JavaFXApplication.INSTANCE.get();
    }

    @Override
    public void start() {
        Platform.runLater(() -> {
            splashScreen.hide();
            application.show();
        });
        JavaFXApplication.FINISHED_SHOWING_FUTURE.join();
    }

    @Override
    public Class<? extends MessageHandler> getMessageHandlerImpl() {
        return JavaFXMessageHandler.class;
    }

    @Override
    protected void handleStartupError0(String message, Throwable error) {
        if (!isJavaFXInitialized()) {
            forceInitializeJavaFX();
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            if (error != null) {
                new ExceptionPopupView(null, message, error).setAllowReport(true).showAndWait();
            } else {
                new ErrorPopupView(null).withMessage(message).showAndWait();
            }
            countDownLatch.countDown();
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {
        }
    }

    private void forceInitializeJavaFX() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        new Thread(() -> PlatformImpl.startup(countDownLatch::countDown)).start();
        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {
        }
    }

    private boolean isJavaFXInitialized() {
        try {
            PlatformImpl.runLater(() -> {
            });
            return true;
        } catch (IllegalStateException ignored) {
            return false;
        }
    }
}
