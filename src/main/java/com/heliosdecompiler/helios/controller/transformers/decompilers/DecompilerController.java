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

package com.heliosdecompiler.helios.controller.transformers.decompilers;

import com.google.inject.Inject;
import com.heliosdecompiler.helios.Message;
import com.heliosdecompiler.helios.controller.PathController;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTask;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTaskHelper;
import com.heliosdecompiler.helios.controller.files.OpenedFile;
import com.heliosdecompiler.helios.controller.transformers.BaseTransformerController;
import com.heliosdecompiler.helios.controller.transformers.TransformerType;
import com.heliosdecompiler.transformerapi.ClassData;
import com.heliosdecompiler.transformerapi.TransformationResult;
import com.heliosdecompiler.transformerapi.decompilers.Decompiler;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public abstract class DecompilerController<SettingObject> extends BaseTransformerController<SettingObject> {
    @Inject
    private PathController pathController;
    @Inject
    private BackgroundTaskHelper backgroundTaskHelper;

    private Decompiler<SettingObject> decompiler;

    public DecompilerController(String name, String id, Decompiler<SettingObject> decompiler) {
        super(TransformerType.DECOMPILER, id, name);
        this.decompiler = decompiler;
    }

    public Decompiler<SettingObject> getDecompiler() {
        return decompiler;
    }

    public void decompile(OpenedFile file, String path, BiConsumer<Boolean, String> consumer) {
        backgroundTaskHelper.submit(new BackgroundTask(Message.TASK_DECOMPILE_FILE.format(path, getDisplayName()), true, () -> {
            try {
                String pre = preDecompile(file, path);
                if (pre != null) {
                    consumer.accept(false, pre);
                } else {
                    byte[] data = file.getContent(path);
                    ClassData cd = ClassData.construct(data);

                    TransformationResult<String> transformationResult = decompiler.decompile(Collections.singleton(cd), createSettings(), getClasspath(file));

                    Map<String, String> results = transformationResult.getTransformationData();

                    System.out.println("Results: " + results.keySet());
                    System.out.println("Looking for: " + StringEscapeUtils.escapeJava(cd.getInternalName()));

                    if (results.containsKey(cd.getInternalName())) {
                        consumer.accept(true, results.get(cd.getInternalName()));
                    } else {
                        StringBuilder output = new StringBuilder();
                        output.append("An error has occurred while decompiling this file.\r\n")
                                .append("If you have not tried another decompiler, try that. Otherwise, you're out of luck.\r\n\r\n")
                                .append("stdout:\r\n")
                                .append(transformationResult.getStdout())
                                .append("\r\nstderr:\r\n")
                                .append(transformationResult.getStderr());
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
    protected String preDecompile(OpenedFile file, String path) {
        byte[] data = file.getContent(path);
        ClassData cd = ClassData.construct(data);
        return cd == null ? "Could not decompile - are you sure that's a class file?" : null;
    }

}
