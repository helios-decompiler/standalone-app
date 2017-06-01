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

package com.heliosdecompiler.helios.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.heliosdecompiler.helios.ui.MessageHandler;
import com.sun.management.HotSpotDiagnosticMXBean;

import javax.management.MBeanServer;
import java.io.IOException;
import java.lang.management.ManagementFactory;

@Singleton
public class ErrorReportingHandler {
    private volatile HotSpotDiagnosticMXBean hotspotMBean;

    @Inject
    public ErrorReportingHandler(MessageHandler messageHandler) {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            hotspotMBean = ManagementFactory.newPlatformMXBeanProxy(server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
        } catch (IOException e) {
            // Should not happen, I think
            throw new RuntimeException(e);
        }
    }

    public void reportError(Throwable throwable) {
        // Nothing for now

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
    }
}
