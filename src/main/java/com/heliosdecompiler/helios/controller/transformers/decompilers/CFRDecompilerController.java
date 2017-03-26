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

package com.heliosdecompiler.helios.controller.transformers.decompilers;

import com.google.inject.Singleton;
import com.heliosdecompiler.helios.controller.configuration.ConfigurationSerializer;
import com.heliosdecompiler.helios.controller.configuration.IntegerSetting;
import com.heliosdecompiler.helios.controller.configuration.Setting;
import com.heliosdecompiler.helios.controller.configuration.Troolean;
import com.heliosdecompiler.transformerapi.StandardTransformers;
import com.heliosdecompiler.transformerapi.decompilers.cfr.CFRSettings;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.getopt.PermittedOptionProvider;

import java.lang.reflect.Field;

@Singleton
public class CFRDecompilerController extends DecompilerController<CFRSettings> {

    public CFRDecompilerController() {
        super("CFR Decompiler", "cfr", StandardTransformers.Decompilers.CFR);
    }

    @Override
    protected CFRSettings defaultSettings() {
        return getDecompiler().defaultSettings();
    }

    @Override
    protected void registerSettings() {
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.SUGAR_STRINGBUFFER, true));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.SUGAR_STRINGBUILDER, true));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.ENUM_SWITCH, true));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.ENUM_SUGAR, true));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.STRING_SWITCH, true));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.ARRAY_ITERATOR, true));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.COLLECTION_ITERATOR, true));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.REWRITE_LAMBDAS, true));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.DECOMPILE_INNER_CLASSES));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.HIDE_UTF8));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.HIDE_LONGSTRINGS));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.REMOVE_BOILERPLATE));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.REMOVE_INNER_CLASS_SYNTHETICS));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.HIDE_BRIDGE_METHODS));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.LIFT_CONSTRUCTOR_INIT));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.REMOVE_DEAD_METHODS));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.REMOVE_BAD_GENERICS));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.SUGAR_ASSERTS));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.SUGAR_BOXING));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.SHOW_CFR_VERSION));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.DECODE_FINALLY));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.TIDY_MONITORS));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.COMMENT_MONITORS));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.LENIENT));
//        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.DUMP_CLASS_PATH));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.DECOMPILER_COMMENTS));
        registerSetting(Troolean.class, new RawTrooleanSetting(OptionsImpl.FORCE_TOPSORT));
        registerSetting(Troolean.class, new RawTrooleanSetting(OptionsImpl.FOR_LOOP_CAPTURE));
        registerSetting(Troolean.class, new RawTrooleanSetting(OptionsImpl.FORCE_TOPSORT_EXTRA));
        registerSetting(Troolean.class, new RawTrooleanSetting(OptionsImpl.FORCE_COND_PROPAGATE));
        registerSetting(Troolean.class, new RawTrooleanSetting(OptionsImpl.FORCE_RETURNING_IFS));
        registerSetting(Troolean.class, new RawTrooleanSetting(OptionsImpl.FORCE_PRUNE_EXCEPTIONS));
        registerSetting(Troolean.class, new RawTrooleanSetting(OptionsImpl.FORCE_AGGRESSIVE_EXCEPTION_AGG));
        registerSetting(Troolean.class, new RawTrooleanSetting(OptionsImpl.RECOVER_TYPECLASHES));
        registerSetting(Troolean.class, new RawTrooleanSetting(OptionsImpl.USE_RECOVERED_ITERATOR_TYPE_HINTS));
//        registerSetting(Troolean.class, new RawTrooleanSetting(OptionsImpl.OUTPUT_DIR));
//        registerSetting(Troolean.class, new RawTrooleanSetting(OptionsImpl.OUTPUT_PATH));
//        registerSetting(Troolean.class, new RawTrooleanSetting(OptionsImpl.CLOBBER_FILES));
        registerSetting(Integer.class, new RawIntegerSetting(OptionsImpl.SHOWOPS, 0, Integer.MAX_VALUE, 1));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.SILENT));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.RECOVER));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.ECLIPSE));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.OVERRIDES, true));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.SHOW_INFERRABLE, true));
