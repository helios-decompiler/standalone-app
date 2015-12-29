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

package com.samczsun.helios.transformers.compilers;

import java.util.HashMap;
import java.util.Map;

public abstract class Compiler {
    private static final Map<String, Compiler> BY_ID = new HashMap<>();

    static {
        new JavaCompiler().register();
    }

    private final String id;
    private final String name;

    public Compiler(String id, String name) {
        this.id = id;
        this.name = name;
    }


    public Compiler register() {
        if (!BY_ID.containsKey(id)) {
            BY_ID.put(id, this);
        }
        return this;
    }

    public abstract byte[] compile(String name, String contents);

    public final String getId() {
        return this.id;
    }

    public final String getName() {
        return this.name;
    }

    public static Compiler getById(String id) {
        return BY_ID.get(id);
    }
}
