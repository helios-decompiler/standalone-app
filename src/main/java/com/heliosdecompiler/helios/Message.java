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

package com.heliosdecompiler.helios;

import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public enum Message {
    // Startup
    STARTUP_PREPARING_ENVIRONMENT("startup.preparing-environment"),
    STARTUP_LOADING_GRAPHICS("startup.loading-graphics"),
    STARTUP_HANDLING_COMMANDLINE("startup.handling-commandline"),
    STARTUP_LOADING_PATH("startup.loading-path"),
    STARTUP_DONE("startup.done"),

    STARTUP_FAILED_TO_LOAD_CONFIGURATION("startup.failed-to-load-configuration"),
    STARTUP_BAD_CHARSET("startup.bad-charset"),
    STARTUP_UNEXPECTED_ERROR("startup.unexpected-error"),

    // Updater
    UPDATER_UPDATE_FOUND("updater.update-found", 2),
    UPDATER_SELECT_SAVE_LOCATION("updater.select-save-location"),
    UPDATER_DOWNLOADING_HELIOS("updater.downloading-helios"),
    UPDATER_UPDATE_SUCCESSFUL("updater.update-successful"),
    UPDATER_UPDATE_TASK_NAME("updater.update-task-name"),

    // Filetype
    FILETYPE_JAVA_ARCHIVE("filetype.java-archive"),
    FILETYPE_JAVA_ARCHIVE_CLASS_FILE("filetype.java-archive-and-class-file"),
    FILETYPE_ANY("filetype.any"),

    // Generic
    GENERIC_OPEN("generic.open"),
    GENERIC_SELECT_FILE("generic.select-file", 1),
    GENERIC_CHOOSE_EXPORT_LOCATION_JAR("generic.choose-export-location-jar"),
    GENERIC_WINDOWS_ONLY("generic.windows-only"),
    GENERIC_DOES_NOT_EXIST("generic.does-not-exist", 1),
    GENERIC_ASSEMBLED("generic.assembled"),
    GENERIC_EXPORTED("generic.exported"),

    // Tasks
    TASK_ASSEMBLE_FILE("task.assemble-file", 1),
    TASK_DISASSEMBLE_FILE("task.disassemble-file", 2),
    TASK_DECOMPILE_FILE("task.decompile-file", 2),
    TASK_LAUNCH_PROCESS("task.launch-process", 1),
    TASK_LOADING_FILE("task.loading-file", 1),
    TASK_RELOADING_FILES("task.reloading-files"),
    TASK_SAVING_FILE("task.saving-file", 1),
    TASK_RELOADING_PATH("task.reloading-path"),

    // Other messages
    ERROR_UNEXPECTED_ERROR("error.unexpected-error", 1),
    ERROR_IOEXCEPTION_OCCURRED("error.ioexception-occurred"),
    ERROR_UNKNOWN_ERROR("error.unknown-error"),
    ERROR_FAILED_TO_ASSEMBLE_KRAKATAU("error.failed-to-assemble-krakatau"),

    // Prompts
    PROMPT_RESET_WORKSPACE("prompt.reset-workspace"),

    // What to do with these? Put in own category or spread around in others?
    CONTEXT_MENU_FAILED("context-menu-failed", 1),
    CONTEXT_MENU_SUCCESSFUL("context-menu-successful", 0),
    COULD_NOTE_LOCATE_HELIOS("could-not-locate-helios", 0),
    COULD_NOT_LOCATE_JAVA("could-not-locate-java", 1),
    ;

    private static ResourceBundle bundle = ResourceBundle.getBundle("HeliosStandaloneLang");

    private String messageKey;
    private int args;

    Message(String messageKey) {
        this(messageKey, 0);
    }

    Message(String messageKey, int args) {
        this.messageKey = messageKey;
        this.args = args;
    }

    public FormattedMessage format(String... args) {
        if (args.length != this.args) {
            throw new IllegalArgumentException("Not enough args: " + args.length + " vs " + this.args);
        }

        return new FormattedMessage(this, args);
    }

    public String getText() {
        return format().getText();
    }

    public String getMessageKey() {
        return messageKey;
    }


    public static class FormattedMessage {
        private Message error;
        private String[] args;

        public FormattedMessage(Message error, String[] args) {
            this.error = error;
            this.args = args;
        }

        public Message getError() {
            return error;
        }

        public String[] getArgs() {
            return args;
        }

        public String getText() {
            try {
                return String.format(bundle.getString(getError().getMessageKey()), (Object[]) getArgs());
            } catch (MissingResourceException e) {
                return getError().getMessageKey() + " " + Arrays.toString(getArgs());
            }
        }
    }
}
