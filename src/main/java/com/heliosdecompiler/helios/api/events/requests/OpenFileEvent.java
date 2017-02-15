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

import com.heliosdecompiler.helios.api.events.Cancellable;
import com.heliosdecompiler.helios.api.events.Event;

import java.io.File;

public class OpenFileEvent extends Event implements Cancellable {

    private boolean cancelled = false;
    private File file;

    public OpenFileEvent(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
