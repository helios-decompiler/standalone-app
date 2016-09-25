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

package com.heliosdecompiler.helios.gui;

import com.eclipsesource.json.JsonObject;
import com.heliosdecompiler.helios.Constants;
import com.heliosdecompiler.helios.Helios;
import com.heliosdecompiler.helios.Resources;
import com.heliosdecompiler.helios.Settings;
import com.heliosdecompiler.helios.api.Addon;
import com.heliosdecompiler.helios.api.events.*;
import com.heliosdecompiler.helios.api.events.Listener;
import com.heliosdecompiler.helios.api.events.requests.RecentFileRequest;
import com.heliosdecompiler.helios.gui.data.FiletypeAssociationData;
import com.heliosdecompiler.helios.gui.popups.AddFiletypeAssociationPopup;
import com.heliosdecompiler.helios.gui.popups.SearchPopup;
import com.heliosdecompiler.helios.handler.addons.AddonHandler;
import com.heliosdecompiler.helios.transformers.Transformer;
import com.heliosdecompiler.helios.transformers.TransformerSettings;
import com.heliosdecompiler.helios.transformers.converters.Converter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUI {
    private final Display display = Display.getDefault();
    private final Shell shell;

    private TreeManager treeManager;
    private ClassManager classManager;
    private SearchPopup searchPopup;
    private SearchPanel searchPanel;

    public GUI(Shell shell) {
        this.shell = shell;
        display.syncExec(() -> {
            display.addFilter(SWT.KeyDown, Helios::checkHotKey);
            this.shell.setImage(Resources.ICON.getImage());
            shell.setLayout(new GridLayout());
            shell.setText(Constants.REPO_NAME + " - " + Constants.REPO_VERSION + " | By samczsun");

            setupMenuBar();
            setupSashForm();

//            Monitor primary = display.getPrimaryMonitor();
//            Rectangle bounds = primary.getBounds();
//            Rectangle rect = shell.getBounds();
//            int x = bounds.x + (bounds.width - rect.width) / 2;
//            int y = bounds.y + (bounds.height - rect.height) / 2;
//            shell.setLocation(x, y);

            shell.setMaximized(true);

            searchPopup = new SearchPopup();
        });
    }

    private void setupMenuBar() {
        Menu menuBar = new Menu(shell, SWT.BAR);
        setupFileMenu(menuBar);
        setupSettingsMenu(menuBar);
        setupAddonsBar(menuBar);

        shell.setMenuBar(menuBar);
    }

    private void setupFileMenu(Menu menuBar) {
        MenuItem fileMenuLabel = new MenuItem(menuBar, SWT.CASCADE);
        fileMenuLabel.setText("&File");
        Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
        fileMenuLabel.setMenu(fileMenu);

        MenuItem reset = new MenuItem(fileMenu, SWT.PUSH);
        reset.setText("&New");
        reset.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Helios.resetWorkSpace(true);
            }
        });
        MenuItem open = new MenuItem(fileMenu, SWT.PUSH);
        open.setText("&Open");
        open.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Helios.promptForFilesToOpen();
            }
        });
        MenuItem openRecent = new MenuItem(fileMenu, SWT.CASCADE);
        openRecent.setText("Open Recen&t");

        setupRecentFiles(openRecent);

        MenuItem refresh = new MenuItem(fileMenu, SWT.PUSH);
        refresh.setText("&Refresh");
        refresh.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Helios.promptForRefresh();
            }
        });

        new MenuItem(fileMenu, SWT.SEPARATOR);

        MenuItem addToContext = new MenuItem(fileMenu, SWT.PUSH);
        addToContext.setText("Add to &Context Menu");
        addToContext.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                Helios.addToContextMenu();
            }
        });

        new MenuItem(fileMenu, SWT.SEPARATOR);

        MenuItem exit = new MenuItem(fileMenu, SWT.PUSH);
        exit.setText("E&xit");
        exit.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                System.exit(0);
            }
        });
    }

    private void setupSettingsMenu(Menu menuBar) {
        MenuItem settingsMenuLabel = new MenuItem(menuBar, SWT.CASCADE);
        settingsMenuLabel.setText("&Settings");
        Menu settingsMenu = new Menu(shell, SWT.DROP_DOWN);
        settingsMenuLabel.setMenu(settingsMenu);

        MenuItem python2 = new MenuItem(settingsMenu, SWT.PUSH);
        python2.setText("Set Python &2.x Executable");
        python2.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Helios.setLocationOf(Settings.PYTHON2_LOCATION);
            }
        });
        MenuItem python3 = new MenuItem(settingsMenu, SWT.PUSH);
        python3.setText("Set Python &3.x Executable");
        python3.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Helios.setLocationOf(Settings.PYTHON3_LOCATION);
            }
        });
        MenuItem rt = new MenuItem(settingsMenu, SWT.PUSH);
        rt.setText("Set &rt.jar Location");
        rt.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Helios.setLocationOf(Settings.RT_LOCATION);
            }
        });
        MenuItem path = new MenuItem(settingsMenu, SWT.PUSH);
        path.setText("Set &path");
        path.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Helios.promptForCustomPath();
            }
        });

        new MenuItem(settingsMenu, SWT.SEPARATOR);

        MenuItem apkTool = new MenuItem(settingsMenu, SWT.CHECK);
        apkTool.setText("&Transform APKs with APKTool");
        apkTool.setSelection(Settings.APKTOOL.get().asBoolean());
        apkTool.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Settings.APKTOOL.set(apkTool.getSelection());
            }
        });

        MenuItem apkConverters = new MenuItem(settingsMenu, SWT.CASCADE);
        apkConverters.setText("APK &Conversion");
        setupApkConversion(apkConverters);

        new MenuItem(settingsMenu, SWT.SEPARATOR);

        setupDecompilerSettings(settingsMenu);

        new MenuItem(settingsMenu, SWT.SEPARATOR);

        MenuItem filetypeAssociations = new MenuItem(settingsMenu, SWT.CASCADE);
        filetypeAssociations.setText("&Filetype Associations");
        setupFiletypeAssociations(filetypeAssociations);
    }

    private void setupDecompilerSettings(Menu settingsMenu) {
        for (Transformer transformer : Transformer.getAllTransformers(Transformer::hasSettings)) {
            MenuItem transformerSettingsMenuItem = new MenuItem(settingsMenu, SWT.CASCADE);
            transformerSettingsMenuItem.setText(transformer.getName());
            Menu transformerSettingsMenu = new Menu(shell, SWT.DROP_DOWN);
            transformerSettingsMenuItem.setMenu(transformerSettingsMenu);
            for (TransformerSettings.Setting setting : transformer.getSettings().getRegisteredSettings()) {
                MenuItem settingItem = new MenuItem(transformerSettingsMenu, SWT.CHECK);
                settingItem.setText(setting.getText());
                settingItem.setSelection(setting.isEnabled());
                settingItem.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        setting.setEnabled(settingItem.getSelection());
                    }
                });
            }
        }
    }

    private void setupFiletypeAssociations(MenuItem filetypeAssociations) {
        Menu filetypeMenu = new Menu(filetypeAssociations);
        Map<String, MenuItem> extensionToItem = new HashMap<>();

        for (JsonObject.Member next : Settings.FILETYPE_ASSOCIATIONS.get().asObject()) {
            FiletypeAssociationData data = new FiletypeAssociationData(next.getName(), next.getValue().asString());
            MenuItem add = new MenuItem(filetypeMenu, SWT.PUSH);
            add.setData(data);
            add.setText(data.formatName());
            add.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent selectionEvent) {
                    selectionEvent.doit = false;
                    new AddFiletypeAssociationPopup(add).open();
                }
            });

            extensionToItem.put(data.getExtension(), add);
        }

        MenuItem add = new MenuItem(filetypeMenu, SWT.PUSH);
        add.setText("Add");
        add.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent selectionEvent) {
                selectionEvent.doit = false;
                new AddFiletypeAssociationPopup().open();
            }
        });

        Events.registerListener(new Listener() {
            @Override
            public void handleFiletypeAssociationCreation(FiletypeAssociationCreateEvent event) {
                display.asyncExec(() -> {
                    FiletypeAssociationData data = new FiletypeAssociationData(event.getExtension(), event.getTransformer().getId());
                    MenuItem add = new MenuItem(filetypeMenu, SWT.PUSH, filetypeMenu.getItemCount() - 1);
                    add.setData(data);
                    add.setText(data.formatName());
                    add.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent selectionEvent) {
                            selectionEvent.doit = false;
                            new AddFiletypeAssociationPopup(add).open();
                        }
                    });

                    extensionToItem.put(data.getExtension(), add);
                    Settings.FILETYPE_ASSOCIATIONS.get().asObject().set(event.getExtension(), event.getTransformer().getId());
                });
            }

            @Override
            public void handleFiletypeAssociationEdit(FiletypeAssociationEditEvent event) {
                display.asyncExec(() -> {
                    MenuItem item = extensionToItem.get(event.getExtension());
                    FiletypeAssociationData data = (FiletypeAssociationData) item.getData();
                    data.setTransformer(event.getTransformer());
                    item.setText(data.formatName());

                    Settings.FILETYPE_ASSOCIATIONS.get().asObject().set(event.getExtension(), event.getTransformer().getId());
                });
            }

            @Override
            public void handleFiletypeAssociationDeletion(FiletypeAssociationDeleteEvent event) {
                display.asyncExec(() -> {
                    MenuItem item = extensionToItem.get(event.getExtension());
                    item.dispose();
                    extensionToItem.remove(event.getExtension());
                    Settings.FILETYPE_ASSOCIATIONS.get().asObject().remove(event.getExtension());
                });
            }
        });

        filetypeAssociations.setMenu(filetypeMenu);
    }

    private void setupAddonsBar(Menu menuBar) {
        MenuItem addonsMenuLabel = new MenuItem(menuBar, SWT.CASCADE);
        addonsMenuLabel.setText("&Addons");
        Menu addonsMenu = new Menu(shell, SWT.DROP_DOWN);
        addonsMenuLabel.setMenu(addonsMenu);

        for (Addon addon : AddonHandler.getAllAddons()) {
            MenuItem addonItem = new MenuItem(addonsMenu, addon.requiresCascade() ? SWT.CASCADE : SWT.PUSH);
            addonItem.setText(addon.getName());
            addon.start(menuBar.getDisplay(), addonItem);
        }
    }

    private void setupApkConversion(MenuItem apkConverters) {
        SelectionAdapter selectionAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                MenuItem item = (MenuItem) e.getSource();
                if (item.getSelection()) {
                    Settings.APK_CONVERSION.set(((Converter) item.getData()).getId());
                }
            }
        };
        Menu apkConverterMenu = new Menu(apkConverters);

        for (Converter converter : Arrays.asList(Converter.NONE, Converter.ENJARIFY, Converter.DEX2JAR)) {
            MenuItem converterItem = new MenuItem(apkConverterMenu, SWT.RADIO);
            converterItem.setText("&" + converter.getName());
            converterItem.setData(converter);
            converterItem.addSelectionListener(selectionAdapter);
            converterItem.setSelection(Settings.APK_CONVERSION.get().asString().equals(converter.getId()));
        }

        apkConverters.setMenu(apkConverterMenu);
    }

    private void setupRecentFiles(MenuItem openRecent) {
        Menu recentFilesMenu = new Menu(openRecent);
        Events.registerListener(new Listener() {
            public void handleRecentFileRequest(RecentFileRequest request) {
                Display display = Display.getDefault();
                display.asyncExec(() -> {
                    List<String> recentFiles = request.getFiles();
                    MenuItem[] items = recentFilesMenu.getItems();
                    int index = 0;
                    if (items.length > 0) {
                        String last = items[0].getText();
                        index = recentFiles.indexOf(last) + 1;
                    }
                    for (int i = index; i < recentFiles.size(); i++) {
                        MenuItem item = new MenuItem(recentFilesMenu, SWT.PUSH, 0);
                        item.setText(recentFiles.get(i));
                        item.addSelectionListener(new SelectionAdapter() {
                            @Override
                            public void widgetSelected(SelectionEvent e) {
                                Helios.openFiles(new File[]{new File(item.getText())}, false);
                            }
                        });
                    }
                });
            }
        });
        openRecent.setMenu(recentFilesMenu);
        Helios.updateRecentFiles();
    }

    private void setupSashForm() {
        SashForm sashForm = new SashForm(shell, SWT.HORIZONTAL);
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
        sashForm.setLayout(new FillLayout());

        setupTree(sashForm);
        setupTabs(sashForm);
        setupRight(sashForm);

        sashForm.setWeights(new int[]{20, 65, 15});

        setupStatusBar();
    }

    private void setupRight(SashForm outer) {
        SashForm sashForm = new SashForm(outer, SWT.VERTICAL);
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
        sashForm.setLayout(new FillLayout());

        setupHierachy(sashForm);
        searchPanel = new SearchPanel(sashForm);

        sashForm.setWeights(new int[]{30, 70});
    }

    private void setupHierachy(SashForm sashForm) {
        Composite comp = new Composite(sashForm, SWT.BORDER);

    }

    private void setupTree(SashForm sashForm) {
        Tree tree = new Tree(sashForm, SWT.BORDER | SWT.MULTI | SWT.VIRTUAL);
        tree.setLayoutData(new GridData(GridData.FILL_BOTH));

        DropTarget dt = new DropTarget(tree, DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK);
        dt.setTransfer(new Transfer[]{FileTransfer.getInstance()});
        dt.addDropListener(new DropTargetAdapter() {
            public void drop(DropTargetEvent event) {
                if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
                    String[] fileNames = (String[]) event.data;
                    File[] files = new File[fileNames.length];
                    for (int i = 0; i < fileNames.length; i++) {
                        files[i] = new File(fileNames[i]);
                    }
                    Helios.openFiles(files, true);
                }
            }
        });

        treeManager = new TreeManager(tree);
    }

    private void setupTabs(SashForm sashForm) {
        Composite classHolder = new Composite(sashForm, SWT.NONE);
        classHolder.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout classHolderLayout = new GridLayout();
        classHolderLayout.numColumns = 1;
        classHolder.setLayout(classHolderLayout);
        CTabFolder tabFolder = new CTabFolder(classHolder, SWT.BORDER);
        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        this.classManager = new ClassManager(shell, tabFolder);
    }

    private void setupStatusBar() {
        Composite composite = new Composite(shell, 0);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        composite.setLayout(new GridLayout(10, false));

        ProgressBar progressBar = new ProgressBar(composite, SWT.RIGHT);
        progressBar.setMaximum(Constants.TOTAL_MEMORY.get());
        progressBar.setSelection(Constants.USED_MEMORY.get());
        progressBar.addPaintListener(e -> {
            Point widgetSize = progressBar.getSize();
            String text = progressBar.getSelection() + " of " + progressBar.getMaximum() + "M";
            Point textSize = e.gc.stringExtent(text);
            e.gc.setForeground(progressBar.getDisplay().getSystemColor(SWT.COLOR_BLACK));
            e.gc.drawString(text, ((widgetSize.x - textSize.x) / 2), ((widgetSize.y - textSize.y) / 2), true);
        });
        progressBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                System.gc();
                System.runFinalization();
            }
        });
        progressBar.pack();
        Label text = new Label(composite, SWT.NONE);
        text.setText("0 background tasks running");
        text.pack();
        composite.pack();

        new Thread(() -> {
            while (!shell.isDisposed()) {
                display.syncExec(() -> {
                    if (!shell.isDisposed()) {
                        progressBar.setSelection(Constants.USED_MEMORY.get());
                        progressBar.setMaximum(Constants.TOTAL_MEMORY.get());
                        progressBar.redraw();
                        text.setText(Helios.getBackgroundTaskHandler().getActiveTasks() + " background tasks running");
                        text.redraw();
                    }
                });
                try {
                    Thread.sleep(500);
                } catch (Throwable ignored) {
                }
            }
        }, "Memory and Task Updater").start();
    }

    public ClassManager getClassManager() {
        return this.classManager;
    }

    public SearchPopup getSearchPopup() {
        return this.searchPopup;
    }

    public Shell getShell() {
        return this.shell;
    }

    public TreeManager getTreeManager() {
        return this.treeManager;
    }

    public SearchPanel getSearchPanel() {
        return this.searchPanel;
    }
}
