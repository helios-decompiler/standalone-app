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

package com.heliosdecompiler.helios;

import com.heliosdecompiler.helios.handler.ExceptionHandler;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class LoadedFile {
    private final File file;
    private final String name; /* The name of the file. No directory */

    private Map<String, byte[]> files; /* Map of ZIP-style path with extension to byte */
    private Map<String, ClassNode> classes; /* Map of internal class name with ClassNode */
    private Map<String, ClassNode> emptyClasses; /* Map of classnodes without code */

    private boolean isPath;

    public LoadedFile(File file) {
        this(file, false);
    }

    public LoadedFile(File file, boolean path) {
        this.file = file;
        this.name = file.getName();
        this.isPath = path;
        reset();
    }

    /*
     * Reset everything in this LoadedFile (eg all the data)
     */
    public void reset() {
        readDataQuick();
        if (files.size() > 0) {
            // Read all data of potential class files
            Helios.submitBackgroundTask(() -> {
                Map<String, ClassNode> emptyClasses = new HashMap<>();
                files.entrySet().stream().filter(ent -> ent.getKey().endsWith(".class")).forEach(ent -> {
                    try {
                        ClassReader classReader = new ClassReader(new ByteArrayInputStream(ent.getValue()));
                        ClassNode classNode = new ClassNode();
                        classReader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

                        // Store by ClassNode name
                        emptyClasses.put(classNode.name, classNode);
                        // Also store by path
                        emptyClasses.put(ent.getKey(), classNode);
                    } catch (Exception ignored) { //Malformed class
                    }
                });
                // Lock the map
                this.emptyClasses = Collections.unmodifiableMap(emptyClasses);
                if (!this.isPath) {
                    // Read the code as well
                    // fixme If path jars are guarenteed to not require code then maybe we can merge emptyClasses and classes
                    // fixme this seems to hog cpu cycles or something
                    Helios.submitBackgroundTask(() -> {
                        Map<String, ClassNode> classes = new HashMap<>();
                        files.entrySet().stream().filter(ent -> ent.getKey().endsWith(".class")).forEach(ent -> {
                            try {
                                ClassReader classReader = new ClassReader(new ByteArrayInputStream(ent.getValue()));
                                ClassNode classNode = new ClassNode();
                                classReader.accept(classNode, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

                                // Store by ClassNode name
                                classes.put(classNode.name, classNode);
                                // Also store by path
                                classes.put(ent.getKey(), classNode);
                            } catch (Exception ignored) { //Malformed class
                            }
                        });
                        // Lock the map
                        this.classes = Collections.unmodifiableMap(classes);
                    });
                }
            });
        }
    }

    public ClassNode getClassNode(String name) {
        return this.classes.get(name);
    }

    /*
     * The goal is to read in all the data as quick as possible
     * and prevent excessive locking of the original file
     */
    private void readDataQuick() {
        // This code should run as fast as possible
        byte[] data = null;
        try (FileInputStream inputStream = new FileInputStream(file)) {
            data = IOUtils.toByteArray(inputStream);
        } catch (IOException ex) {
            ExceptionHandler.handle(new RuntimeException("Error while reading file", ex));
            return;
        }
        // And now we can take our time. The file has been unlocked (unless something went seriously wrong)
        this.files = new HashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                this.files.put(entry.getName(), IOUtils.toByteArray(zipInputStream));
            }
        } catch (Throwable ex) {
            // This should never happen
            ExceptionHandler.handle(new RuntimeException("Error while parsing file (!)", ex));
            return;
        }
        // If files is still empty, then it's not a zip file (or something weird happened)
        if (this.files.size() == 0) {
            this.files.put(file.getName(), data);
        }
        // Lock the map
        this.files = Collections.unmodifiableMap(this.files);
    }

    public Collection<ClassNode> getAllClassNodes() {
        return classes.values();
    }

    public Map<String, ClassNode> getEmptyClasses() {
        return emptyClasses;
    }

    public Map<String, byte[]> getAllData() {
        return files;
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }
}
