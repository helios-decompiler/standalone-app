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

package com.samczsun.helios;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.samczsun.helios.transformers.converters.Converter;
import com.samczsun.helios.transformers.decompilers.Decompiler;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Settings {
    private static final JsonObject INSTANCE = new JsonObject();

    public static final Settings RECENT_FILES = new Settings("recent_files").set(new JsonArray());
    public static final Settings PYTHON2_LOCATION = new Settings("python2location").set("");
    public static final Settings PYTHON3_LOCATION = new Settings("python3location").set("");
    public static final Settings JAVAC_LOCATION = new Settings("javaclocation").set("");
    public static final Settings RT_LOCATION = new Settings("rtlocation").set("");
    public static final Settings PATH = new Settings("path").set("");
    public static final Settings MAX_RECENTFILES = new Settings("max_recentfiles").set(25);
    public static final Settings LAST_DIRECTORY = new Settings("last_directory").set(".");
    public static final Settings APKTOOL = new Settings("apktool").set(true);
    public static final Settings APK_CONVERSION = new Settings("apk_conversion").set(Converter.NONE.getId());

    private final String key;

    private Settings(String key) {
        this.key = key;
    }

    public static void saveSettings() {
        try {
            JsonObject settings = new JsonObject();
            for (Decompiler decompiler : Decompiler.getAllDecompilers()) {
                decompiler.getSettings().saveTo(settings);
            }
            if (settings.get("settings") == null) {
                settings.add("settings", new JsonObject());
            }
            JsonObject rootSettings = settings.get("settings").asObject();
            for (JsonObject.Member val : INSTANCE) {
                rootSettings.set(val.getName(), val.getValue());
            }
            FileOutputStream out = new FileOutputStream(Constants.SETTINGS_FILE);
            out.write(settings.toString().getBytes("UTF-8"));
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadSettings() {
        try {
            JsonObject settings = new JsonObject();
            try {
                settings = JsonObject.readFrom(
                        new InputStreamReader(new FileInputStream(Constants.SETTINGS_FILE), StandardCharsets.UTF_8));
            } catch (ParseException | UnsupportedOperationException e) {
            }
            for (Decompiler decompiler : Decompiler.getAllDecompilers()) {
                decompiler.getSettings().loadFrom(settings);
            }
            if (settings.get("settings") != null) {
                JsonObject rootSettings = settings.get("settings").asObject();
                for (JsonObject.Member val : rootSettings) {
                    INSTANCE.set(val.getName(), val.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JsonValue get() {
        return INSTANCE.get(key);
    }

    public Settings set(int value) {
        INSTANCE.set(key, value);
        return this;
    }

    public Settings set(boolean value) {
        INSTANCE.set(key, value);
        return this;
    }

    public Settings set(double value) {
        INSTANCE.set(key, value);
        return this;
    }

    public Settings set(long value) {
        INSTANCE.set(key, value);
        return this;
    }

    public Settings set(float value) {
        INSTANCE.set(key, value);
        return this;
    }

    public Settings set(String value) {
        INSTANCE.set(key, value);
        return this;
    }

    public Settings set(JsonValue value) {
        INSTANCE.set(key, value);
        return this;
    }
}