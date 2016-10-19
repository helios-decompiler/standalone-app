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

package com.heliosdecompiler.helios;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.google.common.collect.Sets;
import com.heliosdecompiler.helios.api.Addon;
import com.heliosdecompiler.helios.api.events.Events;
import com.heliosdecompiler.helios.api.events.requests.RecentFileRequest;
import com.heliosdecompiler.helios.api.events.requests.RefreshViewRequest;
import com.heliosdecompiler.helios.api.events.requests.SearchBarRequest;
import com.heliosdecompiler.helios.api.events.requests.TreeUpdateRequest;
import com.heliosdecompiler.helios.bootloader.BootSequence;
import com.heliosdecompiler.helios.bootloader.Splash;
import com.heliosdecompiler.helios.gui.GUI;
import com.heliosdecompiler.helios.gui.popups.BackgroundTaskPopup;
import com.heliosdecompiler.helios.handler.BackgroundTaskHandler;
import com.heliosdecompiler.helios.handler.ExceptionHandler;
import com.heliosdecompiler.helios.handler.addons.AddonHandler;
import com.heliosdecompiler.helios.tasks.AddFilesTask;
import com.heliosdecompiler.helios.utils.*;
import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.VMOption;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.management.MBeanServer;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Helios {
    private static Boolean python3Verified = null;
    private static Boolean javaRtVerified = null;
    private static GUI gui;
    private static BackgroundTaskHandler backgroundTaskHandler;
    private static BackgroundTaskPopup backgroundTaskPopup;
    private static LocalSocket socket;

    public static void main(String[] args, Shell shell, Splash splashScreen) {
        ExceptionHandler.registerHandler(new Consumer<Throwable>() {

            @Override
            public void accept(Throwable exception) {
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

            private void sendErrorReport(Throwable throwable) {
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
                    mex.printStackTrace();
                }
            }

            private final String HOTSPOT_BEAN_NAME =
                    "com.sun.management:type=HotSpotDiagnostic";

            // field to store the hotspot diagnostic MBean
            private volatile HotSpotDiagnosticMXBean hotspotMBean;

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
        splashScreen.updateState(BootSequence.LOADING_SETTINGS);
        Settings.loadSettings();
        backgroundTaskPopup = new BackgroundTaskPopup();
        backgroundTaskHandler = BackgroundTaskHandler.INSTANCE;
        splashScreen.updateState(BootSequence.LOADING_ADDONS);
        AddonHandler.registerPreloadedAddons();
        for (File file : Constants.ADDONS_DIR.listFiles()) {
            AddonHandler
                    .getAllHandlers()
                    .stream()
                    .filter(handler -> handler.accept(file))
                    .findFirst()
                    .ifPresent(handler -> {
                        handler.run(file);
                    });
        }
        splashScreen.updateState(BootSequence.SETTING_UP_GUI);
        gui = new GUI(shell);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Settings.saveSettings();
            getBackgroundTaskHandler().shutdown();
            ProcessUtils.destroyAll();
        }));
        try {
            socket = new LocalSocket();
        } catch (IOException e) { // Maybe allow the user to force open a second instance?
//            ExceptionHandler.handle(e);
        }

        handleCommandLine(args);

//        submitBackgroundTask(() -> {
        splashScreen.updateState(BootSequence.CREATING_TEMPORARY_FILES);
        Map<String, LoadedFile> newPath = new HashMap<>();
        for (String strFile : Sets.newHashSet(Settings.PATH.get().asString().split(";"))) {
            File file = new File(strFile);
            if (file.exists()) {
                LoadedFile loadedFile = new LoadedFile(file, true);
                newPath.put(loadedFile.getName(), loadedFile);
            }
        }
        File file = new File(Settings.RT_LOCATION.get().asString());
        if (file.exists()) {
            LoadedFile loadedFile = new LoadedFile(file, true);
            newPath.put(loadedFile.getName(), loadedFile);
        }

        FileManager.updatePath(newPath);
//        });
        
        splashScreen.updateState(BootSequence.COMPLETE);
        while (!splashScreen.isDisposed()) ;
        Display.getDefault().syncExec(() -> {
            shell.open();
            shell.setFocus();
            if (!shell.isFocusControl()) {
                shell.forceFocus();
            }
            shell.setActive();
            shell.forceActive();
        });
    }

    public static void handleCommandLine(String[] args) {
        List<File> open = new ArrayList<>();

        Options options = new Options();
        options.addOption(
                Option.builder("o")
                        .longOpt("open")
                        .hasArg()
                        .desc("Open a file straight away")
                        .build()
        );
        options.addOption(
                Option.builder("h")
                        .longOpt("help")
                        .desc("Help!")
                        .build()
        );
        options.addOption(
                Option.builder("ctx")
                        .longOpt("addtocontextmenu")
                        .desc("Run add to context menu on start")
                        .build()
        );
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, args);
            if (commandLine.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar bootstrapper.jar [options]", options);
            } else {
                if (commandLine.hasOption("addtocontextmenu")) {
                    addToContextMenu();
                }
                if (commandLine.hasOption("open")) {
                    for (String name : commandLine.getOptionValues("open")) {
                        File file = new File(name);
                        if (file.exists()) {
                            open.add(file);
                        }
                    }
                }
            }
        } catch (ParseException e) {
            ExceptionHandler.handle(e);
        }

        if (open.size() > 0) {
            openFiles(open.toArray(new File[open.size()]), true);
        }
    }

    public static void openFiles(final File[] files, final boolean recentFiles) {
        //TODO Loading everything as ClassNode async, handle thread safety, show popup if action requires loaded file but not completely loaded
        submitBackgroundTask(new AddFilesTask(files, recentFiles));
    }

    public static void resetWorkSpace(boolean ask) {
        if (ask && !SWTUtil.promptForYesNo(Constants.REPO_NAME + " - New Workspace",
                "You have not yet saved your workspace. Are you sure you wish to create a new one?")) {
            return;
        }
        FileManager.destroyAll();
        getGui().getTreeManager().reset();
        getGui().getClassManager().reset();
        ProcessUtils.destroyAll();
    }

    public static void addRecentFile(File f) {
        if (!f.getAbsolutePath().isEmpty()) {
            JsonArray array = Settings.RECENT_FILES.get().asArray();
            array.add(f.getAbsolutePath());
            while (array.size() > Settings.MAX_RECENTFILES.get().asInt()) {
                array.remove(0);
            }
            updateRecentFiles();
        }
    }

    public static void updateRecentFiles() {
        Events.callEvent(new RecentFileRequest(Settings.RECENT_FILES
                .get()
                .asArray()
                .values()
                .stream()
                .map(JsonValue::asString)
                .collect(Collectors.toList())));
    }

    public static void promptForFilesToOpen() {
        List<String> validExtensions = Arrays.asList("jar", "zip", "class", "apk", "dex");
        List<File> files1 = FileChooserUtil.chooseFiles(Settings.LAST_DIRECTORY.get().asString(), validExtensions,
                true);
        if (files1.size() > 0) {
            Settings.LAST_DIRECTORY.set(files1.get(0).getAbsolutePath());
            try {
                Helios.openFiles(files1.toArray(new File[files1.size()]), true);
            } catch (Exception e1) {
                ExceptionHandler.handle(e1);
            }
        }
    }

    public static void promptForRefresh() {
        if (SWTUtil.promptForYesNo(Constants.REPO_NAME + " - Refresh", "Are you sure you wish to refresh?")) {
            FileManager.resetAll();
            Events.callEvent(new TreeUpdateRequest());
        }
    }

    public static void promptForCustomPath() {
        SWTUtil.runTaskOnMainThread(() -> {
            Shell shell = new Shell(Display.getDefault(), SWT.ON_TOP | SWT.DIALOG_TRIM);
            shell.setLayout(new GridLayout());
            shell.setImage(Resources.ICON.getImage());
            shell.setText("File paths delimited by ;");
            Text text = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.WRAP);
            text.setText(Settings.PATH.get().asString());
            GridData gridData = new GridData(GridData.FILL_BOTH);
            gridData.heightHint = text.getLineHeight();
            gridData.widthHint = 512;
            text.setLayoutData(gridData);
            shell.addShellListener(new ShellAdapter() {
                @Override
                public void shellClosed(ShellEvent e) {
                    String oldPath = Settings.PATH.get().asString();
                    if (!oldPath.equals(text.getText())) {
                        Settings.PATH.set(text.getText());
                        Future<?> future = submitBackgroundTask(() -> {
                            Map<String, LoadedFile> newPath = new HashMap<>();
                            for (String strFile : Sets.newHashSet(Settings.PATH.get().asString().split(";"))) {
                                File file = new File(strFile);
                                if (file.exists()) {
                                    LoadedFile loadedFile = new LoadedFile(file, true);
                                    newPath.put(loadedFile.getName(), loadedFile);
                                }
                            }
                            FileManager.updatePath(newPath);
                        });
                    }
                }
            });
            text.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.keyCode == SWT.ESC || SWTUtil.isEnter(e.keyCode)) {
                        shell.close();
                    } else if (e.keyCode == 'a' && SWTUtil.isCtrl(e.stateMask)) {
                        text.selectAll();
                        e.doit = false;
                    }
                }
            });
            shell.pack();
            SWTUtil.center(shell);
            shell.open();
        }, true);
    }

    public static boolean ensurePython3Set() {
        return ensurePython3Set0(false);
    }

    public static boolean ensurePython2Set() {
        return SettingsValidator.ensurePython2Set(false).getType() == Result.Type.SUCCESS;
    }

    private static boolean ensurePython3Set0(boolean forceCheck) {
        String python3Location = Settings.PYTHON3_LOCATION.get().asString();
        if (python3Location.isEmpty()) {
            SWTUtil.showMessage("You need to set the location of the Python/PyPy 3.x executable", true);
            setLocationOf(Settings.PYTHON3_LOCATION);
            python3Location = Settings.PYTHON3_LOCATION.get().asString();
        }
        if (python3Location.isEmpty()) {
            return false;
        }
        if (python3Verified == null || forceCheck) {
            try {
                Process process = new ProcessBuilder(python3Location, "-V").start();
                String result = IOUtils.toString(process.getInputStream());
                String error = IOUtils.toString(process.getErrorStream());
                python3Verified = error.startsWith("Python 3") || result.startsWith("Python 3");
            } catch (Throwable t) {
                t.printStackTrace();
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                SWTUtil.showMessage(
                        "The Python 3.x executable is invalid." + Constants.NEWLINE + Constants.NEWLINE + sw.toString());
                python3Verified = false;
            }
        }
        return python3Verified;
    }

    public static void checkHotKey(Event e) {
        if (e.doit) {
            if (SWTUtil.isCtrl(e.stateMask)) {
                if (e.keyCode == 'o') {
                    promptForFilesToOpen();
                    e.doit = false;
                } else if (e.keyCode == 'n') {
                    resetWorkSpace(true);
                    e.doit = false;
                } else if (e.keyCode == 't') {
                    getGui().getClassManager().handleNewTabRequest();
                    e.doit = false;
                } else if (e.keyCode == 'w') {
                    if ((e.stateMask & SWT.SHIFT) == SWT.SHIFT) {
                        getGui().getClassManager().closeCurrentInnerTab();
                    } else {
                        getGui().getClassManager().closeCurrentTab();
                    }
                    e.doit = false;
                } else if (e.keyCode == 'f') {
                    System.out.println(getGui().getShell().getDisplay().getFocusControl().getShell());
                    Events.callEvent(new SearchBarRequest(true));
                    e.doit = false;
                }
            } else {
                if (e.keyCode == SWT.F5) {
                    if ((e.stateMask & SWT.SHIFT) == SWT.SHIFT) {
                        Events.callEvent(new RefreshViewRequest());
                    } else {
                        promptForRefresh();
                    }
                    e.doit = false;
                } else if (e.keyCode == SWT.ESC) {
                    SearchBarRequest closeRequest = new SearchBarRequest(false);
                    Events.callEvent(closeRequest);
                    if (closeRequest.wasSuccessful()) {
                        e.doit = false;
                    } else {
                        // Otherwise try closing other things
                    }
                }
            }
        }
    }

    public static Future<?> submitBackgroundTask(Runnable runnable) {
        return backgroundTaskHandler.submit(runnable);
    }

    public static BackgroundTaskHandler getBackgroundTaskHandler() {
        return backgroundTaskHandler;
    }

    /*
    These are to somewhat separate GUI and logic
     */

    public static GUI getGui() {
        return gui;
    }

    public static void setLocationOf(Settings setting) {
        String temp = setting.get().asString();
        String currentFile = temp == null || temp.isEmpty() ? System.getProperty("user.home") : temp;
        List<File> files = FileChooserUtil.chooseFiles(currentFile, Collections.emptyList(), false);
        if (!files.isEmpty()) {
            setting.set(files.get(0).getAbsolutePath());
            if (setting == Settings.PYTHON2_LOCATION) {
                SettingsValidator.ensurePython2Set(true);
            } else if (setting == Settings.PYTHON3_LOCATION) {
                ensurePython3Set0(true);
            } else if (setting == Settings.RT_LOCATION) {
                SettingsValidator.ensureJavaRtSet(true);
            }
        }
    }

    public static void addToContextMenu() {
        try {
            if (OSUtils.getOS() == OSUtils.OS.WINDOWS) {
                Process process = Runtime.getRuntime().exec("reg add HKCU\\Software\\Classes\\*\\shell\\helios\\command /f");
                process.waitFor();
                if (process.exitValue() == 0) {
                    process = Runtime.getRuntime().exec("reg add HKCU\\Software\\Classes\\*\\shell\\helios /ve /d \"Open with Helios\" /f");
                    process.waitFor();
                    if (process.exitValue() == 0) {
                        File currentJarLocation = getJarLocation();
                        if (currentJarLocation != null) {
                            File javaw = getJavawLocation();
                            if (javaw != null) {
                                process = Runtime.getRuntime().exec("reg add HKCU\\Software\\Classes\\*\\shell\\helios /v MultiSelectModel /d \"Single\" /f"); //Don't allow opening 2 at once... sorry
                                process.waitFor();
                                if (process.exitValue() == 0) {
                                    process = Runtime.getRuntime().exec("reg add HKCU\\Software\\Classes\\*\\shell\\helios\\command /ve /d \"\\\"" + javaw.getAbsolutePath() + "\\\" -jar \\\"" + currentJarLocation.getAbsolutePath() + "\\\" --open \\\"%1\\\"\" /f");
                                    process.waitFor();
                                    if (process.exitValue() == 0) {
                                        SWTUtil.showMessage("Done");
                                    } else {
                                        SWTUtil.showMessage("Failed to set context menu - 4");
                                    }
                                } else {
                                    SWTUtil.showMessage("Failed to set context menu - 3");
                                }
                            } else {
                                SWTUtil.showMessage("Could not set context menu - unable to find javaw.exe");
                            }
                        } else {
                            SWTUtil.showMessage("Could not set context menu - unable to find Helios.jar");
                        }
                    } else {
                        SWTUtil.showMessage("Failed to set context menu - 2");
                    }
                } else {
                    SWTUtil.showMessage("Failed to set context menu - 1");
                }
            } else {
                SWTUtil.showMessage("You may only do this on Windows");
            }
        } catch (Throwable t) {
            ExceptionHandler.handle(t);
        }
    }

    public static void relaunchAsAdmin(String arg) throws IOException, InterruptedException, URISyntaxException {
        File currentJarLocation = getJarLocation();
        File javawLocation = getJavawLocation();
        if (currentJarLocation == null) {
            SWTUtil.showMessage("Could not relaunch as admin - unable to find Helios.jar");
            return;
        }
        if (javawLocation == null) {
            SWTUtil.showMessage("Could not relaunch as admin - unable to find java(w)");
            return;
        }
        File tempVBSFile = File.createTempFile("tmpvbs", ".vbs");
        PrintWriter writer = new PrintWriter(tempVBSFile);
        writer.println("Set objShell = CreateObject(\"Wscript.Shell\")");
        writer.println("strPath = Wscript.ScriptFullName");
        writer.println("Set objFSO = CreateObject(\"Scripting.FileSystemObject\")");
        writer.println("Set objFile = objFSO.GetFile(strPath)");
        writer.println("strFolder = objFSO.GetParentFolderName(objFile)");
        writer.println("Set UAC = CreateObject(\"Shell.Application\")");

        String args = "-jar ``%s`` " + arg;
        args = String.format(args, currentJarLocation.getAbsolutePath()).replace('`', '"');

        String uacCommand = "UAC.ShellExecute ```%s```, `%s`, strFolder, `runas`, 1";
        uacCommand = String.format(uacCommand, javawLocation.getAbsolutePath(), args).replace('`', '"');
        writer.println(uacCommand);
        writer.println("WScript.Quit 0");
        writer.close();

        if (socket != null) {
            try {
                socket.close();
            } catch (Throwable t) {
                ExceptionHandler.handle(t);
            }
        }

        Process process = Runtime.getRuntime().exec("cscript " + tempVBSFile.getAbsolutePath());
        process.waitFor();
        System.exit(process.exitValue());
    }

    private static File getJarLocation() throws URISyntaxException {
        File currentJarLocation = (File) System.getProperties().get("com.heliosdecompiler.bootstrapperFile");
        while (currentJarLocation == null || !currentJarLocation.exists() || !currentJarLocation.isFile()) {
            SWTUtil.showMessage("Could not determine location of Helios. Please select the JAR file", true);
            List<File> chosen = FileChooserUtil.chooseFiles(".", Collections.singletonList("jar"), false);
            if (chosen.size() == 0) {
                return null;
            }
            currentJarLocation = chosen.get(0);
        }
        return currentJarLocation;
    }

    private static File getJavawLocation() {
        File javaw = null;
        String name = "java";

        OSUtils.OS os = OSUtils.getOS();
        if (os == OSUtils.OS.WINDOWS) {
            javaw = new File(System.getProperty("java.home") + File.separator + "bin" + File.separator + "javaw.exe");
            name = "javaw";
        } else if (os == OSUtils.OS.LINUX || os == OSUtils.OS.MAC) {
            javaw = new File(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        }
        while (javaw == null || !javaw.exists() || !javaw.isFile()) {
            SWTUtil.showMessage("Could not determine location of " + name + ". Please select the location of the executable", true);
            List<File> chosen = FileChooserUtil.chooseFiles(System.getProperty("java.home"), Collections.singletonList("exe"), false);
            if (chosen.size() == 0) {
                return null;
            }
            javaw = chosen.get(0);
        }
        return javaw;
    }
}