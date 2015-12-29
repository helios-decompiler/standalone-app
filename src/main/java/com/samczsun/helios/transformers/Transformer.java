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

package com.samczsun.helios.transformers;

import com.samczsun.helios.Constants;
import com.samczsun.helios.Settings;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class Transformer {
    public static final Transformer HEX = new HexViewer();

    protected final TransformerSettings settings = new TransformerSettings(this);

    public final TransformerSettings getSettings() {
        return settings;
    }

    public boolean hasSettings() {
        return settings.size() > 0;
    }

    public abstract String getId();

    public abstract String getName();

    protected String buildPath(File inputJar) {
        return Settings.RT_LOCATION.get().asString() + ";" + inputJar.getAbsolutePath() + (Settings.PATH
                .get()
                .asString()
                .isEmpty() ? "" : ";" + Settings.PATH.get().asString());
    }

    protected String parseException(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        e.printStackTrace();
        String exception = Constants.REPO_NAME + " version " + Constants.REPO_VERSION + "\n" + sw.toString();
        return "An exception occured while performing this task. Please open a GitHub issue with the details below.\n\n" + exception;
    }

    protected byte[] fixBytes(byte[] in) {
        ClassReader reader = new ClassReader(in);
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.EXPAND_FRAMES);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
