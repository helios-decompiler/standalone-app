/*
 * Copyright 2016 Sam Sun <me@samczsun.com>
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

package com.heliosdecompiler.helios.utils;

import com.heliosdecompiler.helios.handler.ExceptionHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class Utils {

    public static void save(File dest, Map<String, byte[]> data) {
        save(dest, data, string -> true);
    }

    public static void saveClasses(File dest, Map<String, byte[]> data) {
        save(dest, data, string -> string.endsWith(".class"));
    }

    public static void save(File dest, Map<String, byte[]> data, Predicate<String> accept) {
        try {
            JarOutputStream out = new JarOutputStream(new FileOutputStream(dest));
            Set<String> added = new HashSet<>();
            for (Entry<String, byte[]> entry : data.entrySet()) {
                String name = entry.getKey();
                if (added.add(name) && accept.test(name)) {
                    out.putNextEntry(new ZipEntry(name));
                    out.write(entry.getValue());
                    out.closeEntry();
                }
            }
            out.close();
        } catch (IOException e) {
            ExceptionHandler.handle(e);
        }
    }
}
