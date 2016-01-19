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
import com.samczsun.helios.Resources;
import com.samczsun.helios.api.events.Events;
import com.samczsun.helios.api.events.Listener;
import com.samczsun.helios.api.events.requests.TreeUpdateRequest;
import com.samczsun.helios.tasks.DecompileAndSaveTask;
import com.samczsun.helios.utils.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class TreeManager {
    private final Tree tree;

    public TreeManager(Tree tree) {
        this.tree = tree;
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (SWTUtil.isEnter(e.keyCode)) {
                    TreeItem[] items = tree.getSelection();
                    for (TreeItem treeItem : items) {
                        if (treeItem.getItemCount() == 0) {
                            click(treeItem);
                        } else {
                            treeItem.setExpanded(!treeItem.getExpanded());
                        }
                    }
                }
            }
        });
        tree.addListener(SWT.MeasureItem, event -> {
        });
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                if (e.button == 1 && e.count % 2 == 0) {
                    TreeItem item = tree.getItem(new Point(e.x, e.y));
                    if (item != null) {
                        if (item.getItemCount() == 0) {
                            click(item);
                        } else {
                            item.setExpanded(!item.getExpanded());
                        }
                    }
                } else if (e.button == 3) {
                    TreeItem[] items = tree.getSelection();
                    TreeItem item = tree.getItem(new Point(e.x, e.y));

                    Menu menu = new Menu(Helios.getGui().getShell(), SWT.POP_UP);

                    MenuItem decompileAllSelected = new MenuItem(menu, SWT.PUSH);
                    decompileAllSelected.setText("Decompile &All Selected");
                    decompileAllSelected.setEnabled(items.length > 0);
                    decompileAllSelected.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            TreeItem[] items = tree.getSelection();
                            List<Pair<String, String>> data = new ArrayList<>();
                            LinkedList<TreeItem> process = new LinkedList<>();
                            process.addAll(Arrays.asList(items));
                            while (!process.isEmpty()) {
                                TreeItem item = process.pop();
                                TreeItem[] children = item.getItems();
                                if (children.length == 0) {
                                    data.add(getFileName(item));
                                } else {
                                    process.addAll(Arrays.asList(children));
                                }
                            }
                            Helios.submitBackgroundTask(new DecompileAndSaveTask(data));
                        }
                    });

                    MenuItem decompileSelected = new MenuItem(menu, SWT.PUSH);
                    decompileSelected.setText("Decompile &Selected");
                    decompileSelected.setEnabled(item != null);
                    decompileSelected.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            if (item != null) {
                                List<Pair<String, String>> data = Arrays.asList(getFileName(item));
                                Helios.submitBackgroundTask(new DecompileAndSaveTask(data));
                            }
                        }
                    });

                    menu.setLocation(SWTUtil.getMouseLocation());
                    menu.setVisible(true);
                }
            }
        });
        Events.registerListener(new Listener() {
            public void handleTreeUpdateRequest(TreeUpdateRequest request) {
                update();
            }
        });
    }

    public void reset() {
        tree.getDisplay().syncExec(() -> dispose(tree.getItems()));
    }

    public void click(TreeItem item) {
        Pair<String, String> info = getFileName(item);
        if (info.getValue1().length() > 0) {
            String fileName = info.getValue1();
            Helios.getGui().getClassManager().openFile(info.getValue0(), fileName);
        }
    }

    public Pair<String, String> getFileName(TreeItem item) {
        TreeItem currentItem = item;
        StringBuilder name = new StringBuilder();
        while (currentItem.getParentItem() != null) {
            name.insert(0, currentItem.getText()).insert(0, "/");
            currentItem = currentItem.getParentItem();
        }
        return new Pair<>(currentItem.getText(),
                name.toString().substring(1)); // Name is something like "/package/goes/here/classname.class"
    }

    private void update() {
        List<SpoofedTreeItem> roots = new ArrayList<>();
        for (LoadedFile loadedFile : Helios.getAllFiles()) {
            Map<String, SpoofedTreeItem> map = new HashMap<>();
            SpoofedTreeItem root = new SpoofedTreeItem();
            roots.add(root);
            root.name = loadedFile.getName();
            for (Map.Entry<String, byte[]> entry : loadedFile.getFiles().entrySet()) {
                final String[] spl = entry.getKey().split("/");
                SpoofedTreeItem last = root;
                for (int i = 0; i < spl.length; i++) {
                    String joined = join(i, spl);
                    SpoofedTreeItem child = map.get(joined);
                    if (child == null) {
                        child = new SpoofedTreeItem();
                        child.parent = last;
                        child.name = spl[i];
                        last.children.add(child);
                        map.put(joined, child);
                    }
                    last = child;
                }
            }
        }
        update(roots);
        sort(roots);
        Display display = tree.getDisplay();
        display.syncExec(() -> {
            roots:
            for (SpoofedTreeItem root : roots) {
                for (TreeItem child : tree.getItems()) {
                    if (child.getText().equals(root.name)) {
                        continue roots;
                    }
                }
                update(new TreeItem(tree, SWT.NONE), root);
            }
        });
    }

    private void sort(List<SpoofedTreeItem> items) {
        Collections.sort(items, (o1, o2) -> {
            int result = o1.type.compareTo(o2.type);
            if (result != 0) return result;
            return o1.name.compareTo(o2.name);
        });
        for (SpoofedTreeItem spoof : items) {
            sort(spoof.children);
        }
    }

    private void update(TreeItem last, SpoofedTreeItem lastspoof) {
        while (last.getDisplay().readAndDispatch()) ; //Is there any way we can make this better? (so GUI doesn't hang)
        last.setText(lastspoof.name);
        last.setImage(lastspoof.icon);
        for (SpoofedTreeItem child : lastspoof.children) {
            update(new TreeItem(last, SWT.NONE), child);
        }
    }

    private String join(int end, String[] arr) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i <= end; i++) {
            out.append(arr[i]).append('/');
        }
        if (out.length() > 0 && out.charAt(out.length() - 1) == '/') {
            out.setLength(out.length() - 1);
        }
        return out.toString();
    }

    private void update(List<SpoofedTreeItem> items) {
        for (SpoofedTreeItem treeItem : items) {
            render(treeItem);
            update(treeItem.children);
        }
    }

    private void dispose(TreeItem[] items) {
        for (TreeItem treeItem : items) {
            dispose(treeItem.getItems());
            treeItem.dispose();
        }
    }

    private TreeItem findChild(TreeItem root, String name) {
        for (TreeItem item : root.getItems()) {
            if (item.getText().equals(name)) {
                return item;
            }
        }
        return null;
    }

    private void render(SpoofedTreeItem item) {
        String name = item.name;
        Resources type = null;
        if (name.endsWith(".jar")) {
            type = Resources.JAR;
        } else if (name.endsWith(".zip")) {
            type = Resources.ZIP;
        } else if (name.endsWith(".bat")) {
            type = Resources.BAT;
        } else if (name.endsWith(".sh")) {
            type = Resources.SH;
        } else if (name.endsWith(".cs")) {
            type = Resources.CSHARP;
        } else if (name.endsWith(".c") || name.endsWith(".cpp") || name.endsWith(".h")) {
            type = Resources.CPLUSPLUS;
        } else if (name.endsWith(".apk") || name.endsWith(".dex")) {
            type = Resources.ANDROID;
        } else if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(
                ".bmp") || name.endsWith(".gif")) {
            type = Resources.IMAGE;
        } else if (name.endsWith(".class")) {
            type = Resources.CLASS;
        } else if (name.endsWith(".java")) {
            type = Resources.JAVA;
        } else if (name.endsWith(".txt") || name.endsWith(".md")) {
            type = Resources.TEXT;
        } else if (name.endsWith(".properties") || name.endsWith(".xml") || name.endsWith(".mf") || name.endsWith(
                ".config") || name.endsWith(".cfg")) {
            type = Resources.CONFIG;
        } else if (item.children.size() <= 0) { //random file
            type = Resources.FILE;
        } else { //folder
            Set<SpoofedTreeItem> checked = new HashSet<>();
            Stack<SpoofedTreeItem> check = new Stack<>();
            boolean isJava = false;
            check.push(item);
            while (!check.isEmpty()) {
                SpoofedTreeItem toCheck = check.pop();
                if (checked.add(toCheck)) {
                    if (toCheck.name.endsWith(".java") || toCheck.name.endsWith(".class")) {
                        isJava = true;
                    }
                    check.addAll(toCheck.children);
                }
                if (isJava) {
                    break;
                }
            }

            if (isJava) {
                type = Resources.PACKAGE;
            } else {
                type = Resources.FOLDER;
            }
        }
        item.icon = type.getImage();
        item.type = type;
    }

    private static class SpoofedTreeItem {
        String name;
        Resources type;
        Image icon;
        final List<SpoofedTreeItem> children = new ArrayList<>();
        SpoofedTreeItem parent;
    }
}
