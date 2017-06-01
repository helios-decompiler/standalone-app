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

package com.heliosdecompiler.helios.transformers.assemblers;

public class SmaliAssembler {

//    public Either<Result, byte[]> assemble(String name, String contents) {
//        File tempDir = null;
//        File tempSmaliFolder = null;
//        File tempSmali = null;
//        File tempDex = null;
//        File tempJar = null;
//        File tempJarFolder = null;
//        try {
//            tempDir = Files.createTempDirectory("smali").toFile();
//            tempSmaliFolder = new File(tempDir, "smalifolder");
//            tempSmaliFolder.mkdir();
//
//            tempSmali = new File(tempDir, "temp.smali");
//            tempDex = new File(tempDir, "temp.dex");
//            tempJar = new File(tempDir, "temp.jar");
//            tempJarFolder = new File(tempDir, "temp-jar");
//
//            FileUtils.write(tempSmali, contents, "UTF-8", false);
////            try {
////                SmaliOptions smaliOptions = new SmaliOptions();
////                smaliOptions.outputDexFile = tempDex.getAbsolutePath();
////                org.jf.smali.main.run(smaliOptions, tempSmaliFolder.getAbsolutePath());
////            } catch (Exception e) {
////                ExceptionHandler.handle(e);
////            }
//
//
////            if (Settings.APK_CONVERSION.get().asString().equals(Converter.ENJARIFY.getId())) {
////                Converter.ENJARIFY.convert(tempDex, tempJar);
////            } else if (Settings.APK_CONVERSION.get().asString().equals(Converter.DEX2JAR.getId())) {
////                Converter.DEX2JAR.convert(tempDex, tempJar);
////            }
//            //todo yeah
////            ZipUtil.unpack(tempJar, tempJarFolder);
//
//            File outputClass = null;
//            boolean found = false;
//            File current = tempJarFolder;
//            try {
//                while (!found) {
//                    File f = current.listFiles()[0];
//                    if (f.isDirectory()) current = f;
//                    else {
//                        outputClass = f;
//                        found = true;
//                    }
//
//                }
//
//                return Either.right(org.apache.commons.io.FileUtils.readFileToByteArray(outputClass));
//            } catch (java.lang.NullPointerException e) {
//
//            }
//        } catch (Exception e) {
////            ExceptionHandler.handle(e);
//        } finally {
//            FileUtils.deleteQuietly(tempDir);
//            FileUtils.deleteQuietly(tempSmaliFolder);
//            FileUtils.deleteQuietly(tempSmali);
//            FileUtils.deleteQuietly(tempDex);
//            FileUtils.deleteQuietly(tempJar);
//            FileUtils.deleteQuietly(tempJarFolder);
//        }
//        // todo fixme
//        return Either.left(null);
//    }
}
