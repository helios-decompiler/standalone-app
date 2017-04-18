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
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.heliosdecompiler.helios.gui.model.CommonError;
import com.heliosdecompiler.helios.gui.model.Message;
import com.heliosdecompiler.helios.gui.view.*;
import com.heliosdecompiler.helios.handler.ExceptionHandler;
import com.heliosdecompiler.helios.ui.MessageHandler;
import com.heliosdecompiler.helios.ui.views.file.FileChooserView;
import com.sun.management.HotSpotDiagnosticMXBean;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.stage.Stage;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Singleton
public class JavaFXMessageHandler implements MessageHandler {

    @Inject
    @Named(value = "mainStage")
    private AtomicReference<Stage> stage;

    @FXML
    public void initialize() {
        ExceptionHandler.registerHandler(new Consumer<Throwable>() {
            private final String HOTSPOT_BEAN_NAME =
                    "com.sun.management:type=HotSpotDiagnostic";
            // field to store the hotspot diagnostic MBean
            private volatile HotSpotDiagnosticMXBean hotspotMBean;

            @Override
            public void accept(Throwable exception) {
                handleException(Message.UNKNOWN_ERROR, exception);
            }

//            private void sendErrorReport(Throwable throwable) {
//                initHotspotMBean();
//                Date date = new Date();
//                StringBuilder reportMessage = new StringBuilder();
//                reportMessage.append("Report generated on ").append(date).append(" (").append(System.currentTimeMillis()).append(")\n");
//                reportMessage.append("\n");
//                reportMessage.append("Error:\n");
//                StringWriter stringWriter = new StringWriter();
//                throwable.printStackTrace(new PrintWriter(stringWriter));
//                reportMessage.append(stringWriter.toString());
//                reportMessage.append("\n");
//                reportMessage.append("Helios Version: ").append(Constants.REPO_VERSION).append("\n");
//                reportMessage.append("Krakatau Verson: ").append(Constants.KRAKATAU_VERSION).append("\n");
//                reportMessage.append("Enjarify Version: ").append(Constants.ENJARIFY_VERSION).append("\n");
//                reportMessage.append("Enabled addons: ").append("\n");
//                for (Addon addon : AddonHandler.getAllAddons()) {
//                    reportMessage.append("\t").append(addon.getName()).append("\n");
//                }
//                reportMessage.append("\n");
//
//                List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
//                reportMessage.append("Input Arguments\n");
//                for (String arg : args) {
//                    reportMessage.append("\t").append(arg).append("\n");
//                }
//                reportMessage.append("\n");
//                reportMessage.append("sun.java.command ").append(System.getProperty("sun.java.command")).append("\n");
//                reportMessage.append("System properties\n");
//                for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
//                    reportMessage.append("\t").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
//                }
//                reportMessage.append("\n");
//                reportMessage.append("Diagnostic Options\n");
//                for (VMOption option : hotspotMBean.getDiagnosticOptions()) {
//                    reportMessage.append("\t").append(option.toString()).append("\n");
//                }
//                String to = "errorreport@heliosdecompiler.com";
//                String from = "heliosdecompilerclient@heliosdecompiler.com";
//                String host = "smtp.heliosdecompiler.com";
//                Properties properties = new Properties();
//                properties.setProperty("mail.smtp.host", host);
//                Session session = Session.getDefaultInstance(properties);
//
//                try {
//                    MimeMessage message = new MimeMessage(session);
//                    message.setFrom(new InternetAddress(from));
//                    message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
//                    message.setSubject("Error report");
//                    message.setText(reportMessage.toString());
//                    Transport.send(message);
//                } catch (MessagingException mex) {
////                    SWTUtil.showMessage("Could not send error report. " + mex.getMessage());
//                    mex.printStackTrace();
//                }
//            }

            // initialize the hotspot diagnostic MBean field
            private void initHotspotMBean() {
                if (hotspotMBean == null) {
                    synchronized (ExceptionHandler.class) {
                        if (hotspotMBean == null) {
                            hotspotMBean = getHotspotMBean();
                        }
                    }
                }
            }

            private HotSpotDiagnosticMXBean getHotspotMBean() {
                try {
                    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                    HotSpotDiagnosticMXBean bean =
                            ManagementFactory.newPlatformMXBeanProxy(server,
                                    HOTSPOT_BEAN_NAME, HotSpotDiagnosticMXBean.class);
                    return bean;
                } catch (RuntimeException re) {
                    throw re;
                } catch (Exception exp) {
                    throw new RuntimeException(exp);
                }
            }
        });
    }

    @Override
    public void handleError(CommonError.FormattedMessage message, Runnable after) {
        // In the future, this will be internationalized
        String formatted = message.getError() + " " + Arrays.toString(message.getArgs());

        Platform.runLater(() -> {
            ErrorPopupView view = new ErrorPopupView(stage.get()).withMessage(formatted);
            if (after != null) {
                view.showAndWait();
                after.run();
            } else {
                view.show();
            }
        });
    }

    @Override
    public void handleMessage(CommonError.FormattedMessage message, Runnable after) {
        // In the future, this will be internationalized
        String formatted = message.getError() + " " + Arrays.toString(message.getArgs());

        Platform.runLater(() -> {
            InfoPopupView view = new InfoPopupView(stage.get()).withMessage(formatted);
            if (after != null) {
                view.showAndWait();
                after.run();
            } else {
                view.show();
            }
        });
    }

    @Override
    public void prompt(CommonError.FormattedMessage message, Consumer<Boolean> consumer) {
        // In the future, this will be internationalized
        String formatted = message.getError() + " " + Arrays.toString(message.getArgs());

        Platform.runLater(() -> {
            consumer.accept(new PromptView(stage.get()).withMessage(formatted).show());
        });
    }

    @Override
    public FileChooserView chooseFile() {
        return new JavaFXFileChooserView(stage.get());
    }

    @Override
    public CompletableFuture<Void> handleLongMessage(Message shortMessage, String longMessage) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (Platform.isFxApplicationThread()) {
            new LongMessagePopupView(stage.get(), shortMessage, longMessage).show();
            future.complete(null);
        } else {
            Platform.runLater(() -> {
                new LongMessagePopupView(stage.get(), shortMessage, longMessage).show();
                future.complete(null);
            });
        }

        return future;
    }

    public void handleException(Message message, Throwable e) {
        e.printStackTrace();
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> {
                new ExceptionPopupView(stage.get(), message, e).show();
            });
        } else {
            new ExceptionPopupView(stage.get(), message, e).show();
        }
    }

    public void handleWarning(CommonError.FormattedMessage message, boolean wait) {
        // In the future, this will be internationalized
        String formatted = message.getError() + " " + Arrays.toString(message.getArgs());

        new WarningPopupView(stage.get()).withMessage(formatted).show();
    }
}
