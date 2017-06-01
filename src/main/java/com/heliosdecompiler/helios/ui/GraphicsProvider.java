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

import com.google.inject.Injector;
import com.heliosdecompiler.helios.Message;

public abstract class GraphicsProvider {

    /**
     * This call should block until the splash screen is visible on the screen, at which point it should return.
     * This is guaranteed to be the first method to be called
     */
    public abstract void startSplash();

    /**
     * Update the message shown on the splash screen
     */
    public final void updateSplash(Message message) {
        setSplashMessage(message.format().getText());
    }

    protected abstract void setSplashMessage(String message);

    /**
     * Prepare the application. This method should block until the application is fully ready to be shown (but not yet shown)
     */
    public abstract void prepare(Injector injector);

    /**
     * Actually show the application. This method should block until the application is shown on the screen
     */
    public abstract void start();

    public abstract Class<? extends MessageHandler> getMessageHandlerImpl();

    public void handleStartupError(Message message, Throwable error) {
        handleStartupError0(message.getText(), error);
    }

    protected abstract void handleStartupError0(String message, Throwable error);
}
