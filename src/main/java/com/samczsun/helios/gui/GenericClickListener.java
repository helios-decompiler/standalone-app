package com.samczsun.helios.gui;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;

import java.util.function.BiConsumer;

public class GenericClickListener implements MouseListener, java.awt.event.MouseListener {

    public enum ClickType {
        LEFT, MIDDLE, RIGHT;
        private static ClickType fromButton(int button) {
            return ClickType.values()[button - 1];
        }
    }

    private final BiConsumer<ClickType, Boolean> handler;

    private final byte buttons;

    private final boolean doubleClick;

    public GenericClickListener(BiConsumer<ClickType, Boolean> handler, ClickType type, ClickType...others) {
        this(handler, false, type, others);
    }

    public GenericClickListener(BiConsumer<ClickType, Boolean> handler, boolean doubleClick, ClickType type, ClickType...others) {
        this.handler = handler;
        this.doubleClick = doubleClick;
        int buttons = 1 << type.ordinal();
        for (ClickType otherType : others) {
            buttons |= 1 << otherType.ordinal();
        }
        this.buttons = (byte) buttons;
    }

    /* *
     * SWT events
     * */

    @Override
    public void mouseDoubleClick(MouseEvent event) {
        handle(ClickType.fromButton(event.button), true);
    }

    @Override
    public void mouseDown(MouseEvent event) {
    }

    private void handle(ClickType type, boolean doubleClick) {
        if (this.doubleClick == doubleClick && (this.buttons & (1 << (type.ordinal()))) != 0) {
            this.handler.accept(type, doubleClick);
        }
    }

    @Override
    public void mouseUp(MouseEvent event) {
        handle(ClickType.fromButton(event.button), false);
    }

    /* *
     * AWT events
     * */

    @Override
    public void mouseClicked(java.awt.event.MouseEvent e) {
        handle(ClickType.fromButton(e.getButton()), false);
    }

    @Override
    public void mousePressed(java.awt.event.MouseEvent e) {
    }

    @Override
    public void mouseReleased(java.awt.event.MouseEvent e) {
    }

    @Override
    public void mouseEntered(java.awt.event.MouseEvent e) {
    }

    @Override
    public void mouseExited(java.awt.event.MouseEvent e) {
    }
}
