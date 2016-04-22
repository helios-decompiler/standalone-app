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

import com.heliosdecompiler.helios.transformers.TransformerSettings;
import com.heliosdecompiler.helios.utils.Utils;
import com.heliosdecompiler.helios.Constants;
import com.heliosdecompiler.helios.Helios;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class KrakatauDecompiler extends Decompiler {
    KrakatauDecompiler() {
        super("krakatau", "Krakatau", Settings.class);
    }

    public boolean decompile(ClassNode classNode, byte[] bytes, StringBuilder output) {
        if (Helios.ensurePython2Set()) {
            if (Helios.ensureJavaRtSet()) {
                File inputJar = null;
                File outputJar = null;
                ZipFile zipFile = null;
                Process createdProcess;
                String log = "";

                try {
                    inputJar = Files.createTempFile("kdein", ".jar").toFile();
                    outputJar = Files.createTempFile("kdeout", ".zip").toFile();
                    Map<String, byte[]> loadedData = Helios.getAllLoadedData();
                    loadedData.put(classNode.name + ".class", bytes);
                    Utils.saveClasses(inputJar, loadedData);

                    createdProcess = Helios.launchProcess(
                            new ProcessBuilder(
                                    com.heliosdecompiler.helios.Settings.PYTHON2_LOCATION.get().asString(),
                                    "-O",
                                    "decompile.py",
                                    "-skip",
                                    "-nauto",
                                    Settings.MAGIC_THROW.isEnabled() ? "-xmagicthrow" : "",
                                    "-path",
                                    buildPath(inputJar),
                                    "-out",
                                    outputJar.getAbsolutePath(),
                                    classNode.name + ".class"
                            ).directory(Constants.KRAKATAU_DIR));

                    log = Utils.readProcess(createdProcess);

                    System.out.println(log);

                    zipFile = new ZipFile(outputJar);
                    ZipEntry zipEntry = zipFile.getEntry(classNode.name + ".java");
                    if (zipEntry == null)
                        throw new IllegalArgumentException("Class failed to decompile (no class in output zip)");
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    byte[] data = IOUtils.toByteArray(inputStream);
                    output.append(new String(data, "UTF-8"));
                    return true;
                } catch (Exception e) {
                    output.append(parseException(e)).append("\n").append(log);
                    return false;
                } finally {
                    IOUtils.closeQuietly(zipFile);
                    FileUtils.deleteQuietly(inputJar);
                    FileUtils.deleteQuietly(outputJar);
                }
            } else {
                output.append("You need to set the location of rt.jar");
            }
        } else {
            output.append("You need to set the location of Python 2.x");
        }
        return false;
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
