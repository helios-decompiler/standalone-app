package com.heliosdecompiler.helios.controller.transformers.disassemblers;

import com.heliosdecompiler.transformerapi.StandardTransformers;
import com.strobel.decompiler.DecompilerSettings;

public class ProcyonDisassemblerController extends DisassemblerController<DecompilerSettings> {
    public ProcyonDisassemblerController() {
        super("Procyon Disassembler", "procyon-disassembler", StandardTransformers.Disassemblers.PROCYON);
    }

    @Override
    protected void registerSettings() {

    }

    @Override
    protected DecompilerSettings defaultSettings() {
        return getDisassembler().defaultSettings();
    }
}
