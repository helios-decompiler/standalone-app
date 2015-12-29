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

package com.samczsun.helios.gui;

import com.samczsun.helios.utils.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class SearchPopup {
    private final Display display = Display.getDefault();
    private Shell shell;

    public SearchPopup() {
        display.asyncExec(() -> {
            shell = new Shell(display, SWT.CLOSE);
            shell.setLayout(new GridLayout());

            Combo comboDropDown = new Combo(shell, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
            shell.pack();

            SWTUtil.center(shell);

            shell.addShellListener(new ShellAdapter() {
                @Override
                public void shellClosed(ShellEvent e) {
                    shell.setVisible(false);
                    e.doit = false;
                }
            });
            shell.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    System.out.println(e.keyCode);
                    if (e.keyCode == SWT.ESC) {
                        shell.setVisible(false);
                    }
                }
            });
        });
    }

    public void open() {
        display.asyncExec(() -> shell.open());
    }
}
