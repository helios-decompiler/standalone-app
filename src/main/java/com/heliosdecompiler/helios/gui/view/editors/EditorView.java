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

package com.heliosdecompiler.helios.gui.view.editors;

import com.heliosdecompiler.helios.controller.files.OpenedFile;
import javafx.scene.Node;

import java.util.concurrent.CompletableFuture;

public abstract class EditorView {

    public final Node createView(OpenedFile file, String path) {
        Node node = createView0(file, path);
        node.getProperties().put("editor", this);
        node.getProperties().put("file", file); // probably a memory leak
        node.getProperties().put("path", path);
        return node;
    }

    protected abstract Node createView0(OpenedFile file, String path);

    public boolean canSave() {
        return true;
    }

    public CompletableFuture<byte[]> save(Node node) {
        return CompletableFuture.completedFuture(new byte[0]);
    }

    public abstract String getDisplayName();
}
