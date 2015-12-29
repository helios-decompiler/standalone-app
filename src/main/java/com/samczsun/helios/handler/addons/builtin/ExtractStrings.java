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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ExtractStrings extends Addon {
    public ExtractStrings() {
        super(false);
    }

    public void start(Display display, MenuItem menuItem) {
        menuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Helios.submitBackgroundTask(() -> {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (ClassNode classNode : Helios.loadAllClasses()) {
                        for (FieldNode fieldNode : classNode.fields) {
                            Object v = fieldNode.value;
                            if (v instanceof String) {
                                String s = (String) v;
                                if (!s.isEmpty()) {
                                    stringBuilder.append(
                                            String.format("%s.%s%s -> \"%s\"\n", classNode.name, fieldNode.name, fieldNode.desc,
                                                    s.replaceAll("\\n", "\\\\n").replaceAll("\\r", "\\\\r")));
                                }
                            }
                            if (v instanceof String[]) {
                                for (int i = 0; i < ((String[]) v).length; i++) {
                                    String s = ((String[]) v)[i];
                                    if (!s.isEmpty()) {
                                        stringBuilder.append(
                                                String.format("%s.%s%s[%s] -> \"%s\"\n", classNode.name, fieldNode.name, fieldNode.desc,
                                                        i, s.replaceAll("\\n", "\\\\n").replaceAll("\\r", "\\\\r")));
                                    }
                                }
                            }
                        }
                        for (MethodNode m : classNode.methods) {
                            InsnList insnList = m.instructions;
                            for (AbstractInsnNode abstractInsnNode : insnList.toArray()) {
                                if (abstractInsnNode instanceof LdcInsnNode) {
                                    if (((LdcInsnNode) abstractInsnNode).cst instanceof String) {
                                        final String s = (String) ((LdcInsnNode) abstractInsnNode).cst;
                                        if (!s.isEmpty()) {
                                            stringBuilder.append(
                                                    String.format("%s.%s%s -> \"%s\"\n", classNode.name, m.name, m.desc,
                                                            s.replaceAll("\\n", "\\\\n").replaceAll("\\r", "\\\\r")));
                                        }
                                    }
                                }
                            }
                        }
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
        return "Extract Strings";
    }
}
