package com.samczsun.helios.transformers;

import com.samczsun.helios.gui.ClassManager;
import com.samczsun.helios.gui.data.ClassData;

import javax.swing.JComponent;

public interface Viewable {
    boolean isApplicable(String className);

    JComponent open(ClassManager cm, ClassData data);
}
