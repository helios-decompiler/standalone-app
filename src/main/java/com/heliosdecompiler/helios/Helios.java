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

package com.heliosdecompiler.helios;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.heliosdecompiler.helios.controller.PathController;
import com.heliosdecompiler.helios.controller.UpdateController;
import com.heliosdecompiler.helios.controller.files.OpenedFileController;
import com.heliosdecompiler.helios.controller.ui.UserInterfaceController;
import com.heliosdecompiler.helios.controller.ui.impl.UnsupportedUIController;
import com.heliosdecompiler.helios.controller.ui.impl.WindowsUIController;
import com.heliosdecompiler.helios.gui.JavaFXGraphicsProvider;
import com.heliosdecompiler.helios.ui.GraphicsProvider;
import com.heliosdecompiler.helios.ui.MessageHandler;
import com.heliosdecompiler.helios.utils.OSUtils;
import org.apache.commons.cli.*;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedBuilderParametersImpl;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class Helios {
    public static void main(String[] args) {
        GraphicsProvider launcher = getGraphicsProvider();

        try {
            Configuration configuration;
            try {
                configuration = loadConfiguration();
            } catch (ConfigurationException ex) {
                launcher.handleStartupError(Message.STARTUP_FAILED_TO_LOAD_CONFIGURATION, ex);
                System.exit(1);
                return;
            }

            launcher.startSplash();
            launcher.updateSplash(Message.STARTUP_PREPARING_ENVIRONMENT);

            // Force UTF-8 as charset if not already
            if (!Charset.defaultCharset().equals(StandardCharsets.UTF_8)) {
                try {
                    Field defaultCharset = Charset.class.getDeclaredField("defaultCharset");
                    defaultCharset.setAccessible(true);
                    defaultCharset.set(null, StandardCharsets.UTF_8);
                } catch (ReflectiveOperationException ex) {
                    launcher.handleStartupError(Message.STARTUP_BAD_CHARSET, ex);
                    System.exit(1);
                    return;
                }
                if (!Charset.defaultCharset().equals(StandardCharsets.UTF_8)) {
                    launcher.handleStartupError(Message.STARTUP_BAD_CHARSET, null);
                    System.exit(1);
                    return;
                }
            }

            if (!Constants.DATA_DIR.exists() && !Constants.DATA_DIR.mkdirs())
                throw new RuntimeException("Could not create data directory");

            Injector mainInjector = Guice.createInjector((Module) binder -> {
                binder.bind(MessageHandler.class).to(launcher.getMessageHandlerImpl());
                binder.bind(UserInterfaceController.class).to(getUIControllerImpl());
                binder.bind(Configuration.class).toInstance(configuration);
                binder.bind(EventBus.class).toInstance(new AsyncEventBus(Executors.newCachedThreadPool()));
            });

            launcher.updateSplash(Message.STARTUP_LOADING_GRAPHICS);
            launcher.prepare(mainInjector);

            launcher.updateSplash(Message.STARTUP_HANDLING_COMMANDLINE);
            handleCommandLine(args, mainInjector);

            launcher.updateSplash(Message.STARTUP_LOADING_PATH);
            mainInjector.getInstance(PathController.class).reloadSync();

            launcher.updateSplash(Message.STARTUP_DONE);
            launcher.start();

            mainInjector.getInstance(UpdateController.class).doUpdate();
        } catch (Throwable t) {
            launcher.handleStartupError(Message.STARTUP_UNEXPECTED_ERROR, t);
            System.exit(1);
            return;
        }
    }

    public static Class<? extends UserInterfaceController> getUIControllerImpl() {
        if (OSUtils.getOS() == OSUtils.OS.WINDOWS) {
            return WindowsUIController.class;
        }

        return UnsupportedUIController.class;
    }

    public static GraphicsProvider getGraphicsProvider() {
        if (System.getProperty("com.heliosdecompiler.standaloneapp.GraphicsProvider") != null) {
            try {
                return Class.forName(System.getProperty("com.heliosdecompiler.standaloneapp.GraphicsProvider")).asSubclass(GraphicsProvider.class).newInstance();
            } catch (Throwable ignored) {
            }
        }

        return new JavaFXGraphicsProvider();
    }

    private static Configuration loadConfiguration() throws ConfigurationException {
        if (!Constants.SETTINGS_FILE_XML.exists()) {
            new FileHandler(new XMLConfiguration()).save(Constants.SETTINGS_FILE_XML);
        }
        FileBasedConfigurationBuilder<XMLConfiguration> builder = new FileBasedConfigurationBuilder<>(XMLConfiguration.class).configure(new Parameters().xml().setFile(Constants.SETTINGS_FILE_XML));
        builder.setAutoSave(true);
        return builder.getConfiguration();
    }

    public static void handleCommandLine(String[] args, Injector injector) {
        Options options = new Options();
        options.addOption(
                Option.builder("o")
                        .longOpt("open")
                        .hasArg()
                        .desc("Open a file straight away")
                        .build()
        );
        try {
            List<File> open = new ArrayList<>();
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, args);
            if (commandLine.hasOption("open")) {
                for (String name : commandLine.getOptionValues("open")) {
                    File file = new File(name);
                    if (file.exists()) {
                        open.add(file);
                    }
                }
            }

            for (File file : open)
                injector.getInstance(OpenedFileController.class).openFileSync(file);

        } catch (ParseException e) {
            injector.getInstance(MessageHandler.class).handleException(Message.ERROR_UNKNOWN_ERROR.format(), e);
        }
    }
}