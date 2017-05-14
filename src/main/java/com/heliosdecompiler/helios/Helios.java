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
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.heliosdecompiler.helios.controller.LanguageController;
import com.heliosdecompiler.helios.controller.PathController;
import com.heliosdecompiler.helios.controller.UpdateController;
import com.heliosdecompiler.helios.controller.files.OpenedFileController;
import com.heliosdecompiler.helios.controller.ui.UserInterfaceController;
import com.heliosdecompiler.helios.controller.ui.impl.UnsupportedUIController;
import com.heliosdecompiler.helios.ui.GraphicsProvider;
import com.heliosdecompiler.helios.ui.MessageHandler;
import com.heliosdecompiler.helios.utils.OSUtils;
import org.apache.commons.cli.*;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class Helios {
    public static void main(String[] args) {
        try {
            LanguageController languageController = new LanguageController(); // blehhhhhh
            Message.init(languageController);

            GraphicsProvider launcher = getGraphicsProvider().newInstance();

            launcher.startSplash();
            launcher.updateSplash(Message.STARTUP_PREPARING_ENVIRONMENT);

            Field defaultCharset = Charset.class.getDeclaredField("defaultCharset");
            defaultCharset.setAccessible(true);
            defaultCharset.set(null, StandardCharsets.UTF_8);
            if (!Charset.defaultCharset().equals(StandardCharsets.UTF_8))
                throw new RuntimeException("Charset: " + Charset.defaultCharset());
            if (!Constants.DATA_DIR.exists() && !Constants.DATA_DIR.mkdirs())
                throw new RuntimeException("Could not create data directory");
            if (!Constants.ADDONS_DIR.exists() && !Constants.ADDONS_DIR.mkdirs())
                throw new RuntimeException("Could not create addons directory");
            if (Constants.DATA_DIR.isFile())
                throw new RuntimeException("Data directory is file");
            if (Constants.ADDONS_DIR.isFile())
                throw new RuntimeException("Addons directory is file");

            EventBus eventBus = new AsyncEventBus(Executors.newCachedThreadPool());

            Configuration configuration = loadConfiguration();
            Class<? extends UserInterfaceController> uiController = getUIControllerImpl();

            Injector mainInjector = Guice.createInjector(
                    new AbstractModule() {
                        @Override
                        protected void configure() {
                            bind(MessageHandler.class).to(launcher.getMessageHandlerImpl());
                            bind(UserInterfaceController.class).to(uiController);
                            bind(Configuration.class).toInstance(configuration);
                            bind(EventBus.class).toInstance(eventBus);
                        }
                    }
            );

            mainInjector.getInstance(UserInterfaceController.class).initialize();

            launcher.updateSplash(Message.STARTUP_LOADING_GRAPHICS);
            launcher.prepare(mainInjector);

            launcher.updateSplash(Message.STARTUP_DONE);
            launcher.start();

            mainInjector.getInstance(PathController.class).reload();
            mainInjector.getInstance(UpdateController.class).doUpdate();
            handleCommandLine(args, mainInjector);
        } catch (Throwable t) {
            displayError(t);
            System.exit(1);
        }
    }

    public static Class<? extends GraphicsProvider> getGraphicsProvider() throws ClassNotFoundException {
        return Class.forName(System.getProperty("com.heliosdecompiler.standaloneapp.GraphicsProvider", "com.heliosdecompiler.helios.gui.JavaFXGraphicsProvider")).asSubclass(GraphicsProvider.class);
    }

    public static void displayError(Throwable t) {
        t.printStackTrace();
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        JOptionPane.showMessageDialog(null, writer.toString(), t.getClass().getSimpleName(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static Class<? extends UserInterfaceController> getUIControllerImpl() throws ClassNotFoundException {
        if (OSUtils.getOS() == OSUtils.OS.WINDOWS) {
            return Class.forName("com.heliosdecompiler.helios.controller.ui.impl.WindowsUIController").asSubclass(UserInterfaceController.class);
        }

        return UnsupportedUIController.class;
    }

    private static Configuration loadConfiguration() throws IOException, ConfigurationException {
        Configurations configurations = new Configurations();
        File file = Constants.SETTINGS_FILE_XML;
        if (!file.exists()) {
            XMLConfiguration tempConfiguration = new XMLConfiguration();
            new FileHandler(tempConfiguration).save(file);
        }
        FileBasedConfigurationBuilder<XMLConfiguration> builder = configurations.xmlBuilder(file);
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
                injector.getInstance(OpenedFileController.class).openFile(file);

        } catch (ParseException e) {
            injector.getInstance(MessageHandler.class).handleException(Message.ERROR_UNKNOWN_ERROR.format(), e);
        }
    }
}