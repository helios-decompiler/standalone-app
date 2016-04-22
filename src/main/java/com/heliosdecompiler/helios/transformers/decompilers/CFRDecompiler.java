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

package com.heliosdecompiler.helios.transformers.decompilers;

import com.heliosdecompiler.helios.Helios;
import com.heliosdecompiler.helios.transformers.TransformerSettings;
import org.benf.cfr.reader.PluginRunner;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CFRDecompiler extends Decompiler {

    CFRDecompiler() {
        super("cfr", "CFR", Settings.class);
    }

    @Override
    public boolean decompile(ClassNode classNode, byte[] bytes, StringBuilder output) {
        PluginRunner pluginRunner = new PluginRunner(generateOptions(), new ClassFileSource() {
            @Override
            public void informAnalysisRelativePathDetail(String s, String s1) {
                System.out.println("Relative: " + s + " " + s1);
            }

            @Override
            public Collection<String> addJar(String s) {
                throw new UnsupportedOperationException("Return paths of all classfiles in jar.");
            }

            @Override
            public Pair<byte[], String> getClassFileContent(String s) throws IOException {
                if (s.equals(classNode.name + ".class")) {
                    return Pair.make(bytes, s);
                }
                return Pair.make(Helios.getAllLoadedData().get(s), s);
            }
        });
        try {
            output.append(pluginRunner.getDecompilationFor(classNode.name));
            return true;
        } catch (Throwable t) {
            output.append(parseException(t));
            return false;
        }
    }

    public Map<String, String> generateOptions() {
        Map<String, String> options = new HashMap<>();
        for (CFRDecompiler.Settings setting : CFRDecompiler.Settings.values()) {
            options.put(setting.getParam(), String.valueOf(setting.isEnabled()));
        }
        return options;
    }

    public enum Settings implements TransformerSettings.Setting {
        DECODE_ENUM_SWITCH("decodeenumswitch", "Decode Enum Switch", true),
        SUGAR_ENUMS("sugarenums", "SugarEnums", true),
        DECODE_STRING_SWITCH("decodestringswitch", "Decode String Switch", true),
        ARRAYITER("arrayiter", "Arrayiter", true),
        COLLECTIONITER("collectioniter", "Collectioniter", true),
        INNER_CLASSES("innerclasses", "Inner Classes", true),
        REMOVE_BOILER_PLATE("removeboilerplate", "Remove Boiler Plate", true),
        REMOVE_INNER_CLASS_SYNTHETICS("removeinnerclasssynthetics", "Remove Inner Class Synthetics", true),
        DECODE_LAMBDAS("decodelambdas", "Decode Lambdas", true),
        HIDE_BRIDGE_METHODS("hidebridgemethods", "Hide Bridge Methods", true),
        LIFT_CONSTRUCTOR_INIT("liftconstructorinit", "Lift Constructor Init", true),
        REMOVE_DEAD_METHODS("removedeadmethods", "Remove Dead Methods", true),
        REMOVE_BAD_GENERICS("removebadgenerics", "Remove Bad Generics", true),
        SUGAR_ASSERTS("sugarasserts", "Sugar Asserts", true),
        SUGAR_BOXING("sugarboxing", "Sugar Boxing", true),
        SHOW_VERSION("showversion", "Show Version", true),
        DECODE_FINALLY("decodefinally", "Decode Finally", true),
        TIDY_MONITORS("tidymonitors", "Tidy Monitors", true),
        LENIENT("lenient", "Lenient"),
        DUMP_CLASS_PATH("dumpclasspath", "Dump Classpath"),
        COMMENTS("comments", "Comments", true),
        FORCE_TOP_SORT("forcetopsort", "Force Top Sort", true),
        FORCE_TOP_SORT_AGGRESSIVE("forcetopsortaggress", "Force Top Sort Aggressive", true),
        STRINGBUFFER("stringbuffer", "StringBuffer"),
        STRINGBUILDER("stringbuilder", "StringBuilder", true),
        SILENT("silent", "Silent", true),
        RECOVER("recover", "Recover", true),
        ECLIPSE("eclipse", "Eclipse", true),
        OVERRIDE("override", "Override", true),
        SHOW_INFERRABLE("showinferrable", "Show Inferrable", true),
        FORCE_AGGRESSIVE_EXCEPTION_AGG("aexagg", "Force Aggressive Exception Aggregation", true),
        FORCE_COND_PROPAGATE("forcecondpropagate", "Force Conditional Propogation", true),
        HIDE_UTF("hideutf", "Hide UTF", true),
        HIDE_LONG_STRINGS("hidelongstrings", "Hide Long Strings"),
        COMMENT_MONITORS("commentmonitors", "Comment Monitors"),
        ALLOW_CORRECTING("allowcorrecting", "Allow Correcting", true),
        LABELLED_BLOCKS("labelledblocks", "Labelled Blocks", true),
        J14_CLASS_OBJ("j14classobj", "Java 1.4 Class Objects"),
        HIDE_LANG_IMPORTS("hidelangimports", "Hide Lang Imports", true),
        RECOVER_TYPE_CLASH("recovertypeclash", "Recover Type Clash", true),
        RECOVER_TYPE_HINTS("recovertypehints", "Recover Type Hints", true),
        FORCE_RETURNING_IFS("forcereturningifs", "Force Returning Ifs", true),
        FOR_LOOP_AGG_CAPTURE("forloopaggcapture", "For Loop Aggressive Capture", true),
        RENAME_ILLEGAL_IDENTIFIERS("renameillegalidents", "Rename illegal identifiers", false),
        RENAME_DUPE_MEMBERS("renamedupmembers", "Rename duplicated member names", false);

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
