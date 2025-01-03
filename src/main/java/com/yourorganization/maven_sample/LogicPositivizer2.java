package com.yourorganization.maven_sample;
//
//import com.github.javaparser.ast.CompilationUnit;
//import com.github.javaparser.ast.expr.BinaryExpr;
//import com.github.javaparser.ast.stmt.IfStmt;
//import com.github.javaparser.ast.stmt.Statement;
//import com.github.javaparser.ast.visitor.ModifierVisitor;
//import com.github.javaparser.ast.visitor.Visitable;
//import com.github.javaparser.utils.CodeGenerationUtils;
//import com.github.javaparser.utils.Log;
//import com.github.javaparser.utils.SourceRoot;
//
//import java.nio.file.Paths;
//
///**
// * Some code that uses JavaParser.
// */
//public class LogicPositivizer {
//    public static void main(String[] args) {
//        // JavaParser has a minimal logging class that normally logs nothing.
//        // Let's ask it to write to standard out:
//        Log.setAdapter(new Log.StandardOutStandardErrorAdapter());
//
//        // SourceRoot is a tool that read and writes Java files from packages on a certain root directory.
//        // In this case the root directory is found by taking the root from the current Maven module,
//        // with src/main/resources appended.
//        SourceRoot sourceRoot = new SourceRoot(CodeGenerationUtils.mavenModuleRoot(LogicPositivizer.class).resolve("src/main/resources"));
//
//        // Our sample is in the root of this directory, so no package name.
//        CompilationUnit cu = sourceRoot.parse("", "Blabla.java");
//
//        Log.info("Positivizing!");
//
//        cu.accept(new ModifierVisitor<Void>() {
//            /**
//             * For every if-statement, see if it has a comparison using "!=".
//             * Change it to "==" and switch the "then" and "else" statements around.
//             */
//            @Override
//            public Visitable visit(IfStmt n, Void arg) {
//                // Figure out what to get and what to cast simply by looking at the AST in a debugger!
//                n.getCondition().ifBinaryExpr(binaryExpr -> {
//                    if (binaryExpr.getOperator() == BinaryExpr.Operator.NOT_EQUALS && n.getElseStmt().isPresent()) {
//                        /* It's a good idea to clone nodes that you move around.
//                            JavaParser (or you) might get confused about who their parent is!
//                        */
//                        Statement thenStmt = n.getThenStmt().clone();
//                        Statement elseStmt = n.getElseStmt().get().clone();
//                        n.setThenStmt(elseStmt);
//                        n.setElseStmt(thenStmt);
//                        binaryExpr.setOperator(BinaryExpr.Operator.EQUALS);
//                    }
//                });
//                return super.visit(n, arg);
//            }
//        }, null);
//
//        // This saves all the files we just read to an output directory.
//        sourceRoot.saveAll(
//                // The path of the Maven module/project which contains the LogicPositivizer class.
//                CodeGenerationUtils.mavenModuleRoot(LogicPositivizer.class)
//                        // appended with a path to "output"
//                        .resolve(Paths.get("output")));
//    }
//}


import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;

public class LogicPositivizer2 {

    public static void main(String[] args) throws FileNotFoundException {
        FileInputStream in = new FileInputStream("src/main/resources/Test.java");

//        FileInputStream in = new FileInputStream("D:\\codefile\\archive\\emotion_master\\src\\main\\java\\com\\project\\controller\\employeeBasicController.java");
//
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File("D:\\codefile\\archive\\emotion_master\\src\\main\\java"));
//        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File("src/main/resources"));

        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(javaParserTypeSolver);

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

        CompilationUnit cu = StaticJavaParser.parse(in);

        // Todo: 未考虑嵌套调用中可能出现的情况

