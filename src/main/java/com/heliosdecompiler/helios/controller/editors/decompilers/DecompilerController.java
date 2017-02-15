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

package com.heliosdecompiler.helios.controller.editors.decompilers;

import com.google.inject.Inject;
import com.heliosdecompiler.helios.controller.PathController;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTask;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTaskHelper;
import com.heliosdecompiler.helios.controller.configuration.Setting;
import com.heliosdecompiler.helios.controller.files.OpenedFile;
import com.heliosdecompiler.transformerapi.ClassData;
import com.heliosdecompiler.transformerapi.Result;
import com.heliosdecompiler.transformerapi.decompilers.Decompiler;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.function.BiConsumer;

public abstract class DecompilerController<SettingObject> {
    @Inject
    private PathController pathController;
    @Inject
    private BackgroundTaskHelper backgroundTaskHelper;
    @Inject
    private Configuration configuration;

    private String name;
    private String id;
    private Decompiler<SettingObject> decompiler;

    private List<Setting<?, SettingObject>> settings = new ArrayList<>();

    public DecompilerController(String name, String id, Decompiler<SettingObject> decompiler) {
        this.name = name;
        this.id = id;
        this.decompiler = decompiler;

        registerSettings();
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public Decompiler<SettingObject> getDecompiler() {
        return decompiler;
    }

    public void decompile(OpenedFile file, String path, BiConsumer<Boolean, String> consumer) {
        backgroundTaskHelper.submit(new BackgroundTask("Decompiling " + path + " with " + getName(), true, () -> {
            try {
                String pre = preDecompile(file, path);
                if (pre != null) {
                    consumer.accept(false, pre);
                } else {
                    byte[] data = file.getContent(path);
                    ClassData cd = ClassData.construct(data);

                    Result result = decompiler.decompile(Collections.singleton(cd), getSettings(), getClasspath(file));

                    Map<String, String> results = result.getDecompiledResult();

                    System.out.println("Results: " + results.keySet());
                    System.out.println("Looking for: " + StringEscapeUtils.escapeJava(cd.getInternalName()));

                    if (results.containsKey(cd.getInternalName())) {
                        consumer.accept(true, results.get(cd.getInternalName()));
                    } else {
                        StringBuilder output = new StringBuilder();
                        output.append("An error has occurred while decompiling this file.\r\n")
                                .append("If you have not tried another decompiler, try that. Otherwise, you're out of luck.\r\n\r\n")
                                .append("stdout:\r\n")
                                .append(result.getStdout())
                                .append("\r\nstderr:\r\n")
                                .append(result.getStderr());
                        consumer.accept(false, output.toString());
                    }
                }
            } catch (Throwable e) {
                StringWriter writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));

                StringBuilder output = new StringBuilder();
                output.append("An error has occurred while decompiling this file.\r\n")
                        .append("If you have not tried another decompiler, try that. Otherwise, you're out of luck.\r\n\r\n")
                        .append("Exception:\r\n")
                        .append(writer.toString());
                consumer.accept(false, output.toString());
            }
        }, () -> {
            consumer.accept(false, "Decompilation aborted");
        }));
    }

    protected Map<String, ClassData> getClasspath(OpenedFile thisFile) {
        Map<String, ClassData> map = new HashMap<>();

        for (Map.Entry<String, byte[]> ent : thisFile.getContents().entrySet()) {
            ClassData data = ClassData.construct(ent.getValue());
            if (data != null && !map.containsKey(data.getInternalName())) {
                map.put(data.getInternalName(), data);
            }
        }

        for (OpenedFile file : pathController.getOpenedFiles()) {
            for (Map.Entry<String, byte[]> ent : file.getContents().entrySet()) {
                ClassData data = ClassData.construct(ent.getValue());
                if (data != null && !map.containsKey(data.getInternalName())) {
                    map.put(data.getInternalName(), data);
                }
            }
        }

        return map;
    }

    protected Configuration getConfiguration() {
        try {
            return ((XMLConfiguration) configuration).configurationAt("decompilers." + getId(), true);
        } catch (RuntimeException ex) {
            configuration.setProperty("decompilers." + getId() + ".configured", true);
            return getConfiguration();
        }
    }

    protected String preDecompile(OpenedFile file, String path) {
        byte[] data = file.getContent(path);
        ClassData cd = ClassData.construct(data);
        return cd == null ? "Could not decompile - are you sure that's a class file?" : null;
    }

    protected abstract void registerSettings();

    protected void registerSetting(Setting<?, SettingObject> setting) {
        this.settings.add(setting);
        this.settings.sort((a, b) -> a.getId().compareToIgnoreCase(b.getId()));
    }

    private <SettingType> void applySetting(SettingObject settingObject, Setting<SettingType, SettingObject> setting) {
        String fromConfig = getConfiguration().getString(setting.getId());
        if (fromConfig == null) {
            fromConfig = String.valueOf(setting.getDefault());
            getConfiguration().setProperty(setting.getId(), fromConfig);
        }
        setting.apply(settingObject, setting.getSerializer().deserialize(fromConfig));
    }

    protected SettingObject getSettings() {
        SettingObject settingsObject = decompiler.defaultSettings();
        for (Setting<?, SettingObject> setting : settings) {
            applySetting(settingsObject, setting);
        }
        return settingsObject;
    }
}
