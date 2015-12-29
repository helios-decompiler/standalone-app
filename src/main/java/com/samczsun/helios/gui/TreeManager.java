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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class TreeManager {
    private final Tree tree;

    public TreeManager(Tree tree) {
        this.tree = tree;
        tree.addListener(SWT.Selection, event -> {
            if ((event.stateMask & SWT.BUTTON1) == SWT.BUTTON1 && ((TreeItem) event.item).getItemCount() == 0) {
                click((TreeItem) event.item);
            }
        });
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.keyCode == SWT.CR || e.keyCode == 'P') { // 'P' is the code for numpad enter
                    TreeItem item = tree.getSelection()[0];
                    if (item.getItemCount() == 0) {
                        click(item);
                    }
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
        TreeItem currentItem = item;
        StringBuilder name = new StringBuilder();
        while (currentItem.getParentItem() != null) {
            name.insert(0, currentItem.getText()).insert(0, "/");
            currentItem = currentItem.getParentItem();
        }
        if (name.length() > 0) {
            String fileName = name.substring(1); // Name is something like "/package/goes/here/classname.class"
            Helios.getGui().getClassManager().openFile(currentItem.getText(), fileName);
        }
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
        } else if (name.equals("decoded resources")) {
            type = Resources.DECODED;
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
