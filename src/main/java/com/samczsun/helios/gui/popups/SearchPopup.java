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

package com.samczsun.helios.gui.popups;

import com.samczsun.helios.Resources;
import com.samczsun.helios.api.events.Events;
import com.samczsun.helios.api.events.requests.SearchRequest;
import com.samczsun.helios.utils.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;

public class SearchPopup {
    private final Display display = Display.getDefault();
    private Shell shell;

    public SearchPopup() {
        display.asyncExec(() -> {
            shell = new Shell(display, SWT.CLOSE | SWT.BORDER);
            shell.setImage(Resources.ICON.getImage());
            shell.setText("Find");

            KeyAdapter adapter = new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.keyCode == SWT.ESC) {
                        shell.setVisible(false);
                    }
                }
            };
            FillLayout shellLayout = new FillLayout();
            shellLayout.type = SWT.VERTICAL;
            shell.setLayout(shellLayout);

            Text text = new Text(shell, SWT.BORDER);
            Composite composite = new Composite(shell, SWT.NONE);
            composite.setLayout(new RowLayout());
            Button wrap = new Button(composite, SWT.CHECK);
            wrap.setText("Wrap Search");
            wrap.setSelection(true);
            Button caseSensitive = new Button(composite, SWT.CHECK);
            caseSensitive.setText("Match case");

            Button up = new Button(composite, SWT.RADIO);
            up.setText("Search up");
            up.setSelection(true);
            Button down = new Button(composite, SWT.RADIO);
            down.setText("Search down");

            shell.pack();

            SWTUtil.center(shell);

            shell.addShellListener(new ShellAdapter() {
                @Override
                public void shellClosed(ShellEvent e) {
                    shell.setVisible(false);
                    e.doit = false;
                }
            });
            shell.addKeyListener(adapter);
            text.addKeyListener(adapter);
            text.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    if (SWTUtil.isEnter(e.keyCode)) {
                        Events.callEvent(new SearchRequest(text.getText(), caseSensitive.getSelection(), wrap.getSelection(), up.getSelection()));
                    }
                }
            });
        });
    }

    public void open() {
        display.asyncExec(() -> {
            shell.open();
        });
    }
}
