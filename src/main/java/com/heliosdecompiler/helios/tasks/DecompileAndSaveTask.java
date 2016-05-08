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

import com.heliosdecompiler.helios.Settings;
import com.heliosdecompiler.helios.handler.ExceptionHandler;
import com.heliosdecompiler.helios.transformers.Transformer;
import com.heliosdecompiler.helios.transformers.decompilers.Decompiler;
import com.heliosdecompiler.helios.utils.FileChooserUtil;
import com.heliosdecompiler.helios.Constants;
import com.heliosdecompiler.helios.Helios;
import com.heliosdecompiler.helios.LoadedFile;
import com.heliosdecompiler.helios.transformers.disassemblers.Disassembler;
import com.heliosdecompiler.helios.utils.SWTUtil;
import org.apache.commons.io.IOUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.javatuples.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
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
        File file = FileChooserUtil.chooseSaveLocation(Settings.LAST_DIRECTORY.get().asString(), Collections.singletonList("zip"));
        if (file == null) return;
        if (file.exists()) {
            boolean delete = SWTUtil.promptForYesNo(Constants.REPO_NAME + " - Overwrite existing file",
                    "The selected file already exists. Overwrite?");
            if (!delete) {
                return;
            }
        }

        AtomicReference<Transformer> transformer = new AtomicReference<>();

        Display display = Display.getDefault();
        display.asyncExec(() -> {
            Shell shell = new Shell(Display.getDefault());
            FillLayout layout = new FillLayout();
            layout.type = SWT.VERTICAL;
            shell.setLayout(layout);
            Transformer.getAllTransformers(t -> {
                return t instanceof Decompiler
                        || t instanceof Disassembler;
            }).forEach(t -> {
                Button button = new Button(shell, SWT.RADIO);
                button.setText(t.getName());
                button.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        transformer.set(t);
                    }
                });
            });
            Button ok = new Button(shell, SWT.NONE);
            ok.setText("OK");
            ok.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    shell.close();
                    shell.dispose();
                    synchronized (transformer) {
                        transformer.notify();
                    }
                }
            });
            shell.pack();
            SWTUtil.center(shell);
            shell.open();
        });

        synchronized(transformer) {
            try {
                transformer.wait();
            } catch (InterruptedException e) {
                ExceptionHandler.handle(e);
            }
        }

        FileOutputStream fileOutputStream = null;
        ZipOutputStream zipOutputStream = null;

        try {
            file.createNewFile();
            fileOutputStream = new FileOutputStream(file);
            zipOutputStream = new ZipOutputStream(fileOutputStream);
            for (Pair<String, String> pair : data) {
                LoadedFile loadedFile = Helios.getLoadedFile(pair.getValue0());
                if (loadedFile != null) {
                    String innerName = pair.getValue1();
                    byte[] bytes = loadedFile.getAllData().get(innerName);
                    if (bytes != null) {
                        if (loadedFile.getClassNode(pair.getValue1()) != null) {
                            StringBuilder buffer = new StringBuilder();
                            transformer.get().transform(loadedFile.getClassNode(pair.getValue1()), bytes, buffer);
                            zipOutputStream.putNextEntry(
                                    new ZipEntry(innerName.substring(0, innerName.length() - 6) + ".java"));
                            zipOutputStream.write(buffer.toString().getBytes(StandardCharsets.UTF_8));
                            zipOutputStream.closeEntry();
                        } else {
                            zipOutputStream.putNextEntry(
                                    new ZipEntry(pair.getValue1()));
                            zipOutputStream.write(loadedFile.getAllData().get(pair.getValue1()));
                            zipOutputStream.closeEntry();
                        }
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
