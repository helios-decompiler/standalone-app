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

package com.samczsun.helios.transformers.disassemblers;

import com.samczsun.helios.transformers.TransformerSettings;
import com.sun.tools.classfile.AccessFlags;
import com.sun.tools.javap.JavapTask;
import org.objectweb.asm.tree.ClassNode;

import java.io.PrintWriter;
import java.io.StringWriter;

public class JavapDisassembler extends Disassembler {
    public JavapDisassembler() {
        super("javap-disassembler", "javap Disassembler");
        for (Settings setting : Settings.values()) {
            settings.registerSetting(setting);
        }
    }

    @Override
    public boolean isApplicable(String className) {
        return className.endsWith(".jar") || className.endsWith(".class");
    }

    public boolean disassembleClassNode(ClassNode cn, byte[] b, StringBuilder output) {
        StringWriter stringWriter = new StringWriter();
        JavapTask task = new JavapTask();
        task.logToUse = new PrintWriter(stringWriter);
        task.classData = b;
        task.className = cn.name;
        task.options.verbose = Settings.VERBOSE.isEnabled();
        task.options.showDescriptors = Settings.VERBOSE.isEnabled();
        task.options.showFlags = Settings.VERBOSE.isEnabled();
        task.options.showAllAttrs = Settings.VERBOSE.isEnabled();
        task.options.showLineAndLocalVariableTables = Settings.SHOW_LINE_AND_LOCAL_VARIABLES.isEnabled();
        task.options.showConstants = Settings.CONSTANTS.isEnabled();
        task.options.showDisassembled = Settings.DISASSEMBLE.isEnabled();
        task.options.showDescriptors = Settings.PRINT_TYPE_SIGNATURES.isEnabled();
        task.options.sysInfo = Settings.SYSINFO.isEnabled();
        if (Settings.PUBLIC.isEnabled()) {
            task.options.accessOptions.add("-public");
            task.options.showAccess = AccessFlags.ACC_PUBLIC;
        }
        if (Settings.PROTECTED.isEnabled()) {
            task.options.accessOptions.add("-protected");
            task.options.showAccess = AccessFlags.ACC_PROTECTED;
        }
        if (Settings.PACKAGE.isEnabled()) {
            task.options.accessOptions.add("-package");
            task.options.showAccess = 0;
        }
        if (Settings.PRIVATE.isEnabled()) {
            task.options.accessOptions.add("-private");
            task.options.showAccess = AccessFlags.ACC_PRIVATE;
        }
        task.run();
        output.append(stringWriter.toString());
        return true;
    }

    public enum Settings implements TransformerSettings.Setting {
        VERBOSE("verbose", "Print additional information", true),
        SHOW_LINE_AND_LOCAL_VARIABLES("l", "Print line number and local variable tables", true),
        PUBLIC("public", "Show only public classes and members"),
        PROTECTED("protected", "Show protected/public classes and members"),
        PACKAGE("package", "Show package/protected/public classes"),
        PRIVATE("private", "Show all classes and members"),
        DISASSEMBLE("c", "Disassemble the code"),
        PRINT_TYPE_SIGNATURES("s", "Print internal type signatures"),
        SYSINFO("sysinfo", "Show system info (path, size, date, MD5 hash) of class being processed"),
        CONSTANTS("constants", "Show final constants");


        private final String name;
        private final String param;
        private boolean on;

        Settings(String param, String name) {
            this(param, name, false);
        }

        Settings(String param, String name, boolean on) {
            this.name = name;
            this.param = param;
            this.on = on;
        }

        public String getParam() {
            return param;
        }

        public String getText() {
            return name;
        }

        public boolean isEnabled() {
            return on;
        }

        public void setEnabled(boolean enabled) {
            this.on = enabled;
        }
    }
}