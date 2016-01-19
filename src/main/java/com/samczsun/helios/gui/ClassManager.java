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

package com.samczsun.helios.gui;

import com.samczsun.helios.Helios;
import com.samczsun.helios.LoadedFile;
import com.samczsun.helios.handler.files.FileHandler;
import com.samczsun.helios.tasks.DecompileTask;
import com.samczsun.helios.transformers.Transformer;
import com.samczsun.helios.transformers.decompilers.Decompiler;
import com.samczsun.helios.transformers.disassemblers.Disassembler;
import com.samczsun.helios.utils.SWTUtil;
import org.eclipse.albireo.core.SwingControl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.fife.ui.hex.swing.HexEditor;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.JComponent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ClassManager {
    private final CTabFolder mainTabs;
    private final Shell shell;

    private final ConcurrentHashMap<String, Boolean> opened = new ConcurrentHashMap<>();

    public ClassManager(Shell rootShell, CTabFolder tabs) {
        this.mainTabs = tabs;
        this.shell = rootShell;
        this.mainTabs.addCTabFolder2Listener(new CTabFolder2Adapter() {
            public void close(CTabFolderEvent event) {
                Object[] o = (Object[]) event.item.getData();
                opened.remove(o[0].toString() + o[1].toString());
            }
        });
    }

    public void openFile(String file, String name) {
        if (!opened.containsKey(file + name)) {
            opened.put(file + name, true);
            Display display = Display.getDefault();
            String simpleName = name;
            if (simpleName.lastIndexOf('/') != -1) {
                simpleName = simpleName.substring(simpleName.lastIndexOf('/') + 1, simpleName.length());
            }
            String finalName = simpleName;
            display.syncExec(() -> {
                CTabItem fileTab = new CTabItem(mainTabs, SWT.BORDER | SWT.CLOSE);
                fileTab.setText(finalName);
                Object[] data = new Object[]{file, name, new ArrayList<String>()}; //File, Name, Tabs open
                fileTab.setData(data);

                CTabFolder innerTabFolder = new CTabFolder(mainTabs, SWT.BORDER);
                fileTab.setControl(innerTabFolder);
                innerTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
                    public void close(CTabFolderEvent event) {
                        ((List<String>) ((Object[]) fileTab.getData())[2]).remove(
                                ((Transformer) event.item.getData()).getId());
                    }
                });

                mainTabs.setSelection(fileTab);

                for (FileHandler handler : FileHandler.getAllHandlers()) {
                    if (handler.accept(name)) {
                        CTabItem nestedItem = new CTabItem(innerTabFolder, SWT.BORDER | SWT.CLOSE);
                        nestedItem.setData(handler.getId());
                        nestedItem.setText(handler.getId());

                        nestedItem.setControl(handler.generateTab(innerTabFolder, file, name));
                        innerTabFolder.setSelection(nestedItem);
                        ((List<String>) ((Object[]) fileTab.getData())[2]).add(handler.getId());
                        return;
                    }
                }
                CTabItem nestedItem = new CTabItem(innerTabFolder, SWT.BORDER | SWT.CLOSE);
                nestedItem.setText(Transformer.HEX.getName());
                nestedItem.setData(Transformer.HEX);

                nestedItem.setControl(FileHandler.ANY.generateTab(innerTabFolder, file, name));
                ((List<String>) ((Object[]) fileTab.getData())[2]).add(Transformer.HEX.getId());
                innerTabFolder.setSelection(nestedItem);
            });
        } else {
            CTabItem[] items = mainTabs.getItems();
            for (CTabItem item : items) {
                Object[] data = (Object[]) item.getData();
                if (file.equals(data[0]) && name.equals(data[1])) {
                    mainTabs.setSelection(item);
                    return;
                }
            }
        }
    }

    public void closeCurrentTab() {
        CTabItem item = mainTabs.getSelection();
        if (item != null) {
            opened.remove(((Object[]) item.getData())[0].toString() + ((Object[]) item.getData())[1].toString());
            item.dispose();
        }
    }

    public void closeCurrentInnerTab() {
        CTabItem item = mainTabs.getSelection();
        if (item != null) {
            CTabFolder nested = (CTabFolder) item.getControl();
            CTabItem nestedItem = nested.getSelection();
            if (nestedItem != null) {
                ((List<String>) ((Object[]) item.getData())[2]).remove(((Transformer) nestedItem.getData()).getId());
                nestedItem.dispose();
            }
        }
    }

    public void reset() {
        mainTabs.getDisplay().syncExec(() -> {
            for (CTabItem item : mainTabs.getItems()) {
                item.dispose();
            }
        });
        this.opened.clear();
    }

    public void handleNewTabRequest() {
        Display display = mainTabs.getDisplay();
        display.asyncExec(() -> {
            if (mainTabs.getSelection() != null) {
                CTabItem item = mainTabs.getSelection();
                Object[] data = ((Object[]) item.getData());
                LoadedFile loadedFile = Helios.getLoadedFile(data[0].toString());
                CTabFolder nested = (CTabFolder) item.getControl();
                Menu menu = new Menu(shell, SWT.POP_UP);
                if (data[1].toString().endsWith(".class")) {
                    List<Transformer> transformers = new ArrayList<>();
                    transformers.addAll(Decompiler.getAllDecompilers());
                    transformers.addAll(Disassembler.getAllDisassemblers());
                    transformers.add(Transformer.HEX);
                    for (Transformer transformer : transformers) {
                        MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
                        menuItem.setText(transformer.getName());
                        menuItem.addListener(SWT.Selection, event -> {
                            List<String> open = (List<String>) data[2];
                            if (!open.contains(transformer.getId())) {
                                CTabItem decompilerTab = new CTabItem(nested, SWT.BORDER | SWT.CLOSE);
                                decompilerTab.setText(transformer.getName());
                                decompilerTab.setData(transformer);
                                open.add(transformer.getId());

                                if (transformer != Transformer.HEX) {
                                    RSyntaxTextArea area = new RSyntaxTextArea();
                                    area.getCaret().setSelectionVisible(true);
                                    RTextScrollPane scrollPane = new RTextScrollPane(area);
                                    scrollPane.setLineNumbersEnabled(true);
                                    scrollPane.setFoldIndicatorEnabled(true);

                                    SwingControl control = new SwingControl(nested, SWT.NONE) {
                                        protected JComponent createSwingComponent() {
                                            return scrollPane;
                                        }

                                        public Composite getLayoutAncestor() {
                                            return shell;
                                        }

                                        protected void afterComponentCreatedSWTThread() {
                                            nested.setSelection(decompilerTab);
                                        }
                                    };
                                    control.setLayout(new FillLayout());
                                    decompilerTab.setControl(control);
                                    area.setText("Decompiling... this may take a while");
                                    Helios.submitBackgroundTask(
                                            new DecompileTask(data[0].toString(), data[1].toString(), area,
                                                    transformer));
                                } else {
                                    final HexEditor editor = new HexEditor();
                                    try {
                                        editor.open(new ByteArrayInputStream(loadedFile.getFiles().get(data[1])));
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }

                                    SwingControl control = new SwingControl(nested, SWT.NONE) {
                                        protected JComponent createSwingComponent() {
                                            return editor;
                                        }

                                        public Composite getLayoutAncestor() {
                                            return shell;
                                        }

                                        protected void afterComponentCreatedSWTThread() {
                                            nested.setSelection(decompilerTab);
                                        }
                                    };
                                    control.setLayout(new FillLayout());
                                    decompilerTab.setControl(control);
                                }
                            } else {
                                CTabItem[] items = nested.getItems();
                                for (CTabItem innerItem : items) {
                                    if (transformer.equals(innerItem.getData())) {
                                        nested.setSelection(innerItem);
                                        return;
                                    }
                                }
                            }
                        });
                    }
                } else {
                    MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
                    menuItem.setText("Text");
                    menuItem.addListener(SWT.Selection, event -> {
                        List<String> open = (List<String>) data[2];
                        if (!open.contains("text")) {
                            CTabItem decompilerTab = new CTabItem(nested, SWT.BORDER | SWT.CLOSE);
                            decompilerTab.setText("text");
                            decompilerTab.setData("text");
                            open.add("text");

                            RSyntaxTextArea area = new RSyntaxTextArea();
                            area.setText(new String(loadedFile.getFiles().get(data[1].toString())));
                            RTextScrollPane scrollPane = new RTextScrollPane(area);

                            SwingControl control = new SwingControl(nested, SWT.NONE) {
                                protected JComponent createSwingComponent() {
                                    return scrollPane;
                                }

                                public Composite getLayoutAncestor() {
                                    return shell;
                                }

                                protected void afterComponentCreatedSWTThread() {
                                    nested.setSelection(decompilerTab);
                                }
                            };
                            control.setLayout(new FillLayout());
                            decompilerTab.setControl(control);
                        } else {
                            CTabItem[] items = nested.getItems();
                            for (CTabItem innerItem : items) {
                                Object[] innerData = (Object[]) item.getData();
                                if ("text".equals(innerData[1])) {
                                    nested.setSelection(innerItem);
                                    return;
                                }
                            }
                        }
                    });
                }
                menu.setLocation(SWTUtil.getMouseLocation());
                menu.setVisible(true);
            }
        });
    }

    public void refreshCurrentView() {
        CTabItem file = mainTabs.getSelection();
        if (file != null) {
            Object[] data = ((Object[]) file.getData());
            LoadedFile loadedFile = Helios.getLoadedFile(data[0].toString());
            CTabItem decompiler = ((CTabFolder) file.getControl()).getSelection();
            if (decompiler != null) {
                SwingControl control = (SwingControl) decompiler.getControl();
                if (decompiler.getData().equals("text")) {
                    RTextScrollPane scrollPane = (RTextScrollPane) control.getSwingComponent();
                    RSyntaxTextArea textArea = (RSyntaxTextArea) scrollPane.getTextArea();
                    textArea.setText(new String(loadedFile.getFiles().get(data[1].toString())));
                } else if (decompiler.getData().equals(Transformer.HEX)) {
                    final HexEditor editor = (HexEditor) control.getSwingComponent();
                    try {
                        editor.open(new ByteArrayInputStream(loadedFile.getFiles().get(data[1])));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } else {

                }
            }
        }
    }

    int lastIndex = 0;
    int lastStr = -1;

    public void search(String find) {
        CTabItem file = mainTabs.getSelection();
        if (file != null) {
            CTabItem decompiler = ((CTabFolder) file.getControl()).getSelection();
            if (decompiler != null) {
                SwingControl control = (SwingControl) decompiler.getControl();
                if (!decompiler.getData().equals(Transformer.HEX)) {
                    RTextScrollPane scrollPane = (RTextScrollPane) control.getSwingComponent();
                    RSyntaxTextArea textArea = (RSyntaxTextArea) scrollPane.getTextArea();
                    String text = textArea.getText();
                    if (text.hashCode() != lastStr) {
                        lastIndex = 0;
                        lastStr = text.hashCode();
                    }
                    if (lastIndex > text.length()) lastIndex = 0;
                    int index = text.substring(lastIndex).indexOf(find);
                    System.out.println(lastIndex + ", " + text.substring(lastIndex));
                    if (index == -1) {
                        index = text.indexOf(find);
                        lastIndex = 0;
                        if (index == -1) {
                            file.getDisplay().beep();
                            return;
                        }
                    }
                    textArea.setCaretPosition(index + lastIndex);
                    textArea.moveCaretPosition(index + lastIndex + find.length());
                    lastIndex += index + find.length();
                    return;
                }
            }
        }
        mainTabs.getDisplay().beep();
    }
}
