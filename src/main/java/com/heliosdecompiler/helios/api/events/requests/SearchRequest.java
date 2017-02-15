/*
 * Copyright 2017 Sam Sun <github-contact@samczsun.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.heliosdecompiler.helios.api.events.requests;

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
