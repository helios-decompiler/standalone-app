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

package com.heliosdecompiler.helios.controller.transformers;

import com.google.inject.Inject;
import com.heliosdecompiler.helios.controller.configuration.Setting;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.XMLConfiguration;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseTransformerController<SettingObject> {
    @Inject
    private Configuration configuration;

    private List<Setting<?, SettingObject>> settings = new ArrayList<>();

    public TransformerType getTransformerType() {
        return transformerType;
    }

    private final TransformerType transformerType;

    public String getInternalName() {
        return internalName;
    }

    public String getDisplayName() {
        return displayName;
    }

    private final String internalName;
    private final String displayName;

    public BaseTransformerController(TransformerType transformerType, String internalName, String displayName) {
        this.transformerType = transformerType;
        this.internalName = internalName;
        this.displayName = displayName;

        registerSettings();
    }

    protected abstract void registerSettings();

    protected abstract SettingObject defaultSettings();

    protected void registerSetting(Setting<?, SettingObject> setting) {
        this.settings.add(setting);
        this.settings.sort((a, b) -> a.getId().compareToIgnoreCase(b.getId()));
    }

    private <SettingType> void applySetting(SettingObject settingObject, Setting<SettingType, SettingObject> setting) {
        String fromConfig = getConfiguration().getString(setting.getId());
        if (fromConfig == null) {
            fromConfig = String.valueOf(setting.getDefault());
            getConfiguration().setProperty(setting.getId(), fromConfig);
        }
        setting.apply(settingObject, setting.getSerializer().deserialize(fromConfig));
    }

    protected SettingObject createSettings() {
        SettingObject settingsObject = defaultSettings();
        for (Setting<?, SettingObject> setting : settings) {
            applySetting(settingsObject, setting);
        }
        return settingsObject;
    }

    public List<Setting<?, SettingObject>> getSettings() {
        return this.settings;
    }

    protected Configuration getConfiguration() {
        try {
            return ((XMLConfiguration) configuration).configurationAt(transformerType.getInternalName() + "." + internalName, true);
        } catch (RuntimeException ex) {
            configuration.setProperty(transformerType.getInternalName() + "." + internalName + ".configured", true);
            return getConfiguration();
        }
    }
}
