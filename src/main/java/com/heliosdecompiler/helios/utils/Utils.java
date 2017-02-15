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

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;

public class Utils {
    public static <T> T find(T needle, Collection<T> haystack) {
        for (Iterator<T> iter = haystack.iterator(); iter.hasNext(); ) {
            T next = iter.next();
            if (next.equals(needle)) {
                return next;
            }
        }
        return null;
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
