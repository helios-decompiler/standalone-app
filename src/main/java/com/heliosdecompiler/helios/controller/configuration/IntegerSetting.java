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

public abstract class IntegerSetting<T> extends Setting<Integer, T> {
    private int min;
    private int max;
    private int step;

    public IntegerSetting(Class<Integer> type, Integer defaultValue, ConfigurationSerializer<Integer> serializer, String id, String desc, int min, int max, int step) {
        super(type, defaultValue, serializer, id, desc);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int getStart() {
        return getDefault();
    }

    public int getStep() {
        return step;
    }
}
