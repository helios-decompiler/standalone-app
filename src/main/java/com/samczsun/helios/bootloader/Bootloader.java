/*
 * Copyright 2015 Sam Sun <me@samczsun.com>
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Bootloader {
    public static void main(String[] args) {
        try {
            if (!Constants.DATA_DIR.exists() && !Constants.DATA_DIR.mkdirs())
                throw new RuntimeException("Could not create data directory");
            if (!Constants.ADDONS_DIR.exists() && !Constants.ADDONS_DIR.mkdirs())
                throw new RuntimeException("Could not create addons directory");
            if (!Constants.SETTINGS_FILE.exists() && !Constants.SETTINGS_FILE.createNewFile())
                throw new RuntimeException("Could not create settings file");

            loadSWTLibrary();

            DisplayPumper displayPumper = new DisplayPumper();
            Thread pumpThread = new Thread(displayPumper);
            pumpThread.setUncaughtExceptionHandler((t, e) -> {
                Bootloader.displayError(e);
                System.exit(1);
            });
            pumpThread.start();
            while (!displayPumper.isReady()) ;

            Display display = displayPumper.getDisplay();
            Shell shell = displayPumper.getShell();
            Splash splashScreen = new Splash(display);
            splashScreen.updateState(BootSequence.CHECKING_LIBRARIES);
            checkPackagedLibrary(splashScreen, "enjarify", Constants.ENJARIFY_VERSION, BootSequence.CHECKING_ENJARIFY,
                    BootSequence.CLEANING_ENJARIFY, BootSequence.MOVING_ENJARIFY);
            checkPackagedLibrary(splashScreen, "Krakatau", Constants.KRAKATAU_VERSION, BootSequence.CHECKING_KRAKATAU,
                    BootSequence.CLEANING_KRAKATAU, BootSequence.MOVING_KRAKATAU);
            Helios.main(args, shell, splashScreen);
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

    private static final void loadSWTLibrary() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String name = getOSName();
        if (name == null) throw new IllegalArgumentException("Cannot determine OS");
        String arch = getArch();
        if (arch == null) throw new IllegalArgumentException("Cannot determine architecture");

        String swtLocation = "/swt/org.eclipse.swt." + name + "." + arch + "-" + Constants.SWT_VERSION + ".jar";

        System.out.println("Loading SWT version " + swtLocation.substring(5));

        InputStream swtIn = Bootloader.class.getResourceAsStream(swtLocation);
        if (swtIn == null) throw new IllegalArgumentException("SWT library not found");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copy(swtIn, outputStream);
        ByteArrayInputStream swt = new ByteArrayInputStream(outputStream.toByteArray());

        URL.setURLStreamHandlerFactory(protocol -> { //JarInJar!
            if (protocol.equals("swt")) {
                return new URLStreamHandler() {
                    protected URLConnection openConnection(URL u) {
                        return new URLConnection(u) {
                            public void connect() {
                            }

                            public InputStream getInputStream() {
                                return swt;
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

        System.out.println("Loaded SWT Library");
    }

    private static final void displayError(Throwable t) {
        t.printStackTrace();
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        JOptionPane.showMessageDialog(null, writer.toString(), t.getClass().getSimpleName(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private static final String getArch() {
        String arch = System.getProperty("sun.arch.data.model");
        if (arch == null) arch = System.getProperty("com.ibm.vm.bitmode");
        return "32".equals(arch) ? "x86" : "64".equals(arch) ? "x86_64" : null;
    }

    private static final String getOSName() {
        String unparsedName = System.getProperty("os.name").toLowerCase();
        if (unparsedName.contains("win")) return "win32.win32";
        if (unparsedName.contains("mac")) return "cocoa.macosx";
        if (unparsedName.contains("linux")) return "gtk.linux";
        return null;
    }
}
