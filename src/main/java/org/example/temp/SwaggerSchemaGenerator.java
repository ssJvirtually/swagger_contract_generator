package org.example.temp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class SwaggerSchemaGenerator {

    public static void main(String[] args) throws Exception {
//        if (args.length < 1) {
//            System.out.println("Usage: java SwaggerSchemaGenerator <path to main package>");
//            return;
//        }

        // Path to the package (directory)
        String packagePath = "C:\\Users\\jakumarr\\Downloads\\jskr456\\java-concepts\\src\\main\\java\\com\\java\\examples";

        // Convert the file path to a package name
        String packageName = convertPathToPackageName(new File(packagePath));

        // Load classes from the package directory
        File packageDirectory = new File(packagePath);
        ClassLoader classLoader = new URLClassLoader(new URL[]{packageDirectory.toURI().toURL()});

        // Recursively get all class files
        Map<String, Schema> schemas = new HashMap<>();
        collectAndProcessClasses(packageDirectory, packageName, classLoader, schemas);

        // Print the final schema
        printSchemasAsJson(schemas);
    }

    private static void collectAndProcessClasses(File directory, String packageName, ClassLoader classLoader, Map<String, Schema> schemas) throws ClassNotFoundException {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Recursively collect classes from subdirectories
                    collectAndProcessClasses(file, packageName + "." + file.getName(), classLoader, schemas);
                } else if (file.getName().endsWith(".class")) {
                    // Process .class files
                    String className = packageName + "." + file.getName().replace(".class", "");
                    Class<?> clazz = classLoader.loadClass(className);
                    schemas.putAll(generateSchemaForClass(clazz));
                }
            }
        }
    }

    private static String convertPathToPackageName(File file) {
        String absolutePath = file.getAbsolutePath();
        return absolutePath.replace(File.separatorChar, '.');
    }

    private static Map<String, Schema> generateSchemaForClass(Class<?> clazz) {
        // Use ModelConverters to generate the OpenAPI Schema for each class
        return ModelConverters.getInstance().readAll(clazz);
    }

    private static void printSchemasAsJson(Map<String, Schema> schemas) {
        try {
            OpenAPI openAPI = new OpenAPI().components(new Components().schemas(schemas));
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(openAPI);
            System.out.println(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
