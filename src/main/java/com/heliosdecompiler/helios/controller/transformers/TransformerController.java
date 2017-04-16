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

package com.heliosdecompiler.helios.controller.transformers;

import com.google.inject.Singleton;
import com.heliosdecompiler.helios.controller.transformers.decompilers.CFRDecompilerController;
import com.heliosdecompiler.helios.controller.transformers.decompilers.FernflowerDecompilerController;
import com.heliosdecompiler.helios.controller.transformers.decompilers.KrakatauDecompilerController;
import com.heliosdecompiler.helios.controller.transformers.decompilers.ProcyonDecompilerController;
import com.heliosdecompiler.helios.controller.transformers.disassemblers.JavapDisassemblerController;
import com.heliosdecompiler.helios.controller.transformers.disassemblers.KrakatauDisassemblerController;
import com.heliosdecompiler.helios.controller.transformers.disassemblers.ProcyonDisassemblerController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.util.List;

@Singleton
public class TransformerController {

    private ObservableMap<TransformerType<?>, List<Class<?>>> transformerControllers = FXCollections.observableHashMap();

    public TransformerController() {
        registerTransformerController(TransformerType.DECOMPILER, CFRDecompilerController.class);
        registerTransformerController(TransformerType.DECOMPILER, FernflowerDecompilerController.class);
        registerTransformerController(TransformerType.DECOMPILER, KrakatauDecompilerController.class);
        registerTransformerController(TransformerType.DECOMPILER, ProcyonDecompilerController.class);
        registerTransformerController(TransformerType.DISASSEMBLER, JavapDisassemblerController.class);
        registerTransformerController(TransformerType.DISASSEMBLER, KrakatauDisassemblerController.class);
        registerTransformerController(TransformerType.DISASSEMBLER, ProcyonDisassemblerController.class);
    }

    public <T extends BaseTransformerController<?>> void registerTransformerController(TransformerType<T> transformerType, Class<? extends T> clazz) {
        transformerControllers.computeIfAbsent(transformerType, key -> FXCollections.observableArrayList()).add(clazz);
    }

    public ObservableMap<TransformerType<?>, List<Class<?>>> getTransformerControllers() {
        return transformerControllers;
    }
}
