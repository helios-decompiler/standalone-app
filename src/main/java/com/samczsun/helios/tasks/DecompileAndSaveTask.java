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

package com.samczsun.helios.tasks;

import com.samczsun.helios.Constants;
import com.samczsun.helios.Helios;
import com.samczsun.helios.LoadedFile;
import com.samczsun.helios.Settings;
import com.samczsun.helios.handler.ExceptionHandler;
import com.samczsun.helios.transformers.Transformer;
import com.samczsun.helios.transformers.decompilers.Decompiler;
import com.samczsun.helios.transformers.disassemblers.Disassembler;
import com.samczsun.helios.utils.FileChooserUtil;
import com.samczsun.helios.utils.SWTUtil;
import com.strobel.decompiler.DecompilerSettings;
import org.apache.commons.io.IOUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.javatuples.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DecompileAndSaveTask implements Runnable {
    private final List<Pair<String, String>> data;

    public DecompileAndSaveTask(List<Pair<String, String>> data) {
        this.data = data == null ? Collections.emptyList() : data;
    }


    @Override
    public void run() {
        File file = FileChooserUtil.chooseSaveLocation(Settings.LAST_DIRECTORY.get().asString(), Arrays.asList("zip"));
        if (file.exists()) {
            boolean delete = SWTUtil.promptForYesNo(Constants.REPO_NAME + " - Overwrite existing file",
                    "The selected file already exists. Overwrite?");
            if (!delete) {
                return;
            }
        }

        AtomicReference<Transformer> transformer = new AtomicReference<>();

        Display display = Display.getDefault();
        display.syncExec(() -> {
            Shell shell = new Shell(Display.getDefault());
            Combo combo = new Combo(shell, SWT.DROP_DOWN | SWT.BORDER);
            List<Transformer> transformers = new ArrayList<>();
            transformers.addAll(Decompiler.getAllDecompilers());
            transformers.addAll(Disassembler.getAllDisassemblers());
            for (Transformer t : transformers) {
                combo.add(t.getName());
            }
            shell.pack();
            shell.open();
            System.out.println(Arrays.toString(combo.getItems()));
        });

        // TODO: Ask for list of decompilers

        FileOutputStream fileOutputStream = null;
        ZipOutputStream zipOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(file);
            zipOutputStream = new ZipOutputStream(fileOutputStream);
            for (Pair<String, String> pair : data) {
                StringBuilder buffer = new StringBuilder();
                LoadedFile loadedFile = Helios.getLoadedFile(pair.getValue0());
                if (loadedFile != null) {
                    String innerName = pair.getValue1();
                    byte[] bytes = loadedFile.getData().get(innerName);
                    if (bytes != null) {
                        Decompiler.getById("cfr-decompiler").decompile(null, bytes, buffer);
                        zipOutputStream.putNextEntry(new ZipEntry(innerName.substring(0, innerName.length() - 6) + ".java"));
                        zipOutputStream.write(buffer.toString().getBytes(StandardCharsets.UTF_8));
                        zipOutputStream.closeEntry();
                    }
                }
            }
        } catch (Exception e) {
            ExceptionHandler.handle(e);
        } finally {
            IOUtils.closeQuietly(zipOutputStream);
            IOUtils.closeQuietly(fileOutputStream);
        }
    }
}
