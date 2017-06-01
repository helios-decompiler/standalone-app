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

package com.heliosdecompiler.helios.transformers.converters;

import com.heliosdecompiler.helios.Constants;
import com.heliosdecompiler.helios.controller.ProcessController;
import com.heliosdecompiler.helios.transformers.Transformer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public abstract class Converter extends Transformer {

    public static final Converter ENJARIFY = new Converter("enjarify", "Enjarify") {
        @Override
        public void convert(File in, File out) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        "python2",
                        "-O",
                        "-m",
                        "enjarify.main",
                        in.getAbsolutePath(),
                        "-o",
                        out.getAbsolutePath(),
                        "-f"
                ).directory(Constants.ENJARIFY_DIR);
                Process process = ((ProcessController) null).launchProcess(pb);
                process.waitFor();
            } catch (Exception e) {
//                ExceptionHandler.handle(e);
            }
        }
    };
    public static final Converter DEX2JAR = new Converter("dex2jar", "Dex2Jar") {
        @Override
        public void convert(File in, File out) {
            try {
                com.googlecode.dex2jar.tools.Dex2jarCmd.main("-o", "-n", out.getAbsolutePath(), "--force",
                        in.getAbsolutePath());
            } catch (Exception e) {
//                ExceptionHandler.handle(e);
            }
        }
    };
    public static final Converter JAR2DEX = new Converter("jar2dex", "Jar2Dex") {
        @Override
        public void convert(File in, File out) {
            throw new IllegalArgumentException("TODO: Use dx from BaksmaliDisassembler");
        }
    };
    public static final Converter NONE = new Converter("none", "None") {
        @Override
        public void convert(File in, File out) {
        }
    };
    private static final Map<String, Converter> BY_ID = new HashMap<>();

    private Converter(String id, String name) {
        super(id, name);
    }

    public Object transform(Object... args) {
        convert((File) args[0], (File) args[1]);
        return null;
    }

    public abstract void convert(File in, File out);
}
