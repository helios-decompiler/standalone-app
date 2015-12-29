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

import com.samczsun.helios.handler.ExceptionHandler;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class LoadedFile {
    private final File file;
    private final String name; /* The name of the file. No directory */
    private final Map<String, byte[]> files = new HashMap<>(); /* Map of ZIP-style path with extension to byte */
    private final Map<String, WrappedClassNode> classes = new HashMap<>(); /* Map of internal class name with ClassNode */

    public LoadedFile(File file) throws IOException {
        this.file = file;
        this.name = file.getName();
        reset();
    }

    public ClassNode getClassNode(String name) {
        if (name.endsWith(".class")) {
            name = name.substring(0, name.length() - 6);
        }
        if (!classes.containsKey(name)) {
            byte[] bytes = getFiles().get(name + ".class");
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

    public void reset() throws IOException {
        files.clear();
        classes.clear();
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                if (!zipEntry.isDirectory()) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    IOUtils.copy(zipFile.getInputStream(zipEntry), outputStream);
                    if (!files.containsKey(zipEntry.getName())) {
                        files.put(zipEntry.getName(), outputStream.toByteArray());
                    } else {
                        System.out.println("Uh oh. Duplicate file...");
                    }
                }
            }
        } catch (ZipException e) { //Probably not a ZIP file
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                IOUtils.copy(inputStream, outputStream);
                this.files.put(this.name, outputStream.toByteArray());
                if (this.name.endsWith(".class")) {
                    try {
                        ClassReader reader = new ClassReader(outputStream.toByteArray());
                        ClassNode classNode = new ClassNode();
                        reader.accept(classNode, ClassReader.EXPAND_FRAMES);
                        classes.put(classNode.name, new WrappedClassNode(this, classNode));
                    } catch (Exception exception) { //Malformed class
                    }
                }
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public Collection<ClassNode> getAllClassNodes() {
        return classes.values().stream().map(WrappedClassNode::getClassNode).collect(Collectors.toList());
    }

    public Map<String, byte[]> getData() {
        return getFiles();
    }

    public File getFile() {
        return file;
    }

    public Map<String, byte[]> getFiles() {
        return files;
    }

    public String getName() {
        return name;
    }
}
