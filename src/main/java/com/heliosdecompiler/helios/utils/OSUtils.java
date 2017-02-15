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

package com.heliosdecompiler.helios.utils;

public class OSUtils {
    // http://www.java-gaming.org/index.php/topic,14110
    public static OS getOS() {
        String osName = System.getProperty("os.name");
        if (osName != null) {
            osName = osName.toLowerCase();
            if (osName.contains("windows")) {
                return OS.WINDOWS;
            } else if (osName.contains("mac")) {
                return OS.MAC;
            } else if (osName.contains("linux")) {
                return OS.LINUX;
            } else if (osName.contains("sunos")) {
                return OS.SUNOS;
            } else if (osName.contains("freebsd")) {
                return OS.FREEBSD;
            }
        }
        return OS.UNKNOWN;
    }

    public enum OS {
        WINDOWS,
        MAC,
        LINUX,
        SUNOS,
        FREEBSD,
        UNKNOWN
    }
}
