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

package com.samczsun.helios.bootloader;

import com.samczsun.helios.Constants;
import com.samczsun.helios.Helios;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.zeroturnaround.zip.ZipUtil;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.concurrent.Executor;

import static com.samczsun.helios.Constants.SWT_VERSION;
import static org.apache.commons.io.IOUtils.copy;

public class Bootloader {
    public static void main(String[] args) {
        try {
            if (!Constants.DATA_DIR.exists() && !Constants.DATA_DIR.mkdirs())
                throw new RuntimeException("Could not create data directory");
            if (!Constants.ADDONS_DIR.exists() && !Constants.ADDONS_DIR.mkdirs())
                throw new RuntimeException("Could not create addons directory");
            if (!Constants.SETTINGS_FILE.exists() && !Constants.SETTINGS_FILE.createNewFile())
                throw new RuntimeException("Could not create settings file");
            if (Constants.DATA_DIR.isFile())
                throw new RuntimeException("Data directory is file");
            if (Constants.ADDONS_DIR.isFile())
                throw new RuntimeException("Addons directory is file");
            if (Constants.SETTINGS_FILE.isDirectory())
                throw new RuntimeException("Settings file is directory");

            try {
                Class.forName("org.eclipse.swt.widgets.Display");
            } catch (ClassNotFoundException ignored) {
                loadSWTLibrary(); // For debugging purposes
            }

            DisplayPumper displayPumper = new DisplayPumper();

            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                System.out.println("Attemting to force main thread");
                Executor executor;
                try {
                    Class<?> dispatchClass = Class.forName("com.apple.concurrent.Dispatch");
                    Object dispatchInstance = dispatchClass.getMethod("getInstance").invoke(null);
                    executor = (Executor) dispatchClass.getMethod("getNonBlockingMainQueueExecutor").invoke(dispatchInstance);
                } catch (Throwable throwable) {
                    throw new RuntimeException("Could not reflectively access Dispatch", throwable);
                }
                if (executor != null) {
                    executor.execute(displayPumper);
                } else {
                    throw new RuntimeException("Could not load executor");
                }
            } else {
                Thread pumpThread = new Thread(displayPumper);
                pumpThread.start();
            }
            while (!displayPumper.isReady()) ;

            Display display = displayPumper.getDisplay();
            Shell shell = displayPumper.getShell();
            Splash splashScreen = new Splash(display);
            splashScreen.updateState(BootSequence.CHECKING_LIBRARIES);
            checkPackagedLibrary(splashScreen, "enjarify", Constants.ENJARIFY_VERSION, BootSequence.CHECKING_ENJARIFY,
                    BootSequence.CLEANING_ENJARIFY, BootSequence.MOVING_ENJARIFY);
            checkPackagedLibrary(splashScreen, "Krakatau", Constants.KRAKATAU_VERSION, BootSequence.CHECKING_KRAKATAU,
                    BootSequence.CLEANING_KRAKATAU, BootSequence.MOVING_KRAKATAU);

            try {
                if (!System.getProperty("os.name").toLowerCase().contains("linux")) {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
            } catch (Exception exception) { //Not important. No point notifying the user
            }

            Helios.main(args, shell, splashScreen);
            while (!displayPumper.isDone()) {
                Thread.sleep(100);
            }
        } catch (Throwable t) {
            displayError(t);
            System.exit(1);
        }
    }

    private static void checkPackagedLibrary(Splash splashScreen, String name, String version, BootSequence checking, BootSequence cleaning, BootSequence moving) {
        splashScreen.updateState(checking);
        File libFolder = new File(Constants.DATA_DIR, name);
        if (libFolder.isFile()) {
            if (!libFolder.delete()) {
                throw new IllegalArgumentException(name + " folder is file and could not be deleted");
            }
        }

        if (!libFolder.exists()) {
            if (!libFolder.mkdirs()) {
                throw new IllegalArgumentException("Could not create folder for " + name);
            }
        }

        File[] files = libFolder.listFiles();
        if (files == null) {
            throw new IllegalArgumentException(name + " folder is file");
        }

        for (File file : files) {
            if (!file.getName().equals(version)) {
                splashScreen.updateState(cleaning);
                FileUtils.deleteQuietly(file);
            }
        }

        File versionDirectory = new File(libFolder, version);
        if (!versionDirectory.exists()) {
            splashScreen.updateState(moving);
            InputStream inputStream = Bootloader.class.getResourceAsStream("/" + name + "-" + version + ".zip");
            if (inputStream == null) {
                throw new IllegalArgumentException(name + " was not packaged with this program");
            }
            ZipUtil.unpack(inputStream, libFolder);
        }
    }


