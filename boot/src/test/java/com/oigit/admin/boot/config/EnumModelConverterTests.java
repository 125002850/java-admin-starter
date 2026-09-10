package com.oigit.admin.boot.config;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.enums.EnableStatusEnum;
import com.oigit.admin.sample.dto.EnumCoverageRspDTO;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnumModelConverterTests {
    @Test
    void resolvesClassAndJavaTypeAsStringAndKeepsDictionaryBinding() {
        var converter = new EnumModelConverter();
        for (var type : new java.lang.reflect.Type[]{EnableStatusEnum.class,
                new ObjectMapper().constructType(EnableStatusEnum.class)}) {
            Schema<?> schema = converter.resolve(new AnnotatedType(type), null, java.util.Collections.emptyIterator());
            assertThat(schema.getType()).isEqualTo("string");
            assertThat(schema.getEnum()).isEqualTo(java.util.List.of("enable", "disable"));
            assertThat(schema.getExtensions()).containsEntry("x-dict-type", "ENABLE_STATUS");
            assertThat(schema.get$ref()).isNull();
        }
    }

    @Test
    void preservesNullableAndDescriptionOnProperties() {
        ModelConverters converters = new ModelConverters();
        converters.addConverter(new EnumModelConverter());
        var resolved = converters.resolveAsResolvedSchema(new AnnotatedType(EnumCoverageRspDTO.class));
        Schema<?> status = (Schema<?>) resolved.schema.getProperties().get("status");
        assertThat(status.getType()).isEqualTo("string");
        assertThat(status.getNullable()).isTrue();
        assertThat(status.getDescription()).isEqualTo("可空状态");
        assertThat(status.getExtensions()).containsEntry("x-dict-type", "ENABLE_STATUS");
    }

    @Test
    void exposesLabeledCodesWithoutChangingJsonSerialization() throws Exception {
        var schema = new EnumModelConverter().resolve(new AnnotatedType(Labeled.class), null,
                java.util.Collections.emptyIterator());
        assertThat(schema.getType()).isEqualTo("string");
        assertThat(schema.getEnum()).isEqualTo(java.util.List.of("ready"));
        assertThat(schema.getExtensions()).containsEntry("x-enum-labels", Map.of("ready", "就绪"));
        assertThat(new ObjectMapper().writeValueAsString(Labeled.READY)).isEqualTo("\"ready\"");
        assertThat(new ObjectMapper().writeValueAsString(EnableStatusEnum.ENABLE)).isEqualTo("\"enable\"");
    }

    @Test
    void scansNewDomainDtosAndHonorsExplicitSchemaNames() {
        Schema<?> property = new Schema<>().$ref("#/components/schemas/LegacyEnum");
        property.setNullable(true);
        property.setDescription("保留描述");
        Schema<?> dto = new Schema<>().addProperty("status", property);
        OpenAPI api = new OpenAPI().components(new Components().addSchemas("RenamedEnumCoverage", dto));
        new OpenApiConfig().baseEnumSchemaCustomizer().customise(api);
        assertThat(property.getType()).isEqualTo("string");
        assertThat(property.getNullable()).isTrue();
        assertThat(property.getDescription()).isEqualTo("保留描述");
        assertThat(property.getExtensions()).containsEntry("x-dict-type", "ENABLE_STATUS");
    }

    public enum Labeled {
        READY;
        @JsonValue public String getCode() { return "ready"; }
        public String getLabel() { return "就绪"; }
    }
}
