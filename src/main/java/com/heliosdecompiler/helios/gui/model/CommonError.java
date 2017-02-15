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

package com.heliosdecompiler.helios.gui.model;

public enum CommonError {
    DOES_NOT_EXIST("does-not-exist", 1),
    NO_READ_PERMISSIONS("no-read-permissions", 1),
    MISMATCHED_CRC("mismatched-crc", 4),
    WINDOWS_ONLY("windows-only", 0),
    CONTEXT_MENU_FAILED("context-menu-failed", 1),
    RELAUNCH_ADMIN_FAILED("relaunch-admin-failed", 1),
    CONTEXT_MENU_SUCCESSFUL("context-menu-successful", 0),
    RESET_WORKSPACE("reset-workspace", 0),
    COULD_NOTE_LOCATE_HELIOS("could-not-locate-helios", 0),
    COULD_NOT_LOCATE_JAVA("could-not-locate-java", 1);

    private String messageKey;
    private int args;

    CommonError(String messageKey, int args) {
        this.messageKey = messageKey;
        this.args = args;
    }

    public FormattedMessage format(String... args) {
        if (args.length != this.args) {
            throw new IllegalArgumentException("Not enough args: " + args.length + " vs " + this.args);
        }

        return new FormattedMessage(this, args);
    }

    public String getMessageKey() {
        return messageKey;
    }

    public static class FormattedMessage {
        private CommonError error;
        private String[] args;

        public FormattedMessage(CommonError error, String[] args) {
            this.error = error;
            this.args = args;
        }

        public CommonError getError() {
            return error;
        }

        public String[] getArgs() {
            return args;
        }
    }
}
