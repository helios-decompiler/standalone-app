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

import com.heliosdecompiler.helios.Constants;
import com.heliosdecompiler.helios.controller.ProcessController;
import com.heliosdecompiler.helios.utils.Either;
import com.heliosdecompiler.helios.utils.Result;
import com.heliosdecompiler.helios.utils.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class KrakatauAssembler extends Assembler {
    public KrakatauAssembler() {
        super("krakatau", "Krakatau");
    }

    /**
     * Possible {@link Result} returns:
     * {@link Result#SUCCESS}
     * {@link Result#ERROR_OCCURED_IN_PROCESS}
     * {@link Result#ERROR_OCCURED}
     * {@link Result#NO_PYTHON2_SET}
     */
    @Override
    public Either<Result, byte[]> assemble(String name, String contents) {
        Path tempFolder = null;
        Path tempFile;
        try {
            tempFolder = Files.createTempDirectory("ka");
            tempFile = tempFolder.resolve(name.replace('/', File.separatorChar) + ".j");
            Files.write(tempFile, contents.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            if (tempFolder != null)
                FileUtils.deleteQuietly(tempFolder.toFile());
            return Either.left(Result.ERROR_OCCURED.create(ex));
        }

        String processLog = "";
        try {
            Process process = ((ProcessController) null).launchProcess(
                    new ProcessBuilder(
                            "python",
                            "-O",
                            "assemble.py",
                            "-out",
                            tempFolder.toAbsolutePath().toString(),
                            tempFile.toAbsolutePath().toString()
                    ).directory(
                            Constants.KRAKATAU_DIR
                    )
            );

            processLog = Utils.readProcess(process);
        } catch (IOException e) {
            FileUtils.deleteQuietly(tempFolder.toFile());
            return Either.left(Result.ERROR_OCCURED_IN_PROCESS.create(e, processLog));
        }

        try {
            return Either.right(FileUtils.readFileToByteArray(new File(tempFile.toAbsolutePath().toString().replace(".j", ".class"))));
        } catch (IOException ex) {
            return Either.left(Result.ERROR_OCCURED.create(ex, processLog));
        } finally {
            FileUtils.deleteQuietly(tempFolder.toFile());
        }
    }
}
