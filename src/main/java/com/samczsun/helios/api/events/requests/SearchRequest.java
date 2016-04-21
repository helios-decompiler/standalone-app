package com.samczsun.helios.api.events.requests;

public class SearchRequest {
    private final String text;
    private final boolean matchCase;
    private final boolean wrap;
    private final boolean searchForward;
    private final boolean regex;

    public SearchRequest(String text, boolean matchCase, boolean wrap, boolean regex, boolean searchForward) {
        this.text = text;
        this.matchCase = matchCase;
        this.wrap = wrap;
        this.searchForward = searchForward;
        this.regex = regex;
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

    public boolean isRegex() {
        return this.regex;
    }
}
