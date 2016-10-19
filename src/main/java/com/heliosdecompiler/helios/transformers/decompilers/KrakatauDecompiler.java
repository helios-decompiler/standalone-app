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

package com.heliosdecompiler.helios.transformers.decompilers;

import com.heliosdecompiler.helios.Constants;
import com.heliosdecompiler.helios.FileManager;
import com.heliosdecompiler.helios.transformers.TransformerSettings;
import com.heliosdecompiler.helios.utils.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class KrakatauDecompiler extends Decompiler {
    public KrakatauDecompiler() {
        super("krakatau", "Krakatau", Settings.class);
    }

    public Either<Result, String> decompile(ClassNode classNode, byte[] bytes) {
        Result python2 = SettingsValidator.ensurePython2Set();
        if (python2.is(Result.Type.SUCCESS)) {
            Result javart = SettingsValidator.ensureJavaRtSet();
            if (javart.is(Result.Type.SUCCESS)) {
                File sessionDirectory = null;
                File inputFile;
                File outputFile;

                Process createdProcess;
                String log = "";

                try {
                    sessionDirectory = Files.createTempDirectory("krakatau-decompile-").toFile();
                    inputFile = new File(sessionDirectory, "input.jar");
                    outputFile = new File(sessionDirectory, "out.jar");

                    Map<String, byte[]> data = new HashMap<>();
                    data.put(classNode.name + ".class", bytes);
                    Utils.saveClasses(inputFile, data);

                    createdProcess = ProcessUtils.launchProcess(
                            new ProcessBuilder(
                                    com.heliosdecompiler.helios.Settings.PYTHON2_LOCATION.get().asString(),
                                    "-O",
                                    "decompile.py",
                                    "-skip",
                                    "-nauto",
                                    Settings.MAGIC_THROW.isEnabled() ? "-xmagicthrow" : "",
                                    "-path",
                                    buildPath(FileManager.buildPath()),
                                    "-out",
                                    outputFile.getAbsolutePath(),
                                    inputFile.getAbsolutePath()
                            ).directory(Constants.KRAKATAU_DIR));

                    log = ProcessUtils.readProcess(createdProcess);
                    System.out.println(log);

                    JarFile zipFile = new JarFile(outputFile);

                    JarEntry target = null;

                    Enumeration<JarEntry> e = zipFile.entries();
                    while (e.hasMoreElements()) {
                        JarEntry next = e.nextElement();
                        if (!next.isDirectory()) {
                            if (target == null) {
                                target = next;
                            } else {
                                throw new IllegalArgumentException("Already had target: " + target + " but got new target " + next);
                            }
                        }
                    }

                    if (target != null) {
                        try (InputStream in = zipFile.getInputStream(target)) {
                            return Either.right(new String(IOUtils.toByteArray(in), "UTF-8"));
                        } finally {
                            zipFile.close();
                        }
                    } else {
                        throw new RuntimeException("Expected entry, but nothing was found");
                    }
                } catch (Exception e) {
                    return Either.right(parseException(e) + "\n" + log);
                } finally {
                    FileUtils.deleteQuietly(sessionDirectory);
                }
            }
            return Either.right("You must specify the location of rt.jar");
        }
        return Either.right("You must specify the location of the Python 2.x executable");
    }


    public enum Settings implements TransformerSettings.Setting {
        MAGIC_THROW("xmagicthrow", "Assume all instructions can throw (disabling can result in inaccurate code)", true);

        private final String name;
        private final String param;
        private boolean on;

        Settings(String param, String name) {
            this(param, name, false);
        }

        Settings(String param, String name, boolean on) {
            this.name = name;
            this.param = param;
            this.on = on;
        }

        public String getParam() {
            return param;
        }

        public String getText() {
            return name;
        }

        public boolean isEnabled() {
            return on;
        }

        public void setEnabled(boolean enabled) {
            this.on = enabled;
        }
    }
}
