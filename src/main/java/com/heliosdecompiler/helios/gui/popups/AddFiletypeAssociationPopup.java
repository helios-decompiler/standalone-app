package com.heliosdecompiler.helios.gui.popups;

import com.heliosdecompiler.helios.Resources;
import com.heliosdecompiler.helios.api.events.Events;
import com.heliosdecompiler.helios.api.events.FiletypeAssociationCreateEvent;
import com.heliosdecompiler.helios.api.events.FiletypeAssociationDeleteEvent;
import com.heliosdecompiler.helios.api.events.FiletypeAssociationEditEvent;
import com.heliosdecompiler.helios.gui.data.FiletypeAssociationData;
import com.heliosdecompiler.helios.transformers.Transformer;
import com.heliosdecompiler.helios.transformers.Viewable;
import com.heliosdecompiler.helios.utils.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

public class AddFiletypeAssociationPopup {
    private final Display display = Display.getDefault();
    private Shell shell;

    public AddFiletypeAssociationPopup() {
        this(null);
    }

    public AddFiletypeAssociationPopup(MenuItem item) {
        display.asyncExec(() -> {
            FiletypeAssociationData data = item == null ? null : (FiletypeAssociationData) item.getData();

            shell = new Shell(display, SWT.CLOSE | SWT.BORDER);
            shell.setImage(Resources.ICON.getImage());
            shell.setText("Add Filetype Association");

            KeyAdapter adapter = new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.keyCode == SWT.ESC) {
                        shell.setVisible(false);
                        shell.dispose();
                        e.doit = false;
                    }
                }
            };

            FillLayout shellLayout = new FillLayout();
            shellLayout.type = SWT.VERTICAL;
            shell.setLayout(shellLayout);

            Composite top = new Composite(shell, SWT.NONE);
            top.setLayout(new FillLayout());
            Composite bottom = new Composite(shell, SWT.NONE);
            bottom.setLayout(new FillLayout());

            GC gc = new GC(shell);
            Text text = new Text(top, SWT.BORDER | (data != null ? SWT.READ_ONLY : 0));
            text.setSize(gc.getFontMetrics().getAverageCharWidth() * 50, gc.getFontMetrics().getHeight() * 2);
            text.setText(data == null ? ".extension" : data.getExtension());

            Combo comboDropDown = new Combo(top, SWT.DROP_DOWN | SWT.READ_ONLY);
            comboDropDown.add("Select a transformer");
            comboDropDown.select(0);

            for (Transformer transformer : Transformer.getAllTransformers(t -> t instanceof Viewable)) {
                comboDropDown.add(transformer.getName());
            }

            if (data != null) {
                comboDropDown.select(comboDropDown.indexOf(data.getTransformer().getName()));
            }

            Button ok = new Button(bottom, SWT.PUSH);
            ok.setText("Ok");
            if (item != null) {
                Button delete = new Button(bottom, SWT.PUSH);
                delete.setText("Delete");
                delete.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        String extension = text.getText();
                        shell.setVisible(false);
                        shell.dispose();
                        FiletypeAssociationDeleteEvent event = new FiletypeAssociationDeleteEvent(extension);
                        Events.callEvent(event);
                    }
                });
            }
            Button cancel = new Button(bottom, SWT.PUSH);
            cancel.setText("Cancel");
            cancel.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    shell.setVisible(false);
                    shell.dispose();
                }
            });

            shell.pack();
            gc.dispose();

            SWTUtil.center(shell);

            shell.addKeyListener(adapter);
            text.addKeyListener(adapter);
            comboDropDown.addKeyListener(adapter);
            ok.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String extension = text.getText();
                    int index = comboDropDown.getSelectionIndex();
                    String name = comboDropDown.getItem(comboDropDown.getSelectionIndex());
                    shell.close();
                    if (index == 0) {
                        SWTUtil.showMessage("You must select a transformer");
                        return;
                    }
                    Transformer transformer = Transformer.getByName(name);
                    if (transformer == null) {
                        SWTUtil.showMessage("Error: Transformer not found");
                        return;
                    }
                    if (!extension.startsWith(".")) {
                        SWTUtil.showMessage("Extension must start with .");
                        return;
                    }
                    if (item == null) {
                        FiletypeAssociationCreateEvent event = new FiletypeAssociationCreateEvent(extension, transformer);
                        Events.callEvent(event);
                    } else {
                        FiletypeAssociationEditEvent event = new FiletypeAssociationEditEvent(extension, transformer);
                        Events.callEvent(event);
                    }
                }
            });
        });
    }

    public void open() {
        display.asyncExec(() -> shell.open());
    }
}
