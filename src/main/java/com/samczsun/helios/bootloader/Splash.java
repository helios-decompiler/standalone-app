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

package com.samczsun.helios.bootloader;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

public class Splash {
    private static final int SPLASH_MAX = 100;
    private static final int SPLASH_FACTOR = 100 / BootSequence.values().length;

    private final Display display;
    private Shell splash = null;

    private ProgressBar bar;
    private Label text;
    private Image image;

    public Splash(final Display display) {
        this.display = display;
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                splash = new Shell(display, SWT.ON_TOP);
                splash.setBackground(new Color(display, 255, 255, 255));

                Label label = new Label(splash, SWT.NONE);
                image = new Image(display, Bootloader.class.getResourceAsStream("/res/256.png"));
                label.setImage(image);
                label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));

                bar = new ProgressBar(splash, SWT.NONE);
                bar.setMaximum(SPLASH_MAX);
                bar.setLayoutData(new GridData(GridData.FILL_BOTH));

                text = new Label(splash, SWT.NONE);
                text.setSize(splash.getSize());
                text.setBackground(new Color(display, 255, 255, 255));
                text.setFont(new Font(display, display.getSystemFont().getFontData()[0].getName(), 12, SWT.NONE));
                text.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.FILL_BOTH));

                splash.setLayout(new GridLayout(1, false));
                splash.pack();

                Rectangle splashRect = splash.getBounds();
                Rectangle displayRect = display.getPrimaryMonitor().getBounds();
                int x = (displayRect.width - splashRect.width) / 2;
                int y = (displayRect.height - splashRect.height) / 2;
                splash.setLocation(x, y);
                splash.open();
            }
        });
    }

    public void updateState(BootSequence value) {
        if (value != BootSequence.COMPLETE) {
            display.syncExec(() -> {
                bar.setSelection(value.ordinal() * SPLASH_FACTOR);
                text.setText(value.getMessage());
                splash.redraw();
            });
        } else {
            display.syncExec(() -> {
                splash.close();
                image.dispose();
                text.dispose();
                bar.dispose();
            });
        }
    }


    public boolean isDisposed() {
        return splash.isDisposed();
    }
}
