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

/* Sourced from The MIT License (MIT)

        Copyright (c) 2011-2014 Lee Benfield - http://www.benf.org/other/cfr

        Permission is hereby granted, free of charge, to any person obtaining a copy
        of this software and associated documentation files (the "Software"), to deal
        in the Software without restriction, including without limitation the rights
        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
        copies of the Software, and to permit persons to whom the Software is
        furnished to do so, subject to the following conditions:

        The above copyright notice and this permission notice shall be included in
        all copies or substantial portions of the Software.

        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
        IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
        FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
        AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
        LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
        OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
        THE SOFTWARE.
 */

package com.samczsun.helios.transformers.decompilers;

import com.samczsun.helios.transformers.TransformerSettings;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.relationship.MemberNameResolver;
import org.benf.cfr.reader.state.ClassFileSourceImpl;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.CannotLoadClassException;
import org.benf.cfr.reader.util.ListFactory;
import org.benf.cfr.reader.util.bytestream.BaseByteData;
import org.benf.cfr.reader.util.getopt.GetOptParser;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.ToStringDumper;
import org.objectweb.asm.tree.ClassNode;

public class CFRDecompiler extends Decompiler {

    public CFRDecompiler() {
        super("cfr-decompiler", "CFR Decompiler");
        for (Settings setting : Settings.values()) {
            settings.registerSetting(setting);
        }
    }

    public static String doClass(DCCommonState dcCommonState, byte[] content1) {
        Options options = dcCommonState.getOptions();
        Dumper d = new ToStringDumper();
        BaseByteData data = new BaseByteData(content1);
        ClassFile var24 = new ClassFile(data, "", dcCommonState);
        dcCommonState.configureWith(var24);

        try {
            var24 = dcCommonState.getClassFile(var24.getClassType());
        } catch (CannotLoadClassException var18) {
        }

        if (options.getOption(OptionsImpl.DECOMPILE_INNER_CLASSES)) {
            var24.loadInnerClasses(dcCommonState);
        }

        if (options.getOption(OptionsImpl.RENAME_MEMBERS)) {
            MemberNameResolver.resolveNames(dcCommonState,
                    ListFactory.newList(dcCommonState.getClassCache().getLoadedTypes()));
        }

        var24.analyseTop(dcCommonState);
        TypeUsageCollector var25 = new TypeUsageCollector(var24);
        var24.collectTypeUsages(var25);
        String var26 = options.getOption(OptionsImpl.METHODNAME);
        if (var26 == null) {
            var24.dump(d);
        } else {
            try {
                for (Method method : var24.getMethodByName(var26)) {
                    method.dump(d, true);
                }
            } catch (NoSuchMethodException var19) {
                throw new IllegalArgumentException("No such method \'" + var26 + "\'.");
            }
        }
        d.print("");
        return d.toString();
    }

    @Override
    public boolean decompile(ClassNode classNode, byte[] bytes, StringBuilder output) {
        try {
            Options options = new GetOptParser().parse(generateMainMethod(classNode.name), OptionsImpl.getFactory());
            ClassFileSourceImpl classFileSource = new ClassFileSourceImpl(options);
            DCCommonState dcCommonState = new DCCommonState(options, classFileSource);
            output.append(doClass(dcCommonState, bytes));
            return true;
        } catch (Exception e) {
            output.append(parseException(e));
            return false;
        }
    }

    public String[] generateMainMethod(String className) {
        String[] result = new String[getSettings().size() * 2 + 1];
        result[0] = className;
        int index = 1;
        for (Settings setting : Settings.values()) {
            result[index++] = "--" + setting.getParam();
            result[index++] = String.valueOf(getSettings().isSelected(setting));
        }
        return result;
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
        FOR_LOOP_AGG_CAPTURE("forloopaggcapture", "For Loop Aggressive Capture", true);

        private final String name;
        private final String param;
        private final boolean on;

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

        public boolean isDefaultOn() {
            return on;
        }
    }
}
