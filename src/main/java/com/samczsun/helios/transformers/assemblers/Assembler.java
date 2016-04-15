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

package com.samczsun.helios.transformers.assemblers;


import com.samczsun.helios.transformers.Transformer;
import com.samczsun.helios.transformers.TransformerSettings;
import com.samczsun.helios.transformers.compilers.JavaCompiler;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Assembler extends Transformer {
    private static final Map<String, Assembler> BY_ID = new LinkedHashMap<>();
    private static final Map<String, Assembler> BY_NAME = new LinkedHashMap<>();

    static {
        new KrakatauAssembler().register();
        new SmaliAssembler().register();
    }

    private final String originalId;
    private final String originalName;

    public Assembler(String id, String name) {
        this(id, name, null);
    }

    public Assembler(String id, String name, Class<? extends TransformerSettings.Setting> settingsClass) {
        super(id + "-assembler", name + " Assembler", settingsClass);
        this.originalId = id;
        this.originalName = name;
    }

    @Override
    public final Assembler register() {
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
        return assemble((String) args[0], (String) args[1]);
    }

    public abstract byte[] assemble(String name, String contents);

    public static Assembler getById(String id) {
        return BY_ID.get(id);
    }

    public static Assembler getByName(String name) {
        return BY_NAME.get(name);
    }

    public static Collection<Assembler> getAllAssemblers() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }
}
