package com.demo.mdm.export;

import com.demo.core.exception.GlobalExceptionHandler;
import com.demo.core.export.model.ExportStoreRequest;
import com.demo.core.export.model.ExportStoredFile;
import com.demo.core.export.model.RenderedExportFile;
import com.demo.core.export.renderer.CsvExportRenderer;
import com.demo.core.export.spi.ExportFileAccessor;
import com.demo.core.export.spi.ExportFileSink;
import com.demo.core.export.support.SpringExportRendererRegistry;
import com.demo.core.export.support.SpringExportSceneRegistry;
import com.demo.core.mybatis.CommonMetaObjectHandler;
import com.demo.core.mybatis.MybatisPlusConfig;
import com.demo.core.operator.OperatorContext;
import com.demo.core.operator.OperatorUsernameResolver;
import com.demo.core.query.executor.MybatisPlusQueryExecutor;
import com.demo.core.query.support.DynamicQueryGuard;
import com.demo.core.query.support.DynamicQuerySummaryRenderer;
import com.demo.core.query.support.QueryComplexityScorer;
import com.demo.mdm.export.app.ExportCenterAppService;
import com.demo.mdm.export.controller.ExportCenterController;
import com.demo.mdm.dict.export.GlobalDictTypeListExportHandler;
import com.demo.mdm.export.query.ExportRecordSceneQueryDefinition;
import com.demo.mdm.export.query.ExportRecordSceneQueryMapper;
import com.demo.mdm.export.service.ExportBatchDownloadService;
import com.demo.mdm.export.service.ExportDownloadService;
import com.demo.mdm.export.service.ExportExecutionService;
import com.demo.mdm.export.service.ExportRecordService;
import com.demo.mdm.dict.query.globaldict.GlobalDictTypeSceneQueryDefinition;
import com.demo.mdm.dict.query.globaldict.GlobalDictTypeSceneQueryMapper;
import com.demo.mdm.dict.service.DictService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = ExportCenterSmokeTests.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:java-demo-mdm-export;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "mybatis-plus.global-config.db-config.logic-delete-value=unix_timestamp()",
                "mybatis-plus.global-config.db-config.logic-not-delete-value=0"
        }
)
@AutoConfigureMockMvc
class ExportCenterSmokeTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("inMemoryExportGateway")
    private InMemoryExportGateway exportGateway;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("drop table if exists sys_export_record_global");
        jdbcTemplate.execute("drop table if exists sys_dict_type_global");
        jdbcTemplate.execute("create table sys_dict_type_global ("
                + "id bigint primary key,"
                + "dict_type_code varchar(64) not null,"
                + "dict_type_name varchar(128) not null,"
                + "remark varchar(512) null,"
                + "status varchar(32) not null default 'enable',"
                + "version int not null default 0,"
                + "create_time timestamp not null default current_timestamp,"
                + "update_time timestamp not null default current_timestamp,"
                + "create_by bigint null,"
                + "update_by bigint null,"
                + "deleted bigint not null default 0,"
                + "constraint uk_sys_dict_type_global_code unique (dict_type_code)"
                + ")");
        jdbcTemplate.execute("create table sys_export_record_global ("
                + "id bigint primary key,"
                + "export_biz_code varchar(64) not null,"
                + "export_biz_name varchar(128) not null,"
                + "file_name varchar(256) not null,"
                + "file_type varchar(32) not null,"
                + "content_type varchar(128) null,"
                + "file_size bigint null,"
                + "object_key varchar(256) null,"
                + "storage_type varchar(32) null,"
                + "status tinyint not null,"
                + "finished_time timestamp null,"
                + "expire_time timestamp not null,"
                + "deleted_time timestamp null,"
                + "delete_reason tinyint null,"
                + "fail_code varchar(64) null,"
                + "fail_message varchar(255) null,"
                + "query_snapshot_json longtext not null,"
                + "query_snapshot_summary varchar(512) not null,"
                + "download_count int not null default 0,"
                + "last_download_time timestamp null,"
                + "last_download_by bigint null,"
                + "expire_seconds int not null,"
                + "version int not null default 0,"
                + "create_time timestamp not null default current_timestamp,"
                + "update_time timestamp not null default current_timestamp,"
                + "create_by bigint null,"
                + "update_by bigint null,"
                + "deleted bigint not null default 0"
                + ")");
    }

    @AfterEach
    void tearDown() {
        OperatorContext.clear();
        jdbcTemplate.execute("drop table if exists sys_export_record_global");
        jdbcTemplate.execute("drop table if exists sys_dict_type_global");
    }

    @Test
    void submit_and_download_should_complete_export_flow() throws Exception {
        OperatorContext.set(8801L, "导出人", null);
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (101, 'user_status', '用户状态', timestamp '2026-06-01 08:00:00', timestamp '2026-06-01 08:00:00', 0)"
        );
        jdbcTemplate.update(
                "insert into sys_dict_type_global (id, dict_type_code, dict_type_name, create_time, update_time, deleted) "
                        + "values (102, 'order_status', '订单状态', timestamp '2026-06-02 08:00:00', timestamp '2026-06-02 08:00:00', 0)"
        );

        mockMvc.perform(post("/api/mdm/export/submit")
                        .header("X-User-Id", "8801")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "sceneCode":"mdm.global.dict.type.list",
                                  "query":{
                                    "condition":{
                                      "nodeType":"text",
                                      "field":"dictTypeCode",
                                      "op":"EQ",
                                      "value":"user_status"
                                    },
                                    "sort":[
                                      {
                                        "field":"id",
                                        "direction":"ASC"
                                      }
                                    ]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.exportBizCode").value("mdm.global.dict.type.list"))
                .andExpect(jsonPath("$.data.statusName").value("SUCCESS"))
                .andExpect(jsonPath("$.data.fileName").value(allOf(startsWith("全局字典类型-筛选-"), endsWith(".csv"))))
                .andExpect(jsonPath("$.data.createBy").value("submit_exporter"))
                .andExpect(jsonPath("$.data.downloadCount").value(1))
                .andExpect(jsonPath("$.data.querySnapshotSummary").value("字典类型编码 等于 user_status"))
                .andExpect(jsonPath("$.data.downloadUrl")
                        .value(startsWith("https://download.example.com/export/mdm_global_dict_type_list/")));

        Long recordId = jdbcTemplate.queryForObject("select id from sys_export_record_global limit 1", Long.class);
        Integer status = jdbcTemplate.queryForObject("select status from sys_export_record_global where id = ?", Integer.class, recordId);
        Long createBy = jdbcTemplate.queryForObject("select create_by from sys_export_record_global where id = ?", Long.class, recordId);
        String objectKey = jdbcTemplate.queryForObject("select object_key from sys_export_record_global where id = ?", String.class, recordId);
        String querySnapshotJson = jdbcTemplate.queryForObject(
                "select query_snapshot_json from sys_export_record_global where id = ?",
                String.class,
                recordId
        );
        String querySnapshotSummary = jdbcTemplate.queryForObject(
                "select query_snapshot_summary from sys_export_record_global where id = ?",
                String.class,
                recordId
        );

        assertThat(status).isEqualTo(2);
        assertThat(createBy).isEqualTo(8801L);
        assertThat(objectKey).contains("全局字典类型-筛选-");
        assertThat(querySnapshotJson)
                .contains("\"nodeType\":\"text\"")
                .contains("\"field\":\"dictTypeCode\"")
                .doesNotContain("\"keyword\"");
        assertThat(querySnapshotSummary).isEqualTo("字典类型编码 等于 user_status");

        Integer downloadCountAfterSubmit = jdbcTemplate.queryForObject(
                "select download_count from sys_export_record_global where id = ?",
                Integer.class,
                recordId
        );
        assertThat(downloadCountAfterSubmit).isEqualTo(1);

        mockMvc.perform(post("/api/mdm/export/download")
                        .header("X-User-Id", "8801")
                        .contentType(APPLICATION_JSON)
                        .content("{\"recordId\":" + recordId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.recordId").value(recordId))
                .andExpect(jsonPath("$.data.downloadUrl").value("https://download.example.com/" + objectKey));

        Integer downloadCount = jdbcTemplate.queryForObject(
                "select download_count from sys_export_record_global where id = ?",
                Integer.class,
                recordId
        );
        assertThat(downloadCount).isEqualTo(2);
    }

    @Test
    void batchDownload_should_zip_owned_success_records_without_creating_export_record() throws Exception {
        OperatorContext.set(8802L, "批量下载人", null);
        byte[] customerCsv = "字典编码,字典名称\nDICT001,字典A\n".getBytes(StandardCharsets.UTF_8);
        byte[] orderCsv = "记录ID,文件大小\nR001,100\n".getBytes(StandardCharsets.UTF_8);
        exportGateway.put("export/source/customer.csv", customerCsv);
        exportGateway.put("export/source/order.csv", orderCsv);
        insertExportRecord(
                301L,
                "mdm.global-dict-type.list",
                "全局字典类型列表导出",
                "全局字典类型-筛选-20260629110000.csv",
                "export/source/customer.csv",
                customerCsv.length,
                8802L
        );
        insertExportRecord(
                302L,
                "mdm.export-record.list",
                "导出记录列表导出",
                "导出记录-筛选-20260629110100.csv",
                "export/source/order.csv",
                orderCsv.length,
                8802L
        );

        mockMvc.perform(post("/api/mdm/export/download/batch")
                        .header("X-User-Id", "8802")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [301, 302]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.recordId").doesNotExist())
                .andExpect(jsonPath("$.data.fileName").value(allOf(startsWith("导出中心批量下载-"), endsWith(".zip"))))
                .andExpect(jsonPath("$.data.downloadUrl").value(startsWith("https://download.example.com/export/mdm_export_center_batch_download/")))
                .andExpect(jsonPath("$.data.contentType").value("application/zip"))
                .andExpect(jsonPath("$.data.fileSize").isNumber());

        Integer batchRecordCount = jdbcTemplate.queryForObject(
                "select count(*) from sys_export_record_global where export_biz_code = ?",
                Integer.class,
                "mdm.export-center.batch-download"
        );
        Integer customerDownloadCount = jdbcTemplate.queryForObject(
                "select download_count from sys_export_record_global where id = ?",
                Integer.class,
                301L
        );
        Integer orderDownloadCount = jdbcTemplate.queryForObject(
                "select download_count from sys_export_record_global where id = ?",
                Integer.class,
                302L
        );
        String batchObjectKey = exportGateway.firstObjectKeyWithPrefix("export/mdm_export_center_batch_download/");

        assertThat(batchRecordCount).isZero();
        assertThat(customerDownloadCount).isEqualTo(1);
        assertThat(orderDownloadCount).isEqualTo(1);
        assertThat(zipEntryNames(exportGateway.content(batchObjectKey)))
                .containsExactly(
                        "全局字典类型-筛选-20260629110000.csv",
                        "导出记录-筛选-20260629110100.csv"
                );
    }

    @Test
    void delete_should_soft_delete_multiple_owned_records() throws Exception {
        OperatorContext.set(8803L, "删除人", null);
        insertExportRecord(
                401L,
                "mdm.global-dict-type.list",
                "全局字典类型列表导出",
                "全局字典类型-筛选-20260629120000.csv",
                "export/source/customer-delete-a.csv",
                128L,
                8803L
        );
        insertExportRecord(
                402L,
                "mdm.export-record.list",
                "导出记录列表导出",
                "导出记录-筛选-20260629120100.csv",
                "export/source/order-delete-b.csv",
                256L,
                8803L
        );

        mockMvc.perform(post("/api/mdm/export/delete")
                        .header("X-User-Id", "8803")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [401, 402]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer deletedCount = jdbcTemplate.queryForObject(
                "select count(*) from sys_export_record_global where id in (401, 402) and deleted > 1 and delete_reason = 1",
                Integer.class
        );
        assertThat(deletedCount).isEqualTo(2);
    }

    @Test
    void delete_should_reject_foreign_record_and_keep_batch_unchanged() throws Exception {
        OperatorContext.set(8804L, "删除人", null);
        insertExportRecord(
                411L,
                "mdm.global-dict-type.list",
                "全局字典类型列表导出",
                "全局字典类型-筛选-20260629121000.csv",
                "export/source/customer-delete-owned.csv",
                128L,
                8804L
        );
        insertExportRecord(
                412L,
                "mdm.export-record.list",
                "导出记录列表导出",
                "导出记录-筛选-20260629121100.csv",
                "export/source/order-delete-foreign.csv",
                256L,
                9904L
        );

        mockMvc.perform(post("/api/mdm/export/delete")
                        .header("X-User-Id", "8804")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "ids": [411, 412]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001103));

        Integer activeCount = jdbcTemplate.queryForObject(
                "select count(*) from sys_export_record_global where id in (411, 412) and deleted = 0",
                Integer.class
        );
        assertThat(activeCount).isEqualTo(2);
    }

    @Test
    void pageMyExports_should_support_dynamic_query_and_keep_owner_constraint() throws Exception {
        OperatorContext.set(8802L, "分页人", null);
        jdbcTemplate.update("""
                insert into sys_export_record_global (
                  id, export_biz_code, export_biz_name, file_name, file_type, status,
                  expire_time, query_snapshot_json, query_snapshot_summary, download_count,
                  expire_seconds, create_time, update_time, create_by, deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                201L, "mdm.global.dict.type.list", "全局字典类型列表导出", "global-dict-types-a.csv", "csv", 2,
                java.sql.Timestamp.valueOf("2026-06-10 00:00:00"), "{}", "ok", 0, 3600,
                java.sql.Timestamp.valueOf("2026-06-03 08:00:00"), java.sql.Timestamp.valueOf("2026-06-03 08:00:00"), 8802L, 0L
        );
        jdbcTemplate.update("""
                insert into sys_export_record_global (
                  id, export_biz_code, export_biz_name, file_name, file_type, status,
                  expire_time, query_snapshot_json, query_snapshot_summary, download_count,
                  expire_seconds, create_time, update_time, create_by, deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                202L, "mdm.global.dict.type.list", "全局字典类型列表导出", "global-dict-types-b.csv", "csv", 4,
                java.sql.Timestamp.valueOf("2026-06-10 00:00:00"), "{}", "ok", 0, 3600,
                java.sql.Timestamp.valueOf("2026-06-02 08:00:00"), java.sql.Timestamp.valueOf("2026-06-02 08:00:00"), 8802L, 0L
        );
        jdbcTemplate.update("""
                insert into sys_export_record_global (
                  id, export_biz_code, export_biz_name, file_name, file_type, status,
                  expire_time, query_snapshot_json, query_snapshot_summary, download_count,
                  expire_seconds, create_time, update_time, create_by, deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                203L, "mdm.global.dict.type.list", "全局字典类型列表导出", "global-dict-types-c.csv", "csv", 2,
                java.sql.Timestamp.valueOf("2026-06-10 00:00:00"), "{}", "ok", 0, 3600,
                java.sql.Timestamp.valueOf("2026-06-04 08:00:00"), java.sql.Timestamp.valueOf("2026-06-04 08:00:00"), 99L, 0L
        );
        jdbcTemplate.update("""
                insert into sys_export_record_global (
                  id, export_biz_code, export_biz_name, file_name, file_type, status,
                  expire_time, query_snapshot_json, query_snapshot_summary, download_count,
                  expire_seconds, create_time, update_time, create_by, deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                204L, "mdm.global.dict.type.list", "全局字典类型列表导出", "global-dict-types-deleted.csv", "csv", 2,
                java.sql.Timestamp.valueOf("2026-06-10 00:00:00"), "{}", "ok", 0, 3600,
                java.sql.Timestamp.valueOf("2026-06-05 08:00:00"), java.sql.Timestamp.valueOf("2026-06-05 08:00:00"), 8802L, 1L
        );

        mockMvc.perform(post("/api/mdm/export/my/page")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 1,
                                  "pageSize": 10,
                                  "condition": {
                                    "nodeType": "compose",
                                    "logic": "AND",
                                    "children": [
                                      {
                                        "nodeType": "text",
                                        "field": "exportBizCode",
                                        "op": "EQ",
                                        "value": "mdm.global.dict.type.list"
                                      },
                                      {
                                        "nodeType": "enum",
"field": "status",
                                        "op": "IN",
                                        "values": [2, 4]
                                      },
                                      {
                                        "nodeType": "dateTime",
                                        "field": "createTime",
                                        "op": "GTE",
                                        "value": "2026-06-01 00:00:00"
                                      }
                                    ]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list.length()").value(2))
                .andExpect(jsonPath("$.data.list[0].recordId").value(201))
                .andExpect(jsonPath("$.data.list[0].createBy").value("page_exporter"))
                .andExpect(jsonPath("$.data.list[1].recordId").value(202));
    }

    @Test
    void detail_should_return_audit_username() throws Exception {
        OperatorContext.set(8802L, "详情查询人", null);
        insertExportRecord(
                205L,
                "mdm.global.dict.type.list",
                "全局字典类型列表导出",
                "global-dict-types-detail.csv",
                "export/source/global-dict-types-detail.csv",
                128L,
                8802L
        );

        mockMvc.perform(post("/api/mdm/export/detail")
                        .header("X-User-Id", "8802")
                        .contentType(APPLICATION_JSON)
                        .content("{\"recordId\":205}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.recordId").value(205))
                .andExpect(jsonPath("$.data.createBy").value("page_exporter"));
    }

    @Test
    void pageMyExports_should_reject_oversized_page_size() throws Exception {
        mockMvc.perform(post("/api/mdm/export/my/page")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 1,
                                   "pageSize": 2001
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3000010));
    }

    @Test
    void pageMyExports_should_reject_oversized_in_values() throws Exception {
        String values = java.util.stream.IntStream.rangeClosed(1, 201)
                .mapToObj(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));

        mockMvc.perform(post("/api/mdm/export/my/page")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "pageNo": 1,
                                  "pageSize": 10,
                                  "condition": {
                                    "nodeType": "enum",
                                    "field": "status",
                                    "op": "IN",
                                    "values": [%s]
                                  }
                                }
                                """.formatted(values)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3000009));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan({
            "com.demo.mdm.dict.infra.mapper",
            "com.demo.mdm.export.infra.mapper"
    })
    @Import({
            GlobalExceptionHandler.class,
            CommonMetaObjectHandler.class,
            MybatisPlusConfig.class,
            QueryComplexityScorer.class,
            DynamicQueryGuard.class,
            DynamicQuerySummaryRenderer.class,
            MybatisPlusQueryExecutor.class,
            GlobalDictTypeSceneQueryDefinition.class,
            GlobalDictTypeSceneQueryMapper.class,
            ExportRecordSceneQueryDefinition.class,
            ExportRecordSceneQueryMapper.class,
            DictService.class,
            ExportCenterAppService.class,
            ExportCenterController.class,
            ExportExecutionService.class,
            ExportDownloadService.class,
            ExportBatchDownloadService.class,
            ExportRecordService.class,
            GlobalDictTypeListExportHandler.class,
            SpringExportSceneRegistry.class,
            SpringExportRendererRegistry.class,
            CsvExportRenderer.class,
            ExportGatewayTestConfig.class
    })
    static class TestApplication {
    }

    @Configuration
    static class ExportGatewayTestConfig {

        @Bean
        InMemoryExportGateway inMemoryExportGateway() {
            return new InMemoryExportGateway();
        }

        @Bean
        ExportFileSink exportFileSink(InMemoryExportGateway gateway) {
            return gateway;
        }

        @Bean
        ExportFileAccessor exportFileAccessor(InMemoryExportGateway gateway) {
            return gateway;
        }

        @Bean
        OperatorUsernameResolver operatorUsernameResolver() {
            return operatorIds -> Map.of(
                    8801L, "submit_exporter",
                    8802L, "page_exporter"
            );
        }
    }

    static class InMemoryExportGateway implements ExportFileSink, ExportFileAccessor {

        private final Map<String, byte[]> store = new ConcurrentHashMap<>();

        @Override
        public ExportStoredFile store(RenderedExportFile file, ExportStoreRequest request) {
            String objectKey = request.getBizPath() + "/" + file.getFileName();
            store.put(objectKey, file.getContent());
            ExportStoredFile storedFile = new ExportStoredFile();
            storedFile.setObjectKey(objectKey);
            storedFile.setStorageType("memory");
            storedFile.setContentType(file.getContentType());
            storedFile.setFileSize((long) file.getContent().length);
            return storedFile;
        }

        @Override
        public String fetchTempUrl(String objectKey) {
            byte[] content = store.get(objectKey);
            if (objectKey.endsWith(".csv")) {
                String csv = new String(content, StandardCharsets.UTF_8);
                assertThat(csv)
                        .contains("字典类型编码")
                        .contains("user_status")
                        .doesNotContain("order_status");
            }
            return "https://download.example.com/" + objectKey;
        }

        @Override
        public byte[] fetchContent(String objectKey) {
            return content(objectKey);
        }

        void put(String objectKey, byte[] content) {
            store.put(objectKey, content);
        }

        byte[] content(String objectKey) {
            return store.get(objectKey);
        }

        String firstObjectKeyWithPrefix(String prefix) {
            return store.keySet().stream()
                    .filter(objectKey -> objectKey.startsWith(prefix))
                    .findFirst()
                    .orElseThrow();
        }
    }

    private void insertExportRecord(
            Long id,
            String exportBizCode,
            String exportBizName,
            String fileName,
            String objectKey,
            long fileSize,
            Long createBy
    ) {
        jdbcTemplate.update("""
                insert into sys_export_record_global (
                  id, export_biz_code, export_biz_name, file_name, file_type, content_type, file_size, object_key,
                  storage_type, status, finished_time, expire_time, query_snapshot_json, query_snapshot_summary,
                  download_count, expire_seconds, create_time, update_time, create_by, update_by, deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                exportBizCode,
                exportBizName,
                fileName,
                "csv",
                "text/csv;charset=UTF-8",
                fileSize,
                objectKey,
                "memory",
                2,
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now().plusHours(1)),
                "{}",
                "动态查询条件",
                0,
                3600,
                Timestamp.valueOf(LocalDateTime.now()),
                Timestamp.valueOf(LocalDateTime.now()),
                createBy,
                createBy,
                0L
        );
    }

    private List<String> zipEntryNames(byte[] content) throws Exception {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
            java.util.ArrayList<String> names = new java.util.ArrayList<>();
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                names.add(entry.getName());
                zipInputStream.closeEntry();
            }
            return names;
        }
    }
}
