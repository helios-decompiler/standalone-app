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

package com.samczsun.helios.transformers.decompilers;

import com.samczsun.helios.Helios;
import com.samczsun.helios.LoadedFile;
import com.samczsun.helios.handler.ExceptionHandler;
import com.samczsun.helios.transformers.TransformerSettings;
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Manifest;

public class FernflowerDecompiler extends Decompiler {

    public FernflowerDecompiler() {
        super("fernflower-decompiler", "Fernflower Decompiler");
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

            Map<String, byte[]> importantClasses = new HashMap<>();
            importantClasses.put(classNode.name + ".class", bytesToUse);
            Set<LoadedFile> files = new HashSet<>();
            files.addAll(Helios.getAllFiles());
            files.addAll(Helios.getPathFiles().values());
            if (classNode.innerClasses != null) {
                Set<String> innerClasses = new HashSet<>();
                LinkedList<String> list = new LinkedList<>();
                list.add(classNode.name);
                while (!list.isEmpty()) {
                    String className = list.poll();
                    if (innerClasses.add(className)) {
                        for (LoadedFile file : files) {
                            if (file.getClassNode(className) != null) {
                                if (!importantClasses.containsKey(className + ".class")) {
                                    importantClasses.put(className + ".class", file.getData().get(className + ".class"));
                                }
                                ClassNode node = file.getClassNode(className);
                                if (node.innerClasses != null) {
                                    node.innerClasses.forEach(icn -> list.add(icn.name));
                                }
                            }
                        }
                    }
                }
            }

            Map<String, Object> options = main(generateMainMethod());

            Object lock = new Object();

            final AtomicReference<String> result = new AtomicReference<>();
            result.set(null);

            BaseDecompiler baseDecompiler = new BaseDecompiler((s, s1) -> {
                byte[] b = importantClasses.get(s);
                byte[] clone = new byte[b.length];
                System.arraycopy(b, 0, clone, 0, b.length);
                return clone;
            }, new IResultSaver() {
                @Override
                public void saveFolder(String s) {

                }

                @Override
                public void copyFile(String s, String s1, String s2) {

                }

                @Override
                public void saveClassFile(String s, String s1, String s2, String s3, int[] ints) {
                    if (s1.equals(classNode.name)) {
                        result.set(s3);
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                }

                @Override
                public void createArchive(String s, String s1, Manifest manifest) {

                }

                @Override
                public void saveDirEntry(String s, String s1, String s2) {

                }

                @Override
                public void copyEntry(String s, String s1, String s2, String s3) {

                }

                @Override
                public void saveClassEntry(String s, String s1, String s2, String s3, String s4) {
                }

                @Override
                public void closeArchive(String s, String s1) {

                }
            }, options, new PrintStreamLogger(System.out));

            System.out.println("Decompiling");
            importantClasses.forEach((str, barr) -> {
                try {
                    baseDecompiler.addSpace(new File(str) {
                        @Override
                        public String getAbsolutePath() {
                            return str; //For wacky zip paths
                        }
                    }, true);
                } catch (IOException e) {
                    ExceptionHandler.handle(e);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            baseDecompiler.decompileContext();
            if (result.get() == null) {
                synchronized (lock) {
                    lock.wait();
                }
            }
            output.append(result.get());
            return true;
        } catch (Exception e) {
            output.append(parseException(e));
            return false;
        }
    }


    public Map<String, Object> main(String[] args) {
        HashMap<String, Object> mapOptions = new HashMap<>();
        boolean isOption = true;

        for (int destination = 0; destination < args.length - 1; ++destination) {
            String logger = args[destination];
            if (isOption && logger.length() > 5 && logger.charAt(0) == 45 && logger.charAt(4) == 61) {
                String decompiler = logger.substring(5);
                if ("true".equalsIgnoreCase(decompiler)) {
                    decompiler = "1";
                } else if ("false".equalsIgnoreCase(decompiler)) {
                    decompiler = "0";
                }

                mapOptions.put(logger.substring(1, 4), decompiler);
            } else {
                isOption = false;
            }
        }

        return mapOptions;
    }

    private String[] generateMainMethod() {
        String[] result = new String[getSettings().size()];
        int index = 0;
        for (Settings setting : Settings.values()) {
            result[index++] = String.format("-%s=%s", setting.getParam(),
                    setting.isEnabled() ? "1" : "0");
        }
        return result;
    }

    public enum Settings implements TransformerSettings.Setting {
        HIDE_BRIDGE_METHODS("rbr", "Hide Bridge Methods", true),
        HIDE_SYNTHETIC_CLASS_MEMBERS("rsy", "Hide Synthetic Class Members"),
        DECOMPILE_INNER_CLASSES("din", "Decompile Inner Classes", true),
        COLLAPSE_14_CLASS_REFERENCES("dc4", "Collapse 1.4 Class References", true),
        DECOMPILE_ASSERTIONS("das", "Decompile Assertions", true),
        HIDE_EMPTY_SUPER_INVOCATION("hes", "Hide Empty Super Invocation", true),
        HIDE_EMPTY_DEFAULT_CONSTRUCTOR("hec", "Hide Empty Default Constructor", true),
        DECOMPILE_GENERIC_SIGNATURES("dgs", "Decompile Generic Signatures"),
        ASSUME_RETURN_NOT_THROWING_EXCEPTIONS("ner", "Assume return not throwing exceptions", true),
        DECOMPILE_ENUMS("den", "Decompile enumerations", true),
        REMOVE_GETCLASS("rgn", "Remove getClass()", true),
        OUTPUT_NUMBERIC_LITERALS("lit", "Output numeric literals 'as-is'"),
        ENCODE_UNICODE("asc", "Encode non-ASCII as unicode escapes"),
        INT_1_AS_BOOLEAN_TRUE("bto", "Assume int 1 is boolean true", true),
        ALLOW_NOT_SET_SYNTHETIC("nns", "Allow not set synthetic attribute", true),
        NAMELESS_TYPES_AS_OBJECT("uto", "Consider nameless types as java.lang.Object", true),
        RECOVER_VARIABLE_NAMES("udv", "Recover variable names", true),
        REMOVE_EMPTY_EXCEPTIONS("rer", "Remove empty exceptions", true),
        DEINLINE_FINALLY("fdi", "De-inline finally", true),
        RENAME_AMBIGIOUS_MEMBERS("ren", "Rename ambigious members"),
        REMOVE_INTELLIJ_NOTNULL("inn", "Remove IntelliJ @NotNull", true),
        DECOMPILE_LAMBDA_TO_ANONYMOUS("lac", "Decompile lambdas to anonymous classes");

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
