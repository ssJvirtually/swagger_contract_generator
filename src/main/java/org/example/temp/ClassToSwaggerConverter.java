package org.example.temp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ClassToSwaggerConverter {
    private static final Map<String, Map<String, Object>> TYPE_MAPPINGS = new HashMap<>();
    private static final Set<String> COLLECTION_TYPES = new HashSet<>(Arrays.asList(
            "List", "ArrayList", "LinkedList", "Set", "HashSet", "TreeSet", "Collection"
    ));
    private static Map<String, ClassOrInterfaceDeclaration> classCache = new HashMap<>();
    private static Map<String, EnumDeclaration> enumCache = new HashMap<>();
    private static CombinedTypeSolver typeSolver;

    static {
        // Initialize primitive types
        addTypeMapping("boolean", "boolean", null);
        addTypeMapping("byte", "integer", "int32");
        addTypeMapping("short", "integer", "int32");
        addTypeMapping("int", "integer", "int32");
        addTypeMapping("int2", "integer", "int32");
        addTypeMapping("long", "integer", "int64");
        addTypeMapping("float", "number", "float");
        addTypeMapping("double", "number", "double");
        addTypeMapping("char", "string", null);

        // Initialize wrapper types
        addTypeMapping("Boolean", "boolean", null);
        addTypeMapping("Byte", "integer", "int32");
        addTypeMapping("Short", "integer", "int32");
        addTypeMapping("Integer", "integer", "int32");
        addTypeMapping("Long", "integer", "int64");
        addTypeMapping("Float", "number", "float");
        addTypeMapping("Double", "number", "double");
        addTypeMapping("Character", "string", null);

        // Common Java types
        addTypeMapping("String", "string", null);
        addTypeMapping("BigDecimal", "number", null);
        addTypeMapping("BigInteger", "integer", null);
        addTypeMapping("Date", "string", "date-time");
        addTypeMapping("LocalDate", "string", "date");
        addTypeMapping("LocalTime", "string", "time");
        addTypeMapping("LocalDateTime", "string", "date-time");
        addTypeMapping("ZonedDateTime", "string", "date-time");
        addTypeMapping("OffsetDateTime", "string", "date-time");
        addTypeMapping("Instant", "string", "date-time");
        addTypeMapping("UUID", "string", "uuid");
        addTypeMapping("URI", "string", "uri");
        addTypeMapping("URL", "string", "url");
    }

    private static void addTypeMapping(String javaType, String swaggerType, String format) {
        Map<String, Object> typeInfo = new HashMap<>();
        typeInfo.put("type", swaggerType);
        if (format != null) {
            typeInfo.put("format", format);
        }
        TYPE_MAPPINGS.put(javaType, typeInfo);
    }

    public static void main(String[] args) {
        try {
            String sourceDir = "C:\\Users\\jskr4\\Downloads\\swagger_contract_generator\\src\\main\\java\\org\\example\\temp\\model";
            String outputFile = "comp.yaml";

            // Initialize type solver
            typeSolver = new CombinedTypeSolver(
                    new ReflectionTypeSolver(),
                    new JavaParserTypeSolver(new File(sourceDir))
            );

            // Configure JavaParser to use the type solver
            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
            StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

            // First pass: cache all classes and enums
            cacheTypesFromDirectory(sourceDir);

            // Second pass: process classes and generate schemas
            Map<String, Map<String, Object>> schemas = processDirectory(sourceDir);
            generateYaml(schemas, outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void cacheTypesFromDirectory(String sourceDir) throws Exception {
        Files.walk(Paths.get(sourceDir))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(path);
                        // Cache classes
                        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
                            classCache.put(classDecl.getNameAsString(), classDecl);
                        });
                        // Cache enums
                        cu.findAll(EnumDeclaration.class).forEach(enumDecl -> {
                            enumCache.put(enumDecl.getNameAsString(), enumDecl);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private static Map<String, Map<String, Object>> processDirectory(String sourceDir) throws Exception {
        Map<String, Map<String, Object>> schemas = new LinkedHashMap<>();

        // Process enums first
        for (EnumDeclaration enumDecl : enumCache.values()) {
            processEnum(enumDecl, schemas);
        }

        // Then process classes
        for (ClassOrInterfaceDeclaration classDecl : classCache.values()) {
            processClass(classDecl, schemas);
        }

        return schemas;
    }

    private static void processEnum(EnumDeclaration enumDecl, Map<String, Map<String, Object>> schemas) {
        String enumName = enumDecl.getNameAsString();

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");

        // Extract enum constants
        List<String> enumValues = new ArrayList<>();
        enumDecl.getEntries().forEach(entry -> {
            enumValues.add(entry.getNameAsString());
        });

        schema.put("enum", enumValues);

        // Add description if JavaDoc is present
        enumDecl.getJavadoc().ifPresent(javadoc -> {
            schema.put("description", javadoc.getDescription().toText());
        });

        schemas.put(enumName, schema);
    }

    private static void processClass(ClassOrInterfaceDeclaration classDecl,
                                     Map<String, Map<String, Object>> schemas) {
        String className = classDecl.getNameAsString();

        if (schemas.containsKey(className)) {
            return;
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        Map<String, Object> properties = new LinkedHashMap<>();

        schema.put("type", "object");

        // Add description if JavaDoc is present
        classDecl.getJavadoc().ifPresent(javadoc -> {
            schema.put("description", javadoc.getDescription().toText());
        });

        // Process parent classes
        try {
            classDecl.getExtendedTypes().forEach(parentType -> {
                String parentName = parentType.getNameAsString();
                if (classCache.containsKey(parentName)) {
                    processClass(classCache.get(parentName), schemas);

                    Map<String, Object> parentSchema = schemas.get(parentName);
                    if (parentSchema != null && parentSchema.containsKey("properties")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parentProps = (Map<String, Object>) parentSchema.get("properties");
                        properties.putAll(parentProps);
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Warning: Could not process inheritance for class " + className + ": " + e.getMessage());
        }

        // Process fields
        classDecl.getFields().forEach(field -> {
            String fieldName = field.getVariable(0).getNameAsString();
            Type fieldType = field.getVariable(0).getType();

            Map<String, Object> propertySchema = createPropertySchema(fieldType);
            if (propertySchema != null) {
                // Add field description if JavaDoc is present
                field.getJavadoc().ifPresent(javadoc -> {
                    propertySchema.put("description", javadoc.getDescription().toText());
                });
                properties.put(fieldName, propertySchema);
            }
        });

        schema.put("properties", properties);
        schemas.put(className, schema);
    }

    private static Map<String, Object> createPropertySchema(Type type) {
        Map<String, Object> schema = new LinkedHashMap<>();

        // Handle arrays
        if (type.isArrayType()) {
            ArrayType arrayType = type.asArrayType();
            schema.put("type", "array");
            schema.put("items", createPropertySchema(arrayType.getComponentType()));
            return schema;
        }

        // Handle collections and generics
        if (type.isClassOrInterfaceType()) {
            ClassOrInterfaceType classType = type.asClassOrInterfaceType();
            String typeName = classType.getNameAsString();

            // Check if it's an enum
            if (enumCache.containsKey(typeName)) {
                schema.put("$ref", "#/components/schemas/" + typeName);
                return schema;
            }

            // Handle collection types
            if (COLLECTION_TYPES.contains(typeName)) {
                schema.put("type", "array");
                if (classType.getTypeArguments().isPresent() && !classType.getTypeArguments().get().isEmpty()) {
                    Type genericType = classType.getTypeArguments().get().get(0);
                    schema.put("items", createPropertySchema(genericType));
                } else {
                    schema.put("items", createObjectSchema());
                }
                return schema;
            }

            // Handle Maps
            if (typeName.equals("Map") || typeName.equals("HashMap") || typeName.equals("TreeMap")) {
                schema.put("type", "object");
                if (classType.getTypeArguments().isPresent() && classType.getTypeArguments().get().size() == 2) {
                    Type valueType = classType.getTypeArguments().get().get(1);
                    schema.put("additionalProperties", createPropertySchema(valueType));
                } else {
                    schema.put("additionalProperties", createObjectSchema());
                }
                return schema;
            }

            // Handle regular types
            String baseType = typeName.replaceAll("<.*>", "");
            if (TYPE_MAPPINGS.containsKey(baseType)) {
                return new HashMap<>(TYPE_MAPPINGS.get(baseType));
            } else {
                schema.put("$ref", "#/components/schemas/" + baseType);
                return schema;
            }
        }

        return createObjectSchema();
    }

    private static Map<String, Object> createObjectSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        return schema;
    }

    private static void generateYaml(Map<String, Map<String, Object>> schemas, String outputFile) throws Exception {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> components = new LinkedHashMap<>();

        root.put("components", components);
        components.put("schemas", schemas);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.writeValue(new File(outputFile), root);

        System.out.println("Generated Swagger schema at: " + outputFile);
    }
}