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
import com.heliosdecompiler.helios.Message;
import com.heliosdecompiler.helios.controller.UpdateController;
import com.heliosdecompiler.helios.controller.ui.UserInterfaceController;
import com.heliosdecompiler.helios.ui.MessageHandler;
import com.heliosdecompiler.helios.ui.views.file.FileFilter;
import com.heliosdecompiler.helios.utils.OSUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.util.function.Consumer;

@Singleton
public class WindowsUIController implements UserInterfaceController {

    @Inject
    private MessageHandler messageHandler;

    @Inject
    private UpdateController updateController;

    public void registerInContextMenu() {
        try {
            if (OSUtils.getOS() == OSUtils.OS.WINDOWS) {
                Process process = Runtime.getRuntime().exec("reg add HKCU\\Software\\Classes\\*\\shell\\helios\\command /f");
                process.waitFor();
                if (process.exitValue() == 0) {
                    process = Runtime.getRuntime().exec("reg add HKCU\\Software\\Classes\\*\\shell\\helios /ve /d \"Open with Helios\" /f");
                    process.waitFor();
                    if (process.exitValue() == 0) {
                        getJarLocation(currentJarLocation -> {
                            if (currentJarLocation != null) {
                                getJavawLocation(javaw -> {
                                    try {
                                        if (javaw != null) {
                                            Process process1 = Runtime.getRuntime().exec("reg add HKCU\\Software\\Classes\\*\\shell\\helios /v MultiSelectModel /d \"Single\" /f"); //Don't allow opening 2 at once... sorry
                                            process1.waitFor();
                                            if (process1.exitValue() == 0) {
                                                process1 = Runtime.getRuntime().exec("reg add HKCU\\Software\\Classes\\*\\shell\\helios\\command /ve /d \"\\\"" + javaw.getAbsolutePath() + "\\\" -jar \\\"" + currentJarLocation.getAbsolutePath() + "\\\" --open \\\"%1\\\"\" /f");
                                                process1.waitFor();
                                                if (process1.exitValue() == 0) {
                                                    messageHandler.handleMessage(Message.CONTEXT_MENU_SUCCESSFUL.format());
                                                } else {
                                                    messageHandler.handleMessage(Message.CONTEXT_MENU_FAILED.format("6"));
                                                }
                                            } else {
                                                messageHandler.handleMessage(Message.CONTEXT_MENU_FAILED.format("5"));
                                            }
                                        } else {
                                            messageHandler.handleMessage(Message.CONTEXT_MENU_FAILED.format("4"));
                                        }
                                    } catch (Throwable t) {
                                        messageHandler.handleException(Message.ERROR_UNKNOWN_ERROR.format(), t);
                                    }
                                });
                            } else {
                                messageHandler.handleMessage(Message.CONTEXT_MENU_FAILED.format("3"));
                            }
                        });
                    } else {
                        messageHandler.handleMessage(Message.CONTEXT_MENU_FAILED.format("2"));
                    }
                } else {
                    messageHandler.handleMessage(Message.CONTEXT_MENU_FAILED.format("1"));
                }
            } else {
                messageHandler.handleMessage(Message.GENERIC_WINDOWS_ONLY.format());
            }
        } catch (Throwable t) {
            messageHandler.handleException(Message.ERROR_UNKNOWN_ERROR.format(), t);
        }
    }

//    public void relaunchAsAdmin(String arg) throws IOException, InterruptedException, URISyntaxException {
//        File currentJarLocation = getJarLocation();
//        File javawLocation = getJavawLocation();
//        if (currentJarLocation == null) {
//            messageHandler.handleMessage(CommonError.RELAUNCH_ADMIN_FAILED.format("1"));
//            return;
//        }
//        if (javawLocation == null) {
//            messageHandler.handleMessage(CommonError.RELAUNCH_ADMIN_FAILED.format("1"));
//            return;
//        }
//        File tempVBSFile = File.createTempFile("tmpvbs", ".vbs");
//        PrintWriter writer = new PrintWriter(tempVBSFile);
//        writer.println("Set objShell = CreateObject(\"Wscript.Shell\")");
//        writer.println("strPath = Wscript.ScriptFullName");
//        writer.println("Set objFSO = CreateObject(\"Scripting.FileSystemObject\")");
//        writer.println("Set objFile = objFSO.GetFile(strPath)");
//        writer.println("strFolder = objFSO.GetParentFolderName(objFile)");
//        writer.println("Set UAC = CreateObject(\"Shell.Application\")");
//
//        String args = "-jar ``%s`` " + arg;
//        args = String.format(args, currentJarLocation.getAbsolutePath()).replace('`', '"');
//
//        String uacCommand = "UAC.ShellExecute ```%s```, `%s`, strFolder, `runas`, 1";
//        uacCommand = String.format(uacCommand, javawLocation.getAbsolutePath(), args).replace('`', '"');
//        writer.println(uacCommand);
//        writer.println("WScript.Quit 0");
//        writer.close();
//
////        if (socket != null) {
////            try {
////                socket.close();
////            } catch (Throwable t) {
////                ExceptionHandler.handle(t);
////            }
////        }
//
//        Process process = Runtime.getRuntime().exec("cscript " + tempVBSFile.getAbsolutePath());
//        process.waitFor();
//        System.exit(process.exitValue());
//    }

    private void getJarLocation(Consumer<File> file) throws URISyntaxException {
        File currentJarLocation = updateController.getHeliosLocation();
        if (currentJarLocation != null && currentJarLocation.exists() && currentJarLocation.isFile()) {
            file.accept(currentJarLocation);
            return;
        }

        messageHandler.handleMessage(Message.COULD_NOTE_LOCATE_HELIOS.format(), () -> {
            file.accept(messageHandler.chooseFile()
                    .withInitialDirectory(new File("."))
                    .withTitle(Message.GENERIC_SELECT_FILE.format("Helios"))
                    .withExtensionFilter(new FileFilter(Message.FILETYPE_JAVA_ARCHIVE.format(), "*.jar"), true)
                    .promptSingle());
        });
    }

    private void getJavawLocation(Consumer<File> consumer) {
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
            consumer.accept(javaw);
            return;
        }

        String fname = name;
        messageHandler.handleMessage(Message.COULD_NOT_LOCATE_JAVA.format(name), () -> {
            consumer.accept(messageHandler.chooseFile()
                    .withInitialDirectory(new File("."))
                    .withTitle(Message.GENERIC_SELECT_FILE.format(fname))
                    .withExtensionFilter(new FileFilter(Message.FILETYPE_ANY.format(), "*"), true)
                    .promptSingle());
        });
    }
}
