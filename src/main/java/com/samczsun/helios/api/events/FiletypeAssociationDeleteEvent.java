package com.samczsun.helios.api.events;

import com.samczsun.helios.transformers.Transformer;
import org.eclipse.swt.widgets.MenuItem;

public class FiletypeAssociationDeleteEvent {
    private String extension;

    public FiletypeAssociationDeleteEvent(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return this.extension;
    }
}
