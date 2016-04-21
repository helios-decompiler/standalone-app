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
import com.samczsun.helios.transformers.Viewable;
import com.samczsun.helios.transformers.decompilers.Decompiler;
import com.samczsun.helios.transformers.disassemblers.Disassembler;
import com.samczsun.helios.utils.SWTUtil;
import org.eclipse.albireo.core.SwingControl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.fife.ui.hex.swing.HexEditor;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.text.View;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public class ClassManager {
    private final Display display;
    private final Shell shell;
    private final CTabFolder mainTabs;

    private final Map<String, ClassData> opened = new ConcurrentHashMap<>();

    ClassManager(Shell rootShell, CTabFolder tabs) {
        this.display = rootShell.getDisplay();
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
        ClassData classData = opened.computeIfAbsent(file + name, obj -> new ClassData(file, name));
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
            openViewWithTransformer(classData, innerTabFolder, defaultTransformer);
        });
    }

    public void closeCurrentTab() {
        CTabItem item = mainTabs.getSelection();
        if (item != null) {
            ClassData data = getCurrentClassData();
            CTabFolder nested = getCurrentTransformers();
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
            ClassData data = getCurrentClassData();
            CTabFolder nested = getCurrentTransformers();
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

    private boolean isFileOpen() {
        return mainTabs.getSelection() != null;
    }

    public void handleNewTabRequest() {
        Display display = mainTabs.getDisplay();
        if (!isFileOpen()) {
            return;
        }
        display.asyncExec(() -> {
            CTabItem selectedFile = mainTabs.getSelection();
            ClassData selectedFileData = getCurrentClassData();
            CTabFolder transformerTabs = getCurrentTransformers();
            Menu menu = new Menu(shell, SWT.POP_UP);
            menu.setLocation(SWTUtil.getMouseLocation());
            Transformer.getAllTransformers(transformer -> transformer instanceof Viewable)
                    .forEach(transformer -> {
                        if (!((Viewable) transformer).isApplicable(selectedFileData.getClassName())) {
                            return;
                        }
                        MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
                        menuItem.setText(transformer.getName());
                        menuItem.addListener(SWT.Selection, event -> {
                            openViewWithTransformer(selectedFileData, transformerTabs, transformer);
                        });
                    });
            menu.setVisible(true);
        });
    }

    private void openViewWithTransformer(ClassData selectedFileData, CTabFolder transformerTabs, Transformer transformer) {
        if (!(transformer instanceof Viewable)) throw new IllegalArgumentException("Transformer is not viewable");
        ClassTransformationData transformerData = selectedFileData.open(transformer);
        if (transformerData.isInitialized()) {
            transformerTabs.setSelection(transformerData.getTransformerTab());
            return;
        }
        CTabItem transformerTab = new CTabItem(transformerTabs, SWT.BORDER | SWT.CLOSE);
        transformerTab.setText(transformer.getName());
        transformerTab.setData(transformer);
        transformerData.setTransformerTab(transformerTab);

        JComponent component = ((Viewable) transformer).open(this, selectedFileData);
        if (component instanceof RTextScrollPane) {
            RTextScrollPane scrollPane = (RTextScrollPane) component;
            if (scrollPane.getTextArea() instanceof ClickableSyntaxTextArea) {
                Future<?> future = Helios.submitBackgroundTask(new DecompileTask(selectedFileData.getFileName(), selectedFileData.getClassName(), (ClickableSyntaxTextArea) scrollPane.getTextArea(), transformer, null));
                transformerData.futures.add(future);
            }
        }

        Composite composite = new Composite(transformerTabs, SWT.NONE);
        GridLayout compositeLayout = new GridLayout();
        compositeLayout.numColumns = 1;
        composite.setLayout(compositeLayout);
        CustomSwingControl control = new CustomSwingControl(composite, transformerTabs, transformerTab, component);
        control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        transformerTab.setControl(composite);
    }

    public void addSearchBar() {
        Composite composite = mainTabs.getParent();
        if (composite.getChildren().length == 1) {
            Composite searchBar = new Composite(composite, SWT.BORDER);
            searchBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            GridLayout searchBarLayout = new GridLayout();
            searchBarLayout.numColumns = 10;
            searchBar.setLayout(searchBarLayout);
            Text text = new Text(searchBar, SWT.SEARCH | SWT.ICON_SEARCH);
            text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
            text.setFocus();

            Button wrap = new Button(searchBar, SWT.CHECK);
            wrap.setText("&Wrap");
            wrap.setSelection(true);
            Button regex = new Button(searchBar, SWT.CHECK);
            regex.setText("&Regex");
            Button matchCase = new Button(searchBar, SWT.CHECK);
            matchCase.setText("Match &Case");
            Button liveSearch = new Button(searchBar, SWT.CHECK);
            liveSearch.setText("&Live Search");

            Button searchUp = new Button(searchBar, SWT.RADIO);
            searchUp.setText("Search &Up");
            searchUp.setSelection(true);
            Button searchDown = new Button(searchBar, SWT.RADIO);
            searchDown.setText("Search &Down");

            text.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    if (SWTUtil.isEnter(e.keyCode) || liveSearch.getSelection()) {
                        Events.callEvent(new SearchRequest(text.getText(), matchCase.getSelection(), wrap.getSelection(), regex.getSelection(), searchUp.getSelection()));
                    }
                }
            });

            composite.layout();
        } else {
            Composite searchBar = (Composite) composite.getChildren()[1];
            ((GridData) composite.getChildren()[1].getLayoutData()).exclude = false;
            Text text = (Text) searchBar.getChildren()[0];
            text.setFocus();
            composite.layout();
        }
    }

    public boolean tryCloseSearchBar() {
        Composite composite = mainTabs.getParent();
        if (composite.getChildren().length > 1) {
            ((GridData) composite.getChildren()[1].getLayoutData()).exclude = true;
            composite.layout();
            SwingControl control = getCurrentSwingControl();
            if (control.getSwingComponent() instanceof RTextScrollPane) {
                RTextScrollPane pane = (RTextScrollPane) control.getSwingComponent();
                SearchEngine.find(pane.getTextArea(), new SearchContext());
            }
            return true;
        }
        return false;
    }

    private void refreshCurrentView() {
        CTabItem file = getCurrentClass();
        if (file == null) {
            return;
        }
        ClassData classData = getCurrentClassData();
        CTabItem transformerItem = getCurrentTransformer();
        if (transformerItem == null) {
            return;
        }
        Transformer transformer = getTransformer();
        closeCurrentInnerTab();
        openViewWithTransformer(classData, getCurrentTransformers(), transformer);
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
        SwingControl control = getCurrentSwingControl();
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
                context.setRegularExpression(request.isRegex());
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
        openViewWithTransformer(getCurrentClassData(), getCurrentTransformers(), currentTransformer);
    }

    private CTabItem getCurrentClass() {
        return mainTabs.getSelection();
    }

    private ClassData getCurrentClassData() {
        return (ClassData) getCurrentClass().getData();
    }

    private CTabFolder getCurrentTransformers() {
        return (CTabFolder) getCurrentClass().getControl();
    }

    private ClassTransformationData getCurrentTransformerData() {
        return (ClassTransformationData) getCurrentTransformers().getData();
    }

    private CTabItem getCurrentTransformer() {
        return getCurrentTransformers().getSelection();
    }

    private CustomSwingControl getCurrentSwingControl() {
        Control[] ctrl = ((Composite) getCurrentTransformer().getControl()).getChildren();
        return (CustomSwingControl) ctrl[ctrl.length - 1];
//        return (CustomSwingControl) getCurrentTransformer().getControl();
    }

    private Transformer getTransformer() {
        return (Transformer) getCurrentTransformer().getData();
    }

    class CustomSwingControl extends SwingControl {
        private JComponent component;
        private CTabFolder transformerTabs;
        private CTabItem transformerTab;

        CustomSwingControl(Composite composite, CTabFolder transformerTabs, CTabItem transformerTab, JComponent component) {
            super(composite, SWT.NONE);
            this.component = component;
            this.transformerTabs = transformerTabs;
            this.transformerTab = transformerTab;
        }

        @Override
        public JComponent createSwingComponent() {
            return component;
        }

        @Override
        public Composite getLayoutAncestor() {
            return shell;
        }

        @Override
        public void afterComponentCreatedSWTThread() {
            transformerTabs.setSelection(transformerTab);
        }
    }
}
