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

package com.samczsun.helios.transformers.converters;

import com.samczsun.helios.Constants;
import com.samczsun.helios.Helios;
import com.samczsun.helios.Settings;
import com.samczsun.helios.handler.ExceptionHandler;
import com.samczsun.helios.transformers.Transformer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public abstract class Converter extends Transformer {

    public static final Map<String, Converter> BY_ID = new HashMap<>();

    public static final Converter ENJARIFY = new Converter() {
        @Override
        public void convert(File in, File out) {
            if (Helios.ensurePython3Set()) {
                try {
                    ProcessBuilder pb = new ProcessBuilder(Settings.PYTHON3_LOCATION.get().asString(), "-O", "-m",
                            "enjarify.main", in.getAbsolutePath(), "-o", out.getAbsolutePath(), "-f").directory(
                            Constants.ENJARIFY_DIR);
                    Process process = Helios.launchProcess(pb);
                    process.waitFor();
                } catch (Exception e) {
                    ExceptionHandler.handle(e);
                }
            }
        }

        @Override
        public String getId() {
            return "enjarify";
        }        @Override
        public String getName() {
            return "Enjarify";
        }


    };

    public static final Converter DEX2JAR = new Converter() {
        @Override
        public void convert(File in, File out) {
            try {
                com.googlecode.dex2jar.tools.Dex2jarCmd.main("-o", out.getAbsolutePath(), "--force",
                        in.getAbsolutePath());
            } catch (Exception e) {
                ExceptionHandler.handle(e);
            }
        }

        @Override
        public String getName() {
            return "Dex2Jar";
        }

        @Override
        public String getId() {
            return "dex2jar";
        }
    };

    public static final Converter JAR2DEX = new Converter() {
        @Override
        public void convert(File in, File out) {
            try {
                com.googlecode.dex2jar.tools.Jar2Dex.main("-o", out.getAbsolutePath(), "--force", in.getAbsolutePath());
            } catch (Exception e) {
                ExceptionHandler.handle(e);
            }
        }

        @Override
        public String getName() {
            return "Jar2Dex";
        }

        @Override
        public String getId() {
            return "jar2dex";
        }
    };

    public static final Converter NONE = new Converter() {
        @Override
        public void convert(File in, File out) {
        }

        @Override
        public String getName() {
            return "None";
        }

        @Override
        public String getId() {
            return "none";
        }
    };

    public abstract void convert(File in, File out);
}
