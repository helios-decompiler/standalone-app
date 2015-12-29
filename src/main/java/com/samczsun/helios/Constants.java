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

package com.samczsun.helios;

import java.io.File;
import java.util.function.Supplier;

public class Constants {
    public static final String REPO_NAME = "Helios";
    public static final String REPO_VERSION = "1.0.0";
    public static final String REPO_BASE = "https://github.com/samczsun/" + REPO_NAME + "/";
    public static final String RAW_REPO_BASE = "https://raw.github.com/samczsun/" + REPO_NAME + "/";

    public static final String CHANGELOG_FILE = RAW_REPO_BASE + "master/CHANGELOG";

    public static final String SWT_VERSION = "4.5.1";
    public static final String KRAKATAU_VERSION = "dd5cf1237404fe5dc666b494948ba8dedaaa242b";
    public static final String ENJARIFY_VERSION = "bf9033b96e5c1695c4838a8edeb53195fa92a831";

    public static final int MB = 1024 * 1024;
    public static final File DATA_DIR = new File(
            System.getProperty("user.home") + File.separator + "." + Constants.REPO_NAME.toLowerCase());
    public static final File KRAKATAU_DIR = new File(DATA_DIR,
            "krakatau" + File.separator + Constants.KRAKATAU_VERSION);
    public static final File ENJARIFY_DIR = new File(DATA_DIR,
            "enjarify" + File.separator + Constants.ENJARIFY_VERSION);
    public static final File ADDONS_DIR = new File(DATA_DIR, "addons");
    public static final File SETTINGS_FILE = new File(DATA_DIR, "settings.json");
    public static final String NEWLINE = System.getProperty("line.separator");
    private static final Runtime RUNTIME = Runtime.getRuntime();
    public static final Supplier<Integer> USED_MEMORY = () -> (int) ((RUNTIME.totalMemory() - RUNTIME.freeMemory()) / MB);
    public static final Supplier<Integer> TOTAL_MEMORY = () -> (int) (RUNTIME.maxMemory() / MB);
}
