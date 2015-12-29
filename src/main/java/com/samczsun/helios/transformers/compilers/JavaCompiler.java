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

package com.samczsun.helios.transformers.compilers;

import com.samczsun.helios.Helios;
import com.samczsun.helios.Settings;
import com.samczsun.helios.handler.ExceptionHandler;
import com.samczsun.helios.utils.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class JavaCompiler extends Compiler {

    JavaCompiler() {
        super("java", "Java");
    }

    @Override
    public byte[] compile(String name, String contents) {
        if (Helios.ensureJavacSet()) {
            try {
                File tempdir1 = Files.createTempDirectory("javac1").toFile();
                File tempdir2 = Files.createTempDirectory("javac2").toFile();
                File java = new File(tempdir1, name + ".java");
                File clazz = new File(tempdir2, name + ".class");
                File classpath = new File(tempdir1, "classpath.jar");

                try {
                    FileUtils.write(java, contents, "UTF-8", false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Utils.save(classpath, Helios.getAllLoadedData());

                boolean cont = true;
                String log = "";
                ProcessBuilder pb;

                if (Settings.PATH.get().asString().isEmpty()) {
                    pb = new ProcessBuilder(Settings.JAVAC_LOCATION.get().asString(), "-d", tempdir2.getAbsolutePath(),
                            "-classpath", classpath.getAbsolutePath(), java.getAbsolutePath());
                } else {
                    pb = new ProcessBuilder(Settings.JAVAC_LOCATION.get().asString(), "-d", tempdir2.getAbsolutePath(),
                            "-classpath", classpath.getAbsolutePath() + ";" + Settings.PATH.get().asString(),
                            java.getAbsolutePath());
                }

                Process process = Helios.launchProcess(pb);

                log += Utils.readProcess(process);
                System.out.println(log);

                if (!clazz.exists()) throw new Exception(log);

                classpath.delete();

                if (cont) {
                    try {
                        return org.apache.commons.io.FileUtils.readFileToByteArray(clazz);
                    } catch (IOException e) {
                        ExceptionHandler.handle(e);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
