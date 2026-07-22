package com.oigit.admin.boot.config;

import com.oigit.admin.core.enums.BaseEnum;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Configuration
public class OpenApiConfig {

    private static final String CONDITION_PROPERTY = "condition";
    private static final String ENUM_VO_REF = "#/components/schemas/EnumVO";
    private static final List<String> OPENAPI_DTO_SCAN_PACKAGES = List.of(
        "com.oigit.admin.core",
        "com.oigit.admin.file.controller.dto",
        "com.oigit.admin.dict.controller.dto",
        "com.oigit.admin.mdm.export.controller.dto",
        "com.oigit.admin.staff.controller.dto"
    );

    private static final List<DynamicQuerySceneSchema> DYNAMIC_QUERY_SCENE_SCHEMAS = List.of(
        new DynamicQuerySceneSchema(
            "GlobalDictTypeDynamicListReqDTO",
            "GlobalDictTypeConditionNode",
            Map.of(
                "GlobalDictTypeGroupCondition", "compose",
                "GlobalDictTypeTextCondition", "text",
                "GlobalDictTypeDateTimeCondition", "dateTime"
            )
        ),
        new DynamicQuerySceneSchema(
            "ExportRecordDynamicPageReqDTO",
            "ExportRecordConditionNode",
            Map.of(
                "ExportRecordGroupCondition", "compose",
                "ExportRecordTextCondition", "text",
                "ExportRecordDateTimeCondition", "dateTime",
                "ExportRecordEnumCondition", "enum"
            )
        ),
        new DynamicQuerySceneSchema(
            "GlobalDictItemDynamicPageReqDTO",
            "GlobalDictItemConditionNode",
            Map.of(
                "GlobalDictItemGroupCondition", "compose",
                "GlobalDictItemTextCondition", "text",
                "GlobalDictItemDateTimeCondition", "dateTime"
            )
        )
    );

