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

import com.android.dx.command.dexer.Main;
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
    private static final Class<?> DEXER;
    private static final Field OUTPUT_DEX;
    private static final Field ARGS;
    private static final Method PROCESS_FILE_BYTES;
    private static final Method WRITE_DEX;

    static {
        DEXER = com.android.dx.command.dexer.Main.class;
        try {
            OUTPUT_DEX = DEXER.getDeclaredField("outputDex");
            OUTPUT_DEX.setAccessible(true);
            ARGS = DEXER.getDeclaredField("args");
            ARGS.setAccessible(true);
            PROCESS_FILE_BYTES = DEXER.getDeclaredMethod("processFileBytes", String.class, long.class, byte[].class);
            PROCESS_FILE_BYTES.setAccessible(true);
            WRITE_DEX = DEXER.getDeclaredMethod("writeDex");
            WRITE_DEX.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public BaksmaliDisassembler() {
        super("baksmali-disassembler", "Baksmali Disassembler");
    }

    @Override
    public boolean isApplicable(String className) {
        return className.endsWith(".class");
    }

    public boolean disassembleClassNode(ClassNode cn, byte[] b, StringBuilder output) {
        try {
            DexOptions dexOptions = new DexOptions();
            dexOptions.targetApiLevel = DexFormat.API_NO_EXTENDED_OPCODES;
            OUTPUT_DEX.set(null, new com.android.dx.dex.file.DexFile(dexOptions));
            Main.Arguments args = new Main.Arguments();
            args.parse(new String[] {"--no-strict", "Helios.class"});
            ARGS.set(null, args);

            PROCESS_FILE_BYTES.invoke(null, "Helios.class", System.currentTimeMillis(), b);
            byte[] bytes = (byte[]) WRITE_DEX.invoke(null);

            OUTPUT_DEX.set(null, null);
            ARGS.set(null, null);

            baksmaliOptions options = new baksmaliOptions();
            options.deodex = true;
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
