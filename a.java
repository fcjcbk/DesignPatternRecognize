import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PackageClassLister {

    public static void main(String[] args) {
        String projectPath = "./src"; // 替换为你的项目路径
        Map<String, Set<String>> packageClassesMap = new HashMap<>();

        try {
            Files.walk(Paths.get(projectPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> processFile(path.toFile(), packageClassesMap));
        } catch (IOException e) {
            e.printStackTrace();
        }

        packageClassesMap.forEach((pkg, classes) -> {
            System.out.println("Package: " + pkg);
            classes.forEach(cls -> System.out.println("  Class: " + cls));
        });

        // print the content in hashmap
        for (var entry : packageClassesMap.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    private static void processFile(File file, Map<String, Set<String>> packageClassesMap) {
        try {
            Files.lines(file.toPath()).forEach(line -> {
                if (line.startsWith("package ")) {
                    String packageName = line.substring(8, line.indexOf(';')).trim();
                    packageClassesMap.putIfAbsent(packageName, new HashSet<>());
                } else if (line.startsWith("public class ") || line.startsWith("class ")) {
                    String className = line.split("\\s+")[2];
                    packageClassesMap.forEach((pkg, classes) -> {
                        if (file.getPath().contains(pkg.replace('.', File.separatorChar))) {
                            classes.add(className);
                        }
                    });
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}  