    @Bean
    public OpenAPI demoOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("java-admin-starter API 文档")
                .description("java-admin-starter 项目接口文档")
                .version("v1")
                .contact(new Contact().name("java-admin-starter")));
    }

    @Bean
    public OpenApiCustomizer enumVoSchemaCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            if (openApi.getComponents().getSchemas().containsKey("EnumVO")) {
                return;
            }
            Schema<?> enumVo = new Schema<>()
                .type("object")
                .addProperty("code", new StringSchema().description("编码"))
                .addProperty("desc", new StringSchema().description("描述"));
            openApi.getComponents().addSchemas("EnumVO", enumVo);
        };
    }

    @Bean
    public OpenApiCustomizer dynamicQuerySchemaRefCustomizer() {
        return openApi -> {
            DYNAMIC_QUERY_SCENE_SCHEMAS.forEach(scene -> {
                rewritePropertyAsRef(
                    openApi,
                    scene.requestSchemaName(),
                    CONDITION_PROPERTY,
                    "#/components/schemas/" + scene.conditionSchemaName()
                );
                rewritePolymorphicNodeSchema(openApi, scene.conditionSchemaName(), scene.nodeMappings());
            });
            deduplicateEnumValues(openApi);
        };
    }

    @Bean
    public OpenApiCustomizer baseEnumSchemaCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            Map<String, Class<?>> schemaClasses = scanOpenApiDtoClasses(schemas.keySet());
            schemaClasses.forEach((schemaName, schemaClass) ->
                rewriteBaseEnumProperties(schemas.get(schemaName), schemaName, schemaClass)
            );
        };
    }

    private static Map<String, Class<?>> scanOpenApiDtoClasses(Set<String> schemaNames) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));

        Map<String, Class<?>> classes = new LinkedHashMap<>();
        Set<String> ambiguousSchemaNames = new LinkedHashSet<>();
        OPENAPI_DTO_SCAN_PACKAGES.forEach(basePackage ->
            scanner.findCandidateComponents(basePackage).forEach(candidate -> {
                Class<?> clazz = loadClass(candidate);
                if (clazz == null || !isDtoSchemaClass(clazz)) {
                    return;
                }
                String schemaName = resolveSchemaName(clazz);
                if (!schemaNames.contains(schemaName)) {
                    return;
                }
                Class<?> previous = classes.putIfAbsent(schemaName, clazz);
                if (previous != null && !previous.equals(clazz)) {
                    ambiguousSchemaNames.add(schemaName);
                }
            })
        );
        ambiguousSchemaNames.forEach(classes::remove);
        return classes;
    }

    private static Class<?> loadClass(BeanDefinition candidate) {
        String className = candidate.getBeanClassName();
        if (className == null) {
            return null;
        }
        try {
            return ClassUtils.forName(className, OpenApiConfig.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError ignored) {
            return null;
        }
    }

    private static boolean isDtoSchemaClass(Class<?> clazz) {
        int modifiers = clazz.getModifiers();
        return !clazz.isInterface()
            && !clazz.isEnum()
            && !Modifier.isAbstract(modifiers)
            && (clazz.getSimpleName().endsWith("ReqDTO") || clazz.getSimpleName().endsWith("RspDTO"));
    }

    private static String resolveSchemaName(Class<?> clazz) {
        io.swagger.v3.oas.annotations.media.Schema schema = clazz.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        if (schema != null && !schema.name().isBlank()) {
            return schema.name();
        }
        return clazz.getSimpleName();
    }

    private static void rewriteBaseEnumProperties(Schema<?> schema, String schemaName, Class<?> schemaClass) {
        if (schema == null) {
            return;
        }
        boolean requestSchema = schemaName.endsWith("ReqDTO");
        boolean responseSchema = schemaName.endsWith("RspDTO");
        if (!requestSchema && !responseSchema) {
            return;
        }
        getAllFields(schemaClass).forEach(field -> {
            if (!BaseEnum.class.isAssignableFrom(field.getType())) {
                return;
            }
            if (requestSchema) {
                rewriteBaseEnumRequestProperty(schema, field);
            } else {
                rewriteBaseEnumResponseProperty(schema, field.getName());
            }
        });
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && !Object.class.equals(current)) {
            Collections.addAll(fields, current.getDeclaredFields());
            current = current.getSuperclass();
        }
        return fields;
    }

    @SuppressWarnings("unchecked")
    private static void rewriteBaseEnumRequestProperty(Schema<?> schema, Field field) {
        Schema<?> propertySchema = findPropertySchema(schema, field.getName());
        if (propertySchema == null) {
            return;
        }
        propertySchema.set$ref(null);
        propertySchema.setType("string");
        propertySchema.setFormat(null);
        setEnumValues(propertySchema, enumCodes((Class<? extends BaseEnum>) field.getType()));
    }

    private static void rewriteBaseEnumResponseProperty(Schema<?> schema, String propertyName) {
        Map<String, Schema> properties = schema.getProperties();
        if (properties == null || !properties.containsKey(propertyName)) {
            return;
        }
        properties.put(propertyName, new Schema<>().$ref(ENUM_VO_REF));
    }

    private static Schema<?> findPropertySchema(Schema<?> schema, String propertyName) {
        if (schema == null) {
            return null;
        }
        Map<String, Schema> properties = schema.getProperties();
        if (properties != null && properties.containsKey(propertyName)) {
            return properties.get(propertyName);
        }
        if (schema.getAllOf() == null) {
            return null;
        }
        for (Schema<?> item : schema.getAllOf()) {
            Schema<?> found = findPropertySchema(item, propertyName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static List<String> enumCodes(Class<? extends BaseEnum> enumType) {
        BaseEnum[] constants = enumType.getEnumConstants();
        if (constants == null) {
            return List.of();
        }
        List<String> codes = new ArrayList<>(constants.length);
        for (BaseEnum constant : constants) {
            codes.add(constant.getCode());
        }
        return codes;
    }

    private static void deduplicateEnumValues(OpenAPI openApi) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        openApi.getComponents().getSchemas().values().forEach(OpenApiConfig::deduplicateSchemaEnums);
    }

    private static void deduplicateSchemaEnums(Schema<?> schema) {
        if (schema == null) {
            return;
        }
        deduplicateEnums(schema);
        if (schema.getProperties() != null) {
            schema.getProperties().values().forEach(OpenApiConfig::deduplicateSchemaEnums);
        }
        if (schema.getAllOf() != null) {
            schema.getAllOf().forEach(OpenApiConfig::deduplicateSchemaEnums);
        }
        if (schema.getItems() != null) {
            deduplicateSchemaEnums(schema.getItems());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void deduplicateEnums(Schema<?> schema) {
        List enums = schema.getEnum();
        if (enums == null || enums.size() <= 1) {
            return;
        }
        LinkedHashSet<Object> deduped = new LinkedHashSet<>(enums);
        if (deduped.size() != enums.size()) {
            schema.setEnum((List) new ArrayList<>(deduped));
        }
    }

    private static void rewritePropertyAsRef(OpenAPI openApi, String schemaName, String propertyName, String ref) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        Schema<?> schema = openApi.getComponents().getSchemas().get(schemaName);
        if (schema == null) {
            return;
        }
        Map<String, Schema> properties = schema.getProperties();
        if (properties == null || !properties.containsKey(propertyName)) {
            return;
        }
        properties.put(propertyName, new Schema<>().$ref(ref));
    }

    private static void rewritePolymorphicNodeSchema(OpenAPI openApi, String baseSchemaName, Map<String, String> discriminatorMappings) {
        Schema<?> baseSchema = getSchema(openApi, baseSchemaName);
        if (baseSchema == null) {
            return;
        }
        fillDiscriminatorMappings(baseSchema, discriminatorMappings);
        populateDisplayPropertiesForPolymorphicNode(openApi, baseSchema, discriminatorMappings);
        discriminatorMappings.forEach((schemaName, discriminatorValue) ->
            addConcreteNodeTypeEnum(openApi, schemaName, discriminatorValue)
        );
    }

    private static Schema<?> getSchema(OpenAPI openApi, String schemaName) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return null;
        }
        return openApi.getComponents().getSchemas().get(schemaName);
    }

    private static void fillDiscriminatorMappings(Schema<?> schema, Map<String, String> discriminatorMappings) {
        Discriminator discriminator = schema.getDiscriminator();
        if (discriminator == null) {
            return;
        }
        Map<String, String> mapping = new LinkedHashMap<>();
        discriminatorMappings.forEach((schemaName, discriminatorValue) ->
            mapping.put(discriminatorValue, "#/components/schemas/" + schemaName)
        );
        discriminator.setMapping(mapping);
    }

    private static void populateDisplayPropertiesForPolymorphicNode(
        OpenAPI openApi,
        Schema<?> baseSchema,
        Map<String, String> discriminatorMappings
    ) {
        LinkedHashMap<String, Schema> mergedProperties = new LinkedHashMap<>();
        mergedProperties.put("nodeType", buildDiscriminatorSchema(discriminatorMappings.values()));
        discriminatorMappings.forEach((schemaName, discriminatorValue) -> {
            Schema<?> schema = getSchema(openApi, schemaName);
            if (schema == null || schema.getAllOf() == null) {
                return;
            }
            Schema<?> detailSchema = schema.getAllOf().stream()
                .filter(item -> Objects.isNull(item.get$ref()))
                .findFirst()
                .orElse(null);
            if (detailSchema == null || detailSchema.getProperties() == null) {
                return;
            }
            detailSchema.getProperties().forEach((propertyName, propertySchema) -> {
                if ("nodeType".equals(propertyName)) {
                    return;
                }
                Schema<?> copiedSchema = copySchemaForDisplay(propertySchema);
                mergedProperties.merge(propertyName, copiedSchema, OpenApiConfig::mergeDisplaySchema);
            });
        });
        baseSchema.setType("object");
        baseSchema.setProperties(mergedProperties);
        baseSchema.setRequired(List.of("nodeType"));
    }

    private static StringSchema buildDiscriminatorSchema(Iterable<String> discriminatorValues) {
        StringSchema nodeTypeSchema = new StringSchema();
        nodeTypeSchema.setDescription("节点类型");
        LinkedHashSet<String> enums = new LinkedHashSet<>();
        discriminatorValues.forEach(enums::add);
        nodeTypeSchema.setEnum(new ArrayList<>(enums));
        return nodeTypeSchema;
    }

    private static Schema<?> copySchemaForDisplay(Schema<?> source) {
        Schema<?> target = new Schema<>();
        target.set$ref(source.get$ref());
        target.setType(source.getType());
        target.setFormat(source.getFormat());
        target.setDescription(source.getDescription());
        if (source.getEnum() != null) {
            setEnumValues(target, source.getEnum());
        }
        if (source.getItems() != null) {
            target.setItems(copySchemaForDisplay(source.getItems()));
        }
        return target;
    }

    private static Schema<?> mergeDisplaySchema(Schema<?> existing, Schema<?> incoming) {
        if (Objects.equals(existing.get$ref(), incoming.get$ref())
            && Objects.equals(existing.getType(), incoming.getType())
            && Objects.equals(existing.getFormat(), incoming.getFormat())) {
            mergeEnums(existing, incoming);
            existing.setDescription(mergeDescription(existing.getDescription(), incoming.getDescription()));
            if ("array".equals(existing.getType()) && existing.getItems() != null && incoming.getItems() != null) {
                if (!Objects.equals(existing.getItems().getType(), incoming.getItems().getType())
                    || !Objects.equals(existing.getItems().get$ref(), incoming.getItems().get$ref())) {
                    existing.setItems(new Schema<>().description("不同节点类型的数组元素类型可能不同"));
                }
            }
            return existing;
        }
        Schema<?> merged = new Schema<>();
        if ("array".equals(existing.getType()) || "array".equals(incoming.getType())) {
            merged.setType("array");
            merged.setItems(new Schema<>().description("不同节点类型的数组元素类型可能不同"));
        }
        merged.setDescription(mergeDescription(existing.getDescription(), incoming.getDescription()));
        return merged;
    }

    private static void mergeEnums(Schema<?> existing, Schema<?> incoming) {
        if (existing.getEnum() == null && incoming.getEnum() == null) {
            return;
        }
        LinkedHashSet<Object> values = new LinkedHashSet<>();
        if (existing.getEnum() != null) {
            values.addAll(existing.getEnum());
        }
        if (incoming.getEnum() != null) {
            values.addAll(incoming.getEnum());
        }
        setEnumValues(existing, new ArrayList<>(values));
    }

    private static String mergeDescription(String left, String right) {
        Set<String> parts = new LinkedHashSet<>();
        if (left != null && !left.isBlank()) {
            parts.add(left);
        }
        if (right != null && !right.isBlank()) {
            parts.add(right);
        }
        return parts.isEmpty() ? null : String.join("；", parts);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setEnumValues(Schema<?> schema, List<?> values) {
        schema.setEnum((List) values);
    }

    private static void addConcreteNodeTypeEnum(OpenAPI openApi, String schemaName, String discriminatorValue) {
        Schema<?> schema = getSchema(openApi, schemaName);
        if (schema == null || schema.getAllOf() == null) {
            return;
        }
        Schema<?> detailSchema = schema.getAllOf().stream()
            .filter(item -> Objects.isNull(item.get$ref()))
            .findFirst()
            .orElse(null);
        if (detailSchema == null) {
            return;
        }
        Map<String, Schema> properties = detailSchema.getProperties();
        if (properties == null) {
            properties = new LinkedHashMap<>();
            detailSchema.setProperties(properties);
        }
        StringSchema nodeTypeSchema = new StringSchema();
        nodeTypeSchema.setDescription("节点类型");
        nodeTypeSchema.setEnum(List.of(discriminatorValue));
        properties.put("nodeType", nodeTypeSchema);
        List<String> required = detailSchema.getRequired();
        if (required == null) {
            detailSchema.setRequired(List.of("nodeType"));
            return;
        }
        if (!required.contains("nodeType")) {
            required.add("nodeType");
        }
    }

    private record DynamicQuerySceneSchema(
        String requestSchemaName,
        String conditionSchemaName,
        Map<String, String> nodeMappings
    ) {
    }
}
