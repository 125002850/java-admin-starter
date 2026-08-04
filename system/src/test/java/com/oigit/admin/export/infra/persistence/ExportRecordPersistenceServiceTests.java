package com.oigit.admin.export.infra.persistence;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.oigit.admin.core.query.executor.MybatisPlusQueryExecutor;
import com.oigit.admin.export.infra.persistence.entity.ExportRecordEntity;
import com.oigit.admin.export.infra.persistence.mapper.ExportRecordMapper;
import com.oigit.admin.export.infra.persistence.service.impl.ExportRecordPersistenceServiceImpl;
import com.oigit.admin.export.infra.query.ExportRecordSceneQueryDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportRecordPersistenceServiceTests {

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MybatisMapperBuilderAssistant assistant = new MybatisMapperBuilderAssistant(configuration, "export-record-persistence");
        assistant.setCurrentNamespace(ExportRecordEntity.class.getName());
        TableInfoHelper.initTableInfo(assistant, ExportRecordEntity.class);
    }

    @Test
    void markSuccess_should_use_atomic_processing_state_condition() {
        ExportRecordMapper mapper = mock(ExportRecordMapper.class);
        when(mapper.update(any(ExportRecordEntity.class), any(LambdaUpdateWrapper.class))).thenReturn(1);
        ExportRecordPersistenceServiceImpl service = service(mapper);

        int affected = service.markSuccess(
                1L,
                "export/demo.csv",
                "text/csv",
                10L,
                "local",
                LocalDateTime.now()
        );

        assertThat(affected).isOne();
        ArgumentCaptor<LambdaUpdateWrapper<ExportRecordEntity>> captor = wrapperCaptor();
        verify(mapper).update(any(ExportRecordEntity.class), captor.capture());
        assertThat(captor.getValue().getSqlSet())
                .contains("status=")
                .contains("object_key=")
                .contains("finished_time=");
        assertThat(captor.getValue().getSqlSegment())
                .contains("id")
                .contains("status")
                .contains("deleted");
    }

    @Test
    void recordDownloadLinksAcquired_should_use_one_atomic_increment_update() {
        ExportRecordMapper mapper = mock(ExportRecordMapper.class);
        when(mapper.update(any(ExportRecordEntity.class), any(LambdaUpdateWrapper.class))).thenReturn(2);
        ExportRecordPersistenceServiceImpl service = service(mapper);

        int affected = service.recordDownloadLinksAcquired(List.of(1L, 2L), 99L, LocalDateTime.now());

        assertThat(affected).isEqualTo(2);
        ArgumentCaptor<LambdaUpdateWrapper<ExportRecordEntity>> captor = wrapperCaptor();
        verify(mapper).update(any(ExportRecordEntity.class), captor.capture());
        assertThat(captor.getValue().getSqlSet()).contains("download_count = coalesce(download_count, 0) + 1");
        assertThat(captor.getValue().getSqlSegment()).contains("id IN").contains("status").contains("deleted");
    }

    private ExportRecordPersistenceServiceImpl service(ExportRecordMapper mapper) {
        return new ExportRecordPersistenceServiceImpl(
                mapper,
                new MybatisPlusQueryExecutor(),
                new ExportRecordSceneQueryDefinition()
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ArgumentCaptor<LambdaUpdateWrapper<ExportRecordEntity>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
    }
}
