package com.heliosdecompiler.helios.transformers;

import com.heliosdecompiler.helios.gui.ClassManager;
import com.heliosdecompiler.helios.gui.data.ClassData;

import javax.swing.JComponent;

public interface Viewable {
    boolean isApplicable(String className);

    JComponent open(ClassManager cm, ClassData data);
}
