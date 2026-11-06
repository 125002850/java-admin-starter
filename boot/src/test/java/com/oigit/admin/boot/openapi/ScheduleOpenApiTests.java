package com.oigit.admin.boot.openapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "springdoc.paths-to-exclude=/api/test/**")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ScheduleOpenApiTests {
    @Autowired private MockMvc mockMvc;

    @Test void scheduleContractAndOptionalExport() throws Exception {
        byte[] response = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/system/schedule/job/page'].post.operationId").value("systemScheduleJobPage"))
                .andExpect(jsonPath("$.paths['/api/system/schedule/job/execution-logs'].post.operationId").value("systemScheduleJobExecutionLogs"))
                .andExpect(jsonPath("$.paths['/api/system/work-calendar/year/detail']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/iam/auth/login']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/staff/list-all']").exists())
                .andExpect(jsonPath("$.components.schemas.ScheduleJobRspDTO.properties.latestExecution.$ref").exists())
                .andExpect(jsonPath("$.components.schemas.ScheduleJobExecutionLogRspDTO.properties.durationMillis").exists())
                .andExpect(jsonPath("$.components.schemas.ScheduleJobExecutionLogRspDTO.properties.status.enum").value(hasItems("running", "success", "failed")))
                .andReturn().getResponse().getContentAsByteArray();
        String exportPath = System.getProperty("openapi.export.path");
        if (exportPath != null) java.nio.file.Files.write(java.nio.file.Path.of(exportPath), response);
    }
}
