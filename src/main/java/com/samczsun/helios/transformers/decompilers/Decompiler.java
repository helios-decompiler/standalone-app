/*
 * Copyright 2016 Sam Sun <me@samczsun.com>
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

package com.samczsun.helios.transformers.decompilers;

import com.samczsun.helios.gui.ClassManager;
import com.samczsun.helios.gui.ClickableSyntaxTextArea;
import com.samczsun.helios.gui.data.ClassData;
import com.samczsun.helios.transformers.Transformer;
import com.samczsun.helios.transformers.TransformerSettings;
import com.samczsun.helios.transformers.Viewable;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.objectweb.asm.tree.ClassNode;

import javax.swing.JComponent;
import java.util.*;

public abstract class Decompiler extends Transformer implements Viewable {
    private static final Map<String, Decompiler> BY_ID = new LinkedHashMap<>();
    private static final Map<String, Decompiler> BY_NAME = new LinkedHashMap<>();

    static {
        new KrakatauDecompiler().register();
        new FernflowerDecompiler().register();
        new CFRDecompiler().register();
        new ProcyonDecompiler().register();
    }

    private final String originalId;
    private final String originalName;

    public Decompiler(String id, String name) {
        this(id, name, null);
    }

    public Decompiler(String id, String name, Class<? extends TransformerSettings.Setting> settingsClass) {
        super(id + "-decompiler", name + " Decompiler", settingsClass);
        this.originalId = id;
        this.originalName = name;
    }

    @Override
    public final Decompiler register() {
        if (BY_ID.containsKey(originalId)) {
            throw new IllegalArgumentException(originalId + " already exists!");
        }
        if (BY_NAME.containsKey(originalName)) {
            throw new IllegalArgumentException(originalName + " already exists!");
        }
        super.register();
        BY_ID.put(originalId, this);
        BY_NAME.put(originalName, this);
        return this;
    }

    @Override
    public boolean isApplicable(String className) {
        return className.endsWith(".class");
    }

    @Override
    public JComponent open(ClassManager cm, ClassData data) {
        ClickableSyntaxTextArea area = new ClickableSyntaxTextArea(cm, this, data.getFileName(), data.getClassName());
        area.getCaret().setSelectionVisible(true);
        area.setText("Decompiling... this may take a while");
        RTextScrollPane scrollPane = new RTextScrollPane(area);
        scrollPane.setLineNumbersEnabled(true);
        scrollPane.setFoldIndicatorEnabled(true);
        return scrollPane;
    }

    public Object transform(Object... args) {
        return decompile((ClassNode) args[0], (byte[]) args[1], (StringBuilder) args[2]);
    }

    public abstract boolean decompile(ClassNode classNode, byte[] bytes, StringBuilder output);

    public static Decompiler getById(String id) {
        return BY_ID.get(id);
    }

    public static Decompiler getByName(String name) {
        return BY_NAME.get(name);
    }

    public static Collection<Decompiler> getAllDecompilers() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }
}
