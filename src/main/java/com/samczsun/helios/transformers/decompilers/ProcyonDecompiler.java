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
 *
 * Copyright (c) 2015 Mike Strobel
 *
 *      This source code is subject to terms and conditions of the Apache License, Version 2.0.
 *      A copy of the license can be found in the License.html file at the root of this distribution.
 *      By using this source code in any fashion, you are agreeing to be bound by the terms of the
 *      Apache License, Version 2.0.
 *
 *      You must not remove this notice, or any other, from this software.
 */

package com.samczsun.helios.transformers.decompilers;

import com.beust.jcommander.JCommander;
import com.samczsun.helios.Helios;
import com.samczsun.helios.transformers.TransformerSettings;
import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.CommandLineOptions;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.Languages;
import org.objectweb.asm.tree.ClassNode;

import java.io.StringWriter;
import java.util.Map;

public class ProcyonDecompiler extends Decompiler {

    public ProcyonDecompiler() {
        super("procyon-decompiler", "Procyon Decompiler");
        for (Settings setting : Settings.values()) {
            settings.registerSetting(setting);
        }
    }

    @Override
    public boolean decompile(final ClassNode classNode, byte[] bytes, StringBuilder output) {
        try {
            if (classNode.version < 49) {
                bytes = fixBytes(bytes);
            }
            final byte[] bytesToUse = bytes;
            final Map<String, byte[]> loadedClasses = Helios.getAllLoadedData();
            DecompilerSettings settings = getDecompilerSettings();
            MetadataSystem metadataSystem = new MetadataSystem(new ITypeLoader() {
                private final InputTypeLoader backLoader = new InputTypeLoader();

                @Override
                public boolean tryLoadType(String s, Buffer buffer) {
                    if (s.equals(classNode.name)) {
                        buffer.putByteArray(bytesToUse, 0, bytesToUse.length);
                        buffer.position(0);
                        return true;
                    } else {
                        byte[] toUse = loadedClasses.get(s + ".class");
                        if (toUse != null) {
                            buffer.putByteArray(toUse, 0, toUse.length);
                            buffer.position(0);
                            return true;
                        } else {
                            return backLoader.tryLoadType(s, buffer);
                        }
                    }
                }
            });
            TypeReference type = metadataSystem.lookupType(classNode.name);
            DecompilationOptions decompilationOptions = new DecompilationOptions();
            decompilationOptions.setSettings(DecompilerSettings.javaDefaults());
            decompilationOptions.setFullDecompilation(true);
            TypeDefinition resolvedType = null;
            if (type == null || ((resolvedType = type.resolve()) == null)) {
                throw new Exception("Unable to resolve type.");
            }
            StringWriter stringwriter = new StringWriter();
            settings.getLanguage().decompileType(resolvedType, new PlainTextOutput(stringwriter), decompilationOptions);
            String decompiledSource = stringwriter.toString();
            output.append(decompiledSource);
            return true;
        } catch (Throwable e) {
            output.append(parseException(e));
            return false;
        }
    }

    public DecompilerSettings getDecompilerSettings() {
        CommandLineOptions options = new CommandLineOptions();
        JCommander jCommander = new JCommander(options); //TODO remove dependency on JCommander?
        String[] args = new String[Settings.values().length * 2];
        int index = 0;
        for (TransformerSettings.Setting setting : Settings.values()) {
            args[index++] = "--" + setting.getParam();
            args[index++] = String.valueOf(setting.isEnabled());
        }
        jCommander.parse(args);
        DecompilerSettings settings = new DecompilerSettings();
        settings.setFlattenSwitchBlocks(options.getFlattenSwitchBlocks());
        settings.setForceExplicitImports(!options.getCollapseImports());
        settings.setForceExplicitTypeArguments(options.getForceExplicitTypeArguments());
        settings.setRetainRedundantCasts(options.getRetainRedundantCasts());
        settings.setShowSyntheticMembers(options.getShowSyntheticMembers());
        settings.setExcludeNestedTypes(options.getExcludeNestedTypes());
        settings.setOutputDirectory(options.getOutputDirectory());
        settings.setIncludeLineNumbersInBytecode(options.getIncludeLineNumbers());
        settings.setRetainPointlessSwitches(options.getRetainPointlessSwitches());
        settings.setUnicodeOutputEnabled(options.isUnicodeOutputEnabled());
        settings.setMergeVariables(options.getMergeVariables());
        settings.setShowDebugLineNumbers(options.getShowDebugLineNumbers());
        settings.setSimplifyMemberReferences(options.getSimplifyMemberReferences());
        settings.setDisableForEachTransforms(options.getDisableForEachTransforms());
        settings.setTypeLoader(new InputTypeLoader());
        if (options.isRawBytecode()) {
            settings.setLanguage(Languages.bytecode());
        } else if (options.isBytecodeAst()) {
            settings.setLanguage(
                    options.isUnoptimized() ? Languages.bytecodeAstUnoptimized() : Languages.bytecodeAst());
        }
        return settings;
    }

    public enum Settings implements TransformerSettings.Setting {
        SHOW_DEBUG_LINE_NUMBERS("debug-line-numbers", "Show Debug Line Numbers"),
        SIMPLIFY_MEMBER_REFERENCES("simplify-member-references", "Simplify Member References"),
        MERGE_VARIABLES("merge-variables", "Merge Variables"),
        UNICODE_OUTPUT("unicode", "Allow Unicode Output"),
        RETAIN_POINTLESS_SWITCHES("retain-pointless-switches", "Retain pointless switches"),
        INCLUDE_LINE_NUMBERS_IN_BYTECODE("with-line-numbers", "Include line numbers in bytecode"),
        RETAIN_REDUNDANT_CASTS("retain-explicit-casts", "Retain redundant casts"),
        SHOW_SYNTHETIC_MEMBERS("show-synthetic", "Show synthetic members"),
        FORCE_EXPLICIT_TYPE_ARGS("explicit-type-arguments", "Force explicit type arguments"),
        FORCE_EXPLICIT_IMPORTS("explicit-imports", "Force explicit imports"),
        FLATTEN_SWITCH_BLOCKS("flatten-switch-blocks", "Flatten switch blocks"),
        EXCLUDE_NESTED_TYPES("exclude-nested", "Exclude nested types");

        private final String name;
        private final String param;
        private boolean on;

        Settings(String param, String name) {
            this(param, name, false);
        }

        Settings(String param, String name, boolean on) {
            this.name = name;
            this.param = param;
            this.on = on;
        }

        public String getParam() {
            return param;
        }

        public String getText() {
            return name;
        }

        public boolean isEnabled() {
            return on;
        }

        public void setEnabled(boolean enabled) {
            this.on = enabled;
        }
    }
}
