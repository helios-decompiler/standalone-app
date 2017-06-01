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

package com.heliosdecompiler.helios.controller;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.heliosdecompiler.helios.Message;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTask;
import com.heliosdecompiler.helios.controller.backgroundtask.BackgroundTaskHelper;
import com.heliosdecompiler.helios.ui.MessageHandler;
import com.heliosdecompiler.helios.ui.views.file.FileFilter;
import com.heliosdecompiler.helios.utils.OSUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

@Singleton
public class UpdateController {
    private static final String UPDATE_URL = "https://ci.samczsun.com/job/helios-decompiler/job/Standalone%20App/lastReleaseBuild/api/json?pretty=true";
    private static final String DOWNLOAD_URL = "https://ci.samczsun.com/job/helios-decompiler/job/Standalone%20App/lastReleaseBuild/artifact/target/helios-standalone.jar";
    private static final String DOWNLOAD_URL_WINDOWS = "https://ci.samczsun.com/job/helios-decompiler/job/Standalone%20App/lastReleaseBuild/artifact/target/helios-standalone.exe";
    private static final String DOWNLOAD_URL_OSX = "https://ci.samczsun.com/job/helios-decompiler/job/Standalone%20App/lastReleaseBuild/artifact/target/helios-standalone.dmg";
    private static final Gson GSON = new Gson();

    @Inject
    private MessageHandler messageHandler;

    @Inject
    private BackgroundTaskHelper backgroundTaskHelper;

    public String getVersion() {
        URL versionURL = getClass().getResource("/VERSION");
        if (versionURL != null) {
            try (InputStream inputStream = versionURL.openStream()) {
                return IOUtils.toString(inputStream, "UTF-8");
            } catch (IOException ex) {
                return "42.0.0";
            }
        }
        return "42.0.0";

    }

    public File getHeliosLocation() {
        ProtectionDomain pd = getClass().getProtectionDomain();
        if (pd != null) {
            CodeSource cs = pd.getCodeSource();
            if (cs != null) {
                URL location = cs.getLocation();
                if (location != null) {
                    try {
                        File file = new File(location.toURI().getPath());
                        if (file.isFile()) {
                            return file;
                        }
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }
        }
        return null;
    }

    public void doUpdate() {
        try {
            Version thisVersion = Version.valueOf(getVersion());

            URL url = new URL(UPDATE_URL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.addRequestProperty("User-Agent", "Helios Standalone App");

            String changelog = null;
            Version latestVersion = null;

            if (urlConnection.getResponseCode() == 200) {
                try (InputStream updateStream = urlConnection.getInputStream()) {
                    JsonObject jsonObject = GSON.fromJson(new InputStreamReader(updateStream, "UTF-8"), JsonObject.class);
                    JsonArray actions = jsonObject.get("actions").getAsJsonArray();
                    for (JsonElement jsonElement : actions) {
                        JsonObject action = jsonElement.getAsJsonObject();
                        if (action.get("_class") != null) {
                            String actionClass = action.get("_class").getAsString();
                            if (actionClass.equals("hudson.plugins.release.SafeParametersAction")) {
                                JsonArray parameters = action.get("parameters").getAsJsonArray();
                                for (JsonElement jsonElement1 : parameters) {
                                    JsonObject parameter = jsonElement1.getAsJsonObject();
                                    if (parameter.get("name").getAsString().equals("CHANGELOG")) {
                                        changelog = parameter.get("value").getAsString();
                                    } else if (parameter.get("name").getAsString().equals("RELEASE_VERSION")) {
                                        latestVersion = Version.valueOf(parameter.get("value").getAsString());
                                    }
                                }
                            }
                        }
                    }
                }
            }

            System.out.println("Latest version: " + latestVersion);
            System.out.println("Changelog: " + changelog);

            if (changelog != null && latestVersion != null) {
                if (latestVersion.greaterThan(thisVersion)) {
                    messageHandler.prompt(Message.UPDATER_UPDATE_FOUND.format(latestVersion.toString(), changelog), result -> {
                        if (result) {
                            String urlToUse = DOWNLOAD_URL;
                            String extensionToUse = ".jar";
                            if (OSUtils.getOS() == OSUtils.OS.WINDOWS) {
                                urlToUse = DOWNLOAD_URL_WINDOWS;
                                extensionToUse = ".exe";
                            } else if (OSUtils.getOS() == OSUtils.OS.MAC) {
                                urlToUse = DOWNLOAD_URL_OSX;
                                extensionToUse = ".dmg";
                            }

                            String downloadUrlStr = urlToUse;

                            File target = messageHandler.chooseFile()
                                    .withTitle(Message.UPDATER_SELECT_SAVE_LOCATION.format())
                                    .withExtensionFilter(new FileFilter(Message.FILETYPE_ANY.format(), extensionToUse), true)
                                    .promptSave();

                            if (target != null) {
                                backgroundTaskHelper.submit(new BackgroundTask(Message.UPDATER_DOWNLOADING_HELIOS.format(), true, () -> {
                                    try {
                                        URL downloadurl = new URL(downloadUrlStr);
                                        HttpURLConnection downloadconnection = (HttpURLConnection) downloadurl.openConnection();
                                        downloadconnection.addRequestProperty("User-Agent", "Helios Standalone App");
                                        try (InputStream downloadStream = downloadconnection.getInputStream();
                                             FileOutputStream outputStream = new FileOutputStream(target)) {
                                            IOUtils.copy(downloadStream, outputStream);
                                        }

                                        messageHandler.handleMessage(Message.UPDATER_UPDATE_SUCCESSFUL.format());
                                    } catch (Throwable t) {
                                        messageHandler.handleException(Message.ERROR_UNEXPECTED_ERROR.format(Message.UPDATER_UPDATE_TASK_NAME.getText()), t);
                                    }
                                }));
                            }
                        }
                    });
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
