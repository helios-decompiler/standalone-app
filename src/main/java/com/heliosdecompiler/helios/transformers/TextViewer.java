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

package com.heliosdecompiler.helios.transformers;

import com.heliosdecompiler.helios.Helios;
import com.heliosdecompiler.helios.gui.data.ClassData;
import com.heliosdecompiler.helios.gui.ClassManager;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.nio.charset.StandardCharsets;

public class TextViewer extends Transformer implements Viewable {
    public TextViewer() {
        super("text", "Text");
    }

    @Override
    public boolean isApplicable(String className) {
        return true;
    }

    @Override
    public Object transform(Object... args) {
        throw new IllegalArgumentException();
    }

    @Override
    public JComponent open(ClassManager cm, ClassData data) {
        RSyntaxTextArea area = new RSyntaxTextArea();
        area.setText(new String(Helios.getLoadedFile(data.getFileName()).getAllData().get(data.getClassName()), StandardCharsets.UTF_8));
        return new RTextScrollPane(area);
    }
}
