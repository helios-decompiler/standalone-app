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

package com.samczsun.helios.handler.addons.builtin;

import com.samczsun.helios.Helios;
import com.samczsun.helios.api.Addon;
import com.samczsun.helios.utils.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
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
                    for (ClassNode classNode : Helios.loadAllClasses()) {
                        classNode.methods
                                .stream()
                                .filter(methodNode -> methodNode.name.equals("main") && methodNode.desc.equals(
                                        "([Ljava/lang/String;)V"))
                                .forEach(methodNode -> stringBuilder.append(
                                        String.format("%s.%s%s\n", classNode.name, methodNode.name, methodNode.desc)));
                    }

                    display.syncExec(() -> {
                        Shell shell = new Shell(display);
                        Text text = new Text(shell, SWT.V_SCROLL | SWT.H_SCROLL);
                        text.setText(stringBuilder.toString());
                        GC gc = new GC(shell);
                        FontMetrics fm = gc.getFontMetrics();
                        int width = 128 * fm.getAverageCharWidth();
                        int height = fm.getHeight() * 32;
                        text.setSize(width, height);
                        shell.pack();
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
