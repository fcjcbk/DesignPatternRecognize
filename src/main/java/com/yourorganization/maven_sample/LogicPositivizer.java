package com.yourorganization.maven_sample;


import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class LogicPositivizer {
    // 收集局部变量的访问者类
    private static HashSet<String> allClass = new HashSet<>();
    final String NOT_FOUND = "not_found";

    private static ArrayList<File> listJavaFilesRecursively(File directory) {
        ArrayList<File> resultList = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    resultList.addAll(listJavaFilesRecursively(file));
                } else if (file.getName().endsWith(".java")) {
                    resultList.add(file);
                }
            }
        }
        return resultList;
    }

    public static void main(String[] args) throws FileNotFoundException {

//        String projectPath = "src/main/resources/mytest";
//        String projectPath = "src/main/resources/design_pattern";
//        String projectPath = "D:\\codefile\\Java\\myPaint\\Paint\\src";
        String projectPath = "D:\\codefile\\Java\\springboot-seckill\\src\\main\\java";

        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(projectPath));

        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(javaParserTypeSolver);

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

        listJavaFilesRecursively(new File(projectPath)).forEach(file -> {
            try {
                FileInputStream in1 = new FileInputStream(file);
                CompilationUnit cu = StaticJavaParser.parse(in1);
                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
                    String className = clazz.resolve().getQualifiedName();
                    allClass.add(className);
                    System.out.println("Class: " + className);

                });

            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

        });


        HashMap<String, ClassInfo> classMap = new HashMap<>();
        // 遍历 CompilationUnit 中的类声明
        listJavaFilesRecursively(new File(projectPath)).forEach(file -> {
            try {
                FileInputStream in1 = new FileInputStream(file);
                CompilationUnit cu = StaticJavaParser.parse(in1);
                cu.accept(new ClassVisitor(), classMap);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

        });

        classMap.values().forEach(classInfo -> {
            System.out.println(classInfo.toString());
        });

        System.out.println("class size: " + allClass.size());
        System.out.println("class size: " + classMap.size());
//        FieldDeclaration fieldDeclaration  = Navigator.demandNodeOfGivenClass(cu, FieldDeclaration.class);
//        System.out.println(fieldDeclaration.getVariables().get(0).getType().resolve().asReferenceType().getQualifiedName());
    }

    private static void checkStatementAndAddAggregation(Statement statement, ClassInfo classInfo, String paramType) {
        try {
            if (statement.isExpressionStmt()) {
                ExpressionStmt exprStmt = statement.asExpressionStmt();
                if (exprStmt.getExpression().isAssignExpr()) {
                    AssignExpr assignExpr = exprStmt.getExpression().asAssignExpr();
                    if (assignExpr.getTarget().isFieldAccessExpr()) {
                        FieldAccessExpr fieldAccessExpr = assignExpr.getTarget().asFieldAccessExpr();
                        if (fieldAccessExpr.getScope().isThisExpr()
                                && fieldAccessExpr.resolve().getType().isReferenceType()) {
                            if (fieldAccessExpr.resolve().getType().asReferenceType().getQualifiedName().equals(paramType)) {
                                classInfo.AddAggregation(paramType);
                            }
                        }
                    }
                }
            }
        } catch (UnsolvedSymbolException e) {
            System.err.println("Unable to resolve variable: " + e.getMessage() + " " + statement);
        }

    }

    private static String getQualifiedName(Type type) {
        String qualifiedName = "";
        try {
            if (type.resolve().isReferenceType()) {
                qualifiedName = type.resolve().asReferenceType().getQualifiedName();
                return qualifiedName;
            }

            // 处理泛型
            // 泛型取第一个处理
            if (type.isClassOrInterfaceType()) {
                ClassOrInterfaceType classType = type.asClassOrInterfaceType();
                if (classType.getTypeArguments().isPresent() && classType.getTypeArguments().get().getFirst().isPresent()) {
                    qualifiedName = classType.getTypeArguments().get().getFirst().get().resolve().asReferenceType().getQualifiedName();
                }
                return qualifiedName;
            }

            // 处理数组
            if (type.isArrayType()) {
                ArrayType arrayType = type.asArrayType();
                Type componentType = arrayType.getComponentType();
                if (componentType.isClassOrInterfaceType()) {
                    ClassOrInterfaceType classType = componentType.asClassOrInterfaceType();
//                System.out.println("Hit array Qualified name: " + classType.resolve().asReferenceType().getQualifiedName() + "[]");
                    qualifiedName = classType.resolve().asReferenceType().getQualifiedName();
                }
                return qualifiedName;
            }
        } catch (UnsolvedSymbolException e) {
            System.err.println("Unable to resolve parameter: " + e.getMessage() + " " + type);
        }
        // not found
        return "not_found";
    }

    private static class LocalVariableCollector extends VoidVisitorAdapter<Void> {
        final private ArrayList<VariableDeclarator> variables = new java.util.ArrayList<>();

        @Override
        public void visit(VariableDeclarator n, Void arg) {
            super.visit(n, arg);
            variables.add(n);
        }

        public ArrayList<VariableDeclarator> getVariables() {
            return variables;
        }
    }

    // 自定义访问者类，用于收集方法调用表达式
    private static class MethodCallVisitor extends VoidVisitorAdapter<ClassInfo> {
        @Override
        public void visit(MethodCallExpr methodCall, ClassInfo classInfo) {
            super.visit(methodCall, classInfo);
            try {
                ResolvedMethodDeclaration resolvedMethodDecl = methodCall.resolve();
                if (resolvedMethodDecl.isStatic()) {
//                System.out.println("Static method call found: " + methodCall.getName() + " in class " +
//                        resolvedMethodDecl.declaringType().getQualifiedName());
                    String qualifiedName = resolvedMethodDecl.declaringType().getQualifiedName();
                    if (!allClass.contains(qualifiedName)) {
                        return;
                    }
                    classInfo.CheckAndAddDependency(qualifiedName);
                }
            } catch (Exception e) {
                System.err.println("Unable to resolve method call: " + e.getMessage() + " " + methodCall);
            }


        }
    }

    private static class AssignExprVisitor extends VoidVisitorAdapter<Void> {
        final private HashSet<String> fields = new HashSet<>();

        @Override
        public void visit(AssignExpr assignExpr, Void arg) {
            super.visit(assignExpr, arg);
            try {

                if (assignExpr.getTarget().isFieldAccessExpr()) {
                    FieldAccessExpr fieldAccessExpr = assignExpr.getTarget().asFieldAccessExpr();
                    if (fieldAccessExpr.resolve().getType().isReferenceType()) {
                        String field = fieldAccessExpr.resolve().getType().asReferenceType().getQualifiedName();
                        fields.add(field);
                    }
                } else if (assignExpr.getTarget().isNameExpr()) {
                    // Todo: 对静态成员变量做处理
                    NameExpr nameExpr = assignExpr.getTarget().asNameExpr();
                    if (nameExpr.resolve().isField()) {
                        fields.add(nameExpr.resolve().getType().asReferenceType().getQualifiedName());
                    }

                }
            } catch (Exception e) {
                System.err.println("Unable to resolve variable: " + e.getMessage() + " " + assignExpr);
            }

        }

        public HashSet<String> getFields() {
            return fields;
        }
    }

    private static class ClassVisitor extends VoidVisitorAdapter<HashMap<String, ClassInfo>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration n, HashMap<String, ClassInfo> arg) {
            super.visit(n, arg);


            // 获取类标识名
            String className = n.resolve().getQualifiedName();
//            System.out.println(className);

            ClassInfo classInfo = new ClassInfo(className);
            arg.put(className, classInfo);

            // 获取聚合类型 同时寻找依赖类型: 1. 使用其他类的静态方法 (done) 2. 接受其他类作为函数参数(done) 3. 使用其他类的局部变量 (done)

            // 获取组合
            n.getConstructors().forEach(constructor -> {
                HashSet<String> params = new HashSet<>();
                constructor.getParameters().forEach(parameter -> {
                    try {
                        String paramName = getQualifiedName(parameter.getType());
                        if (!allClass.contains(paramName)) {
                            return;
                        }
                        params.add(paramName);
                    } catch (UnsolvedSymbolException e) {
                        System.err.println("Unable to resolve constructor: " + e.getMessage() + " " + constructor);
                    }
                });

                AssignExprVisitor assignExprVisitor = new AssignExprVisitor();
                constructor.accept(assignExprVisitor, null);
                for (String field : assignExprVisitor.getFields()) {
                    if (!allClass.contains(field)) {
                        continue;
                    }
                    if (!params.contains(field)) {
                        // 赋值变量从函数参数中获取
                        System.out.println("compose field: " + field);
                        classInfo.AddComposition(field);
                    } else {
                        // 赋值变量未从函数参数中获取
                        System.out.println("aggregation field: " + field);
                        classInfo.AddAggregation(field);
                    }
                }

                for (String param : params) {
                    if (!allClass.contains(param) || !assignExprVisitor.getFields().contains(param)) {
                        continue;
                    }
                    // 函数参数未用于赋值 -> 依赖
                    classInfo.CheckAndAddDependency(param);

                }

                // 获取依赖关系中静态函数调用
                constructor.accept(new MethodCallVisitor(), classInfo);

                // 获取依赖关系中局部变量
                LocalVariableCollector collector = new LocalVariableCollector();
                constructor.accept(collector, null);
                // 获取所有局部变量声明
                ArrayList<VariableDeclarator> variables = collector.getVariables();
                for (VariableDeclarator var : variables) {
                    try {
                        String qualifiedName = getQualifiedName(var.getType());
                        if (!allClass.contains(qualifiedName)) {
                            continue;
                        }
                        classInfo.CheckAndAddDependency(qualifiedName);
                    } catch (UnsolvedSymbolException e) {
                        System.err.println("Unable to resolve variable: " + e.getMessage() + " " + var);
                    }
                }

            });

            // 组合也可能在函数中出现
            n.getMethods().forEach(method -> {
                // 获取组合、聚合、依赖类型
                HashSet<String> params = new HashSet<>();
                method.getParameters().forEach(parameter -> {
                    try {
                        String paramName = getQualifiedName(parameter.getType());
                        if (!allClass.contains(paramName)) {
                            return;
                        }
                        params.add(paramName);
                    } catch (UnsolvedSymbolException e) {
                        System.err.println("Unable to resolve method: " + e.getMessage() + " " + method);
                    }
                });

                AssignExprVisitor assignExprVisitor = new AssignExprVisitor();
                method.accept(assignExprVisitor, null);
                for (String field : assignExprVisitor.getFields()) {
                    if (!allClass.contains(field)) {
                        continue;
                    }
                    if (!params.contains(field)) {
                        // 赋值变量从函数参数中获取 -> 聚合
                        System.out.println("compose field: " + field);
                        classInfo.AddComposition(field);
                    } else {
                        // 赋值变量未从函数参数中获取 -> 组合
                        System.out.println("aggregation field: " + field);
                        classInfo.AddAggregation(field);
                    }
                }

                for (String param : params) {
                    if (!allClass.contains(param)) {
                        continue;
                    }
                    // 函数参数未用于赋值 -> 依赖
                    classInfo.CheckAndAddDependency(param);

                }

                // 获取依赖关系中静态函数调用
                method.accept(new MethodCallVisitor(), classInfo);

                // 获取依赖关系中局部变量
                LocalVariableCollector collector = new LocalVariableCollector();
                method.accept(collector, null);
                // 获取所有局部变量声明
                ArrayList<VariableDeclarator> variables = collector.getVariables();
                for (VariableDeclarator var : variables) {
                    try {
                        String qualifiedName = getQualifiedName(var.getType());
                        if (!allClass.contains(qualifiedName)) {
                            continue;
                        }
                        classInfo.CheckAndAddDependency(qualifiedName);
                    } catch (UnsolvedSymbolException e) {
                        System.err.println("Unable to resolve variable: " + e.getMessage() + " " + var);
                    }
                }
            });

            // 既不是聚合也不是组合的成员变量
            n.getFields().forEach(field -> {
                field.getVariables().forEach(variable -> {
                    String qualifiedName = getQualifiedName(variable.getType());
                    if (!allClass.contains(qualifiedName)) {
                        return;
                    }

                    classInfo.CheckAndAddAssociation(qualifiedName);
                });
            });

            n.getExtendedTypes().forEach(extendedType -> {
                try {
                    String parentClass = extendedType.resolve().asReferenceType().getQualifiedName();
                    if (!allClass.contains(parentClass)) {
                        return;
                    }
                    classInfo.AddExtendType(parentClass);
                } catch (UnsolvedSymbolException e) {
                    System.err.println("Unable to resolve extended type: " + e.getMessage() + " " + extendedType);
                }
            });

            // 获取实现的接口
            n.getImplementedTypes().forEach(implementedType -> {
                try {
                    String interfaceName = implementedType.resolve().asReferenceType().getQualifiedName();
                    if (!allClass.contains(interfaceName)) {
                        return;
                    }
                    classInfo.AddImplementor(interfaceName);
                } catch (UnsolvedSymbolException e) {
                    System.err.println("Unable to resolve parameter: " + e.getMessage() + " " + implementedType);
                }

            });


            // 成员变量
            // 成员变量可以识别的类型有 3 种
            // 1. 组合: 成员变量在构造函数或构造函数调用的其他函数内初始化
            // 2. 聚合: 成员变量可以独立与类生命周期存在，可以通过构造函数或者 set 函数传入
            // 3. 关联: 成员变量中既不是聚合也不是组合
            // Todo: 考虑数组及 ArrayList 等泛型容器的情况
        }
    }
}
