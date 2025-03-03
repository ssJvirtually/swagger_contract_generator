package org.example.config.utils;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertiesToYamlConverter {

    public static void main(String[] args) {
//        if (args.length < 1) {
//            System.out.println("Usage: java PropertiesToYamlConverter <properties-file-path> [output-yaml-file-path]");
//            System.exit(1);
//        }

        String propertiesFilePath = "props.properties"; ;
        String yamlFilePath = args.length > 1 ? args[1] : propertiesFilePath.replace(".properties", ".yml");


        try {
            // Load properties from file
            Properties properties = loadProperties(propertiesFilePath);

            // Convert properties to YAML and save to file
            convertToYamlFile(properties, yamlFilePath);

            System.out.println("Conversion completed successfully!");
            System.out.println("YAML file created at: " + yamlFilePath);

        } catch (IOException e) {
            System.err.println("Error during conversion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Properties loadProperties(String filePath) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            properties.load(fis);
        }
        return properties;
    }

    private static void convertToYamlFile(Properties properties, String yamlFilePath) throws IOException {
        // Build hierarchical structure
        Map<String, Object> yamlMap = buildYamlHierarchy(properties);

        // Generate YAML content
        String yamlContent = convertMapToYaml(yamlMap, 0);

        // Write to file
        try (FileWriter writer = new FileWriter(yamlFilePath)) {
            writer.write(yamlContent);
        }
    }

    private static Map<String, Object> buildYamlHierarchy(Properties properties) {
        Map<String, Object> root = new HashMap<>();

        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);

            // Split the key by dots to create a hierarchy
            String[] parts = key.split("\\.");

            // Start with the root map
            Map<String, Object> current = root;

            // Process all parts except the last one
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];

                // If this level doesn't exist yet, create it
                if (!current.containsKey(part)) {
                    current.put(part, new HashMap<String, Object>());
                }

                // If it exists but is not a Map, we have a conflict
                if (!(current.get(part) instanceof Map)) {
                    // Convert the leaf value to a map with a special empty key
                    Object oldValue = current.get(part);
                    Map<String, Object> newMap = new HashMap<>();
                    newMap.put("", oldValue);
                    current.put(part, newMap);
                }

                // Move to the next level
                current = (Map<String, Object>) current.get(part);
            }

            // Set the value at the last level
            current.put(parts[parts.length - 1], convertValueToAppropriateType(value));
        }

        return root;
    }

    private static Object convertValueToAppropriateType(String value) {
        // Try to convert to appropriate types
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }

        try {
            // Try to parse as integer
            return Integer.parseInt(value);
        } catch (NumberFormatException e1) {
            try {
                // Try to parse as double
                return Double.parseDouble(value);
            } catch (NumberFormatException e2) {
                // Keep as string
                return value;
            }
        }
    }

    private static String convertMapToYaml(Map<String, Object> map, int indentLevel) {
        StringBuilder yaml = new StringBuilder();
        String indent = createIndent(indentLevel * 2);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Skip empty keys (special case for conflicts)
            if (key.isEmpty()) {
                continue;
            }

            yaml.append(indent).append(key).append(":");

            if (value instanceof Map) {
                yaml.append("\n");
                yaml.append(convertMapToYaml((Map<String, Object>) value, indentLevel + 1));
            } else {
                String formattedValue = formatValue(value);
                yaml.append(" ").append(formattedValue).append("\n");
            }
        }

        return yaml.toString();
    }

    private static String createIndent(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    private static String formatValue(Object value) {
        if (value instanceof String) {
            String strValue = (String) value;

            // Check if the string contains special characters that require quotes
            boolean needsQuotes = strValue.isEmpty() ||
                    strValue.contains(":") ||
                    strValue.contains("#") ||
                    strValue.contains("{") ||
                    strValue.contains("}") ||
                    strValue.contains("[") ||
                    strValue.contains("]") ||
                    strValue.contains(",") ||
                    strValue.contains("&") ||
                    strValue.contains("*") ||
                    strValue.contains("?") ||
                    strValue.contains("|") ||
                    strValue.contains(">") ||
                    strValue.contains("!") ||
                    strValue.contains("%") ||
                    strValue.contains("@") ||
                    strValue.contains("`") ||
                    strValue.startsWith(" ") ||
                    strValue.endsWith(" ");

            if (needsQuotes) {
                // Escape double quotes
                strValue = strValue.replace("\"", "\\\"");
                return "\"" + strValue + "\"";
            }
            return strValue;
        }

        return String.valueOf(value);
    }
}
