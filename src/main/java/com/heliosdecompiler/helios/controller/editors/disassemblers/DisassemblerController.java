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

package com.heliosdecompiler.helios.controller.editors.disassemblers;

import com.google.inject.Inject;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTask;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTaskHelper;
import com.heliosdecompiler.helios.controller.files.OpenedFile;
import com.heliosdecompiler.transformerapi.ClassData;
import com.heliosdecompiler.transformerapi.Result;
import com.heliosdecompiler.transformerapi.disassemblers.Disassembler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.function.BiConsumer;

public abstract class DisassemblerController<T> {
    private String name;
    private String id;
    private Disassembler<T> disassembler;
    @Inject
    private BackgroundTaskHelper backgroundTaskHelper;

    public DisassemblerController(String name, String id, Disassembler<T> disassembler) {
        this.name = name;
        this.id = id;
        this.disassembler = disassembler;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public Disassembler<T> getDisassembler() {
        return disassembler;
    }

    public void disassemble(OpenedFile file, String path, BiConsumer<Boolean, String> consumer) {
        backgroundTaskHelper.submit(new BackgroundTask("Disassembling " + path + " with " + getName(), true, () -> {
            try {
                byte[] data = file.getContent(path);
                ClassData cd = ClassData.construct(data);
                if (cd != null) {
                    Result result = disassembler.disassemble(cd, getSettings());

                    Map<String, String> results = result.getDecompiledResult();
                    if (results.containsKey(cd.getInternalName())) {
                        consumer.accept(true, results.get(cd.getInternalName()));
                    } else {
                        StringBuilder output = new StringBuilder();
                        output.append("An error has occurred while disassembling this file.\r\n")
                                .append("If you have not tried another disassembler, try that. Otherwise, you're out of luck.\r\n\r\n")
                                .append("stdout:\r\n")
                                .append(result.getStdout())
                                .append("\r\nstderr:\r\n")
                                .append(result.getStderr());
                        consumer.accept(false, output.toString());
                    }
                } else {
                    consumer.accept(false, "Could not disassemble - are you sure that's a class file?");
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
            consumer.accept(false, "Disassembling aborted");
        }));
    }

    protected abstract T getSettings();
}
