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

public abstract class ConfigurationSerializer<T> {
    public static ConfigurationSerializer<Boolean> BOOLEAN = new ConfigurationSerializer<Boolean>() {
        @Override
        public Boolean deserialize(String in) {
            return Boolean.parseBoolean(in);
        }

        @Override
        public String serialize(Boolean in) {
            return String.valueOf(in);
        }
    };

    public static ConfigurationSerializer<Troolean> TROOLEAN = new ConfigurationSerializer<Troolean>() {
        @Override
        public Troolean deserialize(String in) {
            return Troolean.valueOf(in);
        }

        @Override
        public String serialize(Troolean in) {
            return in.name();
        }
    };

    public static ConfigurationSerializer<Integer> INTEGER = new ConfigurationSerializer<Integer>() {
        @Override
        public Integer deserialize(String in) {
            return Integer.parseInt(in);
        }

        @Override
        public String serialize(Integer in) {
            return String.valueOf(in);
        }
    };

    public static ConfigurationSerializer<String> STRING = new ConfigurationSerializer<String>() {
        @Override
        public String deserialize(String in) {
            return in;
        }

        @Override
        public String serialize(String in) {
            return in;
        }
    };

    public abstract T deserialize(String in);

    public abstract String serialize(T in);
}
