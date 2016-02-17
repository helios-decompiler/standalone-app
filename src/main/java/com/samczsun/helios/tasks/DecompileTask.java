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
import com.github.javaparser.TokenMgrError;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.samczsun.helios.Helios;
import com.samczsun.helios.LoadedFile;
import com.samczsun.helios.WrappedClassNode;
import com.samczsun.helios.api.events.Events;
import com.samczsun.helios.api.events.PreDecompileEvent;
import com.samczsun.helios.gui.ClickableSyntaxTextArea;
import com.samczsun.helios.handler.ExceptionHandler;
import com.samczsun.helios.transformers.Transformer;
import com.samczsun.helios.transformers.decompilers.Decompiler;
import com.samczsun.helios.transformers.disassemblers.Disassembler;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.javatuples.Pair;
import org.objectweb.asm.Type;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DecompileTask implements Runnable {
    private final String fileName;
    private final String className;
    private final ClickableSyntaxTextArea textArea;
    private final Transformer transformer;
    private final String jumpTo;

    private CompilationUnit compilationUnit;
    private List<Integer> lineSizes = new ArrayList<>();

    public DecompileTask(String fileName, String className, ClickableSyntaxTextArea textArea, Transformer transformer, String jumpTo) {
        this.fileName = fileName;
        this.className = className;
        this.textArea = textArea;
        this.transformer = transformer;
        this.jumpTo = jumpTo;
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
                } catch (ParseException | TokenMgrError e) {
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
            if (jumpTo != null)
                Helios.getGui().getShell().getDisplay().asyncExec(() -> {
                    Helios.getGui().getClassManager().search(jumpTo);
                });
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
        for (String s : output.split("\n")) {
            lineSizes.add(s.length());
        }
        comp.accept(
                new VoidVisitorAdapter<Node>() {
                    @Override
                    public void visit(CompilationUnit n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(PackageDeclaration n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ImportDeclaration n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(TypeParameter n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(LineComment n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(BlockComment n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ClassOrInterfaceDeclaration n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(EnumDeclaration n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(EmptyTypeDeclaration n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(EnumConstantDeclaration n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(AnnotationDeclaration n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(AnnotationMemberDeclaration n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(FieldDeclaration n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(VariableDeclarator n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(VariableDeclaratorId n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ConstructorDeclaration n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(MethodDeclaration n, Node arg) {
                        System.out.println("METHOD: " + n.getType() + " " + n.getType().getClass());
                        super.visit(n, n);
                    }

                    @Override
                    public void visit(Parameter n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(MultiTypeParameter n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(EmptyMemberDeclaration n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(InitializerDeclaration n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(JavadocComment n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ClassOrInterfaceType n, Node arg) {
                        recursivelyHandleNameExpr(n);
                        super.visit(n, n);
                    }

                    @Override
                    public void visit(PrimitiveType n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ReferenceType n, Node arg) {
                        super.visit(n, n);
                    }

                    @Override
                    public void visit(VoidType n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(WildcardType n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(UnknownType n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ArrayAccessExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ArrayCreationExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ArrayInitializerExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(AssignExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(BinaryExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(CastExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ClassExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ConditionalExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(EnclosedExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(FieldAccessExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(InstanceOfExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(StringLiteralExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(IntegerLiteralExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(LongLiteralExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(IntegerLiteralMinValueExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(LongLiteralMinValueExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(CharLiteralExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(DoubleLiteralExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(BooleanLiteralExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(NullLiteralExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(MethodCallExpr n, Node arg) {
                        visit(n.getNameExpr(), n);
                        super.visit(n, n);
                    }

                    @Override
                    public void visit(NameExpr n, Node arg) {
                        System.out.println("Visiting nameexpr with type " + (arg == null ? null : arg.getClass()));
                        System.out.println(n + " " + arg);
                        if (arg instanceof MethodCallExpr) {
                            recursivelyHandleNameExpr((MethodCallExpr) arg, n);
                        }
                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ObjectCreationExpr n, Node arg) {
                        super.visit(n, n);
                    }

                    @Override
                    public void visit(QualifiedNameExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ThisExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(SuperExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(UnaryExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(VariableDeclarationExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(MarkerAnnotationExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(SingleMemberAnnotationExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(NormalAnnotationExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(MemberValuePair n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ExplicitConstructorInvocationStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(TypeDeclarationStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(AssertStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(BlockStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(LabeledStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(EmptyStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ExpressionStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(SwitchStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(SwitchEntryStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(BreakStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ReturnStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(IfStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(WhileStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ContinueStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(DoStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ForeachStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ForStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ThrowStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(SynchronizedStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(TryStmt n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(CatchClause n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(LambdaExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(MethodReferenceExpr n, Node arg) {

                        super.visit(n, n);
                    }

                    @Override
                    public void visit(TypeExpr n, Node arg) {

                        super.visit(n, n);
                    }
                }, null);
    }

    private String recursivelyHandleNameExpr(MethodCallExpr methodCallExpr, NameExpr nameExpr) {
        //System.out.println("NameExpr: " + nameExpr);
        //System.out.println("Parent is " + methodCallExpr);
        Pair<Integer, Integer> offsets = getOffsets(lineSizes, nameExpr);
        ClickableSyntaxTextArea.Link link = new ClickableSyntaxTextArea.Link(nameExpr.getBeginLine(), nameExpr.getBeginColumn(), offsets.getValue0(), offsets.getValue1());
        String className = null;
        System.out.println("Scope is " + (methodCallExpr.getScope() == null ? null : methodCallExpr.getScope().getClass()));
        if (methodCallExpr.getScope() instanceof NameExpr) {
            for (ImportDeclaration importDeclaration : compilationUnit.getImports()) {
                if (importDeclaration.getName().getName().equals(methodCallExpr.getScope().toString())) {
                    className = importDeclaration.getName().toString();
                    break;
                }
            }
            if (className == null) {
                String thisClassName = this.className;
                if (thisClassName.lastIndexOf('/') != -1) {
                    thisClassName = thisClassName.substring(thisClassName.lastIndexOf('/') + 1, thisClassName.length()).replace(".class", "");
                }
                //System.out.println("THISCLASSNAME  " + thisClassName);
                if (((NameExpr) methodCallExpr.getScope()).getName().equals(thisClassName)) {
                    className = this.className.replace('/', '.').replace(".class", "");
                } else {
                    className = "java.lang." + methodCallExpr.getScope().toString();
                }
            }
        } else if (methodCallExpr.getScope() instanceof MethodCallExpr) {
            className = recursivelyHandleNameExpr((MethodCallExpr) methodCallExpr.getScope(), ((MethodCallExpr) methodCallExpr.getScope()).getNameExpr());
        } else if (methodCallExpr.getScope() == null) {
            className = this.className.replace('/', '.').replace(".class", "");
        } else if (methodCallExpr.getScope() instanceof ThisExpr && methodCallExpr.getNameExpr() == nameExpr) {
            className = this.className.replace('/', '.').replace(".class", "");
        }
        System.out.println("ClassName for " + nameExpr + " is : " + className);
        if (className != null) {
            String fileName = className.replace('.', '/') + ".class";
            LoadedFile file = Helios.getLoadedFile(this.fileName);
            if (file.getData().get(fileName) != null) {
                System.out.println("Yes for this");
                link.fileName = file.getName();
                link.className = fileName;
                link.jumpTo = " " + nameExpr.getName() + "(";
                textArea.links.add(link);
                try {
                    String internalName = className.replace('.', '/');
                    WrappedClassNode classNode = file.getEmptyClasses().get(internalName);
                    if (classNode != null) {
                        Type returnType = Type.getType(classNode.getClassNode().methods.stream().filter(mn -> mn.name.equals(methodCallExpr.getName())).findFirst().orElse(null).desc);
                        return returnType.getReturnType().getInternalName().replace('/', '.');
                    } else {
                        System.out.println("Could not find class " + internalName);
                        return null;
                    }
                } catch (Exception e) {
//                    e.printStackTrace(System.out);
                }
            } else {
                Set<LoadedFile> check = new HashSet<>();
                check.addAll(Helios.getAllFiles());
                check.addAll(Helios.getPathFiles().values());
                for (LoadedFile loadedFile : check) {
                    System.out.println("Checking " + loadedFile.getName());
                    if (loadedFile.getData().get(fileName) != null) {
                        System.out.println("YES");
                        link.fileName = loadedFile.getName();
                        link.className = fileName;
                        link.jumpTo = " " + nameExpr.getName() + "(";
                        textArea.links.add(link);

                        try {
                            String internalName = className.replace('.', '/');
                            WrappedClassNode classNode = loadedFile.getEmptyClasses().get(internalName);
                            //System.out.println("Looking for method with name " + methodCallExpr.getName() + " in " + internalName);
                            //System.out.println(classNode.getClassNode().name);
                            Type returnType = Type.getType(classNode.getClassNode().methods.stream().filter(mn -> mn.name.equals(methodCallExpr.getName())).findFirst().orElse(null).desc);
                            return returnType.getReturnType().getInternalName().replace('/', '.');
                        } catch (Exception e) {
//                            e.printStackTrace(System.out);
                        }
                    }
                }
            }
        }
        return null;
    }

    private String recursivelyHandleNameExpr(ClassOrInterfaceType classOrInterfaceType) {
        Pair<Integer, Integer> offsets = getOffsets(lineSizes, classOrInterfaceType);
        ClickableSyntaxTextArea.Link link = new ClickableSyntaxTextArea.Link(classOrInterfaceType.getBeginLine(), classOrInterfaceType.getBeginColumn(), offsets.getValue0(), offsets.getValue1());
        StringBuilder fullNameBuilder = new StringBuilder();
        {
            ClassOrInterfaceType type = classOrInterfaceType;
            while (type != null) {
                fullNameBuilder.insert(0, ".");
                fullNameBuilder.insert(0, type.getName());
                type = type.getScope();
            }
            fullNameBuilder.setLength(fullNameBuilder.length() - 1);
        }
        String className = null;
        if (fullNameBuilder.toString().indexOf('.') != -1) {
            className = fullNameBuilder.toString();
        } else {
            for (ImportDeclaration importDeclaration : compilationUnit.getImports()) {
                if (importDeclaration.getName().getName().equals(classOrInterfaceType.getName())) {
                    className = importDeclaration.getName().toString();
                    break;
                }
            }
        }
        if (className == null) {
            className = "java.lang." + classOrInterfaceType.getName();
        }
        System.out.println("ClassName for coitype " + classOrInterfaceType + " is : " + className);
        //System.out.println("CLASS IS " + className);
        String fileName = className.replace('.', '/') + ".class";
        //System.out.println(fileName);
        LoadedFile file = Helios.getLoadedFile(this.fileName);
        if (file.getData().get(fileName) != null) {
            //System.out.println("Yes for this");
            link.fileName = file.getName();
            link.className = fileName;
            link.jumpTo = " " + classOrInterfaceType.getName() + " ";
            textArea.links.add(link);
            try {
                String internalName = className.replace('.', '/');
                WrappedClassNode classNode = file.getEmptyClasses().get(internalName);
                if (classNode != null) {
                    Type returnType = Type.getType(classNode.getClassNode().methods.stream().filter(mn -> mn.name.equals(classOrInterfaceType.getName())).findFirst().orElse(null).desc);
                    return returnType.getReturnType().getInternalName().replace('/', '.');
                } else {
                    System.out.println("Could not find class " + internalName);
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        } else {
            Set<LoadedFile> check = new HashSet<>();
            check.addAll(Helios.getAllFiles());
            check.addAll(Helios.getPathFiles().values());
            for (LoadedFile loadedFile : check) {
                //System.out.println("Checking " + loadedFile.getName() + " for " + fileName);
                if (loadedFile.getData().get(fileName) != null) {
                    //System.out.println("YES");
                    link.fileName = loadedFile.getName();
                    link.className = fileName;
                    link.jumpTo = " " + classOrInterfaceType.getName() + " ";
                    textArea.links.add(link);

                    try {
                        String internalName = className.replace('.', '/');
                        WrappedClassNode classNode = loadedFile.getEmptyClasses().get(internalName);
                        //System.out.println("Looking for method with name " + classOrInterfaceType.getName() + " in " + internalName);
                        //System.out.println(classNode.getClassNode().name);
                        Type returnType = Type.getType(classNode.getClassNode().methods.stream().filter(mn -> mn.name.equals(classOrInterfaceType.getName())).findFirst().orElse(null).desc);
                        return returnType.getReturnType().getInternalName().replace('/', '.');
                    } catch (Exception e) {
//                            e.printStackTrace(System.out);
                    }
                }
            }
        }
        return null;
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
