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

package com.heliosdecompiler.helios.controller.backgroundtask;

import java.util.concurrent.Future;

public class BackgroundTask implements Runnable {

    private String displayName;
    private boolean show;
    private Runnable action;
    private Runnable onCancel;
    private boolean cancelled = false;
    private Future<?> future;

    public BackgroundTask(String displayName, boolean show, Runnable action) {
        this(displayName, show, action, () -> {
        });
    }

    public BackgroundTask(String displayName, boolean show, Runnable action, Runnable onCancel) {
        this.displayName = displayName;
        this.show = show;
        this.action = action;
        this.onCancel = onCancel;
    }

    public void run() {
        this.action.run();
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isShow() {
        return show;
    }

    public void cancel() {
        if (this.future != null && !cancelled) {
            cancelled = true;
            this.future.cancel(true);
            this.onCancel.run();
        }
    }

    void init(Future<?> future) {
        this.future = future;
    }
}
