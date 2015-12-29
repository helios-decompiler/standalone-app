/*
 * Copyright 2015 Sam Sun <me@samczsun.com>
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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class TransformerSettings {
    private final Transformer decompiler;
    private final Set<Setting> registrationOrder = new LinkedHashSet<>();

    public TransformerSettings(Transformer decompiler) {
        this.decompiler = decompiler;
    }

    public void registerSetting(Setting setting) {
        if (registrationOrder.add(setting)) {

        }
    }

    public int size() {
        return registrationOrder.size();
    }

    public void loadFrom(JsonObject rootSettings) {
        if (rootSettings.get("decompilers") != null) {
            JsonObject decompilerSection = rootSettings.get("decompilers").asObject();
            if (decompilerSection.get(decompiler.getId()) != null) {
                JsonObject thisDecompiler = decompilerSection.get(decompiler.getId()).asObject();
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
    }

    public boolean isSelected(Setting setting) {
        return setting.isDefaultOn();
    }

    public Collection<Setting> getRegisteredSettings() {
        return Collections.unmodifiableSet(registrationOrder);
    }

    public interface Setting {
        String getParam();

        String getText();

        boolean isDefaultOn();
    }
}
