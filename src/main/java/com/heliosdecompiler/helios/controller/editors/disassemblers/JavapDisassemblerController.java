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
import com.heliosdecompiler.transformerapi.StandardTransformers;
import com.sun.tools.javap.Options;
import org.apache.commons.configuration2.Configuration;

public class JavapDisassemblerController extends DisassemblerController<Options> {

    @Inject
    private Configuration configuration;

    public JavapDisassemblerController() {
        super("Javap Disassembler", "javap-disassembler", StandardTransformers.Disassemblers.JAVAP);
    }

    @Override
    protected Options getSettings() {
        return getDisassembler().defaultSettings();
    }
}