        // 找到作为函数参数并且赋值到成员变量的类 -> 聚合
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
            clazz.getMethods().forEach(method -> {
                method.getParameters().forEach(parameter -> {
                    if (!parameter.getType().resolve().isReferenceType()) {
                        return;
                    }
                    String paramName = parameter.getType().resolve().asReferenceType().getQualifiedName();
//                    String paramName = parameter.getNameAsString();
                    method.getBody().ifPresent(body -> {
                        body.getStatements().forEach(statement -> {
                            if (statement.isExpressionStmt()) {
                                ExpressionStmt exprStmt = statement.asExpressionStmt();
                                if (exprStmt.getExpression().isAssignExpr()) {
                                    AssignExpr assignExpr = exprStmt.getExpression().asAssignExpr();
                                    if (assignExpr.getTarget().isFieldAccessExpr()) {
                                        FieldAccessExpr fieldAccessExpr = assignExpr.getTarget().asFieldAccessExpr();
                                        if (fieldAccessExpr.getScope().isThisExpr()) {
//                                            ThisExpr thisExpr = fieldAccessExpr.getScope().asThisExpr();
                                            if (fieldAccessExpr.resolve().getType().asReferenceType().getQualifiedName().equals(paramName)) {
                                                System.out.println("Found method: " + method.getNameAsString() + " parameter: " + parameter.getNameAsString());
                                            }
                                        }
                                    }
                                }
                            }
                        });
                    });
                });
            });
            clazz.getConstructors().forEach(constructor -> {
                constructor.getParameters().forEach(parameter -> {
                    if (!parameter.getType().resolve().isReferenceType()) {
                        return;
                    }
                    String paramName = parameter.getType().resolve().asReferenceType().getQualifiedName();
//                    String paramName = parameter.getNameAsString();
                    constructor.getBody().getStatements().forEach(statement -> {
                        if (statement.isExpressionStmt()) {
                            ExpressionStmt exprStmt = statement.asExpressionStmt();
                            if (exprStmt.getExpression().isAssignExpr()) {
                                AssignExpr assignExpr = exprStmt.getExpression().asAssignExpr();
                                if (assignExpr.getTarget().isFieldAccessExpr()) {
                                    FieldAccessExpr fieldAccessExpr = assignExpr.getTarget().asFieldAccessExpr();
                                    if (fieldAccessExpr.getScope().isThisExpr()) {
//                                            ThisExpr thisExpr = fieldAccessExpr.getScope().asThisExpr();
                                        if (fieldAccessExpr.resolve().getType().asReferenceType().getQualifiedName().equals(paramName)) {
                                            System.out.println("Found method: " + constructor.getNameAsString() + "aggregation filed: " + parameter.getNameAsString());
                                        }
                                    }
                                }
                            }
                        }
                    });
                });
            });
        });

        // 找到构造函数体中赋值到成员变量但是未在函数参数中的类 -> 组合
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
            clazz.getConstructors().forEach(constructor -> {
                HashSet<String> params = new HashSet<>();
                constructor.getParameters().forEach(parameter -> {
                    if (!parameter.getType().resolve().isReferenceType()) {
                        return;
                    }
                    String paramName = parameter.getType().resolve().asReferenceType().getQualifiedName();
                    params.add(paramName);
                });
//                    String paramName = parameter.getNameAsString();
                    constructor.getBody().getStatements().forEach(statement -> {
                        if (statement.isExpressionStmt()) {
                            ExpressionStmt exprStmt = statement.asExpressionStmt();
                            if (exprStmt.getExpression().isAssignExpr()) {
                                AssignExpr assignExpr = exprStmt.getExpression().asAssignExpr();
                                if (assignExpr.getTarget().isFieldAccessExpr()) {
                                    FieldAccessExpr fieldAccessExpr = assignExpr.getTarget().asFieldAccessExpr();
                                    if (fieldAccessExpr.getScope().isThisExpr()) {
//                                            ThisExpr thisExpr = fieldAccessExpr.getScope().asThisExpr();
                                        String field = fieldAccessExpr.resolve().getType().asReferenceType().getQualifiedName();
                                        if (!params.contains(field)) {
                                            System.out.println("compose field: " + field);
                                        }
                                    }
                                }
                            }
                        }
                    });

            });
        });

    }

    public static void main1(String[] args) throws FileNotFoundException {
        // 解析 Java 源文件
        FileInputStream in = new FileInputStream("src/main/resources/Test.java");

//        FileInputStream in = new FileInputStream("D:\\codefile\\archive\\emotion_master\\src\\main\\java\\com\\project\\controller\\employeeBasicController.java");
//
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File("D:\\codefile\\archive\\emotion_master\\src\\main\\java"));
//        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File("src/main/resources"));

        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(javaParserTypeSolver);

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

        CompilationUnit cu = StaticJavaParser.parse(in);

        PackageDeclaration packageDeclaration = cu.getPackageDeclaration().orElse(null);

        // 打印包名
        if (packageDeclaration != null) {
            System.out.println("Package Name: " + packageDeclaration.getName());
        } else {
            System.out.println("No package declaration found.");
        }

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
            String className = clazz.resolve().getQualifiedName();
            allClass.add(className);

        });
        ArrayList<ClassInfo> classInfos = new ArrayList<>();
        // 遍历 CompilationUnit 中的类声明
        cu.accept(new ClassVisitor(), classInfos);

