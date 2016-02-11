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

package com.samczsun.helios.transformers;

import com.eclipsesource.json.JsonObject;

import java.util.*;

public class TransformerSettings {
    private final Transformer decompiler;
    private final Map<String, Setting> registrationOrder = new LinkedHashMap<>();

    public TransformerSettings(Transformer decompiler) {
        this.decompiler = decompiler;
    }

    public void registerSetting(Setting setting) {
        registrationOrder.put(setting.getParam(), setting);
    }

    public int size() {
        return registrationOrder.size();
    }

    public void loadFrom(JsonObject rootSettings) {
        if (rootSettings.get("decompilers") != null) {
            JsonObject decompilerSection = rootSettings.get("decompilers").asObject();
            if (decompilerSection.get(decompiler.getId()) != null) {
                JsonObject thisDecompiler = decompilerSection.get(decompiler.getId()).asObject();
                thisDecompiler.forEach(member -> {
                    Setting setting = registrationOrder.get(member.getName());
                    if (setting != null) {
                        setting.setEnabled(member.getValue().asBoolean());
                    }
                });
            }
        }
    }

    public void saveTo(JsonObject rootSettings) {
        if (rootSettings.get("decompilers") == null) {
            rootSettings.add("decompilers", new JsonObject());
        }
        JsonObject decompilerSection = rootSettings.get("decompilers").asObject();
        if (decompilerSection.get(decompiler.getName()) == null) {
            decompilerSection.add(decompiler.getName(), new JsonObject());
        }
        JsonObject thisDecompiler = decompilerSection.get(decompiler.getName()).asObject();
        for (Setting setting : registrationOrder.values()) {
            thisDecompiler.set(setting.getParam(), setting.isEnabled());
        }
    }

    public Collection<Setting> getRegisteredSettings() {
        return Collections.unmodifiableCollection(registrationOrder.values());
    }

    public interface Setting {
        String getParam();

        String getText();

        boolean isEnabled();

        void setEnabled(boolean b);
    }
}
