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

package com.heliosdecompiler.helios.handler.addons;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.heliosdecompiler.helios.Helios;
import com.heliosdecompiler.helios.api.Addon;
import com.heliosdecompiler.helios.handler.ExceptionHandler;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class JarLauncher extends AddonHandler {
    @Override
    public boolean accept(File file) {
        return file.getName().endsWith(".jar");
    }

    @Override
    public void run(File file) {
        JarFile jarFile = null;
        InputStream inputStream = null;
        try {
            System.out.println("Loading addon: " + file.getAbsolutePath());
            jarFile = new JarFile(file);
            ZipEntry entry = jarFile.getEntry("addon.json");
            if (entry != null) {
                inputStream = jarFile.getInputStream(entry);
                JsonObject jsonObject = Json.parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).asObject();
                String main = jsonObject.get("main").asString();
                URL[] url = new URL[]{file.toURI().toURL()};
                ClassLoader classLoader = AccessController.doPrivileged(
                        (PrivilegedAction<ClassLoader>) () -> new URLClassLoader(url, Helios.class.getClassLoader()));
                Class<?> clazz = Class.forName(main, true, classLoader);
                if (Addon.class.isAssignableFrom(clazz)) {
                    Addon addon = Addon.class.cast(clazz.newInstance());
                    registerAddon(addon.getName(), addon);
                } else {
                    throw new IllegalArgumentException("Addon main does not extend Addon");
                }
            } else {
                throw new IllegalArgumentException("No addon.json found");
            }
        } catch (Exception e) {
            ExceptionHandler.handle(e);
        } finally {
            IOUtils.closeQuietly(jarFile);
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    public String getId() {
        return "jar";
    }
}
