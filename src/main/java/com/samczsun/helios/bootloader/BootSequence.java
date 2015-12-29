/*
 * Copyright 2015 Sam Sun <me@samczsun.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.samczsun.helios.bootloader;

public enum BootSequence {
    STARTING("Starting"),
    CHECKING_LIBRARIES("Checking libraries"),
    CHECKING_ENJARIFY("Checking Enjarify"),
    CLEANING_ENJARIFY("Cleaning Enjarify"),
    MOVING_ENJARIFY("Moving Enjarify"),
    CHECKING_KRAKATAU("Checking Krakatau"),
    CLEANING_KRAKATAU("Cleaning Krakatau"),
    MOVING_KRAKATAU("Moving Krakatau"),
    LOADING_SETTINGS("Loading Settings"),
    LOADING_ADDONS("Loading Addons"),
    SETTING_UP_GUI("Setting up GUI"),
    COMPLETE("Complete");

    private final String message;

    BootSequence(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}
