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

package com.heliosdecompiler.helios.utils;

import com.heliosdecompiler.helios.Helios;
import com.heliosdecompiler.helios.Resources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class FileChooserUtil {
    public static List<File> chooseFiles(String startingPath, List<String> extensions, boolean multi) {
        AtomicReference<List<File>> returnValue = new AtomicReference<>();
        if (Thread.currentThread() == Display.getDefault().getThread()) {
            chooseFiles0(startingPath, extensions, multi, returnValue);
        } else {
            Display.getDefault().syncExec(() -> chooseFiles0(startingPath, extensions, multi, returnValue));
        }
        return returnValue.get();
    }

    private static void chooseFiles0(String startingPath, List<String> extensions, boolean multi, AtomicReference<List<File>> result) {
        Shell shell = Helios.getGui().getShell();
        FileDialog dialog = new FileDialog(shell, SWT.OPEN | (multi ? SWT.MULTI : 0));
        if (!extensions.isEmpty()) {
            StringBuilder extension = new StringBuilder();
            for (String extension1 : extensions) {
                extension.append("*.").append(extension1).append(";");
            }
            dialog.setFilterExtensions(new String[]{extension.toString(), "*.*"});
        } else {
            dialog.setFilterExtensions(new String[]{"*.*"});
        }
        File file = new File(startingPath);
        if (file.exists()) {
            dialog.setFilterPath(file.isDirectory() ? startingPath : file.getParent());
            if (file.isFile()) {
                dialog.setFileName(file.getName());
            }
        }
        dialog.open();
        String[] selectedFileNames = dialog.getFileNames();
        if (selectedFileNames.length > 0) {
            List<File> files = new ArrayList<>();
            for (String selectedFileName : selectedFileNames) {
                StringBuilder buf = new StringBuilder(dialog.getFilterPath());
                if (buf.charAt(buf.length() - 1) != File.separatorChar) buf.append(File.separatorChar);
                buf.append(selectedFileName);
                files.add(new File(buf.toString()));
            }
            result.set(files);
        } else {
            result.set(Collections.emptyList());
        }

    }

    public static File chooseSaveLocation(final String startingPath, final List<String> extensions) {
        final AtomicReference<File> returnValue = new AtomicReference<>();
        Display.getDefault().syncExec(() -> {
            String[] validExtensions = new String[extensions.size()];
            for (int index = 0; index < extensions.size(); index++) {
                validExtensions[index] = "*." + extensions.get(index);
            }
            Shell shell = new Shell(Display.getDefault());
            FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.SAVE);
            dialog.setFilterExtensions(validExtensions);
            dialog.setFilterPath(startingPath);
            dialog.open();
            String selectedName = dialog.getFileName();
            shell.close();
            if (!shell.isDisposed()) {
                System.out.println("Shell did not dispose properly");
            }
            if (!selectedName.isEmpty()) {
                StringBuilder buf = new StringBuilder(dialog.getFilterPath());
                if (buf.charAt(buf.length() - 1) != File.separatorChar) buf.append(File.separatorChar);
                buf.append(selectedName);
                returnValue.set(new File(buf.toString()));
            } else {
                returnValue.set(null);
            }
        });
        return returnValue.get();
    }
}
