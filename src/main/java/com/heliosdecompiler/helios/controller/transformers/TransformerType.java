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

import com.heliosdecompiler.helios.controller.transformers.decompilers.DecompilerController;
import com.heliosdecompiler.helios.controller.transformers.disassemblers.DisassemblerController;

import java.util.Objects;

public class TransformerType<T extends BaseTransformerController<?>> {
    public static final TransformerType<DecompilerController<?>> DECOMPILER = new TransformerType<>("decompiler", "Decompiler");
    public static final TransformerType<DisassemblerController<?>> DISASSEMBLER = new TransformerType<>("disassembler", "Disassembler");

    public String getDisplayName() {
        return displayName;
    }

    public String getInternalName() {
        return internalName;
    }

    private final String internalName;
    private final String displayName;

    public TransformerType(String internalName, String displayName) {
        this.internalName = internalName;
        this.displayName = displayName;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransformerType)) return false;
        TransformerType that = (TransformerType) o;
        return Objects.equals(displayName, that.displayName);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(displayName);
    }
}
