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

import java.io.File;
import java.util.function.Supplier;

public class Constants {
    public static final boolean IS_DEV = System.getProperty("com.heliosdecompiler.version") == null;
    public static final String REPO_NAME = "Helios";
    public static final String REPO_VERSION =
            (System.getProperty("com.heliosdecompiler.version") != null ? System.getProperty("com.heliosdecompiler.version") : "Dev")
                    + (System.getProperty("com.heliosdecompiler.buildNumber") != null ? " (Build " + System.getProperty("com.heliosdecompiler.buildNumber") + ")" : "");

    public static final String KRAKATAU_VERSION = "3eff49fe480efeca8a728936f6452ec6853cdc88";
    public static final String ENJARIFY_VERSION = "82d72ee92730e858b6ec3615d6dc74c9331e56e8";

    public static final int MB = 1024 * 1024;
    public static final File DATA_DIR = new File(
            System.getProperty("user.home") + File.separator + "." + Constants.REPO_NAME.toLowerCase());
    public static final File KRAKATAU_DIR = !IS_DEV ? new File(DATA_DIR,
            "Krakatau" + File.separator + Constants.KRAKATAU_VERSION) : new File("." + File.separator + ".." + File.separator + "Krakatau");
    public static final File ENJARIFY_DIR = new File(DATA_DIR,
            "enjarify" + File.separator + Constants.ENJARIFY_VERSION);
    public static final File ADDONS_DIR = new File(DATA_DIR, "addons");
    public static final File SETTINGS_FILE = new File(DATA_DIR, "settings.json");
    public static final File SETTINGS_FILE_XML = new File(DATA_DIR, "settings.xml");
    public static final String NEWLINE = System.lineSeparator();
    private static final Runtime RUNTIME = Runtime.getRuntime();
    public static final Supplier<Integer> USED_MEMORY = () -> (int) ((RUNTIME.totalMemory() - RUNTIME.freeMemory()) / MB);
    public static final Supplier<Integer> TOTAL_MEMORY = () -> (int) (RUNTIME.maxMemory() / MB);
}
