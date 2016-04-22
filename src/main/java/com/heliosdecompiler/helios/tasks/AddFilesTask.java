/*
 * Copyright 2016 Sam Sun <me@samczsun.com>
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

package com.heliosdecompiler.helios.tasks;

import com.heliosdecompiler.helios.transformers.converters.Converter;
import com.heliosdecompiler.helios.Helios;
import com.heliosdecompiler.helios.Settings;
import com.heliosdecompiler.helios.api.events.Events;
import com.heliosdecompiler.helios.api.events.requests.TreeUpdateRequest;
import com.heliosdecompiler.helios.handler.ExceptionHandler;
import com.heliosdecompiler.helios.utils.APKTool;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

public class AddFilesTask implements Runnable {
    private final File[] files;
    private final boolean addToRecentFiles;

    public AddFilesTask(File[] files, boolean addToRecentFiles) {
        this.files = files == null ? new File[0] : files;
        this.addToRecentFiles = addToRecentFiles;
    }

    @Override
    public void run() {
        try {
            Arrays.stream(files).filter(Objects::nonNull).filter(File::exists).map(file -> {
                try {
                    return file.getCanonicalFile();
                } catch (Exception exception) {
                    return file;
                }
            }).forEach(file -> {
                try {
                    if (addToRecentFiles) Helios.addRecentFile(file);
                    handle(file);
                } catch (IOException e) {
                    ExceptionHandler.handle(e);
                }
            });
        } finally {
            Events.callEvent(new TreeUpdateRequest());
        }
    }

    private void handle(File file) throws IOException {
        if (file.getCanonicalFile().isDirectory()) {
            handleDirectory(file);
        } else {
            handleFile(file);
        }
    }

    private void handleDirectory(File file) throws IOException {
        LinkedList<File> filesToProcess = new LinkedList<>();
        filesToProcess.add(file);
        Set<String> filesProcessed = new HashSet<>();

        while (!filesToProcess.isEmpty()) {
            File current = filesToProcess.pop();
            if (current.isFile() && filesProcessed.add(current.getCanonicalPath())) {
                handle(current);
            } else {
                File[] listFiles = current.listFiles();
                if (listFiles != null) {
                    filesToProcess.addAll(Arrays.asList(listFiles));
                }
            }
        }
    }

    private void handleFile(File file) {
        System.out.println("Handling file " + file);
        String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();
        File fileToLoad = file;
        if (extension.equals("apk")) {
            try {
                if (Settings.APKTOOL.get().asBoolean()) {
                    File decodedResources = File.createTempFile("apktoolout", ".apk");
                    decodedResources.deleteOnExit();
                    APKTool.decodeResources(file, decodedResources);
                    fileToLoad = decodedResources;
                }
            } catch (final Exception e) {
                ExceptionHandler.handle(e);
            }
        }
        if (extension.equals("apk") || extension.equals("dex")) {
            try {
                if (Settings.APK_CONVERSION.get().asString().equals(Converter.ENJARIFY.getId())) {
                    File transformedResources = File.createTempFile("enjarifyout", ".jar");
                    transformedResources.deleteOnExit();
                    Converter.ENJARIFY.convert(fileToLoad, transformedResources);
                    fileToLoad = transformedResources;
                } else if (Settings.APK_CONVERSION.get().asString().equals(Converter.DEX2JAR.getId())) {
                    File transformedResources = File.createTempFile("dex2jarout", ".jar");
                    transformedResources.deleteOnExit();
                    Converter.DEX2JAR.convert(fileToLoad, transformedResources);
                    fileToLoad = transformedResources;
                }
            } catch (final Exception e) {
                ExceptionHandler.handle(e);
            }
        }
        try {
            Helios.loadFile(fileToLoad);
        } catch (final Exception e) {
            ExceptionHandler.handle(e);
        }
    }
}
