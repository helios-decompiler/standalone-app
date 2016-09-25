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

package com.heliosdecompiler.helios.handler.addons.builtin;

import com.heliosdecompiler.helios.FileManager;
import com.heliosdecompiler.helios.Helios;
import com.heliosdecompiler.helios.Resources;
import com.heliosdecompiler.helios.api.Addon;
import com.heliosdecompiler.helios.utils.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.objectweb.asm.tree.ClassNode;

public class FindEntryPoints extends Addon {
    public FindEntryPoints() {
        super(false);
    }

    public void start(Display display, MenuItem menuItem) {
        menuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Helios.submitBackgroundTask(() -> {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (ClassNode classNode : FileManager.loadAllClasses()) {
                        classNode.methods
                                .stream()
                                .filter(methodNode -> methodNode.name.equals("main") && methodNode.desc.equals(
                                        "([Ljava/lang/String;)V"))
                                .forEach(methodNode -> stringBuilder.append(
                                        String.format("%s.%s%s\n", classNode.name, methodNode.name, methodNode.desc)));
                    }

                    display.syncExec(() -> {
                        Shell shell = new Shell(display);
                        shell.setImage(Resources.ICON.getImage());
                        shell.setText("Addon | Find Entry Points");
                        shell.setLayout(new FillLayout());
                        Text text = new Text(shell, SWT.V_SCROLL | SWT.H_SCROLL);
                        text.setText(stringBuilder.toString());
                        SWTUtil.center(shell);
                        shell.open();
                    });
                });
            }
        });
    }

    @Override
    public String getName() {
        return "Find Entry Points";
    }
}
