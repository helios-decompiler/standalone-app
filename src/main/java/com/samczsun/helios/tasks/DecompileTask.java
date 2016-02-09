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

package com.samczsun.helios.tasks;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.samczsun.helios.Helios;
import com.samczsun.helios.LoadedFile;
import com.samczsun.helios.api.events.Events;
import com.samczsun.helios.api.events.PreDecompileEvent;
import com.samczsun.helios.handler.ExceptionHandler;
import com.samczsun.helios.transformers.Transformer;
import com.samczsun.helios.transformers.decompilers.Decompiler;
import com.samczsun.helios.transformers.disassemblers.Disassembler;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.Lexer;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class DecompileTask implements Runnable {
    private final String fileName;
    private final String className;
    private final RSyntaxTextArea textArea;
    private final Transformer transformer;

    public DecompileTask(String fileName, String className, RSyntaxTextArea textArea, Transformer transformer) {
        this.fileName = fileName;
        this.className = className;
        this.textArea = textArea;
        this.transformer = transformer;
    }

    @Override
    public void run() {
        LoadedFile loadedFile = Helios.getLoadedFile(fileName);
        byte[] classFile = loadedFile.getFiles().get(className);
        PreDecompileEvent event = new PreDecompileEvent(classFile);
        Events.callEvent(event);
        classFile = event.getBytes();
        StringBuilder output = new StringBuilder();
        if (transformer instanceof Decompiler) {
            if (((Decompiler) transformer).decompile(loadedFile.getClassNode(className), classFile, output)) {
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                textArea.setCodeFoldingEnabled(true);
            }
            textArea.setText(output.toString());
        } else if (transformer instanceof Disassembler) {
            if (((Disassembler) transformer).disassembleClassNode(loadedFile.getClassNode(className), classFile,
                    output)) {
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                textArea.setCodeFoldingEnabled(true);
            }
            textArea.setText(output.toString());
        }
    }
}
