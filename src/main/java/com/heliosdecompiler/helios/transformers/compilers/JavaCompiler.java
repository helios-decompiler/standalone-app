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

package com.heliosdecompiler.helios.transformers.compilers;

import com.sun.tools.javac.main.Main;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;

public class JavaCompiler extends Compiler {

    JavaCompiler() {
        super("java", "Java");
    }

    @Override
    public byte[] compile(String name, String contents) {
        File tmpdir = null;
        File javaFile = null;
        File classFile = null;
        File classpathFile = null;
        try {
            tmpdir = Files.createTempDirectory("javac").toFile();
            javaFile = new File(tmpdir, name + ".java");
            classFile = new File(tmpdir, name + ".class");
            classpathFile = new File(tmpdir, "classpath.jar");
            FileUtils.write(javaFile, contents, "UTF-8", false);
//            Utils.save(classpathFile, FileManager.getAllLoadedData());

            StringWriter stringWriter = new StringWriter();

            com.sun.tools.javac.main.Main compiler =
                    new com.sun.tools.javac.main.Main("javac", new PrintWriter(stringWriter));
            int responseCode = compiler.compile(new String[]{
                    "-d",
                    tmpdir.getAbsolutePath(),
                    "-classpath",
                    /*buildPath(Arrays.asList(classFile)),*/
                    javaFile.getAbsolutePath()
            }).exitCode;

            if (responseCode != Main.Result.OK.exitCode) {
                System.out.println(stringWriter.toString());
//                Shell shell = SWTUtil.generateLongMessage("Error", stringWriter.toString());
//                shell.getDisplay().syncExec(() -> {
//                    SWTUtil.center(shell);
//                    shell.open();
//                });
            } else {
                return FileUtils.readFileToByteArray(classFile);
            }
        } catch (Exception e) {
//            ExceptionHandler.handle(e);
        } finally {
            FileUtils.deleteQuietly(javaFile);
            FileUtils.deleteQuietly(classFile);
            FileUtils.deleteQuietly(classpathFile);
            FileUtils.deleteQuietly(tmpdir);
        }
        return null;
    }
}
