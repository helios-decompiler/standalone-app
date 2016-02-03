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

package com.samczsun.helios;

import com.samczsun.helios.handler.ExceptionHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public enum Resources {
    PACKAGE("package.png"),
    CLASS("class.png"),
    JAVA("java.png"),
    ANDROID("android.png"),
    CONFIG("config.png"),
    TEXT("text.png"),
    CSHARP("c#.png"),
    CPLUSPLUS("c++.png"),
    BAT("bat.png"),
    SH("sh.png"),
    FILE("file.png"),
    ICON("icon.png"),
    IMAGE("image.png"),
    JAR("jar.png"),
    ZIP("zip.png"),
    FOLDER("folder.png");

    private final String filePath;
    private byte[] data;
    private Image image;

    Resources(String path) {
        this.filePath = "/res/" + path;
    }

    public static void loadAllImages() {
        if (Thread.currentThread() != Display.getDefault().getThread()) {
            throw new IllegalArgumentException("Wrong thread");
        }
        for (Resources resources : Resources.values()) {
            try {
                resources.data = IOUtils.toByteArray(Resources.class.getResourceAsStream(resources.filePath));
                resources.image = new Image(Display.getDefault(), resources.getData());
            } catch (IOException exception) {
                ExceptionHandler.handle(exception);
            }
        }
    }

    public ByteArrayInputStream getData() {
        return new ByteArrayInputStream(data);
    }

    public Image getImage() {
        return image;
    }
}
