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

import com.samczsun.helios.handler.ExceptionHandler;
import com.samczsun.helios.transformers.converters.Converter;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.tree.ClassNode;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class BaksmaliDisassembler extends Disassembler {
    public BaksmaliDisassembler() {
        super("baksmali-disassembler", "Baksmali Disassembler");
    }

    public boolean disassembleClassNode(ClassNode cn, byte[] b, StringBuilder output) {
        try {
            File tempDir = Files.createTempDirectory("smali").toFile();
            File tempClass = new File(tempDir, "temp.class");
            File tempZip = new File(tempDir, "temp.jar");
            File tempDex = new File(tempDir, "temp.dex");
            File tempSmali = new File(tempDir, "temp-smali");
            FileOutputStream fos = new FileOutputStream(tempClass);
            fos.write(b);
            fos.close();

            ZipUtil.packEntry(tempClass, tempZip);

            Converter.ENJARIFY.convert(tempZip, tempDex);

            try {
                org.jf.baksmali.main.main(
                        new String[]{"-o", tempSmali.getAbsolutePath(), "-x", tempDex.getAbsolutePath()});
            } catch (Exception e) {
                ExceptionHandler.handle(e);
            }

            File outputSmali = null;

            boolean found = false;
            File current = tempSmali;
            while (!found) {
                File f = current.listFiles()[0];
                if (f.isDirectory()) current = f;
                else {
                    outputSmali = f;
                    found = true;
                }

            }
            try {
                output.append(FileUtils.readFileToString(outputSmali, "UTF-8"));
                return true;
            } catch (Exception e) {
                output.append(parseException(e));
                return false;
            }
        } catch (final IOException e) {
            ExceptionHandler.handle(e);
            return false;
        }
    }

    @Override
    public void disassembleToZip(String zipName) {

    }
}
