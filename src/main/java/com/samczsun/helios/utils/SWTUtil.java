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

package com.samczsun.helios.utils;

import com.samczsun.helios.Constants;
import com.samczsun.helios.Helios;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import sun.awt.DefaultMouseInfoPeer;

import java.awt.MouseInfo;
import java.awt.peer.MouseInfoPeer;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SWTUtil {
    public static boolean promptForYesNo(String question) {
        return promptForYesNo(Constants.REPO_NAME + "- Question", question);
    }

    public static boolean promptForYesNo(final String title, final String question) {
        final Display display = Display.getDefault();
        final AtomicBoolean result = new AtomicBoolean(false);
        display.syncExec(() -> {
            Shell shell = new Shell(display, SWT.ON_TOP);

            MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            messageBox.setMessage(question);
            messageBox.setText(title);

            shell.setSize(0, 0);
            shell.setVisible(true);
            shell.forceFocus();
            shell.forceActive();

            result.set(messageBox.open() == SWT.YES);

            shell.dispose();
        });
        return result.get();
    }

    public static Shell generateLongMessage(String title, String message) {
        Display display = Display.getDefault();
        AtomicReference<Shell> shellReference = new AtomicReference<>();
        display.syncExec(() -> {
            Shell shell = new Shell(display);
            shell.setLayout(new GridLayout());
            shell.setText(title);
            Font mono = new Font(display, "Courier", 10, SWT.NONE);
            Text t = new Text(shell, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
            t.setLayoutData(new GridData(GridData.FILL_BOTH));
            t.setFont(mono);
            t.setText(message);
            t.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    if ((e.stateMask & SWT.CTRL) == SWT.CTRL && e.keyCode == 'a') {
                        t.selectAll();
                    }
                }
            });
            shell.addDisposeListener(disposeEvent -> mono.dispose());
            t.pack();
            shell.pack();
            shellReference.set(shell);
        });
        return shellReference.get();
    }

    public static void showMessage(String message) {
        showMessage(Constants.REPO_NAME + " - Message", message, false);
    }

    public static void showMessage(String message, boolean wait) {
        showMessage(Constants.REPO_NAME + " - Message", message, wait);
    }

    public static void showMessage(final String title, final String message, boolean wait) {
        final Display display = Display.getDefault();
        Runnable todo = () -> {
            Shell shell = new Shell(display, SWT.ON_TOP);
            shell.setLayout(new FillLayout());
            MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION);
            messageBox.setMessage(message);
            messageBox.setText(title);
            shell.setSize(0, 0);
            shell.setVisible(true);
            messageBox.open();
            shell.close();
            shell.dispose();
        };
        if (wait) {
            display.syncExec(todo);
        } else {
            display.asyncExec(todo);
        }
    }

    public static void center(Shell shell) {
        org.eclipse.swt.graphics.Point mainLocation = Helios.getGui().getShell().getLocation();
        org.eclipse.swt.graphics.Point size = Helios.getGui().getShell().getSize();
        Rectangle bounds = new Rectangle(mainLocation.x, mainLocation.y, size.x, size.y);
        Rectangle rect = shell.getBounds();
        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y + (bounds.height - rect.height) / 2;
        shell.setLocation(x, y);
    }

    public static Point getMouseLocation() {
        java.awt.Point mousePoint = MouseInfo.getPointerInfo().getLocation(); //TODO Optimize but Mac doesn't use DefaultMouseInfoPeer
        return new Point(mousePoint.x, mousePoint.y);
    }

    public static boolean isEnter(int keyCode) {
        return keyCode == SWT.CR || keyCode == 16777296;
    }
}
