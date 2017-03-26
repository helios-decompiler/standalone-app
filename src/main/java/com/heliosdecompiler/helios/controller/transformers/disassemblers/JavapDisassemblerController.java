/*
 * Copyright 2017 Sam Sun <github-contact@samczsun.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.heliosdecompiler.helios.controller.transformers.disassemblers;

import com.google.inject.Singleton;
import com.heliosdecompiler.helios.controller.configuration.ConfigurationSerializer;
import com.heliosdecompiler.helios.controller.configuration.Setting;
import com.heliosdecompiler.transformerapi.StandardTransformers;
import com.sun.tools.classfile.AccessFlags;
import com.sun.tools.javap.Options;

import java.util.function.BiConsumer;

@Singleton
public class JavapDisassemblerController extends DisassemblerController<Options> {

    public JavapDisassemblerController() {
        super("Javap Disassembler", "javap-disassembler", StandardTransformers.Disassemblers.JAVAP);
    }

    @Override
    protected void registerSettings() {
        registerSetting(Boolean.class, new RawBooleanSetting("verbose", "Print additional information", true, (obj, val) -> {
            obj.verbose = val;
            obj.showDescriptors = val;
            obj.showFlags = val;
            obj.showAllAttrs = val;
        }));
        registerSetting(Boolean.class, new RawBooleanSetting("l", "Print line number and local variable tables", true, (obj, val) -> {
            obj.showLineAndLocalVariableTables = val;
        }));
        registerSetting(Boolean.class, new RawBooleanSetting("public", "Show only public classes and members", false, (obj, val) -> {
            obj.accessOptions.add("-public");
            obj.showAccess = AccessFlags.ACC_PUBLIC;
        }));
        registerSetting(Boolean.class, new RawBooleanSetting("protected", "Show protected/public classes and members", false, (obj, val) -> {
            obj.accessOptions.add("-protected");
            obj.showAccess = AccessFlags.ACC_PROTECTED;
        }));
        registerSetting(Boolean.class, new RawBooleanSetting("package", "Show package/protected/public classes and members (default)", false, (obj, val) -> {
            obj.accessOptions.add("-package");
            obj.showAccess = 0;
        }));
        registerSetting(Boolean.class, new RawBooleanSetting("private", "Show all classes and members", true, (obj, val) -> {
            if (!obj.accessOptions.contains("-p") &&
                    !obj.accessOptions.contains("-private")) {
                obj.accessOptions.add("-private");
            }
            obj.showAccess = AccessFlags.ACC_PRIVATE;
        }));
        registerSetting(Boolean.class, new RawBooleanSetting("c", "Disassemble the code", true, (obj, val) -> {
            obj.showDisassembled = val;
        }));
        registerSetting(Boolean.class, new RawBooleanSetting("s", "Print internal type signatures", true, (obj, val) -> {
            obj.showDescriptors = val;
        }));
        registerSetting(Boolean.class, new RawBooleanSetting("sysinfo", "Show system info (path, size, date, MD5 hash) of class being processed", true, (obj, val) -> {
            obj.sysInfo = val;
        }));
        registerSetting(Boolean.class, new RawBooleanSetting("constants", "Show static final constants", true, (obj, val) -> {
            obj.showConstants = val;
        }));
        // todo compact, details?
    }

    @Override
    protected Options defaultSettings() {
        return getDisassembler().defaultSettings();
    }

    private class RawBooleanSetting extends Setting<Boolean, Options> {
        private BiConsumer<Options, Boolean> consumer;

        RawBooleanSetting(String id, String desc, boolean defaultValue, BiConsumer<Options, Boolean> consumer) {
            super(Boolean.class, defaultValue, ConfigurationSerializer.BOOLEAN, id, desc);
            this.consumer = consumer;
        }

        @Override
        public void apply(Options settings, Boolean value) {
            consumer.accept(settings, value);
        }
    }
}
