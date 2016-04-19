/*
 * Copyright 2016 Sam Sun <me@samczsun.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.samczsun.helios.handler;

import com.samczsun.helios.Constants;
import com.samczsun.helios.Helios;
import com.samczsun.helios.api.Addon;
import com.samczsun.helios.handler.addons.AddonHandler;
import com.samczsun.helios.utils.SWTUtil;
import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;
import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.management.MBeanServer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ExceptionHandler {

    public static void handle(Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        Shell shell = SWTUtil.generateLongMessage("An error has occured", stringWriter.toString());
        Display display = Display.getDefault();
        display.syncExec(() -> {
            Composite composite = new Composite(shell, SWT.NONE);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));
            composite.setLayout(new FillLayout());
            Button send = new Button(composite, SWT.PUSH);
            send.setText("Send Error Report");
            Button dontsend = new Button(composite, SWT.PUSH);
            dontsend.setText("Don't Send (Not recommended)");
            send.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Helios.getBackgroundTaskHandler().submit(() -> sendErrorReport(exception));
                    shell.close();
                }
            });
            dontsend.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    shell.close();
                }
            });
            composite.pack();
            shell.pack();
            SWTUtil.center(shell);
            shell.open();
        });
    }

    /*
     * Please do not try to spam the email
     */
    private static void sendErrorReport(Throwable throwable) {
        initHotspotMBean();
        Date date = new Date();
        StringBuilder reportMessage = new StringBuilder();
        reportMessage.append("Report generated on ").append(date).append(" (").append(System.currentTimeMillis()).append(")\n");
        reportMessage.append("\n");
        reportMessage.append("Error:\n");
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        reportMessage.append(stringWriter.toString());
        reportMessage.append("\n");
        reportMessage.append("Helios Version: ").append(Constants.REPO_VERSION).append("\n");
        reportMessage.append("Krakatau Verson: ").append(Constants.KRAKATAU_VERSION).append("\n");
        reportMessage.append("Enjarify Version: ").append(Constants.ENJARIFY_VERSION).append("\n");
        reportMessage.append("Enabled addons: ").append("\n");
        for (Addon addon : AddonHandler.getAllAddons()) {
            reportMessage.append("\t").append(addon.getName()).append("\n");
        }
        reportMessage.append("\n");

        List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        reportMessage.append("Input Arguments\n");
        for (String arg : args) {
            reportMessage.append("\t").append(arg).append("\n");
        }
        reportMessage.append("\n");
        reportMessage.append("sun.java.command ").append(System.getProperty("sun.java.command")).append("\n");
        reportMessage.append("System properties\n");
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            reportMessage.append("\t").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
        reportMessage.append("\n");
        reportMessage.append("Diagnostic Options\n");
        for (VMOption option : hotspotMBean.getDiagnosticOptions()) {
            reportMessage.append("\t").append(option.toString()).append("\n");
        }
        String to = "errorreport@heliosdecompiler.com";
        String from = "heliosdecompilerclient@heliosdecompiler.com";
        String host = "smtp.heliosdecompiler.com";
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", host);
        Session session = Session.getDefaultInstance(properties);

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject("Error report");
            message.setText(reportMessage.toString());
            Transport.send(message);
        } catch (MessagingException mex) {
            SWTUtil.showMessage("Could not send error report. " + mex.getMessage());
        }
    }

    private static final String HOTSPOT_BEAN_NAME =
            "com.sun.management:type=HotSpotDiagnostic";

    // field to store the hotspot diagnostic MBean
    private static volatile HotSpotDiagnosticMXBean hotspotMBean;

    // initialize the hotspot diagnostic MBean field
    private static void initHotspotMBean() {
        if (hotspotMBean == null) {
            synchronized (ExceptionHandler.class) {
                if (hotspotMBean == null) {
                    hotspotMBean = getHotspotMBean();
                }
            }
        }
    }

    // get the hotspot diagnostic MBean from the
    // platform MBean server
    private static HotSpotDiagnosticMXBean getHotspotMBean() {
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
}
