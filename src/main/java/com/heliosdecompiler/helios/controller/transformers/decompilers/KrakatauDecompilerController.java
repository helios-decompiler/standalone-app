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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.heliosdecompiler.helios.Settings;
import com.heliosdecompiler.helios.controller.PathController;
import com.heliosdecompiler.helios.controller.ProcessController;
import com.heliosdecompiler.helios.controller.configuration.ConfigurationSerializer;
import com.heliosdecompiler.helios.controller.configuration.Setting;
import com.heliosdecompiler.helios.controller.files.OpenedFile;
import com.heliosdecompiler.transformerapi.ClassData;
import com.heliosdecompiler.transformerapi.StandardTransformers;
import com.heliosdecompiler.transformerapi.decompilers.krakatau.KrakatauDecompilerSettings;
import org.apache.commons.configuration2.Configuration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Singleton
public class KrakatauDecompilerController extends DecompilerController<KrakatauDecompilerSettings> {

    @Inject
    private Configuration configuration;

    @Inject
    private PathController pathController;

    @Inject
    private ProcessController processController;

    public KrakatauDecompilerController() {
        super("Krakatau Decompiler", "krakatau", StandardTransformers.Decompilers.KRAKATAU);
    }

    @Override
    protected KrakatauDecompilerSettings defaultSettings() {
        return getDecompiler().defaultSettings();
    }

    @Override
    protected String preDecompile(OpenedFile file, String path) {
        String superRes = super.preDecompile(file, path);
        if (superRes != null)
            return superRes;

        KrakatauDecompilerSettings settings = createSettings();
        if (settings.getPython2Exe() == null) {
            return "You need to specify the location of Python 2 in order to use the Krakatau decompiler";
        }
        return null;
    }

    @Override
    protected void registerSettings() {
        registerSetting(new RawBooleanSetting("magicthrow", "Assume all instructions can throw an exception (disabling may lead to inaccurate code)", true, KrakatauDecompilerSettings::setMagicThrow));
    }

    @Override
    protected KrakatauDecompilerSettings createSettings() {
        KrakatauDecompilerSettings settings = super.createSettings();
        String location = configuration.getString(Settings.PYTHON2_KEY);
        if (location != null)
            settings.setPythonExecutable(new File(location));
        settings.setPath(pathController.getFiles());
        settings.setProcessCreator(processController::launchProcess);
        return settings;
    }

    @Override
    protected Map<String, ClassData> getClasspath(OpenedFile thisFile) {
        Map<String, ClassData> map = new HashMap<>();

        for (Map.Entry<String, byte[]> ent : thisFile.getContents().entrySet()) {
            ClassData data = ClassData.construct(ent.getValue());
            if (data != null && !map.containsKey(data.getInternalName())) {
                map.put(data.getInternalName(), data);
            }
        }

        return map;
    }

    private class RawBooleanSetting extends Setting<Boolean, KrakatauDecompilerSettings> {
        private BiConsumer<KrakatauDecompilerSettings, Boolean> consumer;

        RawBooleanSetting(String id, String desc, boolean defaultValue, BiConsumer<KrakatauDecompilerSettings, Boolean> consumer) {
            super(Boolean.class, defaultValue, ConfigurationSerializer.BOOLEAN, id, desc);
            this.consumer = consumer;
        }

        @Override
        public void apply(KrakatauDecompilerSettings settings, Boolean value) {
            consumer.accept(settings, value);
        }
    }
}
