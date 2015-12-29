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

package com.samczsun.helios.transformers.disassemblers;

import com.samczsun.helios.Constants;
import com.samczsun.helios.Helios;
import com.samczsun.helios.Settings;
import com.samczsun.helios.handler.ExceptionHandler;
import com.samczsun.helios.utils.Utils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.objectweb.asm.tree.ClassNode;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class KrakatauDisassembler extends Disassembler {
    public KrakatauDisassembler() {
        super("krakatau-disassembler", "Krakatau Disassembler");
    }

    public boolean disassembleClassNode(ClassNode cn, byte[] b, StringBuilder output) {
        if (Helios.ensurePython2Set()) {
            Path inputJar = null;
            Path outputZip = null;

            String processLog = "";

            try {
                inputJar = Files.createTempFile("kdisin", ".jar");
                outputZip = Files.createTempFile("kdisout", ".zip");

                Utils.saveClasses(inputJar.toAbsolutePath().toFile(), Helios.getAllLoadedData());

                Process process = Helios.launchProcess(
                        new ProcessBuilder(Settings.PYTHON2_LOCATION.get().asString(), "-O", "disassemble.py", "-path",
                                inputJar.toAbsolutePath().toString(), "-out", outputZip.toAbsolutePath().toString(),
                                cn.name + ".class").directory(Constants.KRAKATAU_DIR));

                //Read out dir output
                String log = "Process:" + Constants.NEWLINE + Constants.NEWLINE;

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                IOUtils.copy(process.getInputStream(), outputStream);
                log += new String(outputStream.toByteArray(), "UTF-8");

                log += Constants.NEWLINE + Constants.NEWLINE + "Error:" + Constants.NEWLINE + Constants.NEWLINE;

                outputStream = new ByteArrayOutputStream();
                IOUtils.copy(process.getErrorStream(), outputStream);
                log += new String(outputStream.toByteArray(), "UTF-8");

                int exitValue = process.waitFor();
                log += Constants.NEWLINE + Constants.NEWLINE + "Exit Value is " + exitValue;
                processLog = log;

                ZipFile zipFile = new ZipFile(outputZip.toFile());
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                byte[] data = null;
                while (entries.hasMoreElements()) {
                    ZipEntry next = entries.nextElement();
                    if (next.getName().equals(cn.name + ".j")) {
                        data = IOUtils.toByteArray(zipFile.getInputStream(next));
                    }
                }
                zipFile.close();
                output.append(new String(data, "UTF-8"));
                return true;
            } catch (Exception e) {
                output.append(parseException(e)).append(processLog);
                return false;
            } finally {
                try {
                    if (inputJar != null) {
                        Files.delete(inputJar);
                    }
                } catch (IOException e) {
                }
                try {
                    if (outputZip != null) {
                        Files.delete(outputZip);
                    }
                } catch (IOException e) {
                }
            }
        } else {
            output.append("You need to set the location of Python 2.x");
        }
        return false;
    }

    @Override
    public void disassembleToZip(String zipName) {
        if (Helios.ensurePython2Set()) {
            try {
                File tempDirectory = Files.createTempDirectory("kdout").toFile();
                File tempJar = Files.createTempFile("kdin", ".jar").toFile();
                Utils.saveClasses(tempJar, Helios.getAllLoadedData());

                Process process = Helios.launchProcess(
                        new ProcessBuilder(Settings.PYTHON2_LOCATION.get().asString(), "-O", "disassemble.py", "-path",
                                Settings.RT_LOCATION.get().asString() + ";" + tempJar.getAbsolutePath(), "-out",
                                tempDirectory.getAbsolutePath(), tempJar.getAbsolutePath()).directory(
                                Constants.KRAKATAU_DIR));
                process.waitFor();

                ZipUtil.pack(tempDirectory, new File(zipName));

                //tempDirectory.delete();
                tempJar.delete();
            } catch (Exception e) {
                ExceptionHandler.handle(e);
            }
        }
    }
}
