package org.example.temp;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SchemaGenerator {

    public static void main(String[] args) {
//        if (args.length != 2) {
//            System.out.println("Usage: java SchemaGenerator <path/to/main/package> <path/to/output/file.yaml>");
//            return;
//        }

        String mainPackagePath = "C:\\Users\\jakumarr\\Downloads\\jskr456\\java-concepts\\src\\main\\java\\com\\java\\examples";
        String outputFilePath = "C:\\Users\\jakumarr\\Downloads\\jskr456\\java-concepts\\src\\main\\java\\com\\java\\examples\\temp\\output.yaml";

        try {
            List<File> javaFiles = findJavaFiles(new File(mainPackagePath));
            Map<String, Map<String, Object>> allSchemas = new LinkedHashMap<>();

            for (File file : javaFiles) {
                Map<String, Object> schema = generateSchema(file);
                String className = (String) schema.get("title");
                allSchemas.put(className, schema);
            }

            writeSchemaToYamlFile(allSchemas, outputFilePath);
            System.out.println("Schema generation completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<File> findJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        if (!directory.exists() || !directory.isDirectory()) {
            return javaFiles;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    javaFiles.addAll(findJavaFiles(file));
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }
        return javaFiles;
    }

    private static Map<String, Object> generateSchema(File javaFile) throws IOException {
        System.out.println("Generating schema for: " + javaFile.getName());

        String content = readFileContent(javaFile);
        String className = extractClassName(content);
        List<Field> fields = extractFields(content);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("title", className);

        Map<String, Object> properties = new LinkedHashMap<>();
        for (Field field : fields) {
            Map<String, String> fieldSchema = new LinkedHashMap<>();
            fieldSchema.put("type", getJsonType(field.type));
            properties.put(field.name, fieldSchema);
        }
        schema.put("properties", properties);

        return schema;
    }

    private static void writeSchemaToYamlFile(Map<String, Map<String, Object>> allSchemas, String outputFilePath) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(outputFilePath)) {
            yaml.dump(allSchemas, writer);
        }

        System.out.println("All schemas written to: " + outputFilePath);
    }

    private static String readFileContent(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return new String(data, "UTF-8");
        }
    }

    private static String extractClassName(String content) {
        Pattern pattern = Pattern.compile("class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "UnknownClass";
    }

    private static List<Field> extractFields(String content) {
        List<Field> fields = new ArrayList<>();
        Pattern pattern = Pattern.compile("(private|protected|public)\\s+(\\w+)\\s+(\\w+)\\s*;");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            fields.add(new Field(matcher.group(3), matcher.group(2)));
        }
        return fields;
    }

    private static String getJsonType(String javaType) {
        switch (javaType) {
            case "int":
            case "long":
            case "short":
            case "byte":
                return "integer";
            case "float":
            case "double":
                return "number";
            case "boolean":
                return "boolean";
            case "char":
            case "String":
                return "string";
            default:
                return "object";
        }
    }

    private static class Field {
        String name;
        String type;

        Field(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }
}