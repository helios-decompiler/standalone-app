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
import com.heliosdecompiler.helios.ui.MessageHandler;
import org.apache.commons.configuration2.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class RecentFileController {
    @Inject
    private Configuration configuration;

    @Inject
    private MessageHandler messageHandler;

    public void addRecentFile(File file) {
        List<String> recentFiles = configuration.getList(String.class, Settings.RECENT_FILES_KEY, new ArrayList<>());
        int maxRecentFiles = configuration.getInt(Settings.MAX_RECENT_FILES_KEY, 10);

        while (recentFiles.size() > maxRecentFiles - 1) {
            recentFiles.remove(recentFiles.size() - 1);
        }

        try {
            String path = file.getCanonicalPath();
            if (recentFiles.contains(path)) {
                recentFiles.remove(path);
            }
            recentFiles.add(0, path);

            configuration.setProperty(Settings.RECENT_FILES_KEY, recentFiles);
        } catch (IOException e) {
            messageHandler.handleException(Message.ERROR_UNKNOWN_ERROR.format(), e);
        }
    }

    public List<File> getRecentFiles() {
        return configuration.getList(String.class, Settings.RECENT_FILES_KEY, Collections.emptyList())
                .stream()
                .map(File::new)
                .collect(Collectors.toList());
    }
}
