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
 *
 * Copyright 2014 Ryszard Wiśniewski <brut.alll@gmail.com>
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package com.samczsun.helios.utils;

import brut.androlib.AndrolibException;
import brut.androlib.ApkDecoder;
import brut.androlib.err.CantFindFrameworkResException;
import brut.androlib.err.InFileNotFoundException;
import brut.androlib.err.OutDirExistsException;
import brut.directory.DirectoryException;
import com.samczsun.helios.handler.ExceptionHandler;
import org.apache.commons.io.FileUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class APKTool {
    public static void decodeResources(File input, File output) {
        try {
            Path temporaryDirectory = Files.createTempDirectory("apkresources");
            File directory = temporaryDirectory.toFile();
            Files.delete(temporaryDirectory);
            cmdDecode(input, directory);
            File original = new File(directory, "original");
            FileUtils.deleteDirectory(original);
            File apktool = new File(directory, "apktool.yml");
            apktool.delete();
            ZipUtil.pack(directory, output);
            FileUtils.deleteDirectory(directory);
        } catch (Exception e) {
            ExceptionHandler.handle(e);
        }
    }

    private static void cmdDecode(File input, File outputDir) throws AndrolibException {
        final ApkDecoder decoder = new ApkDecoder();
        decoder.setOutDir(outputDir);
        decoder.setApkFile(input);
        decoder.setDecodeSources((short) 0);
        decoder.setForceDelete(true);
        try {
            decoder.decode();
        } catch (OutDirExistsException ex2) {
            System.err.println(
                    "Destination directory (" + outputDir.getAbsolutePath() + ") already exists. Use -f switch if you want to overwrite it.");
        } catch (InFileNotFoundException ex3) {
            System.err.println("Input file (" + input.getAbsolutePath() + ") was not found or was not readable.");
        } catch (CantFindFrameworkResException ex) {
            System.err.println("Can't find framework resources for package of id: " + String.valueOf(
                    ex.getPkgId()) + ". You must install proper framework files, see project website for more info.");
        } catch (IOException ex4) {
            System.err.println("Could not modify file. Please ensure you have permission.");
        } catch (DirectoryException ex5) {
            System.err.println("Could not modify internal dex files. Please ensure you have permission.");
        }
    }
}
