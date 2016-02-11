package com.samczsun.helios.gui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

public class ClickableSyntaxTextArea extends RSyntaxTextArea {
    protected static final Cursor DEFAULT_CURSOR = Cursor.getDefaultCursor();
    protected static final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    public Set<Link> links = new HashSet<>();

    public ClickableSyntaxTextArea() {
        MouseAdapter adapter = new MouseAdapter() {
            int lastX = -1;
            int lastY = -1;
            int lastModifiers = -1;

            public void mouseClicked(MouseEvent e) {
                if ((e.getClickCount() == 1) && ((e.getModifiers() & (Event.ALT_MASK | Event.META_MASK | Event.SHIFT_MASK)) == 0)) {
                    int offset = viewToModel(new Point(e.getX(), e.getY()));
                    if (offset != -1) {
                        Link link = getLinkForOffset(offset);
                        if (link != null) {
                            //Yes
                        }
                    }
                }
            }

            public void mouseMoved(MouseEvent e) {
                if ((e.getX() != lastX) || (e.getX() != lastY) || (lastModifiers != e.getModifiers())) {
                    lastX = e.getX();
                    lastY = e.getX();
                    lastModifiers = e.getModifiers();

                    if ((e.getModifiers() & (Event.ALT_MASK | Event.META_MASK | Event.SHIFT_MASK)) == 0) {
                        int offset = viewToModel(new Point(e.getX(), e.getY()));
                        if (offset != -1) {
                            Link link = getLinkForOffset(offset);
                            if (link != null) {
                                setCursor(HAND_CURSOR);
                                return;
                            }
                        }
                    }
                    setCursor(DEFAULT_CURSOR);
                    return;
                }
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    public boolean getUnderlineForToken(Token t) {
        Link link = getLinkForOffset(t.getOffset());
        if (link != null) {
            return getHyperlinksEnabled();
        }
        return super.getUnderlineForToken(t);
    }

    public Link getLinkForOffset(int offset) {
        for (Link link : links) {
            if (link.offset <= offset && offset <= link.offsetEnd) {
                return link;
            }
        }
        return null;
    }

    public static class Link {
        public String file;
        public int line;
        public int column;
        public int offset;
        public int offsetEnd;

        public Link(String file, int line, int column, int offset, int offsetEnd) {
            this.file = file;
            this.line = line;
            this.column = column;
            this.offset = offset;
            this.offsetEnd = offsetEnd;
        }
    }
}
