package com.heliosdecompiler.helios.api.events.requests;

public class SearchBarRequest {
    private final boolean open;

    private boolean successful;

    public SearchBarRequest(boolean open) {
        this.open = open;
    }

    public boolean shouldOpen() {
        return this.open;
    }

    public boolean shouldClose() {
        return !this.open;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public boolean wasSuccessful() {
        return this.successful;
    }
}
