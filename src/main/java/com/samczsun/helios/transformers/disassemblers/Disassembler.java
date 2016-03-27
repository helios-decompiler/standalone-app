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

package com.samczsun.helios.transformers.disassemblers;

import com.samczsun.helios.transformers.Transformer;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;

public abstract class Disassembler extends Transformer {
    private static final Map<String, Disassembler> BY_ID = new LinkedHashMap<>();
    private static final Map<String, Disassembler> BY_NAME = new LinkedHashMap<>();

    static {
        new JavapDisassembler().register();
        new KrakatauDisassembler().register();
        new BaksmaliDisassembler().register();
        new ProcyonDisassembler().register();
    }

    private final String id;
    private final String name;

    private final String originalId;
    private final String originalName;

    public Disassembler(String id, String name) {
        this.id = id + "-disassembler";
        this.name = name + " Disassembler";
        this.originalId = id;
        this.originalName = name;
    }

    @Override
    public final Disassembler register() {
        super.register();
        if (!BY_ID.containsKey(originalId)) {
            BY_ID.put(originalId, this);
        } else {
            throw new IllegalArgumentException(originalId + " already exists!");
        }
        if (!BY_NAME.containsKey(originalName)) {
            BY_NAME.put(originalName, this);
        } else {
            throw new IllegalArgumentException(originalName + " already exists!");
        }
        return this;
    }

    public final String getId() {
        return this.id;
    }

    public final String getName() {
        return this.name;
    }

    public Object transform(Object... args) {
        return disassembleClassNode((ClassNode) args[0], (byte[]) args[1], (StringBuilder) args[2]);
    }

    public abstract boolean disassembleClassNode(ClassNode classNode, byte[] bytes, StringBuilder output);

    public static Disassembler getById(String id) {
        return BY_ID.get(id);
    }

    public static Disassembler getByName(String name) {
        return BY_NAME.get(name);
    }

    public static Collection<Disassembler> getAllDisassemblers() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }
}
