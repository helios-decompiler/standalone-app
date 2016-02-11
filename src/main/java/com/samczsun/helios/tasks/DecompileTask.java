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

package com.samczsun.helios.tasks;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.samczsun.helios.Helios;
import com.samczsun.helios.LoadedFile;
import com.samczsun.helios.api.events.Events;
import com.samczsun.helios.api.events.PreDecompileEvent;
import com.samczsun.helios.gui.ClickableSyntaxTextArea;
import com.samczsun.helios.handler.ExceptionHandler;
import com.samczsun.helios.transformers.Transformer;
import com.samczsun.helios.transformers.decompilers.Decompiler;
import com.samczsun.helios.transformers.disassemblers.Disassembler;
import com.strobel.decompiler.ast.Variable;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.javatuples.Pair;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DecompileTask implements Runnable {
    private final String fileName;
    private final String className;
    private final ClickableSyntaxTextArea textArea;
    private final Transformer transformer;

    private CompilationUnit compilationUnit;

    public DecompileTask(String fileName, String className, ClickableSyntaxTextArea textArea, Transformer transformer) {
        this.fileName = fileName;
        this.className = className;
        this.textArea = textArea;
        this.transformer = transformer;
    }

    @Override
    public void run() {
        LoadedFile loadedFile = Helios.getLoadedFile(fileName);
        byte[] classFile = loadedFile.getFiles().get(className);
        PreDecompileEvent event = new PreDecompileEvent(classFile);
        Events.callEvent(event);
        classFile = event.getBytes();
        StringBuilder output = new StringBuilder();
        if (transformer instanceof Decompiler) {
            if (((Decompiler) transformer).decompile(loadedFile.getClassNode(className), classFile, output)) {
                CompilationUnit cu = null;
                try {
                    // parse the file
                    cu = JavaParser.parse(new ByteArrayInputStream(output.toString().getBytes(StandardCharsets.UTF_8)));
                    this.compilationUnit = cu;
                } catch (ParseException e) {
                    ExceptionHandler.handle(e);
                } finally {
                    if (cu != null) {
                        String result = output.toString();
                        result = result.replaceAll("\r*\n", "\n");
                        output = new StringBuilder(result);
                        handle(cu, output.toString());
                    }
                }
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                textArea.setCodeFoldingEnabled(true);
            }
            textArea.setText(output.toString());
        } else if (transformer instanceof Disassembler) {
            if (((Disassembler) transformer).disassembleClassNode(loadedFile.getClassNode(className), classFile,
                    output)) {
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                textArea.setCodeFoldingEnabled(true);
            }
            textArea.setText(output.toString());
        }
    }

    private void handle(CompilationUnit comp, String output) {
        List<Integer> lineSizes = new ArrayList<>();
        for (String s : output.split("\n")) {
            lineSizes.add(s.length());
        }
        comp.accept(
                new VoidVisitorAdapter() {
                    @Override
                    public void visit(MethodDeclaration n, Object arg) {
                        System.out.println(n.getName());
                        if (n.getBody() != null)
                            handle(n.getBody(), lineSizes);
                        super.visit(n, arg);
                    }
                }, null);
    }

    private void handle(TryStmt tryStmt, List<Integer> lineSizes) {
        System.out.println("TryStmt: " + tryStmt);
        handle(tryStmt.getTryBlock(), lineSizes);
        tryStmt.getCatchs().forEach(catchClause -> handle(catchClause.getCatchBlock(), lineSizes));
        if (tryStmt.getFinallyBlock() != null)
            handle(tryStmt.getFinallyBlock(), lineSizes);
    }

    private void handle(BlockStmt blockStmt, List<Integer> lineSizes) {
        System.out.println("BlockStmt: " + blockStmt);
        for (Statement statement : blockStmt.getStmts()) {
            handle(statement, lineSizes);
        }
    }

    private void handle(IfStmt ifStmt, List<Integer> lineSizes) {
        System.out.println("IfStmt: " + ifStmt);
        handle(ifStmt.getCondition(), lineSizes);
        handle(ifStmt.getThenStmt(), lineSizes);
        if (ifStmt.getElseStmt() != null)
            handle(ifStmt.getElseStmt(), lineSizes);
    }

    private void handle(MethodCallExpr methodCallExpr, List<Integer> lineSizes) {
        System.out.println("MethodCallExpression: " + methodCallExpr.getName() + " " + methodCallExpr.getScope() + " " + methodCallExpr.getArgs() + " " + methodCallExpr.getTypeArgs());
        handle(methodCallExpr.getNameExpr(), lineSizes);
        methodCallExpr.getArgs().forEach(expression -> handle(expression, lineSizes));
        if (methodCallExpr.getScope() != null) {
            handle(methodCallExpr.getScope(), lineSizes);
        }
    }

    private void handle(NameExpr nameExpr, List<Integer> lineSizes) {
        System.out.println("NameExpr: " + nameExpr);
        Pair<Integer, Integer> offsets = getOffsets(lineSizes, nameExpr);
        textArea.links.add(new ClickableSyntaxTextArea.Link("", nameExpr.getBeginLine(), nameExpr.getBeginColumn(), offsets.getValue0(), offsets.getValue1()));
    }

    private void handle(ExpressionStmt expressionStmt, List<Integer> lineSizes) {
        System.out.println("ExpressionStmt: " + expressionStmt);
        if (expressionStmt.getExpression() != null) {
            handle(expressionStmt.getExpression(), lineSizes);
        }
    }

    private void handle(ReturnStmt returnStmt, List<Integer> lineSizes) {
        System.out.println("ReturnStmt: " + returnStmt);
        handle(returnStmt.getExpr(), lineSizes);
    }

    private void handle(CastExpr castExpr, List<Integer> lineSizes) {
        System.out.println("CastExpr: " + castExpr);
        handle(castExpr.getExpr(), lineSizes);
    }

    private void handle(ThrowStmt throwStmt, List<Integer> lineSizes) {
        System.out.println("ThrowStmt: " + throwStmt);
        handle(throwStmt.getExpr(), lineSizes);
    }

    private void handle(ObjectCreationExpr objectCreationExpr, List<Integer> lineSizes) {
        System.out.println("ObjectCreationExpr: " + objectCreationExpr);
        handle(objectCreationExpr.getType(), lineSizes);
        if (objectCreationExpr.getArgs() != null)
            objectCreationExpr.getArgs().forEach(expression -> handle(expression, lineSizes));
        if (objectCreationExpr.getAnonymousClassBody() != null)
            objectCreationExpr.getAnonymousClassBody().forEach(bodyDeclaration -> handle(bodyDeclaration, lineSizes));
    }

    private void handle(Node node, List<Integer> lineSizes) {
        System.out.println("Handling " + node.getClass() + " " + node);
        if (node instanceof MethodCallExpr) {
            handle((MethodCallExpr) node, lineSizes);
        } else if (node instanceof TryStmt) {
            handle((TryStmt) node, lineSizes);
        } else if (node instanceof BlockStmt) {
            handle((BlockStmt) node, lineSizes);
        } else if (node instanceof ExpressionStmt) {
            handle((ExpressionStmt) node, lineSizes);
        } else if (node instanceof NameExpr) {
            handle((NameExpr) node, lineSizes);
        } else if (node instanceof IfStmt) {
            handle((IfStmt) node, lineSizes);
        } else if (node instanceof ThisExpr) {
        } else if (node instanceof BinaryExpr) {
        } else if (node instanceof UnaryExpr) {
        } else if (node instanceof FieldAccessExpr) {
        } else if (node instanceof VariableDeclarationExpr) {
        } else if (node instanceof AssignExpr) {
        } else if (node instanceof SynchronizedStmt) {
        } else if (node instanceof BodyDeclaration) {
        } else if (node instanceof NullLiteralExpr) {
        } else if (node instanceof EnclosedExpr) {
        } else if (node instanceof BooleanLiteralExpr) {
        } else if (node instanceof ClassOrInterfaceType) {
        } else if (node instanceof ReturnStmt) {
            handle((ReturnStmt) node, lineSizes);
        } else if (node instanceof ThrowStmt) {
            handle((ThrowStmt) node, lineSizes);
        } else if (node instanceof ObjectCreationExpr) {
            handle((ObjectCreationExpr) node, lineSizes);
        } else if (node instanceof StringLiteralExpr) {
        } else if (node instanceof CastExpr) {
            handle((CastExpr) node, lineSizes);
        } else {
            throw new IllegalArgumentException(node.getClass().getCanonicalName());
        }
    }

    private Pair<Integer, Integer> getOffsets(List<Integer> lineSizes, Node node) {
        int offset = 0;
        for (int i = 0; i < node.getBeginLine() - 1; i++) {
            offset += lineSizes.get(i) + 1;
        }
        offset += node.getBeginColumn() - 1;

        int offsetEnd = 0;
        for (int i = 0; i < node.getEndLine() - 1; i++) {
            offsetEnd += lineSizes.get(i) + 1;
        }
        offsetEnd += node.getEndColumn() - 1;
        return new Pair<>(offset, offsetEnd);
    }
}
