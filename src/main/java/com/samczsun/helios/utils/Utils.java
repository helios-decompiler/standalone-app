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

package com.samczsun.helios.utils;

import com.samczsun.helios.handler.ExceptionHandler;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    public static String readProcess(Process process) {
        StringBuilder result = new StringBuilder();
        result.append("--- BEGIN PROCESS DUMP ---").append("\n");
        result.append("---- STDOUT ----").append("\n");
        InputStream inputStream = process.getInputStream();
        byte[] inputStreamBytes = new byte[0];
        try {
            inputStreamBytes = IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            result.append("An error occured while reading from stdout").append("\n");
            result.append("Caused by: ").append(e.getClass()).append(" ").append(e.getMessage()).append("\n");
        } finally {
            if (inputStreamBytes.length > 0) {
                result.append(new String(inputStreamBytes, StandardCharsets.UTF_8));
            }
        }
        result.append("---- STDERR ----").append("\n");
        inputStream = process.getErrorStream();
        inputStreamBytes = new byte[0];
        try {
            inputStreamBytes = IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            result.append("An error occured while reading from stderr").append("\n");
            result.append("Caused by: ").append(e.getClass()).append(" ").append(e.getMessage()).append("\n");
        } finally {
            if (inputStreamBytes.length > 0) {
                result.append(new String(inputStreamBytes, StandardCharsets.UTF_8));
            }
        }

        result.append("---- EXIT VALUE ----").append("\n");

        int exitValue = -0xCAFEBABE;
        try {
            exitValue = process.waitFor();
        } catch (InterruptedException e) {
            result.append("An error occured while obtaining the exit value").append("\n");
            result.append("Caused by: ").append(e.getClass()).append(" ").append(e.getMessage()).append("\n");
        } finally {
            if (exitValue != -0xCAFEBABE) {
                result.append("Process finished with exit code ").append(exitValue).append("\n");
            }
        }

        return result.toString();
    }
}
