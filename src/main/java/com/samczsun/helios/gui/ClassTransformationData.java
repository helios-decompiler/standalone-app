package com.samczsun.helios.gui;

import org.eclipse.swt.custom.CTabItem;

public class ClassTransformationData {
    private CTabItem transformerTab;

    private ClickableSyntaxTextArea area;

    public boolean isInitialized() {
        return this.transformerTab != null;
    }

    public CTabItem getTransformerTab() {
        return transformerTab;
    }

    public void setTransformerTab(CTabItem transformerTab) {
        this.transformerTab = transformerTab;
    }

    public ClickableSyntaxTextArea getArea() {
        return area;
    }

    public void setArea(ClickableSyntaxTextArea area) {
        this.area = area;
    }
}
