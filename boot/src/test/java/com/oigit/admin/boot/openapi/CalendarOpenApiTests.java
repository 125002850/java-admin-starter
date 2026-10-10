package com.oigit.admin.boot.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Iterator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "springdoc.paths-to-exclude=/api/test/**")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CalendarOpenApiTests {
    @Autowired private MockMvc mockMvc;
    @Test
    void calendarHistoryContractAndOptionalExport() throws Exception {
        var response = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/system/work-calendar/version/list'].post.operationId").value("listWorkCalendarVersions"))
                .andExpect(jsonPath("$.paths['/api/system/work-calendar/version/detail'].post.operationId").value("fetchWorkCalendarVersionDetail"))
                .andExpect(jsonPath("$.components.schemas.WorkCalendarVersionSummaryRspDTO.properties.updatedByName").exists())
                .andExpect(jsonPath("$.components.schemas.WorkCalendarVersionSummaryRspDTO.properties.updatedAt").exists())
                .andReturn().getResponse().getContentAsByteArray();
        String exportPath = System.getProperty("openapi.export.path");
        if (exportPath != null) java.nio.file.Files.write(java.nio.file.Path.of(exportPath), response);
    }

}
