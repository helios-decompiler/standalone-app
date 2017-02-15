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

    private final String displayName;
    private final TreeNode parent;
    private final Map<String, TreeNode> children = new HashMap<>();
    private final Map<String, Object> metadata = new HashMap<>();

    public TreeNode(String displayName) {
        this.parent = null;
        this.displayName = displayName;
    }

    public TreeNode(TreeNode parent, String displayName) {
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

    public TreeNode getChild(String displayName) {
        return this.children.get(displayName);
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

    public TreeNode createChild(String displayName) {
        TreeNode child = new TreeNode(this, displayName);
        this.children.put(displayName, child);
        return child;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TreeNode)) return false;
        TreeNode treeNode = (TreeNode) o;
        return Objects.equal(displayName, treeNode.displayName) &&
                Objects.equal(parent, treeNode.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(displayName, parent);
    }
}
