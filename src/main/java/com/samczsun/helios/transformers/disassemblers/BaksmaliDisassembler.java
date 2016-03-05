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

import com.android.dx.dex.DexFormat;
import com.android.dx.dex.DexOptions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.samczsun.helios.handler.ExceptionHandler;
import com.samczsun.helios.transformers.converters.Converter;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.baksmali.baksmaliOptions;
import org.jf.baksmali.main;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.analysis.ClassPath;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedOdexFile;
import org.jf.dexlib2.iface.DexFile;
import org.jf.util.IndentingWriter;
import org.objectweb.asm.tree.ClassNode;
import org.zeroturnaround.zip.ZipUtil;

import javax.annotation.Nonnull;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
        try {
            baksmaliOptions options = new baksmaliOptions();
            options.deodex = true;

            Class<?> clazz = com.android.dx.command.dexer.Main.class;
            Field outputDex = clazz.getDeclaredField("outputDex");
            outputDex.setAccessible(true);
            DexOptions dexOptions = new DexOptions();
            dexOptions.targetApiLevel = DexFormat.API_NO_EXTENDED_OPCODES;
            outputDex.set(null, new com.android.dx.dex.file.DexFile(dexOptions));

            Method processFileBytes = clazz.getDeclaredMethod("processFileBytes", String.class, long.class, byte[].class);
            processFileBytes.setAccessible(true);
            processFileBytes.invoke(null, "Helios.class", System.currentTimeMillis(), b);
            Method writeDex = clazz.getDeclaredMethod("writeDex");
            writeDex.setAccessible(true);
            byte[] bytes = (byte[]) writeDex.invoke(null);

            outputDex.set(null, null);

            DexFile file = new DexBackedDexFile(Opcodes.forApi(options.apiLevel), bytes);
            ClassDefinition classDefinition = new ClassDefinition(options, file.getClasses().iterator().next());
            classDefinition.writeTo(new IndentingWriter(new StringBuilderWriter(output)));
            return true;
        } catch (final Exception e) {
            ExceptionHandler.handle(e);
            return false;
        }
    }
}
