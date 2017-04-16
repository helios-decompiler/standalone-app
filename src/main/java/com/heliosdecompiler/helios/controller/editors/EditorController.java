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

package com.heliosdecompiler.helios.controller.editors;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.heliosdecompiler.helios.controller.transformers.decompilers.CFRDecompilerController;
import com.heliosdecompiler.helios.controller.transformers.decompilers.FernflowerDecompilerController;
import com.heliosdecompiler.helios.controller.transformers.decompilers.KrakatauDecompilerController;
import com.heliosdecompiler.helios.controller.transformers.decompilers.ProcyonDecompilerController;
import com.heliosdecompiler.helios.controller.transformers.disassemblers.JavapDisassemblerController;
import com.heliosdecompiler.helios.controller.transformers.disassemblers.KrakatauDisassemblerController;
import com.heliosdecompiler.helios.controller.transformers.disassemblers.ProcyonDisassemblerController;
import com.heliosdecompiler.helios.gui.view.editors.*;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Singleton
public class EditorController {

    private Map<String, EditorView> registeredEditors = new LinkedHashMap<>();

    @Inject
    public EditorController(Injector injector) {
        registerEditor(new HexEditorView());
        registerEditor(new TextView());
        registerEditor(new DecompilerView(injector.getInstance(CFRDecompilerController.class)));
        registerEditor(new DecompilerView(injector.getInstance(ProcyonDecompilerController.class)));
        registerEditor(new DecompilerView(injector.getInstance(FernflowerDecompilerController.class)));
        registerEditor(new DecompilerView(injector.getInstance(KrakatauDecompilerController.class)));
        registerEditor(new DisassemblerView(injector.getInstance(KrakatauDisassemblerController.class)));
        registerEditor(new DisassemblerView(injector.getInstance(JavapDisassemblerController.class)));
        registerEditor(new DisassemblerView(injector.getInstance(ProcyonDisassemblerController.class)));
    }

    public void registerEditor(EditorView editorView) {
        if (!registeredEditors.containsKey(editorView.getDisplayName())) {
            registeredEditors.put(editorView.getDisplayName(), editorView);
        }
    }

    public Collection<EditorView> getRegisteredEditors() {
        return registeredEditors.values();
    }
}
