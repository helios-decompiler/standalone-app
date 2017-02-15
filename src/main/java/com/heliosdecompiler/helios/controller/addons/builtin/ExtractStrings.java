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

package com.heliosdecompiler.helios.controller.addons.builtin;

public class ExtractStrings /*extends Addon*/ {
//    public ExtractStrings() {
//        super(false);
//    }
//
//    public void start(Display display, MenuItem menuItem) {
//        menuItem.addSelectionListener(new SelectionAdapter() {
//            @Override
//            public void widgetSelected(SelectionEvent e) {
//                Helios.submitBackgroundTask(() -> {
//                    StringBuilder stringBuilder = new StringBuilder();
//                    for (ClassNode classNode : FileManager.loadAllClasses()) {
//                        for (FieldNode fieldNode : classNode.fields) {
//                            Object v = fieldNode.value;
//                            if (v instanceof String) {
//                                String s = (String) v;
//                                if (!s.isEmpty()) {
//                                    stringBuilder.append(
//                                            String.format("%s.%s%s -> \"%s\"\n", classNode.name, fieldNode.name, fieldNode.desc,
//                                                    StringEscapeUtils.escapeJava(s)));
//                                }
//                            }
//                            if (v instanceof String[]) {
//                                for (int i = 0; i < ((String[]) v).length; i++) {
//                                    String s = ((String[]) v)[i];
//                                    if (!s.isEmpty()) {
//                                        stringBuilder.append(
//                                                String.format("%s.%s%s[%s] -> \"%s\"\n", classNode.name, fieldNode.name, fieldNode.desc,
//                                                        i, StringEscapeUtils.escapeJava(s)));
//                                    }
//                                }
//                            }
//                        }
//                        for (MethodNode m : classNode.methods) {
//                            InsnList insnList = m.instructions;
//                            for (AbstractInsnNode abstractInsnNode : insnList.toArray()) {
//                                if (abstractInsnNode instanceof LdcInsnNode) {
//                                    if (((LdcInsnNode) abstractInsnNode).cst instanceof String) {
//                                        final String s = (String) ((LdcInsnNode) abstractInsnNode).cst;
//                                        if (!s.isEmpty()) {
//                                            stringBuilder.append(
//                                                    String.format("%s.%s%s -> \"%s\"\n", classNode.name, m.name, m.desc,
//                                                            StringEscapeUtils.escapeJava(s)));
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    display.syncExec(() -> {
//                        Shell shell = new Shell(display);
//                        shell.setImage(Resources.ICON.getImage());
//                        shell.setText("Addon | Extract Strings");
//                        shell.setLayout(new FillLayout());
//                        Text text = new Text(shell, SWT.V_SCROLL | SWT.H_SCROLL);
//                        text.setText(stringBuilder.toString());
//                        text.addListener(SWT.KeyDown, event -> {
//                            if (event.keyCode == 'a' && (event.stateMask & SWT.CTRL) != 0) {
//                                text.selectAll();
//                                event.doit = false;
//                            }
//                        });
//                        SWTUtil.center(shell);
//                        shell.open();
//                    });
//                });
//            }
//        });
//    }
//
//    @Override
//    public String getName() {
//        return "Extract Strings";
//    }
}