//        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.HELP));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.ALLOW_CORRECTING));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.LABELLED_BLOCKS));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.JAVA_4_CLASS_OBJECTS, true));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.HIDE_LANG_IMPORTS));
        registerSetting(Integer.class, new RawIntegerSetting(OptionsImpl.FORCE_PASS, 0, Integer.MAX_VALUE, 1));
//        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.ANALYSE_AS));
//        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.JAR_FILTER));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.RENAME_MEMBERS));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.RENAME_DUP_MEMBERS));
        registerSetting(Integer.class, new RawIntegerSetting(OptionsImpl.RENAME_SMALL_MEMBERS, 0, Integer.MAX_VALUE, 1));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.RENAME_ILLEGAL_IDENTS));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.RENAME_ENUM_MEMBERS));
        registerSetting(Integer.class, new RawIntegerSetting(OptionsImpl.AGGRESSIVE_SIZE_REDUCTION_THRESHOLD, 0, Integer.MAX_VALUE, 1));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.STATIC_INIT_RETURN));
//        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.FILENAME));
//        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.METHODNAME));
//        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.EXTRA_CLASS_PATH));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.PULL_CODE_CASE));
        registerSetting(Boolean.class, new RawBooleanSetting(OptionsImpl.ELIDE_SCALA));
    }

    private class RawBooleanSetting extends Setting<Boolean, CFRSettings> {

        RawBooleanSetting(PermittedOptionProvider.Argument<Boolean> argument) {
            super(Boolean.class, Boolean.parseBoolean(argument.getFn().getDefaultValue()), ConfigurationSerializer.BOOLEAN, argument.getName(), getHelp(argument));
        }

        RawBooleanSetting(PermittedOptionProvider.ArgumentParam<Boolean, ClassFileVersion> argument, boolean defaultValue) {
            super(Boolean.class, defaultValue, ConfigurationSerializer.BOOLEAN, argument.getName(), getHelp(argument));
        }

        @Override
        public void apply(CFRSettings cfrSettings, Boolean value) {
            cfrSettings.set(getId(), String.valueOf(value));
        }
    }

    private class RawTrooleanSetting extends Setting<Troolean, CFRSettings> {

        RawTrooleanSetting(PermittedOptionProvider.Argument<org.benf.cfr.reader.util.Troolean> argument) {
            super(Troolean.class, Troolean.parseTroolean(argument.getFn().getDefaultValue()), ConfigurationSerializer.TROOLEAN, argument.getName(), getHelp(argument));
        }

        @Override
        public void apply(CFRSettings cfrSettings, Troolean value) {
            if (value == Troolean.NEITHER)
                cfrSettings.set(getId(), null);
            else
                cfrSettings.set(getId(), value.name());
        }
    }

    private class RawIntegerSetting extends IntegerSetting<CFRSettings> {
        RawIntegerSetting(PermittedOptionProvider.Argument<Integer> argument, int min, int max, int step) {
            super(Integer.class, Integer.parseInt(argument.getFn().getDefaultValue()), ConfigurationSerializer.INTEGER, argument.getName(), getHelp(argument), min, max, step);
        }

        @Override
        public void apply(CFRSettings cfrSettings, Integer value) {
            cfrSettings.set(getId(), String.valueOf(value));
        }

        @Override
        public boolean isValid(Integer i) {
            return i >= 0;
        }
    }

    private static String getHelp(PermittedOptionProvider.ArgumentParam<?, ?> argumentParam) {
        try {
            Field field = PermittedOptionProvider.ArgumentParam.class.getDeclaredField("help");
            field.setAccessible(true);
            return (String) field.get(argumentParam);
        } catch (ReflectiveOperationException ex) {
            return "";
        }
    }
}
