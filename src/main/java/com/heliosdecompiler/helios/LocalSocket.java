package com.heliosdecompiler.helios;

import com.heliosdecompiler.helios.handler.ExceptionHandler;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class LocalSocket extends ServerSocket implements Runnable {
    public LocalSocket() throws IOException {
        super(21354);
        new Thread(this, "Inter-Process Communications Socket").start();
    }

    public void run() {
        while (true) {
            try {
                Socket socket = this.accept();
                String args = IOUtils.toString(socket.getInputStream(), "UTF-8");
                Helios.getGui().getShell().getDisplay().asyncExec(() -> {
                    Helios.getGui().getShell().setFocus();
                    Helios.getGui().getShell().forceActive();
                    Helios.getGui().getShell().forceFocus();
                    Helios.handleCommandLine(args.split(" "));
                });
            } catch (Throwable e) {
                ExceptionHandler.handle(e);
            }
        }
    }
}
