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

package com.samczsun.helios.transformers.decompilers;

import com.samczsun.helios.Constants;
import com.samczsun.helios.Helios;
import com.samczsun.helios.Settings;
import com.samczsun.helios.handler.ExceptionHandler;
import com.samczsun.helios.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.tree.ClassNode;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class KrakatauDecompiler extends Decompiler {
    public KrakatauDecompiler() {
        super("krakatau-decompiler", "Krakatau Decompiler");
    }

    public boolean decompile(ClassNode classNode, byte[] bytes, StringBuilder output) {
        if (Helios.ensurePython2Set()) {
            if (Helios.ensureJavaRtSet()) {
                File inputJar = null;
                File outputJar = null;
                ZipFile zipFile = null;
                Process createdProcess = null;
                String log = "";

                try {
                    inputJar = Files.createTempFile("kdein", ".jar").toFile();
                    outputJar = Files.createTempFile("kdeout", ".zip").toFile();
                    Utils.saveClasses(inputJar, Helios.getAllLoadedData());

                    createdProcess = Helios.launchProcess(
                            new ProcessBuilder(Settings.PYTHON2_LOCATION.get().asString(), "-O", "decompile.py",
                                    "-skip", "-nauto", "-path", buildPath(inputJar), "-out",
                                    outputJar.getAbsolutePath(), classNode.name + ".class").directory(
                                    Constants.KRAKATAU_DIR));

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
                    if (zipFile != null) {
                        try {
                            zipFile.close();
                        } catch (IOException e) {
                        }
                    }
                    if (inputJar != null) {
                        if (!inputJar.delete()) {
                            inputJar.deleteOnExit();
                        }
                    }
                    if (outputJar != null) {
                        if (!outputJar.delete()) {
                            outputJar.deleteOnExit();
                        }
                    }
                }
            } else {
                output.append("You need to set the location of rt.jar");
            }
        } else {
            output.append("You need to set the location of Python 2.x");
        }
        return false;
    }

    public void decompile(String zipName) {
        if (Helios.ensurePython2Set()) {
            if (Helios.ensureJavaRtSet()) {
                try {
                    File tempDir = Files.createTempDirectory("krakatauoutput").toFile();
                    File tempJar = new File(tempDir, "temp.jar");
                    Utils.saveClasses(tempJar, Helios.getAllLoadedData());

                    Process process = Helios.launchProcess(
                            new ProcessBuilder(Settings.PYTHON2_LOCATION.get().asString(), "-O", "decompile.py",
                                    "-skip", "-nauto", "-path",
                                    Settings.RT_LOCATION.get().asString() + ";" + tempJar.getAbsolutePath(), "-out",
                                    tempDir.getAbsolutePath(), tempJar.getAbsolutePath()).directory(
                                    Constants.KRAKATAU_DIR));
                    process.waitFor();

                    tempJar.delete();
                    ZipUtil.pack(tempDir, new File(zipName));
                    FileUtils.deleteDirectory(tempDir);
                } catch (Exception e) {
                    ExceptionHandler.handle(e);
                }
            }
        } //TODO Warn
    }
}