    private static byte[] loadSWTLibrary() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        String name = getOSName();
        if (name == null) throw new IllegalArgumentException("Cannot determine OS");
        String arch = getArch();
        if (arch == null) throw new IllegalArgumentException("Cannot determine architecture");

        String artifactId = "org.eclipse.swt." + name + "." + arch;
        String swtLocation = artifactId + "-" + SWT_VERSION + ".jar";

        System.out.println("Loading SWT version " + swtLocation);

        byte[] data = null;

        File savedJar = new File(Constants.DATA_DIR, swtLocation);
        if (savedJar.isDirectory() && !savedJar.delete())
            throw new IllegalArgumentException("Saved file is a directory and could not be deleted");

        if (savedJar.exists() && savedJar.canRead()) {
            try {
                System.out.println("Loading from saved file");
                InputStream inputStream = new FileInputStream(savedJar);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                copy(inputStream, outputStream);
                data = outputStream.toByteArray();
            } catch (IOException exception) {
                System.out.println("Failed to load from saved file.");
                exception.printStackTrace(System.out);
            }
        }
        if (data == null) {
            InputStream fromJar = Bootloader.class.getResourceAsStream("/swt/" + swtLocation);
            if (fromJar != null) {
                try {
                    System.out.println("Loading from within JAR");
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    copy(fromJar, outputStream);
                    data = outputStream.toByteArray();
                } catch (IOException exception) {
                    System.out.println("Failed to load within JAR");
                    exception.printStackTrace(System.out);
                }
            }
        }
        if (data == null) {
            URL url = new URL("https://maven-eclipse.github.io/maven/org/eclipse/swt/" + artifactId + "/" + SWT_VERSION + "/" + swtLocation);
            try {
                System.out.println("Loading over the internet");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                if (connection.getResponseCode() == 200) {
                    InputStream fromURL = connection.getInputStream();
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    copy(fromURL, outputStream);
                    data = outputStream.toByteArray();
                } else {
                    throw new IOException(connection.getResponseCode() + ": " + connection.getResponseMessage());
                }
            } catch (IOException exception) {
                System.out.println("Failed to load over the internet");
                exception.printStackTrace(System.out);
            }
        }

        if (data == null) {
            throw new IllegalArgumentException("Failed to load SWT");
        }

        if (!savedJar.exists()) {
            try {
                System.out.println("Writing to saved file");
                if (savedJar.createNewFile()) {
                    FileOutputStream fileOutputStream = new FileOutputStream(savedJar);
                    fileOutputStream.write(data);
                    fileOutputStream.close();
                } else {
                    throw new IOException("Could not create new file");
                }
            } catch (IOException exception) {
                System.out.println("Failed to write to saved file");
                exception.printStackTrace(System.out);
            }
        }

        byte[] dd = data;

        URL.setURLStreamHandlerFactory(protocol -> { //JarInJar!
            if (protocol.equals("swt")) {
                return new URLStreamHandler() {
                    protected URLConnection openConnection(URL u) {
                        return new URLConnection(u) {
                            public void connect() {
                            }

                            public InputStream getInputStream() {
                                return new ByteArrayInputStream(dd);
                            }
                        };
                    }

                    protected void parseURL(URL u, String spec, int start, int limit) {
                        // Don't parse or it's too slow
                    }
                };
            }
            return null;
        });

        ClassLoader classLoader = Bootloader.class.getClassLoader();
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        method.invoke(classLoader, new URL("swt://load"));

        return data;
    }



    private static void displayError(Throwable t) {
        t.printStackTrace();
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        JOptionPane.showMessageDialog(null, writer.toString(), t.getClass().getSimpleName(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private static String getArch() {
        String arch = System.getProperty("sun.arch.data.model");
        if (arch == null) arch = System.getProperty("com.ibm.vm.bitmode");
        return "32".equals(arch) ? "x86" : "64".equals(arch) ? "x86_64" : null;
    }

    private static String getOSName() {
        String unparsedName = System.getProperty("os.name").toLowerCase();
        if (unparsedName.contains("win")) return "win32.win32";
        if (unparsedName.contains("mac")) return "cocoa.macosx";
        if (unparsedName.contains("linux")) return "gtk.linux";
        return null;
    }
}
