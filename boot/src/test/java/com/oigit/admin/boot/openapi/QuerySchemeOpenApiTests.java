package com.oigit.admin.boot.openapi;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class QuerySchemeOpenApiTests {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper mapper;
 @Test void exposesStableSchemeContracts() throws Exception {
  String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  var root = mapper.readTree(json);
  var operations = Map.of("list", "listQuerySchemes", "create", "createQueryScheme", "update", "updateQueryScheme", "delete", "deleteQueryScheme", "default/update", "updateDefaultQueryScheme");
  operations.forEach((path, operation) -> assertThat(root.path("paths").path("/api/system/query-scheme/" + path).path("post").path("operationId").asText()).isEqualTo(operation));
  assertThat(root.path("components").path("schemas").path("QuerySchemeCreateReqDTO").path("properties").path("contentJson").path("type").asText()).isEqualTo("string");

 }
}
