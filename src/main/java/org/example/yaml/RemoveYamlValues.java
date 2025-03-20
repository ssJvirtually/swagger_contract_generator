package org.example.yaml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class RemoveYamlValues {
    public static void main(String[] args) {
        String inputFilePath = "input.yaml";
        String outputFilePath = "output.yaml";

        try {
            FileInputStream inputStream = new FileInputStream(inputFilePath);
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object data = yaml.load(inputStream);

            if (data instanceof Map) {
                Map<String, Object> updatedYaml = removeValues((Map<String, Object>) data);

                DumperOptions options = new DumperOptions();
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                yaml = new Yaml(options);

                FileWriter writer = new FileWriter(outputFilePath);
                yaml.dump(updatedYaml, writer);
                writer.close();

                System.out.println("YAML values removed successfully. Output saved in: " + outputFilePath);
            } else {
                System.out.println("Invalid YAML structure.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Object> removeValues(Map<String, Object> map) {
        Map<String, Object> cleanedMap = new LinkedHashMap<>();
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof Map) {
                cleanedMap.put(key, removeValues((Map<String, Object>) value));
            } else {
                cleanedMap.put(key, new String()); // Remove value and retain key with an empty value
            }
        }
        return cleanedMap;
    }
}
