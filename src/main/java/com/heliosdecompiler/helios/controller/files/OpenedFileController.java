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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.heliosdecompiler.helios.Message;
import com.heliosdecompiler.helios.controller.RecentFileController;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTask;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTaskHelper;
import com.heliosdecompiler.helios.gui.controller.FileTreeController;
import com.heliosdecompiler.helios.ui.MessageHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class OpenedFileController {
    private ObservableMap<String, OpenedFile> loadedFiles = FXCollections.observableHashMap();

    @Inject
    private MessageHandler messageHandler;

    @Inject
    private BackgroundTaskHelper backgroundTaskHelper;

    @Inject
    private RecentFileController recentFileController;

    public List<OpenedFile> getLoadedFiles() {
        return new ArrayList<>(this.loadedFiles.values());
    }

    public void openFile(File selectedFile) {
        backgroundTaskHelper.submit(new BackgroundTask(Message.TASK_LOADING_FILE.format(selectedFile.getName()), true, () -> {
            openFileSync(selectedFile);
        }));
    }

    public void openFileSync(File selectedFile) {
        recentFileController.addRecentFile(selectedFile);
        this.loadedFiles.put(selectedFile.getName(), new OpenedFile(messageHandler, selectedFile));
    }

    public ObservableMap<String, OpenedFile> loadedFiles() {
        return this.loadedFiles;
    }

    public void clear() {
        loadedFiles.clear();
    }

    public void reload(FileTreeController controller) {
        backgroundTaskHelper.submit(new BackgroundTask(Message.TASK_RELOADING_FILES.format(), true, () -> {
            for (OpenedFile openedFile : this.loadedFiles.values()) {
                openedFile.reset();
            }
            controller.updateTree();
        }));
    }
}
