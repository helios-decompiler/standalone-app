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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class LoadedFile {
    private final File file;
    private final String name; /* The name of the file. No directory */

    private final Map<String, byte[]> files = new HashMap<>(); /* Map of ZIP-style path with extension to byte */
    private final Map<String, WrappedClassNode> classes = new HashMap<>(); /* Map of internal class name with ClassNode */
    private final Map<String, WrappedClassNode> emptyClasses = new HashMap<>(); /* Map of classnodes without code */

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

    public ClassNode getClassNode(String name) {
        if (name.endsWith(".class")) {
            name = name.substring(0, name.length() - 6);
        }
        if (!classes.containsKey(name)) {
            byte[] bytes = getAllData().get(name + ".class");
            if (bytes != null) {
                try {
                    ClassReader reader = new ClassReader(bytes);
                    ClassNode classNode = new ClassNode();
                    reader.accept(classNode, ClassReader.EXPAND_FRAMES);
                    classes.put(name, new WrappedClassNode(this, classNode));
                } catch (Exception t) {
                    ExceptionHandler.handle(t);
                }
            }
        }
        return classes.get(name) != null ? classes.get(name).getClassNode() : null;
    }

    public boolean remove(ClassNode classNode) {
        return classes.remove(classNode.name) != null;
    }

    public void reset() {
        readDataQuick();
        if (files.size() > 0) {
            Helios.submitBackgroundTask(() -> {
                classes.clear();
                for (Map.Entry<String, byte[]> ent : files.entrySet()) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    outputStream.write(ent.getValue(), 0, ent.getValue().length);
                    load(ent.getKey(), outputStream);
                }
            });
        }
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
        this.files.clear();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry entry = null;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                this.files.put(entry.getName(), IOUtils.toByteArray(zipInputStream));
            }
        } catch (Throwable ex) {
            // This should never happen
            ExceptionHandler.handle(new RuntimeException("Error while parsing file (!)", ex));
        }
        // If files is still empty, then it's not a zip file
        if (this.files.size() == 0) {
            this.files.put(file.getName(), data);
        }
    }

    private void load(String entryName, ByteArrayOutputStream outputStream) {
        if (entryName.endsWith(".class")) {
            try {
                ClassReader reader = new ClassReader(outputStream.toByteArray());
                ClassNode classNode = new ClassNode();
                reader.accept(classNode, ClassReader.EXPAND_FRAMES);
                emptyClasses.put(classNode.name, new WrappedClassNode(this, classNode));
                classes.put(classNode.name, emptyClasses.get(classNode.name));
            } catch (Exception e) { //Malformed class
            }
        }
    }

    public Collection<ClassNode> getAllClassNodes() {
        return classes.values().stream().map(WrappedClassNode::getClassNode).collect(Collectors.toList());
    }
    
    public Map<String, WrappedClassNode> getEmptyClasses() {
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
