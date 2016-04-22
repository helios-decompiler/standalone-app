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

package com.heliosdecompiler.helios.handler.addons;

import com.heliosdecompiler.helios.api.Addon;
import com.heliosdecompiler.helios.handler.addons.builtin.ExtractStrings;
import com.heliosdecompiler.helios.handler.addons.builtin.FindEntryPoints;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AddonHandler {
    private static final Map<String, AddonHandler> BY_ID = new HashMap<>();

    static {
        new JarLauncher().register();
    }

    private static final Map<String, Addon> ADDONS = new HashMap<>();

    public static void registerPreloadedAddons() {
        Arrays.asList(new ExtractStrings(), new FindEntryPoints()).forEach(addon -> ADDONS.put(addon.getName(), addon));
    }

    public abstract boolean accept(File file);

    public abstract void run(File file);

    public final AddonHandler register() {
        if (!BY_ID.containsKey(getId())) {
            BY_ID.put(getId(), this);
        }
        return this;
    }

    protected void registerAddon(String name, Addon addon) {
        ADDONS.put(name, addon);
    }

    public static Collection<Addon> getAllAddons() {
        return Collections.unmodifiableCollection(ADDONS.values());
    }

    public static Collection<AddonHandler> getAllHandlers() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    public abstract String getId();
}
