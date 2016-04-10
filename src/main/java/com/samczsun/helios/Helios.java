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

package com.samczsun.helios;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.google.common.collect.Sets;
import com.samczsun.helios.api.events.Events;
import com.samczsun.helios.api.events.requests.RecentFileRequest;
import com.samczsun.helios.api.events.requests.RefreshViewRequest;
import com.samczsun.helios.api.events.requests.TreeUpdateRequest;
import com.samczsun.helios.bootloader.BootSequence;
import com.samczsun.helios.bootloader.Splash;
import com.samczsun.helios.gui.BackgroundTaskGui;
import com.samczsun.helios.gui.GUI;
import com.samczsun.helios.handler.BackgroundTaskHandler;
import com.samczsun.helios.handler.ExceptionHandler;
import com.samczsun.helios.handler.addons.AddonHandler;
import com.samczsun.helios.tasks.AddFilesTask;
import com.samczsun.helios.utils.FileChooserUtil;
import com.samczsun.helios.utils.SWTUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.objectweb.asm.tree.ClassNode;

import javax.swing.UIManager;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.security.Permission;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Helios {
    private static final Map<String, LoadedFile> files = new HashMap<>();
    private static final List<Process> processes = new ArrayList<>();
    private static Boolean python2Verified = null;
    private static Boolean python3Verified = null;
    private static Boolean javaRtVerified = null;
    private static GUI gui;
    private static BackgroundTaskHandler backgroundTaskHandler;
    private static BackgroundTaskGui backgroundTaskGui;

    private static volatile Map<String, LoadedFile> path = new HashMap<>();

    public static void main(String[] args, Shell shell, Splash splashScreen) {
        splashScreen.updateState(BootSequence.LOADING_SETTINGS);
        Settings.loadSettings();
        backgroundTaskGui = new BackgroundTaskGui();
        backgroundTaskHandler = new BackgroundTaskHandler();
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
            processes.forEach(Process::destroy);
        }));
        splashScreen.updateState(BootSequence.COMPLETE);
        while (!splashScreen.isDisposed()) ;
        Display.getDefault().syncExec(() -> getGui().getShell().open());

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
                formatter.printHelp("java -jar bootstrapper.jar -a [options]", options);
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

        submitBackgroundTask(() -> {
            Map<String, LoadedFile> newPath = new HashMap<>();
            for (String strFile : Sets.newHashSet(Settings.PATH.get().asString().split(";"))) {
                File file = new File(strFile);
                if (file.exists()) {
                    try {
                        LoadedFile loadedFile = new LoadedFile(file);
                        newPath.put(loadedFile.getName(), loadedFile);
                    } catch (IOException e1) {
                        ExceptionHandler.handle(e1);
                    }
                }
            }
            File file = new File(Settings.RT_LOCATION.get().asString());
            if (file.exists()) {
                try {
                    LoadedFile loadedFile = new LoadedFile(file);
                    newPath.put(loadedFile.getName(), loadedFile);
                } catch (IOException e1) {
                    ExceptionHandler.handle(e1);
                }
            }
            synchronized (Helios.class) {
                path.clear();
                path.putAll(newPath);
            }
        });

        if (open.size() > 0) {
            openFiles(open.toArray(new File[open.size()]), true);
        }
    }

    public static List<LoadedFile> getFilesForName(String fileName) {
        return files
                .values()
                .stream()
                .filter(loadedFile -> loadedFile.getFiles().containsKey(fileName))
                .collect(Collectors.toList());
    }

    public static void loadFile(File fileToLoad) throws IOException {
        LoadedFile loadedFile = new LoadedFile(fileToLoad);
        files.put(loadedFile.getName(), loadedFile);
    }

    public static List<ClassNode> loadAllClasses() {
        List<ClassNode> classNodes = new ArrayList<>();
        for (LoadedFile loadedFile : files.values()) {
            for (String s : loadedFile.getFiles().keySet()) {
                ClassNode loaded = loadedFile.getClassNode(s);
                if (loaded != null) {
                    classNodes.add(loaded);
                }
            }
        }
        return classNodes;
    }

    public static void openFiles(final File[] files, final boolean recentFiles) {
        submitBackgroundTask(new AddFilesTask(files, recentFiles));
    }

    public static void resetWorkSpace(boolean ask) {
        if (ask && !SWTUtil.promptForYesNo(Constants.REPO_NAME + " - New Workspace",
                "You have not yet saved your workspace. Are you sure you wish to create a new one?")) {
            return;
        }
        files.clear();
        getGui().getTreeManager().reset();
        getGui().getClassManager().reset();
        processes.forEach(Process::destroyForcibly);
        processes.clear();
    }

    public static Process launchProcess(ProcessBuilder launch) throws IOException {
        Process process = launch.start();
        processes.add(process);
        submitBackgroundTask(() -> {
            try {
                process.waitFor();
                if (!process.isAlive()) {
                    processes.remove(process);
                }
            } catch (InterruptedException e) {
                ExceptionHandler.handle(e);
            }
        });
        return process;
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
        submitBackgroundTask(() -> {
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
        });
    }

    public static void promptForRefresh() {
        if (SWTUtil.promptForYesNo(Constants.REPO_NAME + " - Refresh", "Are you sure you wish to refresh?")) {
            for (LoadedFile loadedFile : Helios.files.values()) {
                try {
                    loadedFile.reset();
                } catch (IOException e) {
                    ExceptionHandler.handle(e);
                }
            }
            Events.callEvent(new TreeUpdateRequest());
        }
    }

    private static volatile Shell customPathShell;

    public static void promptForCustomPath() {
        synchronized (Helios.class) {
            if (customPathShell != null) {
                customPathShell.setFocus();
                return;
            }
        }
        Display.getDefault().asyncExec(() -> {
            synchronized (Helios.class) {
                Display display = Display.getDefault();
                Shell shell = new Shell(display);
                shell.setLayout(new GridLayout());
                shell.setImage(Resources.ICON.getImage());
                shell.setText("Set your PATH variable");
                final Text text = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.WRAP);
                text.setText(Settings.PATH.get().asString());
                GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
                gridData.heightHint = text.getLineHeight();
                gridData.widthHint = 512;
                text.setLayoutData(gridData);
                shell.addShellListener(new ShellAdapter() {
                    @Override
                    public void shellClosed(ShellEvent e) {
                        synchronized (Helios.class) {
                            customPathShell = null;
                        }
                        String oldPath = Settings.PATH.get().asString();
                        if (!oldPath.equals(text.getText())) {
                            Settings.PATH.set(text.getText());
                            submitBackgroundTask(() -> {
                                Map<String, LoadedFile> newPath = new HashMap<>();
                                for (String strFile : Sets.newHashSet(Settings.PATH.get().asString().split(";"))) {
                                    File file = new File(strFile);
                                    if (file.exists()) {
                                        try {
                                            LoadedFile loadedFile = new LoadedFile(file);
                                            newPath.put(loadedFile.getName(), loadedFile);
                                        } catch (IOException e1) {
                                            ExceptionHandler.handle(e1);
                                        }
                                    }
                                }
                                synchronized (Helios.class) {
                                    path.clear();
                                    path.putAll(newPath);
                                }
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
                customPathShell = shell;
            }
        });
    }

    public static synchronized Map<String, LoadedFile> getPathFiles() {
        return path;
    }

    public static boolean ensurePython3Set() {
        return ensurePython3Set0(false);
    }

    public static boolean ensurePython2Set() {
        return ensurePython2Set0(false);
    }

    public static boolean ensureJavaRtSet() {
        return ensureJavaRtSet0(false);
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

    private static boolean ensurePython2Set0(boolean forceCheck) {
        String python2Location = Settings.PYTHON2_LOCATION.get().asString();
        if (python2Location.isEmpty()) {
            SWTUtil.showMessage("You need to set the location of the Python/PyPy 2.x executable", true);
            setLocationOf(Settings.PYTHON2_LOCATION);
            python2Location = Settings.PYTHON2_LOCATION.get().asString();
        }
        if (python2Location.isEmpty()) {
            return false;
        }
        if (python2Verified == null || forceCheck) {
            try {
                Process process = new ProcessBuilder(python2Location, "-V").start();
                String result = IOUtils.toString(process.getInputStream());
                String error = IOUtils.toString(process.getErrorStream());
                python2Verified = error.startsWith("Python 2") || result.startsWith("Python 2");
            } catch (Throwable t) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                SWTUtil.showMessage(
                        "The Python 2.x executable is invalid." + Constants.NEWLINE + Constants.NEWLINE + sw.toString());
                t.printStackTrace();
                python2Verified = false;
            }
        }
        return python2Verified;
    }

    private static boolean ensureJavaRtSet0(boolean forceCheck) {
        String javaRtLocation = Settings.RT_LOCATION.get().asString();
        if (javaRtLocation.isEmpty()) {
            SWTUtil.showMessage("You need to set the location of Java's rt.jar", true);
            setLocationOf(Settings.RT_LOCATION);
            javaRtLocation = Settings.RT_LOCATION.get().asString();
        }
        if (javaRtLocation.isEmpty()) {
            return false;
        }
        if (javaRtVerified == null || forceCheck) {
            ZipFile zipFile = null;
            try {
                File rtjar = new File(javaRtLocation);
                if (rtjar.exists()) {
                    zipFile = new ZipFile(rtjar);
                    ZipEntry object = zipFile.getEntry("java/lang/Object.class");
                    if (object != null) {
                        javaRtVerified = true;
                    }
                }
            } catch (Throwable t) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                SWTUtil.showMessage(
                        "The selected Java rt.jar is invalid." + Constants.NEWLINE + Constants.NEWLINE + sw.toString());
                t.printStackTrace();
                javaRtVerified = false;
            } finally {
                IOUtils.closeQuietly(zipFile);
                if (javaRtVerified == null) {
                    javaRtVerified = false;
                }
            }
        }
        return javaRtVerified;
    }

    public static void checkHotKey(Event e) {
        if (e.doit) {
            if ((e.stateMask & SWT.CTRL) == SWT.CTRL) {
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
                    getGui().getSearchPopup().open();
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
                }
            }
        }
    }

    public static LoadedFile getLoadedFile(String file) {
        return files.containsKey(file) ? files.get(file) : getPathFiles().get(file);
    }

    public static void submitBackgroundTask(Runnable runnable) {
        getBackgroundTaskHandler().submit(runnable);
    }

    public static Collection<LoadedFile> getAllFiles() {
        return Collections.unmodifiableCollection(files.values());
    }

    public static Map<String, byte[]> getAllLoadedData() {
        Map<String, byte[]> data = new HashMap<>();
        for (LoadedFile loadedFile : files.values()) {
            data.putAll(loadedFile.getData());
        }
        for (LoadedFile loadedFile : path.values()) {
            data.putAll(loadedFile.getData());
        }
        return data;
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
                ensurePython2Set0(true);
            } else if (setting == Settings.PYTHON3_LOCATION) {
                ensurePython3Set0(true);
            } else if (setting == Settings.RT_LOCATION) {
                ensureJavaRtSet0(true);
            }
        }
    }

    public static void addToContextMenu() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Process process = Runtime.getRuntime().exec("reg add HKCR\\*\\shell\\helios\\command /f");
                process.waitFor();
                if (process.exitValue() == 0) {
                    process = Runtime.getRuntime().exec("reg add HKCR\\*\\shell\\helios /ve /d \"Open with Helios\" /f");
                    process.waitFor();
                    if (process.exitValue() == 0) {
                        File currentJarLocation = getJarLocation();
                        if (currentJarLocation != null) {
                            File javaw = getJavawLocation();
                            if (javaw != null) {
                                process = Runtime.getRuntime().exec("reg add HKCR\\*\\shell\\helios\\command /ve /d \"\\\"" + javaw.getAbsolutePath() + "\\\" -jar \\\"" + currentJarLocation.getAbsolutePath() + "\\\" -a \\\"--open \\\\\\\"%1\\\\\\\"\\\"\" /f");
                                process.waitFor();
                                if (process.exitValue() == 0) {
                                    SWTUtil.showMessage("Done");
                                } else {
                                    SWTUtil.showMessage("Failed to set context menu");
                                }
                            } else {
                                SWTUtil.showMessage("Could not set context menu - unable to find javaw.exe");
                            }
                        } else {
                            SWTUtil.showMessage("Could not set context menu - unable to find Helios.jar");
                        }
                    } else {
                        SWTUtil.showMessage("Failed to set context menu");
                    }
                } else {
                    if (SWTUtil.promptForYesNo("UAC", "Helios must be run as an administrator to do this. Relaunch as administrator?")) {
                        relaunchAsAdmin();
                    }
                }
            } else {
                SWTUtil.showMessage("You may only do this on Windows");
            }
        } catch (Throwable t) {
            ExceptionHandler.handle(t);
        }
    }

    public static void relaunchAsAdmin() throws IOException, InterruptedException, URISyntaxException {
        File currentJarLocation = getJarLocation();
        File javawLocation = getJavawLocation();
        if (currentJarLocation == null) {
            SWTUtil.showMessage("Could not relaunch as admin - unable to find Helios.jar");
            return;
        }
        if (javawLocation == null) {
            SWTUtil.showMessage("Could not relaunch as admin - unable to find javaw.exe");
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
        writer.println("UAC.ShellExecute \"\"\"" + javawLocation.getAbsolutePath() + "\"\"\", \"-jar \"\"C:\\Users\\Sam\\IdeaProjects\\Helios\\bootstrapper\\target\\bootstrapper-0.0.3.jar\"\" \"\"-a\"\" \"\"-ctx\"\"\", strFolder, \"runas\", 1");
        writer.println("WScript.Quit 0");
        writer.close();

        Process process = Runtime.getRuntime().exec("cscript " + tempVBSFile.getAbsolutePath());
        process.waitFor();
        System.exit(process.exitValue());
    }

    private static File getJarLocation() throws URISyntaxException {
        File currentJarLocation = (File) System.getProperties().get("com.heliosdecompiler.bootstrapperFile");
        while (!currentJarLocation.exists() || !currentJarLocation.isFile()) {
            SWTUtil.showMessage("Could not determine location of Helios. Please select the JAR file", true);
            List<File> chosen = FileChooserUtil.chooseFiles(".", Arrays.asList("jar"), false);
            if (chosen.size() == 0) {
                return null;
            }
            currentJarLocation = chosen.get(0);
        }
        return currentJarLocation;
    }

    private static File getJavawLocation() {
        File javaw = new File(System.getProperty("java.home") + File.separator + "bin" + File.separator + "javaw.exe");
        while (!javaw.exists() || !javaw.isFile()) {
            SWTUtil.showMessage("Could not determine location of javaw. Please select the location of the executable", true);
            List<File> chosen = FileChooserUtil.chooseFiles(System.getProperty("java.home"), Arrays.asList("exe"), false);
            if (chosen.size() == 0) {
                return null;
            }
            javaw = chosen.get(0);
        }
        return javaw;
    }
}