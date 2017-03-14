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
import com.heliosdecompiler.transformerapi.StandardTransformers;
import com.strobel.decompiler.DecompilerSettings;

import java.util.function.BiConsumer;

@Singleton
public class ProcyonDecompilerController extends DecompilerController<DecompilerSettings> {

    public ProcyonDecompilerController() {
        super("Procyon Decompiler", "procyon", StandardTransformers.Decompilers.PROCYON);
    }

    @Override
    protected DecompilerSettings defaultSettings() {
        return getDecompiler().defaultSettings();
    }

    @Override
    protected void registerSettings() {
        registerSetting(new RawBooleanSetting("includelinenumbers", "Include line numbers in raw bytecode mode", true, DecompilerSettings::setIncludeLineNumbersInBytecode));
        registerSetting(new RawBooleanSetting("showsyntheticmembers", "Show synthetic (compiler-generated) members.", false, DecompilerSettings::setShowSyntheticMembers));
        registerSetting(new RawBooleanSetting("genexforcatch", "Always generate exception variables for catch blocks", true, DecompilerSettings::setAlwaysGenerateExceptionVariableForCatchBlocks));
        registerSetting(new RawBooleanSetting("forceexplicitimports", "[DEPRECATED] Explicit imports are now enabled by default.  This option will be removed in a future release.", false, DecompilerSettings::setForceExplicitImports));
        registerSetting(new RawBooleanSetting("forceexplicittypeargs", "Always print type arguments to generic methods.", false, DecompilerSettings::setForceExplicitTypeArguments));
        registerSetting(new RawBooleanSetting("flattenswitch", "Drop the braces statements around switch sections when possible.", false, DecompilerSettings::setFlattenSwitchBlocks));
        registerSetting(new RawBooleanSetting("exclnested", "Exclude nested types when decompiling their enclosing types.", false, DecompilerSettings::setExcludeNestedTypes));
        registerSetting(new RawBooleanSetting("retaincasts", "Do not remove redundant explicit casts.", false, DecompilerSettings::setRetainRedundantCasts));
        registerSetting(new RawBooleanSetting("retainswitches", "Do not lift the contents of switches having only a default label.", false, DecompilerSettings::setRetainPointlessSwitches));
        registerSetting(new RawBooleanSetting("unicode", "Enable Unicode output (printable non-ASCII characters will not be escaped).", false, DecompilerSettings::setUnicodeOutputEnabled));
//        registerSetting(new RawBooleanSetting("errordiag", "Include error diagnostics", true, DecompilerSettings::setIncludeErrorDiagnostics));
        registerSetting(new RawBooleanSetting("mergevar", "Attempt to merge as many variables as possible.  This may lead to fewer declarations, but at the expense of inlining and useful naming.  This feature is experimental and may be removed or become the standard behavior in future releases.", false, DecompilerSettings::setMergeVariables));
        registerSetting(new RawBooleanSetting("disablefortransform", "Disable \'for each\' loop transforms.", false, DecompilerSettings::setDisableForEachTransforms));
        registerSetting(new RawBooleanSetting("showlinenumbers", "For debugging, show Java line numbers as inline comments (implies including line numbers)", true, DecompilerSettings::setShowDebugLineNumbers));
        registerSetting(new RawBooleanSetting("simplifymembers", "Simplify type-qualified member references in Java output [EXPERIMENTAL].", false, DecompilerSettings::setSimplifyMemberReferences));

    }

    private class RawBooleanSetting extends Setting<Boolean, DecompilerSettings> {
        private BiConsumer<DecompilerSettings, Boolean> consumer;

        RawBooleanSetting(String id, String desc, boolean defaultValue, BiConsumer<DecompilerSettings, Boolean> consumer) {
            super(Boolean.class, defaultValue, ConfigurationSerializer.BOOLEAN, id, desc);
            this.consumer = consumer;
        }

        @Override
        public void apply(DecompilerSettings settings, Boolean value) {
            consumer.accept(settings, value);
        }
    }
}
