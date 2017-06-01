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

package com.heliosdecompiler.helios.controller.ui.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.heliosdecompiler.helios.Message;
import com.heliosdecompiler.helios.controller.ui.UserInterfaceController;
import com.heliosdecompiler.helios.ui.MessageHandler;

@Singleton
public class UnsupportedUIController implements UserInterfaceController {

    @Inject
    private MessageHandler messageHandler;

    @Override
    public void registerInContextMenu() {
        messageHandler.handleMessage(Message.GENERIC_WINDOWS_ONLY.format());
    }
}
