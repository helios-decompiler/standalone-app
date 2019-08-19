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

package com.heliosdecompiler.helios.controller.files;

import com.heliosdecompiler.helios.Message;
import com.heliosdecompiler.helios.gui.model.TreeNode;
import com.heliosdecompiler.helios.ui.MessageHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class OpenedFile {
    public static final String FULL_PATH_KEY = "full-path";
    public static final String IS_ROOT_FILE = "root-file";
    public static final String IS_LEAF = "is-leaf";
    public static final String OPENED_FILE = "opened-file";

    private final MessageHandler messageHandler;
    private final File target;

    private byte[] fileData;
    private Map<String, byte[]> fileContents = new HashMap<>();

    private TreeNode root;

    public OpenedFile(MessageHandler messageHandler, File target) {
        this.messageHandler = messageHandler;
        this.target = target;

        reset();
    }

    public TreeNode getRoot() {
        return this.root;
    }

    public File getTarget() {
        return this.target;
    }

    public void reset() {
        this.root = new TreeNode(this.target.getName());
        this.root.setFlag(IS_ROOT_FILE, true);
        this.root.getMetadata().put(OPENED_FILE, this);

        readQuick();
        constructNodes();
    }

    private void constructNodes() {
        class Node {
            private Node parent;
            private String name;
            private Map<String, Node> children = new HashMap<>(0);

            private Node(Node parent, String name) {
                this.parent = parent;
                this.name = name;
            }

            @Override
            public String toString() {
                return children.toString();
            }

            public String path() {
                if (parent.parent == null) {
                    return name;
                }
                return parent.path() + "/" + name;
            }
        }

        Set<Node> leaves = new HashSet<>();
        Node root = new Node(null, "");
        for (String path : this.fileContents.keySet()) {
            String[] parts = path.split("/", -1);
            Node now = root;
            for (String part : parts) {
                Node n = now;
                now = now.children.computeIfAbsent(part, k -> new Node(n, part));
            }
            if (now.parent != null) {
                leaves.add(now.parent);
            }
        }

        System.out.println("merging");

        ArrayDeque<Node> toVisit = new ArrayDeque<>(leaves);

        while (!toVisit.isEmpty()) {
            Node now = toVisit.poll();

            if (now.parent == null) continue;

            Node parent = now.parent;
            ArrayDeque<String> newName = new ArrayDeque<>();
            newName.push(now.name);

            while (parent.parent != null && parent.children.size() == 1) {
                newName.push(parent.name);
                parent = parent.parent;
            }

            parent.children.remove(newName.getFirst());

            now.parent = parent;
            now.name = String.join("/", newName);
            now.parent.children.put(now.name, now);

            toVisit.add(now.parent);
        }

        System.out.println(root);

        Map<Node, TreeNode> counterpart = new IdentityHashMap<>();
        counterpart.put(root, this.root);
        toVisit.clear();
        toVisit.add(root);
        while (!toVisit.isEmpty()) {
            Node virtualNow = toVisit.poll();
            TreeNode realNow = counterpart.get(virtualNow);

            virtualNow.children.forEach((k, v) -> {
                String displayName = k;
                if (displayName.isEmpty()) {
                    displayName = virtualNow.name;
                }
                TreeNode realV = realNow.createChild(k, displayName);
                realV.getMetadata().put(FULL_PATH_KEY, v.path());
                realV.getMetadata().put(OPENED_FILE, this);

                if (v.children.isEmpty()) {
                    realV.setFlag(IS_LEAF, true);
                }

                counterpart.put(v, realV);
            });
            toVisit.addAll(virtualNow.children.values());
        }
        System.out.println("done virtualizing");
    }

    public byte[] getContent(String path) {
        System.out.println("getting content of " + path);
        return Arrays.copyOf(this.fileContents.get(path), this.fileContents.get(path).length);
    }

    private void readQuick() {
        this.fileContents.clear();
        this.fileContents = new HashMap<>();

        try {
            this.fileData = FileUtils.readFileToByteArray(this.target);

            try (ZipFile zipFile = new ZipFile(this.target)) {
                Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
                while (enumeration.hasMoreElements()) {
                    ZipEntry entry = enumeration.nextElement();

                    InputStream inputStream = zipFile.getInputStream(entry);
                    int read = inputStream.read();
                    if (read == -1) {
                        continue;
                    }
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    out.write(read);
                    IOUtils.copy(inputStream, out);
                    this.fileContents.put(entry.getName(), out.toByteArray());
                }
            } catch (ZipException ignored) {
            }

            System.out.println("done loading");

            // If files is still empty, then it's not a zip file (or something weird happened)
            if (this.fileContents.size() == 0) {
                this.fileContents.put(this.target.toString(), fileData);
            }
        } catch (Exception ex) {
            this.messageHandler.handleException(Message.ERROR_UNKNOWN_ERROR.format(), ex);
        }
    }

    public Map<String, byte[]> getContents() {
        return this.fileContents;
    }

    public void putContent(String path, byte[] data) {
        this.fileContents.put(path, data);
    }
}
