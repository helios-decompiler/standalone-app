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

package com.heliosdecompiler.helios.ui.views.file;

import com.heliosdecompiler.helios.Message;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class FileChooserView {
    private String title = "Choose a File";
    private List<FileFilter> filters = new ArrayList<>();
    private FileFilter defaultFilter;
    private File initialDir = new File(".");

    private String initialFile;

    public FileChooserView withTitle(Message.FormattedMessage message) {
        this.title = message.getText();
        return this;
    }

    public FileChooserView withInitialDirectory(File initialDir) {
        if (initialDir != null && initialDir.isDirectory())
            this.initialDir = initialDir;
        return this;
    }

    public FileChooserView withInitialFile(File initialFile) {
        if (initialFile != null) {
            withInitialDirectory(initialFile.getParentFile());
            this.initialFile = initialFile.getName();
        }
        return this;
    }

    public FileChooserView withExtensionFilter(FileFilter filter, boolean selected) {
        filters.add(filter);
        if (selected)
            this.defaultFilter = filter;
        return this;
    }

    public abstract File promptSingle();

    public abstract File promptSave();

    public abstract List<File> promptMultiple();

    public String getTitle() {
        return title;
    }

    public List<FileFilter> getFilters() {
        return filters;
    }

    public FileFilter getDefaultFilter() {
        return defaultFilter;
    }

    public File getInitialDir() {
        return initialDir;
    }

    public String getInitialFile() {
        return initialFile;
    }
}
