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

package com.heliosdecompiler.helios.controller.configuration;

public abstract class Setting<SettingType, SettingOwner> {

    private Class<SettingType> clazz;
    private ConfigurationSerializer<SettingType> serializer;
    private String id;
    private SettingType defaultValue;

    private String desc;

    public Setting(Class<SettingType> type, SettingType defaultValue, ConfigurationSerializer<SettingType> serializer,
                   String id, String desc) {
        this.clazz = type;
        this.id = id;
        this.desc = desc;
        this.serializer = serializer;
        this.defaultValue = defaultValue;
    }

    public SettingType getDefault() {
        return this.defaultValue;
    }

    public String getId() {
        return this.id;
    }

    public ConfigurationSerializer<SettingType> getSerializer() {
        return this.serializer;
    }

    public abstract void apply(SettingOwner owner, SettingType value);

    public boolean isValid(SettingType value) {
        return true;
    }
}
