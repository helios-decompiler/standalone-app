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

package com.heliosdecompiler.helios.transformers.compilers;

import com.heliosdecompiler.helios.transformers.Transformer;
import com.heliosdecompiler.helios.transformers.TransformerSettings;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Compiler extends Transformer {
    private static final Map<String, Compiler> BY_ID = new LinkedHashMap<>();
    private static final Map<String, Compiler> BY_NAME = new LinkedHashMap<>();

    static {
        new JavaCompiler().register();
    }

    private final String originalId;
    private final String originalName;

    public Compiler(String id, String name) {
        this(id, name, null);
    }

    public Compiler(String id, String name, Class<? extends TransformerSettings.Setting> settingsClass) {
        super(id + "-compiler", name + " Compiler", settingsClass);
        this.originalId = id;
        this.originalName = name;
    }

    public static Compiler getById(String id) {
        return BY_ID.get(id);
    }

    public static Compiler getByName(String name) {
        return BY_NAME.get(name);
    }

    public static Collection<Compiler> getAllCompilers() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    @Override
    public final Compiler register() {
        if (BY_ID.containsKey(originalId)) {
            throw new IllegalArgumentException(originalId + " already exists!");
        }
        if (BY_NAME.containsKey(originalName)) {
            throw new IllegalArgumentException(originalName + " already exists!");
        }
        super.register();
        BY_ID.put(originalId, this);
        BY_NAME.put(originalName, this);
        return this;
    }

    public Object transform(Object... args) {
        return compile((String) args[0], (String) args[1]);
    }

    public abstract byte[] compile(String name, String contents);
}
