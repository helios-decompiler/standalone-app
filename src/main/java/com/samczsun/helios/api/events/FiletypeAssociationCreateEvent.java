package com.samczsun.helios.api.events;

import com.samczsun.helios.transformers.Transformer;

public class FiletypeAssociationCreateEvent {
    private String extension;
    private Transformer transformer;

    public FiletypeAssociationCreateEvent(String extension, Transformer transformer) {
        this.extension = extension;
        this.transformer = transformer;
    }

    public String getExtension() {
        return this.extension;
    }

    public Transformer getTransformer() {
        return this.transformer;
    }
}
