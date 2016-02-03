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

package com.samczsun.helios.handler.files;

import com.samczsun.helios.Helios;
import com.samczsun.helios.LoadedFile;
import org.eclipse.albireo.core.SwingControl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.JComponent;
import java.nio.charset.StandardCharsets;

public class JavaHandler extends FileHandler {
    @Override
    public Control generateTab(CTabFolder parent, String file, String name) {
        RSyntaxTextArea area = new RSyntaxTextArea();
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        RTextScrollPane scrollPane = new RTextScrollPane(area);

        SwingControl control = new SwingControl(parent, SWT.NONE) {
            protected JComponent createSwingComponent() {
                return scrollPane;
            }

            public Composite getLayoutAncestor() {
                return parent;
            }
        };

        while (parent.getDisplay().readAndDispatch()) ;

        LoadedFile loadedFile = Helios.getLoadedFile(file);
        byte[] data = loadedFile.getFiles().get(name);

        area.setText(new String(data, StandardCharsets.UTF_8));

        return control;
    }

    @Override
    public boolean accept(String name) {
        return name.endsWith(".java");
    }

    @Override
    public String getId() {
        return "Java";
    }
}
