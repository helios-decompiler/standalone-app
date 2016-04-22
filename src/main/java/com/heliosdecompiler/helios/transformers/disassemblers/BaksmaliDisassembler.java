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

package com.heliosdecompiler.helios.transformers.disassemblers;

import com.android.dx.command.dexer.Main;
import com.android.dx.dex.DexFormat;
import com.android.dx.dex.DexOptions;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.io.output.StringBuilderWriter;
import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.baksmali.baksmaliOptions;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.util.IndentingWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.util.Iterator;

public class BaksmaliDisassembler extends Disassembler {
    private static final MethodHandle OUTPUT_DEX;
    private static final MethodHandle ARGS;
    private static final MethodHandle PROCESS_FILE_BYTES;
    private static final MethodHandle WRITE_DEX;

    static {
        Class<?> dexer = com.android.dx.command.dexer.Main.class;
        try {
            Constructor<MethodHandles.Lookup> lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            lookupConstructor.setAccessible(true);
            MethodHandles.Lookup lookup = lookupConstructor.newInstance(Object.class, -1);
            OUTPUT_DEX = lookup.findStaticSetter(dexer, "outputDex", com.android.dx.dex.file.DexFile.class);
            ARGS = lookup.findStaticSetter(dexer, "args", Main.Arguments.class);
            PROCESS_FILE_BYTES = lookup.findStatic(dexer, "processFileBytes", MethodType.methodType(boolean.class, String.class, long.class, byte[].class));
            WRITE_DEX = lookup.findStatic(dexer, "writeDex", MethodType.methodType(byte[].class));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public BaksmaliDisassembler() {
        super("baksmali", "Baksmali");
    }

    @Override
    public boolean isApplicable(String className) {
        return className.endsWith(".class");
    }

    public boolean disassembleClassNode(ClassNode cn, byte[] b, StringBuilder output) {
        try {
            PrintStream oldErr = System.err;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            System.setErr(new PrintStream(outputStream));

            DexOptions dexOptions = new DexOptions();
            dexOptions.targetApiLevel = DexFormat.API_NO_EXTENDED_OPCODES;
            OUTPUT_DEX.invoke(new com.android.dx.dex.file.DexFile(dexOptions));
            Main.Arguments args = new Main.Arguments();
            args.parse(new String[]{"--no-strict", "Helios.class"});
            ARGS.invoke(args);

            PROCESS_FILE_BYTES.invoke("Helios.class", System.currentTimeMillis(), b);
            byte[] bytes = (byte[]) WRITE_DEX.invoke();

            OUTPUT_DEX.invoke((Object) null);
            ARGS.invoke((Object) null);

            baksmaliOptions options = new baksmaliOptions();
            options.deodex = true;
            DexFile file = new DexBackedDexFile(Opcodes.forApi(options.apiLevel), bytes);
            Iterator<? extends ClassDef> it = file.getClasses().iterator();

            System.setErr(oldErr);

            if (it.hasNext()) {
                ClassDefinition classDefinition = new ClassDefinition(options, it.next());
                classDefinition.writeTo(new IndentingWriter(new StringBuilderWriter(output)));
                return true;
            } else {
                throw new IllegalStateException("Baksmali failed to read the class file\n" + new String(outputStream.toByteArray(), "UTF-8"));
            }
        } catch (final Throwable e) {
            output.setLength(0);
            output.append(parseException(e));
            return false;
        }
    }
}
