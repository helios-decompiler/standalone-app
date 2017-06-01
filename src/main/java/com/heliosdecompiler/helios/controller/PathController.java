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

package com.heliosdecompiler.helios.controller;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.heliosdecompiler.helios.Message;
import com.heliosdecompiler.helios.Settings;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTask;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTaskHelper;
import com.heliosdecompiler.helios.controller.files.OpenedFile;
import com.heliosdecompiler.helios.ui.MessageHandler;
import org.apache.commons.configuration2.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Singleton
public class PathController {

    private ReentrantLock lock = new ReentrantLock();
    private List<OpenedFile> pathOpenedFiles = new ArrayList<>();
    private List<File> pathFiles = new ArrayList<>();

    @Inject
    private Configuration configuration;

    @Inject
    private BackgroundTaskHelper tasks;

    @Inject
    private MessageHandler messageHandler;

    public void reload() {
        tasks.submit(new BackgroundTask(Message.TASK_RELOADING_PATH.format(), true, () -> {
            reloadSync();
        }));
    }

    public void reloadSync() {
        List<OpenedFile> reloaded = new ArrayList<>();
        List<File> reloadedFiles = new ArrayList<>();
        List<String> path = configuration.getList(String.class, Settings.PATH_KEY, Collections.emptyList());
        for (String filepath : path) {
            File file = new File(filepath);
            if (file.exists()) {
                reloaded.add(new OpenedFile(messageHandler, file));
                reloadedFiles.add(file);
            }
        }

        lock.lock();
        try {
            pathOpenedFiles = reloaded;
            pathFiles = reloadedFiles;
        } finally {
            lock.unlock();
        }
    }

    public List<File> getFiles() {
        lock.lock();
        try {
            return pathFiles;
        } finally {
            lock.unlock();
        }
    }

    public List<OpenedFile> getOpenedFiles() {
        lock.lock();
        try {
            return pathOpenedFiles;
        } finally {
            lock.unlock();
        }
    }
}