//        FieldDeclaration fieldDeclaration  = Navigator.demandNodeOfGivenClass(cu, FieldDeclaration.class);
//        System.out.println(fieldDeclaration.getVariables().get(0).getType().resolve().asReferenceType().getQualifiedName());
    }

    private static HashSet<String> allClass;
    private static class ClassVisitor extends VoidVisitorAdapter<ArrayList<ClassInfo>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration n, ArrayList<ClassInfo> arg) {
            super.visit(n, arg);


            // 获取类标识名
            String className = n.resolve().getQualifiedName();
            System.out.println(className);

            ClassInfo classInfo = new ClassInfo(className);
            arg.add(classInfo);

            // 获取聚合类型
            n.getMethods().forEach(method -> {
                method.getParameters().forEach(parameter -> {
                    if (!parameter.getType().resolve().isReferenceType()) {
                        return;
                    }
                    String paramType = parameter.getType().resolve().asReferenceType().getQualifiedName();
                    if (!allClass.contains(paramType)) {
                        return;
                    }
//                    String paramName = parameter.getNameAsString();
                    method.getBody().ifPresent(body -> {
                        body.getStatements().forEach(statement -> {
                            if (statement.isExpressionStmt()) {
                                ExpressionStmt exprStmt = statement.asExpressionStmt();
                                if (exprStmt.getExpression().isAssignExpr()) {
                                    AssignExpr assignExpr = exprStmt.getExpression().asAssignExpr();
                                    if (assignExpr.getTarget().isFieldAccessExpr()) {
                                        FieldAccessExpr fieldAccessExpr = assignExpr.getTarget().asFieldAccessExpr();
                                        if (fieldAccessExpr.getScope().isThisExpr()) {
//                                            ThisExpr thisExpr = fieldAccessExpr.getScope().asThisExpr();
                                            if (fieldAccessExpr.resolve().getType().asReferenceType().getQualifiedName().equals(paramType)) {
                                                System.out.println("Found method: " + method.getNameAsString() + " parameter: " + parameter.getNameAsString());
                                                classInfo.AddAggregation(paramType);
                                            }
                                        }
                                    }
                                }
                            }
                        });
                    });
                });
            });
            n.getConstructors().forEach(constructor -> {
                constructor.getParameters().forEach(parameter -> {
                    if (!parameter.getType().resolve().isReferenceType()) {
                        return;
                    }
                    String paramType = parameter.getType().resolve().asReferenceType().getQualifiedName();
                    if (!allClass.contains(paramType)) {
                        return;
                    }
//                    String paramName = parameter.getNameAsString();
                    constructor.getBody().getStatements().forEach(statement -> {
                        if (statement.isExpressionStmt()) {
                            ExpressionStmt exprStmt = statement.asExpressionStmt();
                            if (exprStmt.getExpression().isAssignExpr()) {
                                AssignExpr assignExpr = exprStmt.getExpression().asAssignExpr();
                                if (assignExpr.getTarget().isFieldAccessExpr()) {
                                    FieldAccessExpr fieldAccessExpr = assignExpr.getTarget().asFieldAccessExpr();
                                    if (fieldAccessExpr.getScope().isThisExpr()) {
//                                            ThisExpr thisExpr = fieldAccessExpr.getScope().asThisExpr();
                                        if (fieldAccessExpr.resolve().getType().asReferenceType().getQualifiedName().equals(paramType)) {
                                            System.out.println("Found method: " + constructor.getNameAsString() + "aggregation filed: " + parameter.getNameAsString());
                                            classInfo.AddAggregation(paramType);
                                        }
                                    }
                                }
                            }
                        }
                    });
                });
            });

            // 获取组合
            n.getConstructors().forEach(constructor -> {
                HashSet<String> params = new HashSet<>();
                constructor.getParameters().forEach(parameter -> {
                    if (!parameter.getType().resolve().isReferenceType()) {
                        return;
                    }
                    String paramName = parameter.getType().resolve().asReferenceType().getQualifiedName();
                    if (!allClass.contains(paramName)) {
                        return;
                    }
                    params.add(paramName);
                });
//                    String paramName = parameter.getNameAsString();
                constructor.getBody().getStatements().forEach(statement -> {
                    if (statement.isExpressionStmt()) {
                        ExpressionStmt exprStmt = statement.asExpressionStmt();
                        if (exprStmt.getExpression().isAssignExpr()) {
                            AssignExpr assignExpr = exprStmt.getExpression().asAssignExpr();
                            if (assignExpr.getTarget().isFieldAccessExpr()) {
                                FieldAccessExpr fieldAccessExpr = assignExpr.getTarget().asFieldAccessExpr();
                                if (fieldAccessExpr.getScope().isThisExpr()) {
//                                            ThisExpr thisExpr = fieldAccessExpr.getScope().asThisExpr();
                                    String field = fieldAccessExpr.resolve().getType().asReferenceType().getQualifiedName();
                                    if (!params.contains(field)) {
                                        System.out.println("compose field: " + field);
                                        classInfo.AddComposition(field);
                                    }
                                }
                            }
                        }
                    }
                });

            });

            // 既不是聚合也不是组合的成员变量
            n.getFields().forEach(field -> {
                field.getVariables().forEach(variable -> {

                    if (variable.getType().resolve().isReferenceType()) {
                        String qualifiedName = variable.getType().resolve().asReferenceType().getQualifiedName();
                        if (!allClass.contains(qualifiedName)) {
                            return;
                        }

                        if (!classInfo.getAggregations().contains(qualifiedName) &&
                                !classInfo.getCompositions().contains(qualifiedName)) {
                            classInfo.AddAssociation(qualifiedName);
                            return;
                        }

                        // 处理泛型
                        Type type = variable.getType();
                        if (type instanceof ClassOrInterfaceType) {
                            ClassOrInterfaceType classType = (ClassOrInterfaceType) type;
                            if (classType.getTypeArguments().isPresent()) {
                                classType.getTypeArguments().get().forEach(typeArgument -> {
                                    if (typeArgument.resolve().isReferenceType()) {
                                        System.out.println("Generic type: " + typeArgument.resolve().asReferenceType().getQualifiedName());
                                    }
                                });
                            }
                        }
                    }

                    // 处理数组
                    Type fieldType = variable.getType();
                    if (fieldType instanceof ArrayType) {
                        ArrayType arrayType = (ArrayType) fieldType;
                        Type componentType = arrayType.getComponentType();
                        if (componentType instanceof ClassOrInterfaceType) {
                            ClassOrInterfaceType classType = (ClassOrInterfaceType) componentType;
                            System.out.println("Qualified name: " + classType.resolve().asReferenceType().getQualifiedName() + "[]");
                        }
                    }
                });
            });



            // 获取继承的类
            n.getExtendedTypes().forEach(extendedType -> {
                String parentClass = extendedType.getNameAsString();
                System.out.println(className + " extends " + parentClass);
            });

            // 获取实现的接口
            n.getImplementedTypes().forEach(implementedType -> {
                String interfaceName = implementedType.getNameAsString();
                System.out.println(className + " implements " + interfaceName);
            });


            // 成员变量
            // 成员变量可以识别的类型有 3 种
            // 1. 组合: 成员变量在构造函数或构造函数调用的其他函数内初始化
            // 2. 聚合: 成员变量可以独立与类生命周期存在，可以通过构造函数或者 set 函数传入
            // 3. 关联: 成员变量中既不是聚合也不是组合
            // Todo: 考虑数组及 ArrayList 等泛型容器的情况
            n.getFields().forEach(field -> {
                field.getVariables().forEach(variable -> {
                    System.out.println("Field: " + variable.getNameAsString() + " of type " + variable.getType());

                    if (variable.getType().resolve().isReferenceType()) {
                        System.out.println(variable.getType().resolve().asReferenceType().getQualifiedName());


                        // not in the class set return
                        String qualifyName = variable.getType().resolve().asReferenceType().getQualifiedName();

                        Type type = variable.getType();
                        if (type instanceof ClassOrInterfaceType) {
                            ClassOrInterfaceType classType = (ClassOrInterfaceType) type;
                            if (classType.getTypeArguments().isPresent()) {
                                classType.getTypeArguments().get().forEach(typeArgument -> {
                                    if (typeArgument.resolve().isReferenceType()) {
                                        System.out.println("Generic type: " + typeArgument.resolve().asReferenceType().getQualifiedName());
                                    }
                                });
                            }
                        }
                    }

                    Type fieldType = variable.getType();
                    if (fieldType instanceof ArrayType) {
                        ArrayType arrayType = (ArrayType) fieldType;
                        Type componentType = arrayType.getComponentType();
                        if (componentType instanceof ClassOrInterfaceType) {
                            ClassOrInterfaceType classType = (ClassOrInterfaceType) componentType;
                            System.out.println("Qualified name: " + classType.resolve().asReferenceType().getQualifiedName() + "[]");
                        }
                    }

                });
            });

            // 获取成员变量
//            List<FieldDeclaration> fields = n.getFields();
//            for (FieldDeclaration field : fields) {
//                for (VariableDeclarator variable : field.getVariables()) {
//                    System.out.println("Field: " + variable.getNameAsString() + " of type " + variable.getType());
//                }
//            }

            // 获取方法
//            List<MethodDeclaration> methods = n.getMethods();
//            for (MethodDeclaration method : methods) {
//                System.out.println("Method: " + method.getNameAsString());
//            }

            // 方法
            n.getMethods().forEach(method -> {
                System.out.println("Method: " + method.getNameAsString());

                method.getParameters().forEach(parameter -> {
                    System.out.println("Parameter: " + parameter.getType() + " name: " + parameter.getNameAsString());

                    if (parameter.getType().resolve().isReferenceType()) {
                        System.out.println("param qualify name: " + parameter.getType().resolve().asReferenceType().getQualifiedName());

                    }

                });

            });

            n.getConstructors().forEach(constructor -> {
                System.out.println("Constructor: " + constructor.getNameAsString());
                constructor.getParameters().forEach(parameter -> {
                    System.out.println("Parameter: " + parameter.getType() + " name: " + parameter.getNameAsString());
                });

                // 使用访问者模式遍历构造函数体
                constructor.getBody().accept(new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(MethodCallExpr methodCall, Void arg) {
                        super.visit(methodCall, arg);
                        System.out.println("Method Call: " + methodCall.getName() + " at line " + methodCall.getBegin().get().line);
                    }

                    @Override
                    public void visit(VariableDeclarator variable, Void arg) {
                        super.visit(variable, arg);
                        System.out.println("Variable Declaration: " + variable.getName() + " of type " + variable.getType() + " at line " + variable.getBegin().get().line);
                    }
                }, null);
            });


        }
    }
}


