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

package com.heliosdecompiler.helios.controller.ui.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.heliosdecompiler.helios.controller.ui.UserInterfaceController;
import com.heliosdecompiler.helios.gui.model.CommonError;
import com.heliosdecompiler.helios.gui.model.Message;
import com.heliosdecompiler.helios.ui.MessageHandler;
import com.heliosdecompiler.helios.ui.views.file.FileFilter;
import com.heliosdecompiler.helios.utils.OSUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;

@Singleton
public class WindowsUIController implements UserInterfaceController {

    @Inject
    private MessageHandler messageHandler;


    public void registerInContextMenu() {
        try {
            if (OSUtils.getOS() == OSUtils.OS.WINDOWS) {
                Process process = Runtime.getRuntime().exec("reg add HKCU\\Software\\Classes\\*\\shell\\helios\\command /f");
                process.waitFor();
                if (process.exitValue() == 0) {
                    process = Runtime.getRuntime().exec("reg add HKCU\\Software\\Classes\\*\\shell\\helios /ve /d \"Open with Helios\" /f");
                    process.waitFor();
                    if (process.exitValue() == 0) {
                        File currentJarLocation = getJarLocation();
                        if (currentJarLocation != null) {
                            File javaw = getJavawLocation();
                            if (javaw != null) {
                                process = Runtime.getRuntime().exec("reg add HKCU\\Software\\Classes\\*\\shell\\helios /v MultiSelectModel /d \"Single\" /f"); //Don't allow opening 2 at once... sorry
                                process.waitFor();
                                if (process.exitValue() == 0) {
                                    process = Runtime.getRuntime().exec("reg add HKCU\\Software\\Classes\\*\\shell\\helios\\command /ve /d \"\\\"" + javaw.getAbsolutePath() + "\\\" -jar \\\"" + currentJarLocation.getAbsolutePath() + "\\\" --open \\\"%1\\\"\" /f");
                                    process.waitFor();
                                    if (process.exitValue() == 0) {
                                        messageHandler.handleMessage(CommonError.CONTEXT_MENU_SUCCESSFUL.format());
                                    } else {
                                        messageHandler.handleMessage(CommonError.CONTEXT_MENU_FAILED.format("6"));
                                    }
                                } else {
                                    messageHandler.handleMessage(CommonError.CONTEXT_MENU_FAILED.format("5"));
                                }
                            } else {
                                messageHandler.handleMessage(CommonError.CONTEXT_MENU_FAILED.format("4"));
                            }
                        } else {
                            messageHandler.handleMessage(CommonError.CONTEXT_MENU_FAILED.format("3"));
                        }
                    } else {
                        messageHandler.handleMessage(CommonError.CONTEXT_MENU_FAILED.format("2"));
                    }
                } else {
                    messageHandler.handleMessage(CommonError.CONTEXT_MENU_FAILED.format("1"));
                }
            } else {
                messageHandler.handleMessage(CommonError.WINDOWS_ONLY.format());
            }
        } catch (Throwable t) {
            messageHandler.handleException(Message.UNKNOWN_ERROR, t);
        }
    }

    public void relaunchAsAdmin(String arg) throws IOException, InterruptedException, URISyntaxException {
        File currentJarLocation = getJarLocation();
        File javawLocation = getJavawLocation();
        if (currentJarLocation == null) {
            messageHandler.handleMessage(CommonError.RELAUNCH_ADMIN_FAILED.format("1"));
            return;
        }
        if (javawLocation == null) {
            messageHandler.handleMessage(CommonError.RELAUNCH_ADMIN_FAILED.format("1"));
            return;
        }
        File tempVBSFile = File.createTempFile("tmpvbs", ".vbs");
        PrintWriter writer = new PrintWriter(tempVBSFile);
        writer.println("Set objShell = CreateObject(\"Wscript.Shell\")");
        writer.println("strPath = Wscript.ScriptFullName");
        writer.println("Set objFSO = CreateObject(\"Scripting.FileSystemObject\")");
        writer.println("Set objFile = objFSO.GetFile(strPath)");
        writer.println("strFolder = objFSO.GetParentFolderName(objFile)");
        writer.println("Set UAC = CreateObject(\"Shell.Application\")");

        String args = "-jar ``%s`` " + arg;
        args = String.format(args, currentJarLocation.getAbsolutePath()).replace('`', '"');

        String uacCommand = "UAC.ShellExecute ```%s```, `%s`, strFolder, `runas`, 1";
        uacCommand = String.format(uacCommand, javawLocation.getAbsolutePath(), args).replace('`', '"');
        writer.println(uacCommand);
        writer.println("WScript.Quit 0");
        writer.close();

//        if (socket != null) {
//            try {
//                socket.close();
//            } catch (Throwable t) {
//                ExceptionHandler.handle(t);
//            }
//        }

        Process process = Runtime.getRuntime().exec("cscript " + tempVBSFile.getAbsolutePath());
        process.waitFor();
        System.exit(process.exitValue());
    }


    private File getJarLocation() throws URISyntaxException {
        File currentJarLocation = (File) System.getProperties().get("com.heliosdecompiler.bootstrapperFile");
        if (currentJarLocation != null && currentJarLocation.exists() && currentJarLocation.isFile()) {
            return currentJarLocation;
        }
        messageHandler.handleMessage(CommonError.COULD_NOTE_LOCATE_HELIOS.format(), true);
        return messageHandler.chooseFile()
                .withInitialDirectory(new File("."))
                .withTitle("Choose location of Helios bootstrapper")
                .withExtensionFilter(new FileFilter("Java Archive", "*.jar"), true)
                .promptSingle();
    }

    private File getJavawLocation() {
        File javaw = null;
        String name = "java";

        OSUtils.OS os = OSUtils.getOS();
        if (os == OSUtils.OS.WINDOWS) {
            javaw = new File(System.getProperty("java.home") + File.separator + "bin" + File.separator + "javaw.exe");
            name = "javaw";
        } else if (os == OSUtils.OS.LINUX || os == OSUtils.OS.MAC) {
            javaw = new File(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        }


        if (javaw != null && javaw.exists() && javaw.isFile()) {
            return javaw;
        }
        messageHandler.handleMessage(CommonError.COULD_NOT_LOCATE_JAVA.format(name), true);
        return messageHandler.chooseFile()
                .withInitialDirectory(new File("."))
                .withTitle("Choose location of " + name)
                .withExtensionFilter(new FileFilter("Executable", "*"), true)
                .promptSingle();
    }
}
