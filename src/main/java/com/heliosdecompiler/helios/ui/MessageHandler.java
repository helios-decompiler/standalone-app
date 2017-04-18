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

package com.heliosdecompiler.helios.ui;

import com.heliosdecompiler.helios.gui.model.CommonError;
import com.heliosdecompiler.helios.gui.model.Message;
import com.heliosdecompiler.helios.ui.views.file.FileChooserView;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface MessageHandler {
    CompletableFuture<Void> handleLongMessage(Message shortMessage, String longMessage);

    void handleException(Message message, Throwable exception);

    default void handleError(CommonError.FormattedMessage message) {
        handleError(message, null);
    }

    void handleError(CommonError.FormattedMessage message, Runnable after);

    default void handleMessage(CommonError.FormattedMessage message) {
        handleMessage(message, null);
    }

    void handleMessage(CommonError.FormattedMessage message, Runnable after);

    void prompt(CommonError.FormattedMessage format, Consumer<Boolean> result);

    FileChooserView chooseFile();
}
