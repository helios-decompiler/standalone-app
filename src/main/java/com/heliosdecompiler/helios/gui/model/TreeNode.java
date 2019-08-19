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

package com.heliosdecompiler.helios.gui.model;

import com.google.common.base.Objects;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TreeNode {

    private final String name;
    private final String displayName;
    private final TreeNode parent;
    private final Map<String, TreeNode> children = new HashMap<>(0);
    private final Map<String, Object> metadata = new HashMap<>(0);

    public TreeNode(String name) {
        this(null, name);
    }

    public TreeNode(TreeNode parent, String name) {
        this(parent, name, name);
    }

    public TreeNode(TreeNode parent, String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
        this.parent = parent;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public TreeNode getParent() {
        return this.parent;
    }

    public Collection<TreeNode> getChildren() {
        return children.values();
    }

    public TreeNode getChild(String name) {
        return this.children.get(name);
    }

    public Map<String, Object> getMetadata() {
        return this.metadata;
    }

    public void setFlag(String key, boolean value) {
        this.metadata.put(key, value);
    }

    public boolean testFlag(String key) {
        return this.metadata.containsKey(key) && this.metadata.get(key) instanceof Boolean && (Boolean) this.metadata.get(key);
    }

    public TreeNode createChild(String name) {
        return createChild(name, name);
    }

    public TreeNode createChild(String name, String displayName) {
        TreeNode child = new TreeNode(this, name, displayName);
        this.children.put(name, child);
        return child;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TreeNode)) return false;
        TreeNode treeNode = (TreeNode) o;
        return Objects.equal(name, treeNode.name) &&
                Objects.equal(parent, treeNode.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, parent);
    }
}
