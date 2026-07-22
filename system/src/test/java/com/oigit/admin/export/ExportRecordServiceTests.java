package com.oigit.admin.export;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.core.query.ast.ConditionLeafAst;
import com.oigit.admin.core.query.ast.QueryAst;
import com.oigit.admin.core.query.ast.QueryOperator;
import com.oigit.admin.core.query.ast.SortSpec;
import com.oigit.admin.core.query.dto.SortItemDTO;
import com.oigit.admin.core.query.executor.MybatisPlusQueryExecutor;
import com.oigit.admin.core.query.scene.SceneQueryDefinition;
import com.oigit.admin.export.enums.ExportCenterErrorCode;
import com.oigit.admin.export.enums.ExportDeleteReason;
import com.oigit.admin.export.enums.ExportRecordStatus;
import com.oigit.admin.export.infra.entity.ExportRecordEntity;
import com.oigit.admin.export.infra.mapper.ExportRecordMapper;
import com.oigit.admin.export.service.ExportRecordService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportRecordServiceTests {

    @Mock
    private ExportRecordMapper exportRecordMapper;

    private ExportRecordService exportRecordService;
    private final MybatisPlusQueryExecutor mybatisPlusQueryExecutor = new MybatisPlusQueryExecutor();

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MybatisMapperBuilderAssistant assistant = new MybatisMapperBuilderAssistant(configuration, "export-record-service");
        assistant.setCurrentNamespace(ExportRecordEntity.class.getName());
        TableInfoHelper.initTableInfo(assistant, ExportRecordEntity.class);
    }

    @BeforeEach
    void setUp() {
        exportRecordService = new ExportRecordService(exportRecordMapper, mybatisPlusQueryExecutor);
    }

    @Test
    void createProcessingRecord_should_initialize_processing_status() {
        ExportRecordEntity entity = buildRecord(1L, ExportRecordStatus.SUCCESS);

        Long recordId = exportRecordService.createProcessingRecord(entity);

        assertThat(recordId).isEqualTo(1L);
        assertThat(entity.getStatus()).isEqualTo(ExportRecordStatus.PROCESSING.getIntCode());
        assertThat(entity.getDeleted()).isEqualTo(0L);
        verify(exportRecordMapper).insert(entity);
    }

    @Test
    void markSuccess_should_reject_non_processing_record() {
        when(exportRecordMapper.selectById(1L)).thenReturn(buildRecord(1L, ExportRecordStatus.SUCCESS));

        assertThatThrownBy(() -> exportRecordService.markSuccess(
            1L,
            "export/test.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            1024L,
            "local"
        ))
            .isInstanceOf(BizException.class)
            .hasMessage(ExportCenterErrorCode.EXPORT_RECORD_STATUS_INVALID.getMsg());
    }

    @Test
    void markSuccess_should_persist_file_metadata() {
        when(exportRecordMapper.selectById(1L)).thenReturn(buildRecord(1L, ExportRecordStatus.PROCESSING));

        exportRecordService.markSuccess(
            1L,
            "export/test.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            1024L,
            "local"
        );

        ArgumentCaptor<ExportRecordEntity> captor = ArgumentCaptor.forClass(ExportRecordEntity.class);
        verify(exportRecordMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ExportRecordStatus.SUCCESS.getIntCode());
        assertThat(captor.getValue().getObjectKey()).isEqualTo("export/test.xlsx");
        assertThat(captor.getValue().getContentType())
            .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(captor.getValue().getFileSize()).isEqualTo(1024L);
        assertThat(captor.getValue().getStorageType()).isEqualTo("local");
        assertThat(captor.getValue().getFinishedTime()).isNotNull();
    }

    @Test
    void markFailed_should_reject_non_processing_record() {
        when(exportRecordMapper.selectById(1L)).thenReturn(buildRecord(1L, ExportRecordStatus.SUCCESS));

        assertThatThrownBy(() -> exportRecordService.markFailed(1L, "EXPORT_FAILED", "导出失败"))
            .isInstanceOf(BizException.class)
            .hasMessage(ExportCenterErrorCode.EXPORT_RECORD_STATUS_INVALID.getMsg());
    }

    @Test
    void markExpired_should_allow_success_record() {
        when(exportRecordMapper.selectById(1L)).thenReturn(buildRecord(1L, ExportRecordStatus.SUCCESS));

        exportRecordService.markExpired(1L);

        ArgumentCaptor<ExportRecordEntity> captor = ArgumentCaptor.forClass(ExportRecordEntity.class);
        verify(exportRecordMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ExportRecordStatus.EXPIRED.getIntCode());
    }

    @Test
    void markExpired_should_reject_failed_record() {
        when(exportRecordMapper.selectById(1L)).thenReturn(buildRecord(1L, ExportRecordStatus.FAILED));

        assertThatThrownBy(() -> exportRecordService.markExpired(1L))
            .isInstanceOf(BizException.class)
            .hasMessage(ExportCenterErrorCode.EXPORT_RECORD_STATUS_INVALID.getMsg());
    }

    @Test
    void markDeleted_should_soft_delete_any_status() {
        ExportRecordEntity entity = buildRecord(1L, ExportRecordStatus.SUCCESS);
        entity.setDeleted(0L);
        when(exportRecordMapper.selectById(1L)).thenReturn(entity);

        exportRecordService.markDeleted(1L, ExportDeleteReason.MANUAL);

        ArgumentCaptor<LambdaUpdateWrapper<ExportRecordEntity>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(exportRecordMapper).update(isNull(), captor.capture());
        assertThat(captor.getValue().getSqlSet())
                .contains("deleted=")
                .contains("delete_reason=")
                .contains("deleted_time=");
        assertThat(captor.getValue().getSqlSegment())
                .contains("id")
                .contains("deleted");
    }

    @Test
    void getRequired_should_throw_when_record_missing() {
        when(exportRecordMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> exportRecordService.getRequired(1L))
            .isInstanceOf(BizException.class)
            .hasMessage(ExportCenterErrorCode.EXPORT_RECORD_NOT_FOUND.getMsg());
    }

    @Test
    void pageMyRecords_should_apply_owner_status_and_default_sort() {
        when(exportRecordMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QueryAst queryAst = new QueryAst();
        queryAst.setPageNo(2L);
        queryAst.setPageSize(5L);
        queryAst.setRoot(and(
                leaf("OWNER_ID", QueryOperator.EQ, 0L),
                leaf("STATUS", QueryOperator.IN, List.of(2, 4)),
                leaf("CREATE_TIME", QueryOperator.GTE, LocalDateTime.of(2026, 6, 1, 0, 0, 0))
        ));

        exportRecordService.pageMyRecords(queryAst, exportRecordSceneDefinition());

        ArgumentCaptor<Page<ExportRecordEntity>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<LambdaQueryWrapper<ExportRecordEntity>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(exportRecordMapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(2L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(5L);
        assertThat(wrapperCaptor.getValue().getSqlSegment())
                .contains("create_by")
                .contains("status")
                .contains("create_time")
                .contains("ORDER BY create_time DESC");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs().values())
                .contains(0L, 2, 4);
    }

    private ExportRecordEntity buildRecord(Long id, ExportRecordStatus status) {
        ExportRecordEntity entity = new ExportRecordEntity();
        entity.setId(id);
        entity.setExportBizCode("demo.export");
        entity.setExportBizName("演示导出");
        entity.setFileName("demo.xlsx");
        entity.setFileType("EXCEL");
        entity.setStatus(status.getIntCode());
        entity.setExpireTime(LocalDateTime.now().plusDays(1));
        entity.setExpireSeconds(3600);
        entity.setQuerySnapshotJson("{\"keyword\":\"demo\"}");
        entity.setQuerySnapshotSummary("keyword=demo");
        entity.setDeleted(0L);
        return entity;
    }

    private ConditionLeafAst leaf(String fieldKey, QueryOperator operator, Object typedValue) {
        ConditionLeafAst leaf = new ConditionLeafAst();
        leaf.setFieldKey(fieldKey);
        leaf.setOperator(operator);
        leaf.setTypedValue(typedValue);
        return leaf;
    }

    private com.oigit.admin.core.query.ast.ConditionGroupAst and(ConditionLeafAst... children) {
        com.oigit.admin.core.query.ast.ConditionGroupAst group = new com.oigit.admin.core.query.ast.ConditionGroupAst();
        group.setLogic(com.oigit.admin.core.query.ast.QueryLogicOperator.AND);
        group.setChildren(List.of(children));
        return group;
    }

    private SceneQueryDefinition<ExportRecordEntity> exportRecordSceneDefinition() {
        return new SceneQueryDefinition<>() {
            @Override
            public String sceneCode() {
                return "mdm.export.record.page";
            }

            @Override
            public Map<String, SFunction<ExportRecordEntity, String>> textFields() {
                return Map.of(
                        "EXPORT_BIZ_CODE", ExportRecordEntity::getExportBizCode,
                        "EXPORT_BIZ_NAME", ExportRecordEntity::getExportBizName,
                        "FILE_NAME", ExportRecordEntity::getFileName
                );
            }

            @Override
            public Map<String, SFunction<ExportRecordEntity, LocalDateTime>> dateTimeFields() {
                return Map.of(
                        "CREATE_TIME", ExportRecordEntity::getCreateTime,
                        "FINISHED_TIME", ExportRecordEntity::getFinishedTime,
                        "EXPIRE_TIME", ExportRecordEntity::getExpireTime
                );
            }

            @Override
            public Map<String, SFunction<ExportRecordEntity, ?>> enumFields() {
                return Map.of(
                        "STATUS", ExportRecordEntity::getStatus,
                        "OWNER_ID", ExportRecordEntity::getCreateBy
                );
            }

            @Override
            public Map<String, SFunction<ExportRecordEntity, ?>> sortFields() {
                return Map.of(
                        "CREATE_TIME", ExportRecordEntity::getCreateTime,
                        "FINISHED_TIME", ExportRecordEntity::getFinishedTime,
                        "EXPIRE_TIME", ExportRecordEntity::getExpireTime,
                        "DOWNLOAD_COUNT", ExportRecordEntity::getDownloadCount
                );
            }

            @Override
            public Set<QueryOperator> allowedOperators(String fieldKey) {
                return switch (fieldKey) {
                    case "EXPORT_BIZ_CODE", "EXPORT_BIZ_NAME", "FILE_NAME" -> Set.of(
                            QueryOperator.EQ,
                            QueryOperator.CONTAINS,
                            QueryOperator.STARTS_WITH,
                            QueryOperator.ENDS_WITH,
                            QueryOperator.IN
                    );
                    case "STATUS" -> Set.of(QueryOperator.EQ, QueryOperator.IN);
                    case "OWNER_ID" -> Set.of(QueryOperator.EQ);
                    case "CREATE_TIME", "FINISHED_TIME", "EXPIRE_TIME" -> Set.of(
                            QueryOperator.GT,
                            QueryOperator.GTE,
                            QueryOperator.LT,
                            QueryOperator.LTE,
                            QueryOperator.BETWEEN
                    );
                    default -> Set.of();
                };
            }

            @Override
            public List<SortSpec> defaultSorts() {
                return List.of(new SortSpec("CREATE_TIME", SortItemDTO.SortDirection.DESC));
            }
        };
    }
}
