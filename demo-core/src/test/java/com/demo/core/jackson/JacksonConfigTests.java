package com.demo.core.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTests {

    @Test
    void auditUserIdsShouldRemainJsonNumbers() throws Exception {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        ObjectMapper objectMapper = new JacksonConfig().objectMapper(
                new Jackson2ObjectMapperBuilder(),
                beanFactory.getBeanProvider(Module.class)
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(new AuditDTO(101L, 202L)));

        assertThat(json.path("createBy").isIntegralNumber()).isTrue();
        assertThat(json.path("createBy").longValue()).isEqualTo(101L);
        assertThat(json.path("updateBy").isIntegralNumber()).isTrue();
        assertThat(json.path("updateBy").longValue()).isEqualTo(202L);
    }

    private record AuditDTO(Long createBy, Long updateBy) {
    }
}
