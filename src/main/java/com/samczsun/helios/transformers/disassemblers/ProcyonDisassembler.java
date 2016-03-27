package com.samczsun.helios.transformers.disassemblers;

import com.samczsun.helios.Helios;
import com.samczsun.helios.transformers.TransformerSettings;
import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.*;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.Languages;
import org.objectweb.asm.tree.ClassNode;

import java.io.StringWriter;
import java.util.Map;

public class ProcyonDisassembler extends Disassembler {
    public ProcyonDisassembler() {
        super("procyon", "Procyon");
        for (Settings setting : Settings.values()) {
            settings.registerSetting(setting);
        }
    }

    @Override
    public boolean disassembleClassNode(ClassNode classNode, byte[] bytes, StringBuilder output) {
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
        DecompilerSettings settings = new DecompilerSettings();
        settings.setIncludeLineNumbersInBytecode(Settings.INCLUDE_LINE_NUMBERS_IN_BYTECODE.isEnabled());
        settings.setUnicodeOutputEnabled(Settings.UNICODE_OUTPUT.isEnabled());
        settings.setTypeLoader(new InputTypeLoader());
        if (Settings.BYTECODE.isEnabled()) {
            settings.setLanguage(Languages.bytecode());
        } else if (Settings.AST_UNOPTIMIZED.isEnabled()) {
            settings.setLanguage(Languages.bytecodeAstUnoptimized());
        } else if (Settings.AST.isEnabled()) {
            settings.setLanguage(Languages.bytecodeAst());
        } else {
            settings.setLanguage(Languages.bytecode());
        }
        return settings;
    }

    @Override
    public boolean isApplicable(String className) {
        return className.endsWith(".class");
    }



    public enum Settings implements TransformerSettings.Setting {
        UNICODE_OUTPUT("unicode", "Allow Unicode Output"),
        INCLUDE_LINE_NUMBERS_IN_BYTECODE("with-line-numbers", "Include line numbers in bytecode"),
        BYTECODE("bytecode", "Use raw bytecode", true),
        AST_UNOPTIMIZED("ast-unoptimized", "Use unoptimized AST"),
        AST("ast", "Use optimized AST");

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
