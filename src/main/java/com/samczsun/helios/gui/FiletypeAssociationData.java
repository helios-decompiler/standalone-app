package com.samczsun.helios.gui;

import com.samczsun.helios.transformers.Transformer;

public class FiletypeAssociationData {
    private String extension;
    private String transformerId;

    public FiletypeAssociationData(String extension, String transformerId) {
        this.extension = extension;
        this.transformerId = transformerId;
    }

    public String getExtension() {
        return this.extension;
    }

    public String getTransformerId() {
        return this.transformerId;
    }

    public Transformer getTransformer() {
        return Transformer.getById(transformerId);
    }

    public void setTransformer(Transformer transformer) {
        this.transformerId = transformer.getId();
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String formatName() {
        return extension + " - " + getTransformer().getName();
    }
}
