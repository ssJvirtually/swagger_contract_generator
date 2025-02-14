package org.example.temp;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class PropertiesToYamlConverter {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java PropertiesToYamlConverter <input-file> <output-file>");
            return;
        }

        String inputFile = args[0];
        String outputFile = args[1];

        try {
            Map<String, Object> yamlData = parseMixedConfig(inputFile);
            writeYamlFile(yamlData, outputFile);
            System.out.println("Conversion successful! Output saved to " + outputFile);
        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
        }
    }

    private static Map<String, Object> parseMixedConfig(String inputFile) throws IOException {
        Map<String, Object> yamlData = new LinkedHashMap<>();
        Properties properties = new Properties();
        boolean isYamlBlock = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                if (line.contains(":") && !line.contains("=")) {
                    isYamlBlock = true;  // Detecting YAML block
                }

                if (isYamlBlock) {
                    parseYamlLine(yamlData, line);
                } else {
                    parsePropertiesLine(properties, line);
                }
            }
        }

        // Merge properties into the final YAML map
        properties.forEach((key, value) -> yamlData.put(key.toString(), value));
        return yamlData;
    }

    private static void parsePropertiesLine(Properties properties, String line) {
        String[] parts = line.split("=", 2);
        if (parts.length == 2) {
            properties.setProperty(parts[0].trim(), parts[1].trim());
        }
    }

    private static void parseYamlLine(Map<String, Object> yamlData, String line) {
        int colonIndex = line.indexOf(":");
        if (colonIndex > 0 && colonIndex < line.length() - 1) {
            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();
            yamlData.put(key, value);
        } else {
            yamlData.put(line, null);
        }
    }

    private static void writeYamlFile(Map<String, Object> yamlData, String outputFile) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);
        try (Writer writer = new FileWriter(outputFile)) {
            yaml.dump(yamlData, writer);
        }
    }
}
