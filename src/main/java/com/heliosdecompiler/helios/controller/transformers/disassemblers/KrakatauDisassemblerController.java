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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.heliosdecompiler.helios.Settings;
import com.heliosdecompiler.helios.controller.ProcessController;
import com.heliosdecompiler.helios.controller.configuration.ConfigurationSerializer;
import com.heliosdecompiler.helios.controller.configuration.Setting;
import com.heliosdecompiler.transformerapi.StandardTransformers;
import com.heliosdecompiler.transformerapi.disassemblers.krakatau.KrakatauDisassemblerSettings;
import org.apache.commons.configuration2.Configuration;

import java.io.File;
import java.util.function.BiConsumer;

@Singleton
public class KrakatauDisassemblerController extends DisassemblerController<KrakatauDisassemblerSettings> {

    @Inject
    private Configuration configuration;

    @Inject
    private ProcessController processController;

    public KrakatauDisassemblerController() {
        super("Krakatau Disassembler", "krakatau-disassembler", StandardTransformers.Disassemblers.KRAKATAU);
    }

    @Override
    protected void registerSettings() {
        registerSetting(Boolean.class, new RawBooleanSetting("roundtrip", "Disassemble for roundtrip assembly (preserves constant pool ordering)", false, KrakatauDisassemblerSettings::setRoundtrip));
    }

    @Override
    protected KrakatauDisassemblerSettings defaultSettings() {
        return getDisassembler().defaultSettings();
    }

    @Override
    protected KrakatauDisassemblerSettings createSettings() {
        KrakatauDisassemblerSettings settings = new KrakatauDisassemblerSettings();
        settings.setPythonExecutable(new File(configuration.getString(Settings.PYTHON2_KEY)));
        settings.setProcessCreator(processController::launchProcess);
        return settings;
    }

    private class RawBooleanSetting extends Setting<Boolean, KrakatauDisassemblerSettings> {
        private BiConsumer<KrakatauDisassemblerSettings, Boolean> consumer;

        RawBooleanSetting(String id, String desc, boolean defaultValue, BiConsumer<KrakatauDisassemblerSettings, Boolean> consumer) {
            super(Boolean.class, defaultValue, ConfigurationSerializer.BOOLEAN, id, desc);
            this.consumer = consumer;
        }

        @Override
        public void apply(KrakatauDisassemblerSettings settings, Boolean value) {
            consumer.accept(settings, value);
        }
    }
}
