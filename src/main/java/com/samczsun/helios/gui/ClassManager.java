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

package com.samczsun.helios.gui;

import com.samczsun.helios.Helios;
import com.samczsun.helios.LoadedFile;
import com.samczsun.helios.handler.ExceptionHandler;
import com.samczsun.helios.transformers.Transformer;
import com.samczsun.helios.transformers.decompilers.Decompiler;
import com.samczsun.helios.transformers.disassemblers.Disassembler;
import com.samczsun.helios.utils.MultiIterator;
import com.samczsun.helios.utils.SWTUtil;
import org.eclipse.albireo.core.SwingControl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;
import org.fife.ui.hex.swing.HexEditor;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class ClassManager {

    private final CTabFolder mainTabs;
    private final Shell shell;

    private final ConcurrentHashMap<String, ClassData> opened = new ConcurrentHashMap<>();

    public ClassManager(Shell rootShell, CTabFolder tabs) {
        this.mainTabs = tabs;
        this.shell = rootShell;
        this.mainTabs.addCTabFolder2Listener(new CTabFolder2Adapter() {
            public void close(CTabFolderEvent event) {
                ClassData data = (ClassData) event.item.getData();
                opened.remove(data.getFileName() + data.getClassName());
            }
        });
    }

    public void openFile(String file, String name) {
        Display display = Display.getDefault();
        final ClassData classData = opened.computeIfAbsent(file + name, obj -> new ClassData(file, name));
        String finalName = name.substring(name.lastIndexOf('/') + 1, name.length());
        display.syncExec(() -> {
            if (classData.getFileTab() != null) {
                this.mainTabs.setSelection(classData.getFileTab());
                return;
            }
            CTabItem fileTab = new CTabItem(mainTabs, SWT.BORDER | SWT.CLOSE);
            classData.setFileTab(fileTab);
            fileTab.setText(finalName);
            fileTab.setData(classData);

            CTabFolder innerTabFolder = new CTabFolder(mainTabs, SWT.BORDER);
            fileTab.setControl(innerTabFolder);
            innerTabFolder.addMouseListener(new GenericClickListener((clickType, doubleClick) -> {
                ClassManager.this.handleNewTabRequest();
            }, GenericClickListener.ClickType.RIGHT));
            innerTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
                public void close(CTabFolderEvent event) {
                    ((ClassData) fileTab.getData()).close((Transformer) event.item.getData());
                }
            });

            mainTabs.setSelection(fileTab);

            ClassTransformationData transformationData = classData.open(Transformer.HEX);
            CTabItem nestedItem = new CTabItem(innerTabFolder, SWT.BORDER | SWT.CLOSE);
            nestedItem.setText(Transformer.HEX.getName());
            nestedItem.setData(Transformer.HEX);
            transformationData.setTransformerTab(nestedItem);
            nestedItem.setControl(generateTab(innerTabFolder, file, name));
            innerTabFolder.setSelection(nestedItem);
        });

    }

    private Control generateTab(CTabFolder parent, String file, String name) {
        LoadedFile loadedFile = Helios.getLoadedFile(file);
        final HexEditor editor = new HexEditor();
        try {
            editor.open(new ByteArrayInputStream(loadedFile.getFiles().get(name)));
        } catch (IOException e1) {
            ExceptionHandler.handle(e1);
        }
        editor.getViewport().getView().addMouseListener(new GenericClickListener((clickType, doubleClick) -> {
            Helios.getGui().getClassManager().handleNewTabRequest();
        }, GenericClickListener.ClickType.RIGHT));

        SwingControl control = new SwingControl(parent, SWT.NONE) {
            protected JComponent createSwingComponent() {
                return editor;
            }

            public Composite getLayoutAncestor() {
                return parent;
            }
        };
        while (parent.getDisplay().readAndDispatch()) ;
        return control;
    }

    public void closeCurrentTab() {
        CTabItem item = mainTabs.getSelection();
        if (item != null) {
            ClassData data = (ClassData) item.getData();
            opened.remove(data.getFileName() + data.getClassName());
            item.dispose();
        }
    }

    public void closeCurrentInnerTab() {
        CTabItem item = mainTabs.getSelection();
        if (item != null) {
            CTabFolder nested = (CTabFolder) item.getControl();
            CTabItem nestedItem = nested.getSelection();
            if (nestedItem != null) {
                ((ClassData) item.getData()).close(((Transformer) nestedItem.getData()));
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
        if (mainTabs.getSelection() == null) {
            return;
        }
        display.asyncExec(() -> {
            CTabItem item = mainTabs.getSelection();
            ClassData data = (ClassData) item.getData();
            CTabFolder nested = (CTabFolder) item.getControl();
            Menu menu = new Menu(shell, SWT.POP_UP);
            Iterable<? extends Transformer> transformers = MultiIterator.of(Decompiler.getAllDecompilers(),
                    Disassembler.getAllDisassemblers(),
                    Arrays.asList(Transformer.HEX, Transformer.TEXT))
                    .toIterable();
            for (Transformer transformer : transformers) {
                if (!transformer.isApplicable(data.getClassName())) {
                    continue;
                }
                MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
                menuItem.setText(transformer.getName());
                menuItem.addListener(SWT.Selection, event -> {
                    ClassTransformationData transformerData = data.open(transformer);
                    if (transformerData.isInitialized()) {
                        nested.setSelection(transformerData.getTransformerTab());
                        return;
                    }
                    CTabItem decompilerTab = new CTabItem(nested, SWT.BORDER | SWT.CLOSE);
                    decompilerTab.setText(transformer.getName());
                    decompilerTab.setData(transformer);
                    transformerData.setTransformerTab(decompilerTab);

                    JComponent component = transformer.open(this, data, null);
                    SwingControl control = new SwingControl(nested, SWT.NONE) {
                        protected JComponent createSwingComponent() {
                            return component;
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

                });
            }
            menu.setLocation(SWTUtil.getMouseLocation());
            menu.setVisible(true);
        });
    }

    public void refreshCurrentView() {
        CTabItem file = mainTabs.getSelection();
        if (file == null) {
            return;
        }
        CTabItem decompiler = ((CTabFolder) file.getControl()).getSelection();
        if (decompiler == null) {
            return;
        }
        ClassData data = (ClassData) file.getData();
        LoadedFile loadedFile = Helios.getLoadedFile(data.getFileName());
        SwingControl control = (SwingControl) decompiler.getControl();
        if (decompiler.getData().equals("text")) {
            RTextScrollPane scrollPane = (RTextScrollPane) control.getSwingComponent();
            RSyntaxTextArea textArea = (RSyntaxTextArea) scrollPane.getTextArea();
            textArea.setText(new String(loadedFile.getFiles().get(data.getClassName())));
        } else if (decompiler.getData().equals(Transformer.HEX)) {
            final HexEditor editor = (HexEditor) control.getSwingComponent();
            try {
                editor.open(new ByteArrayInputStream(loadedFile.getFiles().get(data.getClassName())));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void search(String find) {
        CTabItem file = mainTabs.getSelection();
        if (file == null) {
            return;
        }
        CTabItem decompiler = ((CTabFolder) file.getControl()).getSelection();
        if (decompiler == null) {
            return;
        }
        SwingControl control = (SwingControl) decompiler.getControl();
        if (!decompiler.getData().equals(Transformer.HEX)) {
            RTextScrollPane scrollPane = (RTextScrollPane) control.getSwingComponent();
            RSyntaxTextArea textArea = (RSyntaxTextArea) scrollPane.getTextArea();
            SearchContext context = new SearchContext();
            context.setSearchFor(find);
            context.setMatchCase(false);
            try {
                if (SearchEngine.find(textArea, context).wasFound()) {
                    return;
                }
            } catch (Throwable t) {
                ExceptionHandler.handle(t);
            }
        }

        // Why?
        mainTabs.getDisplay().beep();
    }

    public void openFileAndDecompile(String fileName, String className, Transformer currentTransformer, String jumpTo) {
        System.out.println("Opening " + fileName + " " + className);
        openFile(fileName, className);
        mainTabs.getDisplay().syncExec(() -> {
            CTabItem item = mainTabs.getSelection();
            ClassData data = (ClassData) item.getData();
            ClassTransformationData transformationData = data.open(currentTransformer);
            if (transformationData.isInitialized()) {
                CTabFolder nested = (CTabFolder) item.getControl();
                nested.setSelection(transformationData.getTransformerTab());
                return;
            }
            CTabFolder nested = (CTabFolder) item.getControl();
            // Menu menu = new Menu(shell, SWT.POP_UP);
            CTabItem decompilerTab = new CTabItem(nested, SWT.BORDER | SWT.CLOSE);
            decompilerTab.setText(currentTransformer.getName());
            decompilerTab.setData(currentTransformer);
            transformationData.setTransformerTab(decompilerTab);

            JComponent scrollPane = currentTransformer.open(this, data, jumpTo);
            // transformationData.setArea(area);
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
        });
    }
}
