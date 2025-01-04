package com.yourorganization.maven_sample;


import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
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

//        String projectPath = "src/main/resources";
        String projectPath = "D:\\codefile\\Java\\myPaint\\Paint\\src";

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
            if (type instanceof ClassOrInterfaceType) {
                ClassOrInterfaceType classType = (ClassOrInterfaceType) type;
                if (classType.getTypeArguments().isPresent() && classType.getTypeArguments().get().getFirst().isPresent()) {
                    qualifiedName = classType.getTypeArguments().get().getFirst().get().resolve().asReferenceType().getQualifiedName();
                }
                return qualifiedName;
            }

            // 处理数组
            if (type instanceof ArrayType) {
                ArrayType arrayType = (ArrayType) type;
                Type componentType = arrayType.getComponentType();
                if (componentType instanceof ClassOrInterfaceType) {
                    ClassOrInterfaceType classType = (ClassOrInterfaceType) componentType;
//                System.out.println("Hit array Qualified name: " + classType.resolve().asReferenceType().getQualifiedName() + "[]");
                    qualifiedName = classType.resolve().asReferenceType().getQualifiedName();
                }
                return qualifiedName;
            }
        } catch (UnsolvedSymbolException e) {
            System.err.println("Unable to resolve parameter: " + e.getMessage() + " " + type);
        }
        // not found
        return "not found";
    }

    private static class LocalVariableCollector extends VoidVisitorAdapter<Void> {
        private ArrayList<VariableDeclarator> variables = new java.util.ArrayList<>();

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
            } catch (UnsolvedSymbolException e) {
                System.err.println("Unable to resolve method call: " + e.getMessage() + " " + methodCall);
            }


        }
    }

    private static class ClassVisitor extends VoidVisitorAdapter<HashMap<String, ClassInfo>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration n,HashMap<String, ClassInfo> arg) {
            super.visit(n, arg);


            // 获取类标识名
            String className = n.resolve().getQualifiedName();
//            System.out.println(className);

            ClassInfo classInfo = new ClassInfo(className);
            arg.put(className, classInfo);

            // 获取聚合类型 同时寻找依赖类型: 1. 使用其他类的静态方法 (done) 2. 接受其他类作为函数参数(done) 3. 使用其他类的局部变量 (done)
            n.getMethods().forEach(method -> {
                method.accept(new MethodCallVisitor(), classInfo);

                LocalVariableCollector collector = new LocalVariableCollector();
                method.accept(collector, null);
                // 获取所有局部变量声明
                ArrayList<VariableDeclarator> variables = collector.getVariables();
                for (VariableDeclarator var : variables) {
                    try {
                        if (!var.getType().resolve().isReferenceType()) {
                            continue;
                        }
                        String qualifiedName = var.getType().resolve().asReferenceType().getQualifiedName();
                        if (!allClass.contains(qualifiedName)) {
                            return;
                        }
                        classInfo.CheckAndAddDependency(qualifiedName);
                    } catch (UnsolvedSymbolException e) {
                        System.err.println("Unable to resolve variable: " + e.getMessage() + " " + var);
                    }
                }

                method.getParameters().forEach(parameter -> {
                    String parameterType = "";
                    try {
                        if (!parameter.getType().resolve().isReferenceType()) {
                            return;
                        }
                        String paramType = parameter.getType().resolve().asReferenceType().getQualifiedName();
                        if (!allClass.contains(paramType)) {
                            return;
                        }
                        method.getBody().ifPresent(body -> {
                            body.getStatements().forEach(statement -> {
                                checkStatementAndAddAggregation(statement, classInfo, paramType);
                            });
                        });
                        classInfo.CheckAndAddDependency(paramType);
                    } catch (UnsolvedSymbolException e) {
                        System.err.println("Unable to resolve parameter: " + e.getMessage() + " " + parameter);
                    }
                });
            });
            n.getConstructors().forEach(constructor -> {
                constructor.getParameters().forEach(parameter -> {
                    try {
                        if (!parameter.getType().resolve().isReferenceType()) {
                            return;
                        }
                        String paramType = parameter.getType().resolve().asReferenceType().getQualifiedName();
                        if (!allClass.contains(paramType)) {
                            return;
                        }
                        constructor.getBody().getStatements().forEach(statement -> {
                            checkStatementAndAddAggregation(statement, classInfo, paramType);
                        });
                    } catch (UnsolvedSymbolException e) {
                        System.err.println("Unable to resolve constructor: " + e.getMessage() + " " + constructor);
                    }

                });
            });

            // 获取组合
            n.getConstructors().forEach(constructor -> {
                HashSet<String> params = new HashSet<>();
                constructor.getParameters().forEach(parameter -> {
                    try {

                        if (!parameter.getType().resolve().isReferenceType()) {
                            return;
                        }
                        String paramName = parameter.getType().resolve().asReferenceType().getQualifiedName();
                        if (!allClass.contains(paramName)) {
                            return;
                        }
                        params.add(paramName);
                    } catch (UnsolvedSymbolException e) {
                        System.err.println("Unable to resolve constructor: " + e.getMessage() + " " + constructor);
                    }
                });
//                    String paramName = parameter.getNameAsString();
                constructor.getBody().getStatements().forEach(statement -> {
                    try {
                        if (statement.isExpressionStmt()) {
                            ExpressionStmt exprStmt = statement.asExpressionStmt();
                            if (exprStmt.getExpression().isAssignExpr()) {
                                AssignExpr assignExpr = exprStmt.getExpression().asAssignExpr();
                                if (assignExpr.getTarget().isFieldAccessExpr()) {
                                    FieldAccessExpr fieldAccessExpr = assignExpr.getTarget().asFieldAccessExpr();
                                    if (fieldAccessExpr.getScope().isThisExpr()
                                            && fieldAccessExpr.resolve().getType().isReferenceType()) {
                                        String field = fieldAccessExpr.resolve().getType().asReferenceType().getQualifiedName();
                                        if (allClass.contains(field) && !params.contains(field)) {
                                            System.out.println("compose field: " + field);
                                            classInfo.AddComposition(field);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (UnsolvedSymbolException e) {
                        System.err.println("Unable to resolve constructor: " + e.getMessage() + " " + constructor);
                    }
                });

            });

            // 既不是聚合也不是组合的成员变量
            n.getFields().forEach(field -> {
                field.getVariables().forEach(variable -> {
                    String qualifiedName = getQualifiedName(variable.getType());
                    if (!allClass.contains(qualifiedName)) {
                        return;
                    }

                    if (!classInfo.getAggregations().contains(qualifiedName) &&
                            !classInfo.getCompositions().contains(qualifiedName)) {
                        classInfo.AddAssociation(qualifiedName);
                    }
                });
            });


            // 获取继承的类
            n.getExtendedTypes().forEach(extendedType -> {
                String parentClass = extendedType.resolve().asReferenceType().getQualifiedName();
                if (!allClass.contains(parentClass)) {
                    return;
                }
                classInfo.AddExtendType(parentClass);
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
