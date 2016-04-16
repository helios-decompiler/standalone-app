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
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.samczsun.helios.Helios;
import com.samczsun.helios.LoadedFile;
import com.samczsun.helios.WrappedClassNode;
import com.samczsun.helios.api.events.Events;
import com.samczsun.helios.api.events.PreDecompileEvent;
import com.samczsun.helios.api.events.requests.SearchRequest;
import com.samczsun.helios.gui.ClickableSyntaxTextArea;
import com.samczsun.helios.transformers.Transformer;
import com.samczsun.helios.transformers.decompilers.CFRDecompiler;
import com.samczsun.helios.transformers.decompilers.Decompiler;
import com.samczsun.helios.transformers.disassemblers.Disassembler;
import org.apache.commons.io.output.StringBuilderWriter;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.javatuples.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DecompileTask implements Runnable {
    private final String fileName;
    private final String className;
    private final String simpleName;
    private final String packageName;
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

        String simpleName = this.className;
        int lastIndex;
        if ((lastIndex = simpleName.lastIndexOf('/')) != -1) {
            simpleName = simpleName.substring(lastIndex + 1, simpleName.length());
            simpleName = simpleName.substring(0, simpleName.length() - 6);
        }
        this.simpleName = simpleName;

        String packageName = this.className;
        if ((lastIndex = packageName.lastIndexOf('/')) != -1) {
            packageName = packageName.substring(0, lastIndex);
        } else {
            packageName = null;
        }
        this.packageName = packageName;
    }

    @Override
    public void run() {
        LoadedFile loadedFile = Helios.getLoadedFile(fileName);
        byte[] classFile = loadedFile.getFiles().get(className);
        PreDecompileEvent event = new PreDecompileEvent(transformer, classFile);
        Events.callEvent(event);
        classFile = event.getBytes();
        StringBuilder output = new StringBuilder();
        if (transformer instanceof Decompiler) {
            if (((Decompiler) transformer).decompile(loadedFile.getClassNode(className), classFile, output)) {
                CompilationUnit cu = null;
                try {
                    cu = JavaParser.parse(new ByteArrayInputStream(output.toString().getBytes(StandardCharsets.UTF_8)), "UTF-8", false);
                    this.compilationUnit = cu;
                } catch (ParseException | TokenMgrError e) {
                    StringBuilder message = new StringBuilder("/*\n");
                    Consumer<String> write = msg -> {
                        message.append(" * ").append(msg).append("\n");
                    };
                    write.accept("Error: Helios could not parse this file. Hyperlinks will not be inserted");
                    write.accept("The error has been inserted at the bottom of the output");
                    if (transformer instanceof CFRDecompiler) {
                        write.accept("");
                        write.accept("I noticed you are using CFR");
                        write.accept("Try nagging the author to output valid Java even if the code is undecompilable");
                    }
                    message.append(" */\n\n");
                    output.insert(0, message.toString());

                    message.setLength(0);
                    message.append("\n/*\n");
                    StringBuilder exceptionToString = new StringBuilder();
                    e.printStackTrace(new PrintWriter(new StringBuilderWriter(exceptionToString)));
                    String[] lines = exceptionToString.toString().split("\r*\n");
                    for (String line : lines) {
                        write.accept(line);
                    }
                    message.append(" */");
                    output.append(message.toString());
                } finally {
                    if (cu != null && false) {
                        String result = output.toString();
                        result = result.replaceAll("\r*\n", "\n");
                        output = new StringBuilder(result);
                        try {
                            handle(cu, output.toString());
                        } catch (Throwable t) {
                            textArea.links.clear();
                            StringBuilder message = new StringBuilder("/*\n");
                            Consumer<String> write = msg -> {
                                message.append(" * ").append(msg).append("\n");
                            };
                            write.accept("Error: Helios could not parse this file. Hyperlinks will not be inserted");
                            write.accept("The error has been inserted at the bottom of the output");
                            message.append(" */\n\n");
                            output.insert(0, message.toString());

                            message.setLength(0);
                            message.append("\n/*\n");
                            StringBuilder exceptionToString = new StringBuilder();
                            t.printStackTrace(new PrintWriter(new StringBuilderWriter(exceptionToString)));
                            String[] lines = exceptionToString.toString().split("\r*\n");
                            for (String line : lines) {
                                write.accept(line);
                            }
                            message.append(" */");
                            output.append(message.toString());
                            t.printStackTrace();
                        }
                    }
                }
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                textArea.setCodeFoldingEnabled(true);
            }
            textArea.setText(output.toString());
            if (jumpTo != null) {
                Events.callEvent(new SearchRequest(jumpTo));
            }
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
        imports:
        for (ImportDeclaration decl : comp.getImports()) {
            String fullName = decl.getName().toString();
            if (fullName.endsWith("*")) continue; //Ignore wildcard imports
            String internalName = fullName.replace('.', '/');
            Set<LoadedFile> check = new HashSet<>();
            check.addAll(Helios.getAllFiles());
            check.addAll(Helios.getPathFiles().values());
            for (LoadedFile loadedFile : check) {
                if (loadedFile.getData().containsKey(internalName + ".class")) {
                    Pair<Integer, Integer> offsets = getOffsets(lineSizes, decl.getName());
                    ClickableSyntaxTextArea.Link link = new ClickableSyntaxTextArea.Link(decl.getName().getBeginLine(), decl.getName().getBeginColumn(), offsets.getValue0(), offsets.getValue1());
                    link.fileName = loadedFile.getName();
                    link.className = internalName + ".class";
                    link.jumpTo = "";
                    textArea.links.add(link);
                    continue imports;
                }
            }
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
                        recursivelyHandleNameExpr(n, n.getNameExpr(), 0);
                        visit(n.getNameExpr(), n);
                        super.visit(n, n);
                    }

                    @Override
                    public void visit(ClassOrInterfaceType n, Node arg) {
                        handleClassOrInterfaceType(n);
                        super.visit(n, n);
                    }

                    @Override
                    public void visit(NameExpr n, Node arg) {
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

    private void print(int d, String msg) {
        String space = "";
        for (int i = 0; i < d; i++) {
            space += "  ";
        }
        System.out.println(space + msg);
    }

    private List<String> generatePossibilities(ClassOrInterfaceType type) {
        List<String> possibilities = new ArrayList<>();

        /*
         * This is the ClassOrInterfaceType as the entire string.
         * Eg. java.lang.System or System
         */
        String fullName = type.toString();
        /*
         * This will only be one word.
         * Eg. System
         */
        String simpleName = type.getName();
        /*
         * If the fullName is something like Outer.Inner, asInnerClass will be Outer$Inner
         *
         * However, if the fullName is a FQN, then asInnerClass will not be valid
         * Eg, java$lang$System
         */
        String asInnerClass = fullName.replace('.', '$');

        for (ImportDeclaration importDeclaration : compilationUnit.getImports()) {
            if (importDeclaration.isAsterisk()) {
                String fullImport = importDeclaration.getName().toString();
                String internalName = fullImport.replace('.', '/');
                possibilities.add(internalName + "/" + asInnerClass + ".class");
            } else if (importDeclaration.isStatic()) {

            } else {
                NameExpr importName = importDeclaration.getName();
                if (importName.getName().equals(simpleName)) {
                    String javaName = importDeclaration.getName().toString();
                    String internalName = javaName.replace('.', '/');
                    possibilities.add(internalName + ".class");
                }
            }
        }

        /*
         * We must consider Fully Qualified Names
         * Therefore the string java.lang.System could be java$lang$System, java.lang$System, or java.lang.System
         */
        String[] split = fullName.split("\\.");
        for (int i = 0; i < split.length; i++) {
            StringBuilder builder = new StringBuilder();
            for (int start = 0; start < i; start++) {
                builder.append(split[start]).append("/");
            }
            for (int start = i; start < split.length; start++) {
                builder.append(split[start]).append("$");
            }
            if (builder.length() > 0 && (builder.charAt(builder.length() - 1) == '$' || builder.charAt(builder.length() - 1) == '/')) {
                builder.setLength(builder.length() - 1);
            }
            builder.append(".class");
            possibilities.add(builder.toString());
        }

        return possibilities;
    }

    private String recursivelyHandleNameExpr(MethodCallExpr methodCallExpr, NameExpr nameExpr, int depth) { //fixme generics
        if (methodCallExpr.getNameExpr() != nameExpr) return null;
        print(depth, "RHNE " + methodCallExpr + " " + nameExpr);
        print(depth, "Scope is " + ((methodCallExpr.getScope() == null) ? null : methodCallExpr.getScope().getClass()) + " " + methodCallExpr.getScope());
        Pair<Integer, Integer> offsets = getOffsets(lineSizes, nameExpr);
        ClickableSyntaxTextArea.Link link = new ClickableSyntaxTextArea.Link(nameExpr.getBeginLine(), nameExpr.getBeginColumn(), offsets.getValue0(), offsets.getValue1());

        Set<String> possibleClassNames = new HashSet<>();

        if (methodCallExpr.getScope() instanceof NameExpr || methodCallExpr.getScope() instanceof ArrayAccessExpr) {
            Node tmp = methodCallExpr.getScope();

            if (tmp instanceof ArrayAccessExpr) {
                ArrayAccessExpr expr = (ArrayAccessExpr) tmp;
                tmp = expr.getName(); //todo could be other than nameexpr
            }

            /*
             * Cases:
             * Static method
             *   SomeClass.someStaticMethod()
             * Variable
             *   myVar.someVirtualMethod()
             * Field
             *   field.someVirtualMethod()
             */
            Node fnode = tmp;
            NameExpr scopeExpr = (NameExpr) tmp;
            String scope = scopeExpr.toString();
            if (scope.contains(".")) {
                throw new IllegalArgumentException("Was not expecting '.' in " + scope);
            }

            /*
             * In Java, variables have priority
             * Therefore, something like this
             *
             * Object Integer = null;
             * Integer.parseInt("4");
             *
             * would fail
             */

            Node node = methodCallExpr.getParentNode();
            List<com.github.javaparser.ast.type.Type> ref = new ArrayList<>();
            List<Node> parentChain = new ArrayList<>();
            Node tmpNode = node;
            while (tmpNode != null) {
                parentChain.add(tmpNode);
                tmpNode = tmpNode.getParentNode();
            }
            while (ref.size() == 0 && node != null) {
                print(depth, "Trying to find localvar in " + node.getClass());
                node.accept(
                        new VoidVisitorAdapter<Node>() {
                            @Override
                            public void visit(VariableDeclarationExpr n, Node arg) {
                                boolean equals = false;
                                for (VariableDeclarator var : n.getVars()) {
                                    if (var.getId().getName().equals(scopeExpr.getName())) {
                                        equals = true;
                                    }
                                }
                                if (equals) {
                                    print(depth, "Found VariableDeclarationExpr " + n);
                                    print(depth, "This is it! Type is " + n.getType());
                                    ref.add(n.getType());
                                }
                                super.visit(n, n);
                            }

                            @Override
                            public void visit(MultiTypeParameter n, Node arg) {
                                if (n.getId().getName().equals(((NameExpr) fnode).getName())) {
                                    print(depth, "Found VariableDeclarationExpr " + n);
                                    print(depth, "This is it! Type is " + n.getTypes());
                                    ref.addAll(n.getTypes());
                                }
                            }

                            @Override
                            public void visit(Parameter n, Node arg) {
                                if (n.getId().getName().equals(((NameExpr) fnode).getName())) {
                                    print(depth, "Found Parameter " + n);
                                    print(depth, "This is it! Type is " + n.getType());
                                    ref.add(n.getType());
                                }
                            }

                            @Override
                            public void visit(BlockStmt n, Node arg) {
                                if (parentChain.contains(n)) {
                                    super.visit(n, n);
                                }
                            }
                        }, null);
                if (node instanceof BodyDeclaration) {
                    // We don't want to check for variables outside of this method. That would be a field
                    break;
                }
                node = node.getParentNode();
            }
            if (ref.size() > 0) {
                if (ref.size() > 1) {
                    throw new IllegalArgumentException("Was not expecting more than one localvar " + ref);
                }
                com.github.javaparser.ast.type.Type type = ref.get(0); //fixme check all
                while (type instanceof ReferenceType) {
                    type = ((ReferenceType) type).getType();
                }
                print(depth, "Final type is " + type.getClass() + " " + type);
                if (type instanceof ClassOrInterfaceType) {
                    ClassOrInterfaceType coit = (ClassOrInterfaceType) type;
                    possibleClassNames.addAll(generatePossibilities(coit));
                    possibleClassNames.add("java/lang/" + coit.getName() + ".class");
                    if (packageName != null) {
                        possibleClassNames.add(packageName + "/" + coit.getName() + ".class");
                    }
                } else {
                    throw new IllegalArgumentException("Got unexpected type " + type.getClass());
                }
            }

            /*
             * Check for static method invocation
             * If this class was called "Test" we want to check for
             *
             * Test.staticMethod();
             */

            print(depth, "Simple name is " + simpleName);
            if (scopeExpr.getName().equals(simpleName)) {
                possibleClassNames.add(this.className);
            }

            /*
             * Finally, check imports
             */
            for (ImportDeclaration importDeclaration : compilationUnit.getImports()) {
                if (importDeclaration.isAsterisk()) {
                    String fullImport = importDeclaration.getName().toString();
                    String internalName = fullImport.replace('.', '/');
                    possibleClassNames.add(internalName + "/" + scope + ".class");
                } else if (importDeclaration.isStatic()) {

                } else {
                    NameExpr importName = importDeclaration.getName();
                    if (importName.getName().equals(scope)) {
                        String javaName = importDeclaration.getName().toString();
                        String internalName = javaName.replace('.', '/');
                        possibleClassNames.add(internalName + ".class");
                    }
                }
            }

            /*
             * java.lang.* classes don't need to be imported
             * Add it just in case
             */
            possibleClassNames.add("java/lang/" + scope + ".class");

            FieldAccessExpr expr = new FieldAccessExpr(null, scope);
            Set<String> owners = handleFieldExpr(expr, className, depth);
            possibleClassNames.addAll(owners);

            /*
             * Classes in the current package don't need to be imported
             * Add it just in case
             */
            if (packageName != null) {
                possibleClassNames.add(packageName + "/" + scope + ".class");
            }
        } else if (methodCallExpr.getScope() instanceof MethodCallExpr) {
            /*
             * Recursively handle the chained method. The return should be the class name we want
             */
            possibleClassNames.add(recursivelyHandleNameExpr((MethodCallExpr) methodCallExpr.getScope(), ((MethodCallExpr) methodCallExpr.getScope()).getNameExpr(), depth + 1));
        } else if (methodCallExpr.getScope() == null) {
            /*
             * Another way of calling a static/virtual method within the same class.
             *
             * someStaticMethod();
             */
            possibleClassNames.add(this.className);
        } else if (methodCallExpr.getScope() instanceof ThisExpr) {
            /*
             * Another way of calling a static/virtual method within the same class
             *
             * this.someVirtualMethod();
             *
             * fixme what about Outer.this.method();
             */
            possibleClassNames.add(this.className);
        } else if (methodCallExpr.getScope() instanceof SuperExpr) {
            /*
             * Calling a super method
             *
             * super.someVirtualMethod();
             */
            LoadedFile loadedFile = Helios.getLoadedFile(fileName);
            ClassNode node = loadedFile.getClassNode(this.className);
            possibleClassNames.add(node.superName);
        } else if (methodCallExpr.getScope() instanceof EnclosedExpr) {
            /*
             * fixme We could be missing CastExprs elsewhere but it's unlikely
             *
             * EnclosedExpr represents an expression surrounded by brackets
             * It's assumed that there may be a cast within
             *
             * ((String) obj).toCharArray();
             */
            EnclosedExpr enclosedExpr = (EnclosedExpr) methodCallExpr.getScope();
            if (enclosedExpr.getInner() instanceof CastExpr) {
                CastExpr castExpr = (CastExpr) enclosedExpr.getInner();
                com.github.javaparser.ast.type.Type type = castExpr.getType();
                while (type instanceof ReferenceType) {
                    type = ((ReferenceType) type).getType();
                }
                if (type instanceof ClassOrInterfaceType) {
                    ClassOrInterfaceType coit = (ClassOrInterfaceType) type;
                    possibleClassNames.addAll(handleClassOrInterfaceType(coit));
                } else {
                    throw new IllegalArgumentException("Got unexpected type " + type.getClass());
                }
            }
        } else if (methodCallExpr.getScope() instanceof FieldAccessExpr) { // Handle fields
            /*
             * Could either be a field OR a FQN
             *
             * System.out.println(); -> System.out is the FieldAccessExpr
             *
             * java.lang.System.out.println(); -> java.lang.System.out is the FieldAccessExpr
             */

            FieldAccessExpr expr = (FieldAccessExpr) methodCallExpr.getScope();

            String left = expr.getScope().toString();
            Set<String> possible;
            if (left.equals("this")) {
                possible = new HashSet<>();
                possible.add(className);
            } else {
                ClassOrInterfaceType type = new ClassOrInterfaceType(left);
                type.setBeginLine(expr.getScope().getBeginLine());
                type.setEndLine(expr.getScope().getEndLine());
                type.setBeginColumn(expr.getScope().getBeginColumn());
                type.setEndColumn(expr.getScope().getEndColumn());
                possible = handleClassOrInterfaceType(type);
            }

            if (possible.size() > 0) { // Maybe field
                print(depth, "FieldAccessExpr field: " + expr.getScope() + " " + expr.getField() + " " + expr.getScope().getClass() + " " + possible);
                for (String p : possible) {
                    Set<String> types = handleFieldExpr(expr, p, depth);
                    possibleClassNames.addAll(types);
                }
            } else {
                ClassOrInterfaceType type = new ClassOrInterfaceType(expr.toString());
                type.setBeginLine(expr.getBeginLine());
                type.setEndLine(expr.getEndLine());
                type.setBeginColumn(expr.getBeginColumn());
                type.setEndColumn(expr.getEndColumn());
                possible = handleClassOrInterfaceType(type);
                if (possible.size() == 0) {
                    print(depth, "Error: Could not parse FieldAccessExpr");
                } else {
                    print(depth, "FieldAccessExpr fqn: " + expr.getScope() + " " + expr.getField() + " " + expr.getScope().getClass() + " " + possible);
                    possibleClassNames.addAll(possible);
                }
            }
        } else if (methodCallExpr.getScope() instanceof ArrayAccessExpr) {
            /*
             * somearray[index].method()
             */
        } else if (methodCallExpr.getScope() instanceof ObjectCreationExpr) {
            /*
             * new Object().method()
             */
            ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr) methodCallExpr.getScope();
            possibleClassNames.addAll(handleClassOrInterfaceType(objectCreationExpr.getType()));
        }

        print(depth, possibleClassNames.toString());
        Map<String, LoadedFile> mapping = possibleClassNames.stream().map(name -> new AbstractMap.SimpleEntry<>(name, getFileFor(name)))
                .filter(ent -> ent.getValue() != null)
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

        if (mapping.size() == 0) {
            print(depth, "Error: Could not find classname");
        } else if (mapping.size() > 1) {
            print(depth, "Error: More than one classname found: " + mapping.keySet()); //fixme filter by which one contains the method
        } else {
            print(depth, "ClassName is " + mapping.keySet());
            String className = mapping.keySet().iterator().next();
            String internalName = className.substring(0, className.length() - 6);

            try {
                while (true) {
                    LoadedFile readFrom = null;

                    String fileName = internalName + ".class";
                    LoadedFile file = Helios.getLoadedFile(this.fileName);
                    if (file.getData().get(fileName) != null) {
                        readFrom = file;
                    } else {
                        Set<LoadedFile> check = new HashSet<>();
                        check.addAll(Helios.getAllFiles());
                        check.addAll(Helios.getPathFiles().values());
                        for (LoadedFile loadedFile : check) {
                            if (loadedFile.getData().get(fileName) != null) {
                                readFrom = loadedFile;
                                break;
                            }
                        }
                    }
                    if (readFrom != null) {
                        print(depth, "Found in " + readFrom.getName());
                        link.fileName = readFrom.getName();
                        link.className = fileName;
                        link.jumpTo = " " + nameExpr.getName() + "(";
                        textArea.links.add(link);

                        WrappedClassNode classNode = readFrom.getEmptyClasses().get(internalName);
                        print(depth, "Looking for method with name " + methodCallExpr.getName() + " in " + internalName + " " + classNode);
                        MethodNode node = classNode.getClassNode().methods.stream().filter(mn -> mn.name.equals(methodCallExpr.getName())).findFirst().orElse(null);
                        if (node != null) {
                            link.className = internalName + ".class";
                            Type returnType = Type.getType(node.desc);
                            if (returnType.getReturnType().getSort() == Type.OBJECT) {
                                print(depth, "Found method with return type " + returnType);
                                return returnType.getReturnType().getInternalName() + ".class";
                            } else if (returnType.getReturnType().getSort() == Type.ARRAY) {
                                return "java/lang/Object.class";
                            } else {
                                return null;
                            }
                        } else {
                            print(depth, "Could not find methodnode " + methodCallExpr.getName());
                        }
                        if (internalName.equals("java/lang/Object")) {
                            break;
                        }
                        internalName = classNode.getClassNode().superName;
                    } else {
                        print(depth, "Could not find readfrom ");
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
        return null;
    }

    private Map<ClassOrInterfaceType, Set<String>> handled = new IdentityHashMap<>();
    private Map<ClassOrInterfaceType, String> tostring = new HashMap<>();

    private Function<ClassOrInterfaceType, String> toStringComputer = type -> {
        StringBuilder fullNameBuilder = new StringBuilder();
        while (type != null) {
            fullNameBuilder.insert(0, ".");
            fullNameBuilder.insert(0, type.getName());
            type = type.getScope();
        }
        fullNameBuilder.setLength(fullNameBuilder.length() - 1);
        return fullNameBuilder.toString();
    };

    private Set<String> handleFieldExpr(FieldAccessExpr fieldAccessExpr, String owner, int depth) {
        Set<String> types = new HashSet<>();

        Pair<Integer, Integer> offsets = getOffsets(lineSizes, fieldAccessExpr);
        ClickableSyntaxTextArea.Link link = new ClickableSyntaxTextArea.Link(fieldAccessExpr.getBeginLine(), fieldAccessExpr.getBeginColumn(), offsets.getValue0(), offsets.getValue1());

        String className = owner;
        String internalName = className.substring(0, className.length() - 6);

        try {
            while (true) {
                LoadedFile readFrom = null;

                String fileName = internalName + ".class";
                LoadedFile file = Helios.getLoadedFile(this.fileName);
                if (file.getData().get(fileName) != null) {
                    readFrom = file;
                } else {
                    Set<LoadedFile> check = new HashSet<>();
                    check.addAll(Helios.getAllFiles());
                    check.addAll(Helios.getPathFiles().values());
                    for (LoadedFile loadedFile : check) {
                        if (loadedFile.getData().get(fileName) != null) {
                            readFrom = loadedFile;
                            break;
                        }
                    }
                }
                if (readFrom != null) {
                    print(depth, "Found in " + readFrom.getName());
                    link.fileName = readFrom.getName();
                    link.className = fileName;
                    link.jumpTo = " " + fieldAccessExpr.getField();
                    textArea.links.add(link);

                    WrappedClassNode classNode = readFrom.getEmptyClasses().get(internalName);
                    print(depth, "Looking for field with name " + fieldAccessExpr.getField() + " in " + internalName + " " + classNode);
                    List<FieldNode> fields = classNode.getClassNode().fields.stream().filter(f -> f.name.equals(fieldAccessExpr.getField())).collect(Collectors.toList());
                    if (fields.size() > 0) {
                        link.className = internalName + ".class";
                        for (FieldNode fieldNode : fields) {
                            Type type = Type.getType(fieldNode.desc);
                            types.add(type.getInternalName() + ".class");
                        }
                        return types;
                    } else {
                        print(depth, "Could not find field " + fieldAccessExpr.getField());
                    }
                    if (internalName.equals("java/lang/Object")) {
                        break;
                    }
                    internalName = classNode.getClassNode().superName;
                } else {
                    print(depth, "Could not find readfrom ");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        return types;
    }

    private Set<String> handleClassOrInterfaceType(ClassOrInterfaceType classOrInterfaceType) {
        Set<String> result = handled.get(classOrInterfaceType);
        if (result != null) {
            return result;
        }
        result = new HashSet<>();
        handled.put(classOrInterfaceType, result);

        System.out.println("Handling ClassOrInterfaceType " + classOrInterfaceType + " on line " + classOrInterfaceType.getBeginLine());

        /*
         * Possibilities:
         * Simple name: System
         * -> Could be imported direcly
         * -> Could be imported using wildcard
         * -> Could be package-local
         * -> Could be java.lang
         * Inner class (Java): System.Inner
         * Inner class (Internal): System$Inner
         * Fully Qualified Name (Java): java.lang.System
         * Fully Qualified Name with inner (Java): java.lang.System.Inner
         * Fully Qualified Name with inner (internal): java.lang.System$Inner
         */
        Pair<Integer, Integer> offsets = getOffsets(lineSizes, classOrInterfaceType);
        ClickableSyntaxTextArea.Link link = new ClickableSyntaxTextArea.Link(classOrInterfaceType.getBeginLine(), classOrInterfaceType.getBeginColumn(), offsets.getValue0(), offsets.getValue1());

        /* Could be any of the above possibilities */
        String fullName = tostring.computeIfAbsent(classOrInterfaceType, toStringComputer);

        Set<String> allPossibilitiesWithoutImports = new HashSet<>();
        String[] split = fullName.split("\\.");
        for (int i = 0; i < split.length; i++) {
            StringBuilder builder = new StringBuilder();
            for (int start = 0; start < i; start++) {
                builder.append(split[start]).append("/");
            }
            for (int start = i; start < split.length; start++) {
                builder.append(split[start]).append("$");
            }
            if (builder.length() > 0 && (builder.charAt(builder.length() - 1) == '$' || builder.charAt(builder.length() - 1) == '/')) {
                builder.setLength(builder.length() - 1);
            }
            builder.append(".class");
            allPossibilitiesWithoutImports.add(builder.toString());
        }

        Set<String> possibleClassNames = new HashSet<>();
        possibleClassNames.addAll(allPossibilitiesWithoutImports);

        for (ImportDeclaration importDeclaration : compilationUnit.getImports()) {
            String javaName = importDeclaration.getName().toString();
            if (importDeclaration.isAsterisk()) {
                String fullImport = importDeclaration.getName().toString();
                String internalName = fullImport.replace('.', '/');
                for (String name : allPossibilitiesWithoutImports) {
                    possibleClassNames.add(internalName + "/" + name);
                }
            } else if (importDeclaration.isStatic()) {

            } else {
                for (String name : allPossibilitiesWithoutImports) {
                    String nameWithoutClass = name.substring(0, name.length() - 6);
                    String simple = nameWithoutClass;
                    int index;
                    if ((index = simple.indexOf('.')) != -1) {
                        simple = simple.substring(0, index);
                        if (importDeclaration.getName().getName().equals(simple)) {
                            possibleClassNames.add(javaName.replace('.', '/') + "$" + simple.substring(index + 1).replace('.', '$') + ".class");
                        }
                    }
                    if (importDeclaration.getName().getName().equals(nameWithoutClass)) {
                        possibleClassNames.add(javaName.replace('.', '/') + ".class");
                    }
                }
            }
        }

        possibleClassNames.add("java/lang/" + classOrInterfaceType.getName() + ".class");

        if (packageName != null) {
            possibleClassNames.add(packageName + "/" + classOrInterfaceType.getName() + ".class");
        }

        System.out.println(possibleClassNames);

        Map<String, LoadedFile> mapping = possibleClassNames.stream().map(name -> new AbstractMap.SimpleEntry<>(name, getFileFor(name)))
                .filter(ent -> ent.getValue() != null)
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

        System.out.println("Result: " + mapping.keySet());
        if (mapping.size() == 0) {
            System.out.println("ERROR: Could not find file which contains " + possibleClassNames);
        } else if (mapping.size() > 1) {
            System.out.println("ERROR: Multiple results: " + mapping.keySet());
        } else {
            Map.Entry<String, LoadedFile> entry = mapping.entrySet().iterator().next();
            link.fileName = entry.getValue().getName();
            link.className = entry.getKey();
            link.jumpTo = " " + classOrInterfaceType.getName() + " ";
            textArea.links.add(link);
        }

        result.addAll(mapping.keySet());

        return result;
    }

    private LoadedFile getFileFor(String fileName) {
        LoadedFile file = Helios.getLoadedFile(this.fileName);
        if (file.getData().get(fileName) != null) {
            return file;
        } else {
            Set<LoadedFile> check = new HashSet<>();
            check.addAll(Helios.getAllFiles());
            check.addAll(Helios.getPathFiles().values());
            for (LoadedFile loadedFile : check) {
                if (loadedFile.getData().get(fileName) != null) {
                    return loadedFile;
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
