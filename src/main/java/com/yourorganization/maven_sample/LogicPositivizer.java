package com.yourorganization.maven_sample;


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
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;

public class LogicPositivizer {

    public static void main2(String[] args) throws FileNotFoundException {
        FileInputStream in = new FileInputStream("src/main/resources/Blabla.java");

//        FileInputStream in = new FileInputStream("D:\\codefile\\archive\\emotion_master\\src\\main\\java\\com\\project\\controller\\employeeBasicController.java");
//
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
//        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File("D:\\codefile\\archive\\emotion_master\\src\\main\\java"));
        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File("src/main/resources"));

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

    public static void main(String[] args) throws FileNotFoundException {
        // 解析 Java 源文件
        FileInputStream in = new FileInputStream("src/main/resources/Test.java");

//        FileInputStream in = new FileInputStream("D:\\codefile\\archive\\emotion_master\\src\\main\\java\\com\\project\\controller\\employeeBasicController.java");

        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
//        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File("D:\\codefile\\archive\\emotion_master\\src\\main\\java"));
        TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File("src/main/resources"));

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
            System.out.println("Class: " + className);

        });
        ArrayList<ClassInfo> classInfos = new ArrayList<>();
        // 遍历 CompilationUnit 中的类声明
        cu.accept(new ClassVisitor(), classInfos);

        // 输出所有静态方法调用表达式及其所属的类
        // Todo: 存在问题
        for (MethodCallExpr mc : methodCalls) {
//            try {
                ResolvedMethodDeclaration resolvedMethodDecl = mc.resolve();
                if (resolvedMethodDecl.isStatic()) {
                    System.out.println("Static method call found: " + mc.getName() + " in class " +
                            resolvedMethodDecl.getClassName());
                }
//            } catch (Exception e) {
//                System.err.println("Unable to resolve method call: " + mc.toString());
//            }
        }

        for (ClassInfo classInfo : classInfos) {
            System.out.println(classInfo.getClassName());
            System.out.println("Associations: ");
            for (String association : classInfo.getAssociations()) {
                System.out.print(association + " ");
            }
            System.out.println();

            System.out.print("Compositions: ");
            for (String composition : classInfo.getCompositions()) {
                System.out.print(composition + " ");
            }
            System.out.println();


            System.out.print("Aggregations: ");
            for (String aggregation : classInfo.getAggregations()) {
                System.out.print(aggregation + " ");
            }

            System.out.println();


            System.out.print("Extends: ");
            for (String extendtype : classInfo.getExtendTypes()) {
                System.out.print(extendtype + " ");
            }

            System.out.println();


            System.out.print("Implements: ");
            for (String implementType : classInfo.getImplementors()) {
                System.out.print(implementType + " ");
            }
            System.out.println();

            System.out.print("Dependency: ");
            for (String dependency : classInfo.getDependency()) {
                System.out.print(dependency + " ");
            }
            System.out.println();

        }

//        FieldDeclaration fieldDeclaration  = Navigator.demandNodeOfGivenClass(cu, FieldDeclaration.class);
//        System.out.println(fieldDeclaration.getVariables().get(0).getType().resolve().asReferenceType().getQualifiedName());
    }
    static ArrayList<MethodCallExpr> methodCalls = new ArrayList<>();

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
            ResolvedMethodDeclaration resolvedMethodDecl = methodCall.resolve();
            if (resolvedMethodDecl.isStatic()) {
                System.out.println("Static method call found: " + methodCall.getName() + " in class " +
                        resolvedMethodDecl.declaringType().getQualifiedName());
            }
            classInfo.CheckAndAddDependency(resolvedMethodDecl.declaringType().getQualifiedName());

        }
    }

    // 收集局部变量的访问者类
    private static HashSet<String> allClass = new HashSet<>();
    private static class ClassVisitor extends VoidVisitorAdapter<ArrayList<ClassInfo>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration n, ArrayList<ClassInfo> arg) {
            super.visit(n, arg);


            // 获取类标识名
            String className = n.resolve().getQualifiedName();
            System.out.println(className);

            ClassInfo classInfo = new ClassInfo(className);
            arg.add(classInfo);

            // 获取聚合类型 同时寻找依赖类型: 1. 使用其他类的静态方法 (done) 2. 接受其他类作为函数参数(done) 3. 使用其他类的局部变量
            n.getMethods().forEach(method -> {
                method.accept(new MethodCallVisitor(), classInfo);

                LocalVariableCollector collector = new LocalVariableCollector();
                method.accept(collector, null);
                // 获取所有局部变量声明
                ArrayList<VariableDeclarator> variables = collector.getVariables();
                for (VariableDeclarator var : variables) {
                    String qualifiedName = var.getType().resolve().asReferenceType().getQualifiedName();
                    if (!allClass.contains(qualifiedName)) {
                        return;
                    }
                    classInfo.CheckAndAddDependency(qualifiedName);
                }

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
                    classInfo.CheckAndAddDependency(paramType);
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
                String parentClass = extendedType.resolve().asReferenceType().getQualifiedName();
                if (!allClass.contains(parentClass)) {
                    return;
                }
                classInfo.AddExtendType(parentClass);
            });

            // 获取实现的接口
            n.getImplementedTypes().forEach(implementedType -> {
                String interfaceName = implementedType.resolve().asReferenceType().getQualifiedName();
                if (!allClass.contains(interfaceName)) {
                    return;
                }
                classInfo.AddImplementor(interfaceName);
            });


            // 成员变量
            // 成员变量可以识别的类型有 3 种
            // 1. 组合: 成员变量在构造函数或构造函数调用的其他函数内初始化
            // 2. 聚合: 成员变量可以独立与类生命周期存在，可以通过构造函数或者 set 函数传入
            // 3. 关联: 成员变量中既不是聚合也不是组合
            // Todo: 考虑数组及 ArrayList 等泛型容器的情况
//            n.getFields().forEach(field -> {
//                field.getVariables().forEach(variable -> {
//                    System.out.println("Field: " + variable.getNameAsString() + " of type " + variable.getType());
//
//                    if (variable.getType().resolve().isReferenceType()) {
//                        System.out.println(variable.getType().resolve().asReferenceType().getQualifiedName());
//
//
//                        // not in the class set return
//                        String qualifyName = variable.getType().resolve().asReferenceType().getQualifiedName();
//
//                        Type type = variable.getType();
//                        if (type instanceof ClassOrInterfaceType) {
//                            ClassOrInterfaceType classType = (ClassOrInterfaceType) type;
//                            if (classType.getTypeArguments().isPresent()) {
//                                classType.getTypeArguments().get().forEach(typeArgument -> {
//                                    if (typeArgument.resolve().isReferenceType()) {
//                                        System.out.println("Generic type: " + typeArgument.resolve().asReferenceType().getQualifiedName());
//                                    }
//                                });
//                            }
//                        }
//                    }
//
//                    Type fieldType = variable.getType();
//                    if (fieldType instanceof ArrayType) {
//                        ArrayType arrayType = (ArrayType) fieldType;
//                        Type componentType = arrayType.getComponentType();
//                        if (componentType instanceof ClassOrInterfaceType) {
//                            ClassOrInterfaceType classType = (ClassOrInterfaceType) componentType;
//                            System.out.println("Qualified name: " + classType.resolve().asReferenceType().getQualifiedName() + "[]");
//                        }
//                    }
//
//                });
//            });



        }
    }
}
