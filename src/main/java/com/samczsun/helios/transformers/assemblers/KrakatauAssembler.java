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

package com.samczsun.helios.transformers.assemblers;

import com.samczsun.helios.Constants;
import com.samczsun.helios.Helios;
import com.samczsun.helios.Settings;
import com.samczsun.helios.handler.ExceptionHandler;
import com.samczsun.helios.utils.SWTUtil;
import com.samczsun.helios.utils.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class KrakatauAssembler extends Assembler {

    KrakatauAssembler() {
        super("krakatau-assembler", "Krakatau Assembler");
    }

    @Override
    public byte[] assemble(String name, String contents) {
        if (Helios.ensurePython2Set()) {
            File tempFolder = null;
            File tempFile = null;
            String processLog = "";
            try {
                tempFolder = Files.createTempDirectory("ka").toFile();
                tempFile = new File(tempFolder, name.replace('/', File.separatorChar) + ".j");
                FileUtils.write(tempFile, contents, "UTF-8", false);
                Process process = Helios.launchProcess(
                        new ProcessBuilder(Settings.PYTHON2_LOCATION.get().asString(), "-O", "assemble.py", "-out",
                                tempFolder.getAbsolutePath(), tempFile.getAbsolutePath()).directory(
                                Constants.KRAKATAU_DIR));

                processLog = Utils.readProcess(process);

                return FileUtils.readFileToByteArray(new File(tempFile.toString().replace(".j", ".class")));
            } catch (Exception e) {
                ExceptionHandler.handle(e);
                SWTUtil.showMessage(processLog);
            } finally {
                try {
                    if (tempFolder != null) {
                        FileUtils.deleteDirectory(tempFolder);
                    }
                } catch (IOException e) {
                }
                if (tempFile != null) {
                    tempFile.delete();
                }
            }
        } else {
            SWTUtil.showMessage("You need to set Python!");
        }
        return null;
    }

}
