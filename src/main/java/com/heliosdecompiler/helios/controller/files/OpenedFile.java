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

import com.google.common.base.Joiner;
import com.heliosdecompiler.helios.gui.model.CommonError;
import com.heliosdecompiler.helios.gui.model.Message;
import com.heliosdecompiler.helios.gui.model.TreeNode;
import com.heliosdecompiler.helios.ui.MessageHandler;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class OpenedFile {
    public static final String FULL_PATH_KEY = "full-path";
    public static final String IS_ROOT_FILE = "root-file";
    public static final String IS_LEAF = "is-leaf";
    public static final String OPENED_FILE = "opened-file";

    private final MessageHandler messageHandler;
    private final Path target;

    private Map<String, byte[]> fileContents = new HashMap<>();

    private TreeNode root;

    public OpenedFile(MessageHandler messageHandler, File target) {
        this.messageHandler = messageHandler;
        this.target = target.toPath();

        reset();
    }

    public TreeNode getRoot() {
        return this.root;
    }

    public Path getTarget() {
        return this.target;
    }

    public void reset() {
        this.root = new TreeNode(this.target.getFileName().toString());
        this.root.setFlag(IS_ROOT_FILE, true);

        readQuick();

        for (String path : fileContents.keySet()) {
            String[] split = path.split("/");

            TreeNode now = root;

            for (int i = 0; i < split.length; i++) {
                String segment = split[i];
                TreeNode next = now.getChild(segment);
                if (next == null) {
                    next = now.createChild(segment);
                    next.getMetadata().put(FULL_PATH_KEY, Joiner.on('/').join(Arrays.asList(split).subList(0, i + 1)));
                    next.getMetadata().put(OPENED_FILE, this);

                    if (i == split.length - 1) {
                        next.setFlag(IS_LEAF, true);
                    }
                }
                now = next;
            }
        }
    }

    public byte[] getContent(String path) {
        return Arrays.copyOf(this.fileContents.get(path), this.fileContents.get(path).length);
    }

    private void readQuick() {
        if (!Files.exists(this.target)) {
            this.messageHandler.handleError(CommonError.DOES_NOT_EXIST.format(this.target.toString()));
            return;
        }
        if (!Files.isReadable(this.target)) {
            this.messageHandler.handleError(CommonError.NO_READ_PERMISSIONS.format(this.target.toString()));
            return;
        }

        byte[] fileData;

        try {
            fileData = Files.readAllBytes(this.target);
        } catch (IOException e) {
            this.messageHandler.handleException(Message.IOEXCEPTION_OCCURRED, e);
            return;
        }

        this.fileContents.clear();
        this.fileContents = new HashMap<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(fileData))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                // todo warn about CRC
                if (!entry.isDirectory()) {
                    this.fileContents.put(entry.getName(), IOUtils.toByteArray(zipInputStream));
                }
            }
        } catch (Exception ex) {
            this.messageHandler.handleException(Message.UNKNOWN_ERROR, ex);
            return;
        }

        // If files is still empty, then it's not a zip file (or something weird happened)
        if (this.fileContents.size() == 0) {
            this.fileContents.put(this.target.toString(), fileData);
        }
    }

    public Map<String, byte[]> getContents() {
        return this.fileContents;
    }
}
