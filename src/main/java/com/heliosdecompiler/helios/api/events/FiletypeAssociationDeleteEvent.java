package com.heliosdecompiler.helios.api.events;

public class FiletypeAssociationDeleteEvent {
    private String extension;

    public FiletypeAssociationDeleteEvent(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return this.extension;
    }
}
