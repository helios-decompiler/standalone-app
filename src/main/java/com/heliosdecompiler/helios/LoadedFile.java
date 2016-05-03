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

public class LoadedFile {
    private final File file;
    private final String name; /* The name of the file. No directory */
    private final Map<String, byte[]> files = new HashMap<>(); /* Map of ZIP-style path with extension to byte */
    private final Map<String, WrappedClassNode> classes = new HashMap<>(); /* Map of internal class name with ClassNode */
    private final Map<String, WrappedClassNode> emptyClasses = new HashMap<>(); /* Map of classnodes without code */

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
        readDataQuick();
        classes.clear();
        for (Map.Entry<String, byte[]> ent : files.entrySet()) {
            load(ent.getKey(), new ByteArrayInputStream(ent.getValue()));
        }
    }

    private void readDataQuick() {
        this.files.clear();
        try (ZipFile zipFile = new ZipFile(file)){
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                if (!zipEntry.isDirectory()) {
                    byte[] bytes = IOUtils.toByteArray(zipFile.getInputStream(zipEntry));
                    this.files.put(zipEntry.getName(), bytes);
                }
            }
        } catch (ZipException e) { //Probably not a ZIP file
            try (FileInputStream inputStream = new FileInputStream(file)) {
                this.files.put(file.getName(), IOUtils.toByteArray(inputStream));
            } catch (IOException ex) {
                ExceptionHandler.handle(ex);
            }
        } catch (IOException ex) {
            ExceptionHandler.handle(ex);
        }
    }

    private void load(String entryName, InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copy(inputStream, outputStream);
        if (entryName.endsWith(".class")) {
            try {
                ClassReader reader = new ClassReader(outputStream.toByteArray());
                ClassNode classNode = new ClassNode();
                reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                emptyClasses.put(classNode.name, new WrappedClassNode(this, classNode));
            } catch (Exception e) { //Malformed class
            }
        }
        if (!files.containsKey(entryName)) {
            files.put(entryName, outputStream.toByteArray());
        } else {
            System.out.println("Uh oh. Duplicate file...");
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

    public Map<String, WrappedClassNode> getEmptyClasses() {
        return emptyClasses;
    }
}