//public class MethodFinder {

//}

//import com.github.javaparser.JavaParser;
//import com.github.javaparser.StaticJavaParser;
//import com.github.javaparser.ast.CompilationUnit;
//import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
//import com.github.javaparser.ast.body.FieldDeclaration;
//import com.github.javaparser.ast.body.MethodDeclaration;
//import com.github.javaparser.ast.body.VariableDeclarator;
//import com.github.javaparser.ast.type.ClassOrInterfaceType;
//
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.util.List;
//
//public class LogicPositivizer {
//    public static void main(String[] args) throws FileNotFoundException {
//        // 解析 Java 文件
//        FileInputStream in = new FileInputStream("src/main/resources/Blabla.java");
//        CompilationUnit cu = StaticJavaParser.parse(in);
//
//        // 获取类声明
//        ClassOrInterfaceDeclaration classDeclaration = cu.findFirst(ClassOrInterfaceDeclaration.class).get();
//
//        // 获取类名
//        String className = classDeclaration.getNameAsString();
//        System.out.println("Class Name: " + className);
//
//        // 获取继承信息
//        List<ClassOrInterfaceType> extendedTypes = classDeclaration.getExtendedTypes();
//        for (ClassOrInterfaceType extendedType : extendedTypes) {
//            System.out.println("Extends: " + extendedType.getNameAsString());
//        }
//
//        // 获取实现的接口信息
//        List<ClassOrInterfaceType> implementedTypes = classDeclaration.getImplementedTypes();
//        for (ClassOrInterfaceType implementedType : implementedTypes) {
//            System.out.println("Implements: " + implementedType.getNameAsString());
//        }
//
//        // 获取成员变量
//        List<FieldDeclaration> fields = classDeclaration.getFields();
//        for (FieldDeclaration field : fields) {
//            for (VariableDeclarator variable : field.getVariables()) {
//                System.out.println("Field: " + variable.getNameAsString() + " of type " + variable.getType());
//            }
//        }
//
//        // 获取方法
//        List<MethodDeclaration> methods = classDeclaration.getMethods();
//        for (MethodDeclaration method : methods) {
//            System.out.println("Method: " + method.getNameAsString());
//        }
//    }
//}