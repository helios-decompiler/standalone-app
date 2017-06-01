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

package com.heliosdecompiler.helios.gui.controller.tree;

import com.heliosdecompiler.helios.gui.model.TreeNode;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;

import java.lang.ref.WeakReference;
import java.util.function.Function;

public class DefaultTreeCell<T> extends TreeCell<T> {

    private HBox hbox;

    private WeakReference<TreeItem<T>> treeItemRef;

    private InvalidationListener treeItemGraphicListener = observable -> {
        updateDisplay(getItem(), isEmpty());
    };
    private WeakInvalidationListener weakTreeItemGraphicListener =
            new WeakInvalidationListener(treeItemGraphicListener);
    private InvalidationListener treeItemListener = new InvalidationListener() {
        @Override
        public void invalidated(Observable observable) {
            TreeItem<T> oldTreeItem = treeItemRef == null ? null : treeItemRef.get();
            if (oldTreeItem != null) {
                oldTreeItem.graphicProperty().removeListener(weakTreeItemGraphicListener);
            }

            TreeItem<T> newTreeItem = getTreeItem();
            if (newTreeItem != null) {
                newTreeItem.graphicProperty().addListener(weakTreeItemGraphicListener);
                treeItemRef = new WeakReference<TreeItem<T>>(newTreeItem);
            }
        }
    };
    private WeakInvalidationListener weakTreeItemListener =
            new WeakInvalidationListener(treeItemListener);

    private Function<T, ContextMenu> contextMenuFunction;

    public DefaultTreeCell(Function<T, ContextMenu> contextMenuFunction) {
        treeItemProperty().addListener(weakTreeItemListener);

        if (getTreeItem() != null) {
            getTreeItem().graphicProperty().addListener(weakTreeItemGraphicListener);
        }

        this.contextMenuFunction = contextMenuFunction;
    }

    void updateDisplay(T item, boolean empty) {
        if (item == null || empty) {
            hbox = null;
            setText(null);
            setGraphic(null);
        } else {
            // update the graphic if one is set in the TreeItem
            TreeItem<T> treeItem = getTreeItem();
            if (treeItem != null && treeItem.getGraphic() != null) {
                if (item instanceof Node) {
                    setText(null);

                    // the item is a Node, and the graphic exists, so
                    // we must insert both into an HBox and present that
                    // to the user (see RT-15910)
                    if (hbox == null) {
                        hbox = new HBox(3);
                    }
                    hbox.getChildren().setAll(treeItem.getGraphic(), (Node) item);
                    setGraphic(hbox);
                } else {
                    hbox = null;
                    setText(item instanceof TreeNode ? ((TreeNode) item).getDisplayName() : item.toString());
                    setGraphic(treeItem.getGraphic());
                }
            } else {
                hbox = null;
                if (item instanceof Node) {
                    setText(null);
                    setGraphic((Node) item);
                } else {
                    setText(item instanceof TreeNode ? ((TreeNode) item).getDisplayName() : item.toString());
                    setGraphic(null);
                }
            }
        }
    }

    @Override
    public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        updateDisplay(item, empty);

        if (item != null && contextMenuFunction != null) {
            setContextMenu(contextMenuFunction.apply(item));
        }
    }
}
