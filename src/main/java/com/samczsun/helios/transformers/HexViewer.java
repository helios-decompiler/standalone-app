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

package com.samczsun.helios.transformers;

import com.samczsun.helios.Helios;
import com.samczsun.helios.gui.data.ClassData;
import com.samczsun.helios.gui.ClassManager;
import com.samczsun.helios.gui.GenericClickListener;
import org.fife.ui.hex.swing.HexEditor;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class HexViewer extends Transformer {
    public HexViewer() {
        super("hex", "Hex");
    }

    @Override
    public Object transform(Object... args) {
        throw new IllegalArgumentException();
    }

    @Override
    public boolean isApplicable(String className) {
        return className.endsWith(".class");
    }

    @Override
    public JComponent open(ClassManager cm, ClassData data) {
        final HexEditor editor = new HexEditor();
        try {
            editor.open(new ByteArrayInputStream(Helios.getLoadedFile(data.getFileName()).getFiles().get(data.getClassName())));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        editor.getViewport().getView().addMouseListener(new GenericClickListener((clickType, doubleClick) -> {
            cm.handleNewTabRequest();
        }, GenericClickListener.ClickType.RIGHT));
        return editor;
    }
}
