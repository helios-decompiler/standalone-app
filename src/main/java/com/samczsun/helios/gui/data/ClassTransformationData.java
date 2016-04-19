package com.samczsun.helios.gui.data;

import com.samczsun.helios.gui.ClickableSyntaxTextArea;
import org.eclipse.swt.custom.CTabItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

public class ClassTransformationData {
    private CTabItem transformerTab;

    private ClickableSyntaxTextArea area;

    public List<Future<?>> futures = Collections.synchronizedList(new ArrayList<>());

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
