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
import com.samczsun.helios.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class KrakatauDisassembler extends Disassembler {
    public KrakatauDisassembler() {
        super("krakatau-disassembler", "Krakatau Disassembler");
    }

    public boolean disassembleClassNode(ClassNode cn, byte[] b, StringBuilder output) {
        if (Helios.ensurePython2Set()) {
            File inputJar = null;
            File outputZip = null;

            String processLog = "";
            try {
                inputJar = Files.createTempFile("kdisin", ".jar").toFile();
                outputZip = Files.createTempFile("kdisout", ".zip").toFile();

                Utils.saveClasses(inputJar, Helios.getAllLoadedData());

                Process process = Helios.launchProcess(
                        new ProcessBuilder(Settings.PYTHON2_LOCATION.get().asString(), "-O", "disassemble.py", "-path",
                                inputJar.getAbsolutePath(), "-out", outputZip.getAbsolutePath(),
                                cn.name + ".class").directory(Constants.KRAKATAU_DIR));

                processLog = Utils.readProcess(process);

                ZipFile zipFile = new ZipFile(outputZip);
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
                FileUtils.deleteQuietly(inputJar);
                FileUtils.deleteQuietly(outputZip);
            }
        } else {
            output.append("You need to set the location of Python 2.x");
        }
        return false;
    }
}
