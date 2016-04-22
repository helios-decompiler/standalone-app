package com.samczsun.helios.gui;

import com.samczsun.helios.Helios;
import com.samczsun.helios.tasks.CompileTask;
import com.samczsun.helios.transformers.Transformer;
import com.samczsun.helios.transformers.assemblers.Assembler;
import com.samczsun.helios.transformers.decompilers.Decompiler;
import com.samczsun.helios.transformers.disassemblers.Disassembler;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import com.samczsun.helios.transformers.compilers.Compiler;


public class ClickableSyntaxTextArea extends RSyntaxTextArea {
    protected static final Cursor DEFAULT_CURSOR = Cursor.getDefaultCursor();
    protected static final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    public Set<Link> links = new HashSet<>(); //fixme concurrency issues

    private final String fileName;

    private final String className;
    private final Transformer currentTransformer;


    public ClickableSyntaxTextArea(ClassManager manager, Transformer currentTransformer, String fileName, String className) {
        this.fileName = fileName;
        this.className = className;
        this.currentTransformer = currentTransformer;
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
                            System.out.println(link.fileName + " " + link.className);
                            if (link.fileName != null && link.className != null) {
                                Helios.submitBackgroundTask(() -> {
                                    manager.openFileAndDecompile(link.fileName, link.className, currentTransformer, link.jumpTo);
                                });
                            }
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

    @Override
    protected JPopupMenu createPopupMenu() {
        JPopupMenu menu = super.createPopupMenu();
        menu.addSeparator();
        JMenu decompileWith = new JMenu("Decompile with");
        for (Decompiler decompiler : Decompiler.getAllDecompilers()) {
            JMenuItem decomp = new JMenuItem(decompiler.getName());
            decomp.setEnabled(true);
            decomp.addActionListener(e -> Helios.getGui().getClassManager().openFileAndDecompile(this.fileName, this.className, decompiler, null));
            decompileWith.add(decomp);
        }
        menu.add(decompileWith);
        menu.addSeparator();
        JMenu disassembleWith = new JMenu("Disassemble with");
        for (Disassembler decompiler : Disassembler.getAllDisassemblers()) {
            JMenuItem decomp = new JMenuItem(decompiler.getName());
            decomp.setEnabled(true);
            decomp.addActionListener(e -> Helios.getGui().getClassManager().openFileAndDecompile(this.fileName, this.className, decompiler, null));
            disassembleWith.add(decomp);
        }
        menu.add(disassembleWith);
        menu.addSeparator();
        if (currentTransformer instanceof Decompiler) {
            JMenu compileMenu = new JMenu("Compile with");
            for (Compiler compiler : Compiler.getAllCompilers()) {
                JMenuItem compileOption = new JMenuItem(compiler.getName());
                compileOption.setEnabled(true);
                compileMenu.add(compileOption);
            }
            menu.add(compileMenu);
        } else if (currentTransformer instanceof Disassembler) {
            JMenu assembleMenu = new JMenu("Assemble with");
            for (Assembler assembler : Assembler.getAllAssemblers()) {
                JMenuItem assembleOption = new JMenuItem(assembler.getName());
                assembleOption.setEnabled(true);
                assembleMenu.add(assembleOption);
            }
            menu.add(assembleMenu);
        }
        return menu;
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
        public String fileName;
        public String className;
        public String jumpTo;
        public int line;
        public int column;
        public int offset;
        public int offsetEnd;

        public Link(int line, int column, int offset, int offsetEnd) {
            this.line = line;
            this.column = column;
            this.offset = offset;
            this.offsetEnd = offsetEnd;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Link link = (Link) o;

            if (line != link.line) return false;
            if (column != link.column) return false;
            if (offset != link.offset) return false;
            if (offsetEnd != link.offsetEnd) return false;
            if (fileName != null ? !fileName.equals(link.fileName) : link.fileName != null) return false;
            return className != null ? className.equals(link.className) : link.className == null;

        }

        @Override
        public int hashCode() {
            int result = fileName != null ? fileName.hashCode() : 0;
            result = 31 * result + (className != null ? className.hashCode() : 0);
            result = 31 * result + line;
            result = 31 * result + column;
            result = 31 * result + offset;
            result = 31 * result + offsetEnd;
            return result;
        }
    }
}
