package com.samczsun.helios.api.events.requests;

public class SearchRequest {
    private final String text;
    private final boolean matchCase;
    private final boolean wrap;
    private final boolean searchForward;

    public SearchRequest(String text, boolean matchCase, boolean wrap, boolean searchForward) {
        this.text = text;
        this.matchCase = matchCase;
        this.wrap = wrap;
        this.searchForward = searchForward;
    }

    public String getText() {
        return this.text;
    }

    public boolean isMatchCase() {
        return this.matchCase;
    }

    public boolean isWrap() {
        return this.wrap;
    }

    public boolean isSearchForward() {
        return this.searchForward;
    }
}
