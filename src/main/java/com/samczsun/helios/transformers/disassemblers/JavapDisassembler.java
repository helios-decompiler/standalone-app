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

package com.samczsun.helios.transformers.disassemblers;

import com.sun.tools.javap.JavapTask;
import org.objectweb.asm.tree.ClassNode;

import java.io.PrintWriter;
import java.io.StringWriter;

public class JavapDisassembler extends Disassembler {
    public JavapDisassembler() {
        super("javap-disassembler", "javap Disassembler");
    }

    public boolean disassembleClassNode(ClassNode cn, byte[] b, StringBuilder output) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        JavapTask task = new JavapTask();
        task.logToUse = printWriter;
        task.classData = b;
        task.className = cn.name;
        task.options.verbose = true;
        task.options.showDescriptors = true;
        task.options.showFlags = true;
        task.options.showAllAttrs = true;
        task.options.showLineAndLocalVariableTables = true;
        task.options.showConstants = true;
        task.run();
        output.append(stringWriter.toString());
        return true;
    }
}