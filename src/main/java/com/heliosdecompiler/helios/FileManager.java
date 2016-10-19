package com.heliosdecompiler.helios;

import com.heliosdecompiler.helios.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class FileManager {
    private static final Map<String, LoadedFile> FILES = Collections.synchronizedMap(new HashMap<>());

    private static volatile Map<String, LoadedFile> PATH = Collections.synchronizedMap(new HashMap<>());

    public static List<LoadedFile> getFilesForName(String fileName) {
        return FILES
                .values()
                .stream()
                .filter(loadedFile -> loadedFile.getAllData().containsKey(fileName))
                .collect(Collectors.toList());
    }

    public static void loadFile(File fileToLoad) throws IOException {
        LoadedFile loadedFile = new LoadedFile(fileToLoad);
        FILES.put(loadedFile.getName(), loadedFile);
    }

    public static List<ClassNode> loadAllClasses() {
        List<ClassNode> classNodes = new ArrayList<>();
        for (LoadedFile loadedFile : FILES.values()) {
            for (String s : loadedFile.getAllData().keySet()) {
                ClassNode loaded = loadedFile.getClassNode(s);
                if (loaded != null) {
                    classNodes.add(loaded);
                }
            }
        }
        return classNodes;
    }

    public static Collection<LoadedFile> getAllFiles() {
        return Collections.unmodifiableCollection(FILES.values());
    }

    public static LoadedFile getLoadedFile(String file) {
        return FILES.containsKey(file) ? FILES.get(file) : PATH.get(file);
    }

    public static Map<String, byte[]> getAllLoadedData() {
        Map<String, byte[]> data = new HashMap<>();
        for (LoadedFile loadedFile : FILES.values()) {
            data.putAll(loadedFile.getAllData());
        }
        for (LoadedFile loadedFile : PATH.values()) {
            data.putAll(loadedFile.getAllData());
        }
        return data;
    }

    public static synchronized Map<String, LoadedFile> getPathFiles() {
        return PATH;
    }

    public static void destroyAll() {
        FILES.clear();
    }

    public static void resetAll() {
        FILES.values().forEach(LoadedFile::reset);
    }

    public static synchronized void updatePath(Map<String, LoadedFile> newPath) {
        PATH.clear();
        PATH.putAll(newPath);
    }

    public static List<File> buildPath() {
        List<File> result = new ArrayList<>();

        for (LoadedFile lf : FILES.values()) {
            if (!lf.hasBeenModified()) {
                result.add(lf.getFile());
            } else {
                boolean successful = false;
                File tmpFile = null;
                try {
                    tmpFile = File.createTempFile("tmp", ".jar");
                    tmpFile.deleteOnExit();
                    Utils.save(tmpFile, lf.getAllData());
                    successful = true;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (!successful) {
                        result.add(lf.getFile());
                    } else {
                        result.add(tmpFile);
                    }
                }
            }
        }

        return result;
    }
}
