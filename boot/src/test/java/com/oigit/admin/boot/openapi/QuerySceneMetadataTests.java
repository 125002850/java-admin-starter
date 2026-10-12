package com.oigit.admin.boot.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import com.oigit.admin.core.query.validation.DynamicQueryLimits;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class QuerySceneMetadataTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired List<SceneQueryDefinition<?>> scenes;

    @Test
    void exposesEverySceneFromItsAuthoritativeDefinition() throws Exception {
        String json = mvc.perform(post("/api/system/query-scene/metadata")
                .header("X-User-Id", "901"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        var root = mapper.readTree(json);
        assertThat(root.path("code").asInt()).isEqualTo(200);
        var data = root.path("data");
        assertThat(data.size()).isEqualTo(scenes.size()).isPositive();
        for (var scene : scenes) {
            var metadata = data.path(scene.sceneCode());
            var fields = metadata.path("fields");
            var keys = new java.util.TreeSet<String>();
            keys.addAll(scene.textFields().keySet());
            keys.addAll(scene.dateTimeFields().keySet());
            keys.addAll(scene.enumFields().keySet());
            keys.addAll(scene.booleanFields().keySet());
            keys.addAll(scene.numberFields().keySet());
            keys.addAll(scene.customFieldTypes().keySet());
            assertThat(fields.size()).isEqualTo(keys.size());
            for (String key : keys) {
                var field = fields.path(key);
                assertThat(field.size()).isEqualTo(4);
                assertThat(field.path("label").asText()).isEqualTo(scene.fieldLabels().getOrDefault(key, key));
                assertThat(strings(field.path("operators"))).containsExactlyInAnyOrderElementsOf(
                        scene.allowedOperators(key).stream().map(Enum::name).toList());
                assertThat(field.path("valueLabels")).isEqualTo(mapper.valueToTree(scene.valueLabels().getOrDefault(key, java.util.Map.of())));
                assertThat(field.path("conditionType").asText()).isEqualTo(switch (scene.fieldType(key)) {
                    case TEXT -> "text";
                    case DATE_TIME -> "dateTime";
                    case ENUM -> "enum";
                    case NUMBER -> "number";
                    case BOOLEAN -> "boolean";
                });
            }
            for (String key : scene.sortFields().keySet()) {
                if (!keys.contains(key)) assertThat(fields.has(key)).isFalse();
            }
            assertThat(metadata.path("limits").path("maxDepth").asInt()).isEqualTo(DynamicQueryLimits.MAX_DEPTH);
            assertThat(metadata.path("limits").path("maxNodes").asInt()).isEqualTo(DynamicQueryLimits.MAX_NODE_COUNT);
            assertThat(metadata.path("limits").path("maxInSize").asInt()).isEqualTo(DynamicQueryLimits.MAX_IN_SIZE);
            assertThat(metadata.path("limits").path("maxComplexityScore").asInt()).isEqualTo(scene.maxComplexityScore());
        }
        String output = System.getProperty("admin.metadata.output");
        if (output != null) Files.writeString(Path.of(output), json);
    }

    @Test
    void publishesTypedMetadataAndStableOperationId() throws Exception {
        var json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        var root = mapper.readTree(json);
        assertThat(root.path("paths").path("/api/system/query-scene/metadata").path("post").path("operationId").asText())
                .isEqualTo("getQuerySceneMetadata");
        String output = System.getProperty("admin.openapi.output");
        if (output != null) Files.writeString(Path.of(output), json);
        var schemas = root.path("components").path("schemas");
        var field = schemas.path("QuerySceneFieldRspDTO");
        assertThat(strings(field.path("required"))).containsExactlyInAnyOrder("label", "conditionType", "operators", "valueLabels");
        var type = field.path("properties").path("conditionType");
        assertThat(type.path("type").asText()).isEqualTo("string");
        assertThat(strings(type.path("enum"))).containsExactly("text", "dateTime", "enum", "number", "boolean");
        assertThat(type.has("pattern")).isFalse();
        assertThat(type.path("nullable").asBoolean()).isFalse();
        assertThat(field.path("properties").path("operators").path("type").asText()).isEqualTo("array");
        assertThat(strings(field.path("properties").path("operators").path("items").path("enum")))
                .containsExactlyInAnyOrder("EQ", "NE", "CONTAINS", "NOT_CONTAINS", "STARTS_WITH", "ENDS_WITH", "IN", "NOT_IN", "IS_NULL", "IS_NOT_NULL", "GT", "GTE", "LT", "LTE", "BETWEEN");
        assertThat(schemas.path("QuerySceneMetadataRspDTO").path("properties").path("fields").path("additionalProperties").path("$ref").asText())
                .isEqualTo("#/components/schemas/QuerySceneFieldRspDTO");
    }

    @Test
    void rejectsInvalidGatewayIdentity() throws Exception {
        mvc.perform(post("/api/system/query-scene/metadata").header("X-User-Id", "invalid"))
                .andExpect(status().isBadRequest());
    }

    private static List<String> strings(JsonNode node) {
        var values = new ArrayList<String>();
        node.forEach(value -> values.add(value.asText()));
        return values;
    }
}
