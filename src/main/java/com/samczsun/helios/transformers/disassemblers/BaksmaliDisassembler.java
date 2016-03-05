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

package com.samczsun.helios.transformers.disassemblers;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.samczsun.helios.handler.ExceptionHandler;
import com.samczsun.helios.transformers.converters.Converter;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jf.baksmali.baksmaliOptions;
import org.jf.baksmali.main;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.analysis.ClassPath;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedOdexFile;
import org.jf.dexlib2.iface.DexFile;
import org.objectweb.asm.tree.ClassNode;
import org.zeroturnaround.zip.ZipUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

public class BaksmaliDisassembler extends Disassembler {
    public BaksmaliDisassembler() {
        super("baksmali-disassembler", "Baksmali Disassembler");
    }

    @Override
    public boolean isApplicable(String className) {
        return className.endsWith(".apk") || true;
    }

    public boolean disassembleClassNode(ClassNode cn, byte[] b, StringBuilder output) {
        File tempDir = null;
        File tempClass = null;
        File tempZip = null;
        File tempDex = null;
        File tempSmali = null;
        try {
            tempDir = Files.createTempDirectory("smali").toFile();
            tempClass = new File(tempDir, "temp.class");
            tempZip = new File(tempDir, "temp.jar");
            tempDex = new File(tempDir, "temp.dex");
            tempSmali = new File(tempDir, "temp-smali");
            FileOutputStream fos = new FileOutputStream(tempClass);
            fos.write(b);
            fos.close();

            ZipUtil.packEntry(tempClass, tempZip);

            Converter.JAR2DEX.convert(tempZip, tempDex);

            baksmaliOptions options = new baksmaliOptions();
            options.outputDirectory = tempSmali.getAbsolutePath();
            options.deodex = true;

            DexFile file = DexFileFactory.loadDexFile(tempDex, options.dexEntry, options.apiLevel, options.experimental);

            //org.jf.baksmali.main.run(options, file)

            org.jf.baksmali.main.main(new String[]{"-o", tempSmali.getAbsolutePath(), "-x", tempDex.getAbsolutePath()});

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
            output.append(FileUtils.readFileToString(outputSmali, "UTF-8"));
            return true;
        } catch (SecurityException e) {
            return false;
        } catch (final Exception e) {
            ExceptionHandler.handle(e);
            return false;
        } finally {
//            FileUtils.deleteQuietly(tempClass);
//            FileUtils.deleteQuietly(tempZip);
//            FileUtils.deleteQuietly(tempDex);
//            FileUtils.deleteQuietly(tempSmali);
//            FileUtils.deleteQuietly(tempDir);
        }
    }
}
