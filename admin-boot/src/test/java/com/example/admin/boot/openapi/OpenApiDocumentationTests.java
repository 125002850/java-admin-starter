package com.example.admin.boot.openapi;

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

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OpenApiDocumentationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void openApiJsonShouldExposeFoundationEndpointsOnly() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi").exists())
            .andExpect(jsonPath("$.info.title").value("java-admin-starter API 文档"))
            .andExpect(content().string(containsString("/api/mdm/dict/global/types/list")))
            .andExpect(content().string(containsString("/api/mdm/dict/global/types/list-all")))
            .andExpect(content().string(containsString("/api/mdm/dict/global/items/by-type")))
            .andExpect(content().string(containsString("/api/mdm/export/submit")))
            .andExpect(content().string(containsString("/api/mdm/export/my/page")))
            .andExpect(content().string(containsString("/api/mdm/export/download/batch")))
            .andExpect(content().string(containsString("/api/file/storage/object/upload")))
            .andExpect(content().string(containsString("/api/file/storage/object/temp-url/batch-fetch")))
            .andExpect(content().string(containsString("/api/staff/list-all")))
            .andExpect(content().string(not(containsString("postloan"))))
            .andExpect(content().string(not(containsString("tb_track_"))));
    }

    @Test
    void knife4jPageShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/doc.html"))
            .andExpect(status().isOk());
    }

    @Test
    void endpointsShouldExposeStableOperationIds() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/mdm/dict/global/types/list'].post.operationId")
                    .value("mdmDictGlobalTypesList"))
            .andExpect(jsonPath("$.paths['/api/mdm/dict/global/items/by-type'].post.operationId")
                    .value("mdmDictGlobalItemsByType"))
            .andExpect(jsonPath("$.paths['/api/mdm/export/submit'].post.operationId")
                    .value("submitExport"))
            .andExpect(jsonPath("$.paths['/api/mdm/export/my/page'].post.operationId")
                    .value("pageMyExportRecords"))
            .andExpect(jsonPath("$.paths['/api/mdm/export/download/batch'].post.operationId")
                    .value("batchDownloadExportRecords"))
            .andExpect(jsonPath("$.paths['/api/file/storage/object/upload'].post.operationId")
                    .value("uploadFileObject"))
            .andExpect(jsonPath("$.paths['/api/file/storage/object/temp-url/fetch'].post.operationId")
                    .value("fileStorageObjectTempUrlFetch"))
            .andExpect(jsonPath("$.paths['/api/file/storage/object/temp-url/batch-fetch'].post.operationId")
                    .value("batchFetchFileObjectTempUrls"))
            .andExpect(jsonPath("$.paths['/api/staff/list-all'].post.operationId")
                    .value("staffListAll"));
    }

    @Test
    void fileUploadEndpointShouldDescribeMultipartRequestBody() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/file/storage/object/upload'].post.requestBody.content['multipart/form-data']").exists())
            .andExpect(jsonPath("$.paths['/api/file/storage/object/upload'].post.requestBody.content['multipart/form-data'].schema.properties.file.type").value("string"))
            .andExpect(jsonPath("$.paths['/api/file/storage/object/upload'].post.requestBody.content['multipart/form-data'].schema.properties.file.format").value("binary"))
            .andExpect(jsonPath("$.paths['/api/file/storage/object/upload'].post.requestBody.content['multipart/form-data'].schema.properties.request").doesNotExist())
            .andExpect(content().string(containsString("\"name\":\"bizPath\",\"in\":\"query\"")))
            .andExpect(content().string(containsString("\"name\":\"objectKey\",\"in\":\"query\"")))
            .andExpect(content().string(not(containsString("\"name\":\"file\",\"in\":\"query\""))));
    }

    @Test
    void dynamicQuerySchemasShouldExposeConditionNodeRefsAndDiscriminators() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.schemas.GlobalDictTypeDynamicListReqDTO.properties.condition['$ref']")
                    .value("#/components/schemas/GlobalDictTypeConditionNode"))
            .andExpect(jsonPath("$.components.schemas.GlobalDictTypeConditionNode.discriminator.propertyName").value("nodeType"))
            .andExpect(jsonPath("$.components.schemas.GlobalDictTypeConditionNode.discriminator.mapping.compose")
                    .value("#/components/schemas/GlobalDictTypeGroupCondition"))
            .andExpect(jsonPath("$.components.schemas.GlobalDictTypeConditionNode.properties.nodeType.enum", hasItems("compose", "text", "dateTime")))
            .andExpect(jsonPath("$.components.schemas.GlobalDictTypeConditionNode.properties.field.enum", hasItems("dictTypeCode", "dictTypeName", "createTime")))
            .andExpect(jsonPath("$.components.schemas.GlobalDictItemDynamicPageReqDTO.properties.condition['$ref']")
                    .value("#/components/schemas/GlobalDictItemConditionNode"))
            .andExpect(jsonPath("$.components.schemas.GlobalDictItemConditionNode.properties.field.enum", hasItems("dictTypeCode", "dictItemCode", "dictItemName", "createTime")))
            .andExpect(jsonPath("$.components.schemas.ExportRecordDynamicPageReqDTO.properties.condition['$ref']")
                    .value("#/components/schemas/ExportRecordConditionNode"))
            .andExpect(jsonPath("$.components.schemas.ExportRecordConditionNode.discriminator.mapping.enum")
                    .value("#/components/schemas/ExportRecordEnumCondition"))
            .andExpect(jsonPath("$.components.schemas.ExportRecordConditionNode.properties.nodeType.enum", hasItems("compose", "text", "dateTime", "enum")))
            .andExpect(jsonPath("$.components.schemas.ExportRecordConditionNode.properties.field.enum", hasItems("exportBizCode", "fileName", "createTime", "status")))
            .andExpect(content().string(containsString("\"oneOf\"")))
            .andExpect(content().string(containsString("\"propertyName\":\"nodeType\"")));
    }

    @Test
    void enumSchemasShouldUseCodesForRequestsAndEnumVoForResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.schemas.GlobalDictItemCreateReqDTO.properties.status.enum",
                    contains("enable", "disable")))
            .andExpect(jsonPath("$.components.schemas.GlobalDictItemUpdateReqDTO.properties.status.enum",
                    contains("enable", "disable")))
            .andExpect(jsonPath("$.components.schemas.EnumVO").exists())
            .andExpect(jsonPath("$.components.schemas.EnumVO.type").value("object"))
            .andExpect(jsonPath("$.components.schemas.EnumVO.properties.code.type").value("string"))
            .andExpect(jsonPath("$.components.schemas.EnumVO.properties.desc.type").value("string"))
            .andExpect(jsonPath("$.components.schemas.DictItemRspDTO.properties.status.type").value("string"))
            .andExpect(jsonPath("$.components.schemas.DictItemRspDTO.properties.status.enum").isArray())
            .andExpect(jsonPath("$.components.schemas.GlobalDictTypeRspDTO.properties.status.type").value("string"))
            .andExpect(jsonPath("$.components.schemas.GlobalDictTypeRspDTO.properties.status.enum").isArray());
    }

    @Test
    void auditResponseSchemasShouldExposeUsernamesAsStrings() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.schemas.GlobalDictTypeRspDTO.properties.createBy.type").value("string"))
            .andExpect(jsonPath("$.components.schemas.GlobalDictTypeRspDTO.properties.updateBy.type").value("string"))
            .andExpect(jsonPath("$.components.schemas.DictItemRspDTO.properties.createBy.type").value("string"))
            .andExpect(jsonPath("$.components.schemas.DictItemRspDTO.properties.updateBy.type").value("string"))
            .andExpect(jsonPath("$.components.schemas.ExportRecordRspDTO.properties.createBy.type").value("string"));
    }

    @Test
    void pageResultSchemasShouldRequireTotalAndList() throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode schemas = objectMapper.readTree(content).path("components").path("schemas");
        int checkedSchemaCount = 0;
        Iterator<Map.Entry<String, JsonNode>> schemaIterator = schemas.fields();
        while (schemaIterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = schemaIterator.next();
            JsonNode properties = entry.getValue().path("properties");
            if (!properties.has("total") || !properties.has("list")) {
                continue;
            }

            checkedSchemaCount++;
            JsonNode required = entry.getValue().path("required");
            assertThat(required.isArray()).as("%s required", entry.getKey()).isTrue();
            assertThat(textValues(required)).as(entry.getKey()).contains("total", "list");
        }
        assertThat(checkedSchemaCount).isGreaterThan(0);
    }

    @Test
    void removedSystemGroupsAndPathsShouldNotBeExposed() throws Exception {
        mockMvc.perform(get("/v3/api-docs/system-auth"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v3/api-docs/system-tenant"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v3/api-docs/system-user"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/api/mdm/dict/types/list"))))
                .andExpect(content().string(not(containsString("/api/system/"))))
                .andExpect(content().string(not(containsString("/api/framework/qiniu/"))))
                .andExpect(content().string(not(containsString("/api/postloan/"))));
    }

    private static java.util.List<String> textValues(JsonNode array) {
        java.util.List<String> values = new java.util.ArrayList<>();
        array.forEach(item -> values.add(item.asText()));
        return values;
    }
}
