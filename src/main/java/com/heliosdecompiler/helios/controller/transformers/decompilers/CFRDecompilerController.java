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
        registerSetting(new RawBooleanSetting(OptionsImpl.SUGAR_STRINGBUFFER, true));
        registerSetting(new RawBooleanSetting(OptionsImpl.SUGAR_STRINGBUILDER, true));
        registerSetting(new RawBooleanSetting(OptionsImpl.ENUM_SWITCH, true));
        registerSetting(new RawBooleanSetting(OptionsImpl.ENUM_SUGAR, true));
        registerSetting(new RawBooleanSetting(OptionsImpl.STRING_SWITCH, true));
        registerSetting(new RawBooleanSetting(OptionsImpl.ARRAY_ITERATOR, true));
        registerSetting(new RawBooleanSetting(OptionsImpl.COLLECTION_ITERATOR, true));
        registerSetting(new RawBooleanSetting(OptionsImpl.REWRITE_LAMBDAS, true));
        registerSetting(new RawBooleanSetting(OptionsImpl.DECOMPILE_INNER_CLASSES));
        registerSetting(new RawBooleanSetting(OptionsImpl.HIDE_UTF8));
        registerSetting(new RawBooleanSetting(OptionsImpl.HIDE_LONGSTRINGS));
        registerSetting(new RawBooleanSetting(OptionsImpl.REMOVE_BOILERPLATE));
        registerSetting(new RawBooleanSetting(OptionsImpl.REMOVE_INNER_CLASS_SYNTHETICS));
        registerSetting(new RawBooleanSetting(OptionsImpl.HIDE_BRIDGE_METHODS));
        registerSetting(new RawBooleanSetting(OptionsImpl.LIFT_CONSTRUCTOR_INIT));
        registerSetting(new RawBooleanSetting(OptionsImpl.REMOVE_DEAD_METHODS));
        registerSetting(new RawBooleanSetting(OptionsImpl.REMOVE_BAD_GENERICS));
        registerSetting(new RawBooleanSetting(OptionsImpl.SUGAR_ASSERTS));
        registerSetting(new RawBooleanSetting(OptionsImpl.SUGAR_BOXING));
        registerSetting(new RawBooleanSetting(OptionsImpl.SHOW_CFR_VERSION));
        registerSetting(new RawBooleanSetting(OptionsImpl.DECODE_FINALLY));
        registerSetting(new RawBooleanSetting(OptionsImpl.TIDY_MONITORS));
        registerSetting(new RawBooleanSetting(OptionsImpl.COMMENT_MONITORS));
        registerSetting(new RawBooleanSetting(OptionsImpl.LENIENT));
//        registerSetting(new RawBooleanSetting(OptionsImpl.DUMP_CLASS_PATH));
        registerSetting(new RawBooleanSetting(OptionsImpl.DECOMPILER_COMMENTS));
        registerSetting(new RawBooleanSetting(OptionsImpl.COMMENT_MONITORS));
        registerSetting(new RawTrooleanSetting(OptionsImpl.FORCE_TOPSORT));
        registerSetting(new RawTrooleanSetting(OptionsImpl.FOR_LOOP_CAPTURE));
        registerSetting(new RawTrooleanSetting(OptionsImpl.FORCE_TOPSORT_EXTRA));
        registerSetting(new RawTrooleanSetting(OptionsImpl.FORCE_COND_PROPAGATE));
        registerSetting(new RawTrooleanSetting(OptionsImpl.FORCE_RETURNING_IFS));
        registerSetting(new RawTrooleanSetting(OptionsImpl.FORCE_PRUNE_EXCEPTIONS));
        registerSetting(new RawTrooleanSetting(OptionsImpl.FORCE_AGGRESSIVE_EXCEPTION_AGG));
        registerSetting(new RawTrooleanSetting(OptionsImpl.RECOVER_TYPECLASHES));
        registerSetting(new RawTrooleanSetting(OptionsImpl.USE_RECOVERED_ITERATOR_TYPE_HINTS));
//        registerSetting(new RawTrooleanSetting(OptionsImpl.OUTPUT_DIR));
//        registerSetting(new RawTrooleanSetting(OptionsImpl.OUTPUT_PATH));
//        registerSetting(new RawTrooleanSetting(OptionsImpl.CLOBBER_FILES));
        registerSetting(new RawIntegerSetting(OptionsImpl.SHOWOPS));
        registerSetting(new RawBooleanSetting(OptionsImpl.SILENT));
        registerSetting(new RawBooleanSetting(OptionsImpl.RECOVER));
        registerSetting(new RawBooleanSetting(OptionsImpl.ECLIPSE));
        registerSetting(new RawBooleanSetting(OptionsImpl.OVERRIDES, true));
        registerSetting(new RawBooleanSetting(OptionsImpl.SHOW_INFERRABLE, true));
//        registerSetting(new RawBooleanSetting(OptionsImpl.HELP));
        registerSetting(new RawBooleanSetting(OptionsImpl.ALLOW_CORRECTING));
        registerSetting(new RawBooleanSetting(OptionsImpl.LABELLED_BLOCKS));
        registerSetting(new RawBooleanSetting(OptionsImpl.JAVA_4_CLASS_OBJECTS, true));
        registerSetting(new RawBooleanSetting(OptionsImpl.HIDE_LANG_IMPORTS));
        registerSetting(new RawIntegerSetting(OptionsImpl.FORCE_PASS));
//        registerSetting(new RawBooleanSetting(OptionsImpl.ANALYSE_AS));
//        registerSetting(new RawBooleanSetting(OptionsImpl.JAR_FILTER));
        registerSetting(new RawBooleanSetting(OptionsImpl.RENAME_MEMBERS));
        registerSetting(new RawBooleanSetting(OptionsImpl.RENAME_DUP_MEMBERS));
        registerSetting(new RawIntegerSetting(OptionsImpl.RENAME_SMALL_MEMBERS));
        registerSetting(new RawBooleanSetting(OptionsImpl.RENAME_ILLEGAL_IDENTS));
        registerSetting(new RawBooleanSetting(OptionsImpl.RENAME_ENUM_MEMBERS));
        registerSetting(new RawIntegerSetting(OptionsImpl.AGGRESSIVE_SIZE_REDUCTION_THRESHOLD));
        registerSetting(new RawBooleanSetting(OptionsImpl.STATIC_INIT_RETURN));
//        registerSetting(new RawBooleanSetting(OptionsImpl.FILENAME));
//        registerSetting(new RawBooleanSetting(OptionsImpl.METHODNAME));
//        registerSetting(new RawBooleanSetting(OptionsImpl.EXTRA_CLASS_PATH));
        registerSetting(new RawBooleanSetting(OptionsImpl.PULL_CODE_CASE));
        registerSetting(new RawBooleanSetting(OptionsImpl.ELIDE_SCALA));
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

    private class RawIntegerSetting extends Setting<Integer, CFRSettings> {

        RawIntegerSetting(PermittedOptionProvider.Argument<Integer> argument) {
            super(Integer.class, Integer.parseInt(argument.getFn().getDefaultValue()), ConfigurationSerializer.INTEGER, argument.getName(), getHelp(argument));
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
