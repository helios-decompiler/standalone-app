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

package com.samczsun.helios.handler;

import com.samczsun.helios.utils.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionHandler {

    public static void handle(Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        Shell shell = SWTUtil.generateLongMessage("An error has occured", stringWriter.toString());
        Display display = Display.getDefault();
        display.syncExec(() -> {
            Composite composite = new Composite(shell, SWT.NONE);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));
            composite.setLayout(new FillLayout());
            Button send = new Button(composite, SWT.PUSH);
            send.setText("Send Error Report");
            Button dontsend = new Button(composite, SWT.PUSH);
            dontsend.setText("Don't Send");
            send.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    shell.close();
                }
            });
            dontsend.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    shell.close();
                }
            });
            composite.pack();
            shell.pack();
            shell.open();
        });
    }
}
