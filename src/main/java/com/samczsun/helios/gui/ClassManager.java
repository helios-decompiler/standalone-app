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

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.samczsun.helios.Helios;
import com.samczsun.helios.LoadedFile;
import com.samczsun.helios.Settings;
import com.samczsun.helios.api.events.*;
import com.samczsun.helios.api.events.Listener;
import com.samczsun.helios.api.events.requests.RefreshViewRequest;
import com.samczsun.helios.api.events.requests.SearchRequest;
import com.samczsun.helios.gui.data.ClassData;
import com.samczsun.helios.gui.data.ClassTransformationData;
import com.samczsun.helios.handler.ExceptionHandler;
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
import org.eclipse.swt.widgets.*;
import org.fife.ui.hex.swing.HexEditor;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class ClassManager {

    private final Shell shell;
    private final CTabFolder mainTabs;

    private final ConcurrentHashMap<String, ClassData> opened = new ConcurrentHashMap<>();

    ClassManager(Shell rootShell, CTabFolder tabs) {
        this.mainTabs = tabs;
        this.shell = rootShell;
        this.mainTabs.addCTabFolder2Listener(new CTabFolder2Adapter() {
            public void close(CTabFolderEvent event) {
                ClassData data = (ClassData) event.item.getData();
                opened.remove(data.getFileName() + data.getClassName());
            }
        });

        Events.registerListener(new Listener() {
            @Override
            public void handleSearchRequest(SearchRequest request) {
                shell.getDisplay().asyncExec(() -> search(request));
            }

            @Override
            public void handleRefreshViewRequest(RefreshViewRequest request) {
                shell.getDisplay().asyncExec(() -> refreshCurrentView());
            }
        });
    }

    void openFile(String file, String name) {
        String extension = name.lastIndexOf('.') == -1 ? "" : name.substring(name.lastIndexOf('.'));
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
            innerTabFolder.addMouseListener(new GenericClickListener((clickType, doubleClick) -> ClassManager.this.handleNewTabRequest(), GenericClickListener.ClickType.RIGHT));
            innerTabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
                public void close(CTabFolderEvent event) {
                    ((ClassData) fileTab.getData()).close((Transformer) event.item.getData());
                }
            });

            mainTabs.setSelection(fileTab);

            Transformer defaultTransformer = null;

            JsonValue star = Settings.FILETYPE_ASSOCIATIONS.get().asObject().get(".*");

            for (JsonObject.Member member : Settings.FILETYPE_ASSOCIATIONS.get().asObject()) {
                if (member.getName().equals(extension)) {
                    if (defaultTransformer == null) {
                        defaultTransformer = Transformer.getById(member.getValue().asString());
                    }
                }
            }

            if (defaultTransformer == null && star != null) {
                defaultTransformer = Transformer.getById(star.asString());
            }

            if (defaultTransformer == null) {
                defaultTransformer = Transformer.HEX;
            }


            Transformer transformer = defaultTransformer;

            ClassTransformationData transformationData = classData.open(defaultTransformer);
            CTabItem nestedItem = new CTabItem(innerTabFolder, SWT.BORDER | SWT.CLOSE);
            nestedItem.setText(defaultTransformer.getName());
            nestedItem.setData(defaultTransformer);
            transformationData.setTransformerTab(nestedItem);
            nestedItem.setControl(new SwingControl(innerTabFolder, SWT.NONE) {
                protected JComponent createSwingComponent() {
                    JComponent component = transformer.open(ClassManager.this, classData);
                    if (component instanceof RTextScrollPane) {
                        RTextScrollPane scrollPane = (RTextScrollPane) component;
                        Future<?> future = Helios.submitBackgroundTask(new DecompileTask(classData.getFileName(), classData.getClassName(), (ClickableSyntaxTextArea) scrollPane.getTextArea(), transformer, null));
                        transformationData.futures.add(future);
                    }
                    return component;
                }

                public Composite getLayoutAncestor() {
                    return innerTabFolder;
                }

                protected void afterComponentCreatedSWTThread() {
                    innerTabFolder.setSelection(nestedItem);
                }
            });
        });
    }

    public void closeCurrentTab() {
        CTabItem item = mainTabs.getSelection();
        if (item != null) {
            ClassData data = (ClassData) item.getData();
            CTabFolder nested = (CTabFolder) item.getControl();
            for (CTabItem decompilerTab : nested.getItems()) {
                Transformer transformer = (Transformer) decompilerTab.getData();
                ClassTransformationData ctd = data.close(transformer);
                for (Future<?> future : ctd.futures) {
                    future.cancel(true);
                }
            }
            opened.remove(data.getFileName() + data.getClassName());
            item.dispose();
        }
    }

    public void closeCurrentInnerTab() {
        CTabItem item = mainTabs.getSelection();
        if (item != null) {
            ClassData data = (ClassData) item.getData();
            CTabFolder nested = (CTabFolder) item.getControl();
            CTabItem nestedItem = nested.getSelection();
            if (nestedItem != null) {
                Transformer transformer = (Transformer) nestedItem.getData();
                ClassTransformationData ctd = data.close(transformer);
                for (Future<?> future : ctd.futures) {
                    System.out.println("Cancelling future");
                    future.cancel(true);
                }
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
            Stream.of(Decompiler.getAllDecompilers(), Disassembler.getAllDisassemblers(), Arrays.asList(Transformer.HEX, Transformer.TEXT))
                    .flatMap(Collection::stream)
                    .forEach(transformer -> {
                        if (!transformer.isApplicable(data.getClassName())) {
                            return;
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

                            JComponent component = transformer.open(this, data);
                            if (component instanceof RTextScrollPane) {
                                RTextScrollPane scrollPane = (RTextScrollPane) component;
                                if (scrollPane.getTextArea() instanceof ClickableSyntaxTextArea) {
                                    Future<?> future = Helios.submitBackgroundTask(new DecompileTask(data.getFileName(), data.getClassName(), (ClickableSyntaxTextArea) scrollPane.getTextArea(), transformer, null));
                                    transformerData.futures.add(future);
                                }
                            }
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
                    });
            menu.setLocation(SWTUtil.getMouseLocation());
            menu.setVisible(true);
        });
    }

    private void refreshCurrentView() {
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

    private void search(SearchRequest request) {
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
            SwingUtilities.invokeLater(() -> {
                Field dotField = null;
                Field markField = null;
                try {
                    dotField = DefaultCaret.class.getDeclaredField("dot");
                    dotField.setAccessible(true);
                    markField = DefaultCaret.class.getDeclaredField("mark");
                    markField.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
                SearchContext context = new SearchContext();
                context.setSearchFor(request.getText());
                context.setMatchCase(request.isMatchCase());
                try {
                    if (!SearchEngine.find(textArea, context).wasFound()) {
                        if (request.isWrap()) {
                            int old = textArea.getCaretPosition();
                            int omark = textArea.getCaret().getMark();
                            if (request.isSearchForward()) {
                                dotField.setInt(textArea.getCaret(), 0);
                                markField.setInt(textArea.getCaret(), 0);
                                if (!SearchEngine.find(textArea, context).wasFound()) {
                                    dotField.setInt(textArea.getCaret(), old);
                                    markField.setInt(textArea.getCaret(), omark);
                                    shell.getDisplay().asyncExec(() -> mainTabs.getDisplay().beep());
                                }
                            } else {
                                dotField.setInt(textArea.getCaret(), textArea.getDocument().getLength() - 1);
                                markField.setInt(textArea.getCaret(), textArea.getDocument().getLength() - 1);
                                if (!SearchEngine.find(textArea, context).wasFound()) {
                                    dotField.setInt(textArea.getCaret(), old);
                                    markField.setInt(textArea.getCaret(), omark);
                                    shell.getDisplay().asyncExec(() -> mainTabs.getDisplay().beep());
                                }
                            }
                        } else {
                            shell.getDisplay().asyncExec(() -> mainTabs.getDisplay().beep());
                        }
                    }
                } catch (Throwable t) {
                    ExceptionHandler.handle(t);
                }
            });
        } else {
            mainTabs.getDisplay().beep();
        }
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

            JComponent component = currentTransformer.open(this, data);
            if (component instanceof RTextScrollPane) {
                RTextScrollPane scrollPane = (RTextScrollPane) component;
                Future<?> future = Helios.submitBackgroundTask(new DecompileTask(data.getFileName(), data.getClassName(), (ClickableSyntaxTextArea) scrollPane.getTextArea(), currentTransformer, jumpTo));
                transformationData.futures.add(future);
            }
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
}
