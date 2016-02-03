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

package com.samczsun.helios.transformers.decompilers;

import com.samczsun.helios.transformers.Transformer;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class Decompiler extends Transformer {
    private static final Map<String, Decompiler> BY_ID = new HashMap<>();

    static {
        new ProcyonDecompiler().register();
        new FernflowerDecompiler().register();
        new CFRDecompiler().register();
        new KrakatauDecompiler().register();
    }

    private final String id;
    private final String name;

    public Decompiler(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public final Decompiler register() {
        if (!BY_ID.containsKey(id)) {
            BY_ID.put(id, this);
        }
        return this;
    }

    public final String getId() {
        return this.id;
    }

    public final String getName() {
        return this.name;
    }

    public boolean hasSettings() {
        return settings.size() > 0;
    }

    public Object transform(Object... args) {
        return decompile((ClassNode) args[0], (byte[]) args[1], (StringBuilder) args[2]);
    }

    public abstract boolean decompile(ClassNode classNode, byte[] bytes, StringBuilder output);

    public static Decompiler getById(String id) {
        return BY_ID.get(id);
    }

    public static Collection<Decompiler> getAllDecompilers() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }
}
