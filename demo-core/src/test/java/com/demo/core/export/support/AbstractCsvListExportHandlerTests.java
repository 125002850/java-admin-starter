package com.demo.core.export.support;

import com.demo.core.export.model.ExportColumn;
import com.demo.core.export.model.ExportMeta;
import com.demo.core.export.model.ExportScope;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractCsvListExportHandlerTests {

    @Test
    void buildMeta_should_generate_business_csv_meta() {
        TestExportHandler handler = new TestExportHandler();

        ExportMeta meta = handler.buildMeta(new TestQuery(true));

        assertThat(meta.getFileType()).isEqualTo("csv");
        assertThat(meta.getFileName()).isEqualTo("跟进记录-选中2条-20260629094530.csv");
        assertThat(meta.getExpireSeconds()).isEqualTo(3600);
        assertThat(meta.getQuerySnapshotSummary()).isEqualTo("按ID导出 2 条");
    }

    private static class TestExportHandler extends AbstractCsvListExportHandler<TestQuery> {

        @Override
        public String sceneCode() {
            return "test.track-record.list";
        }

        @Override
        public Class<TestQuery> queryType() {
            return TestQuery.class;
        }

        @Override
        protected String businessName() {
            return "跟进记录";
        }

        @Override
        protected ExportScope resolveExportScope(TestQuery query) {
            return query.selected() ? ExportScope.selectedCount(2) : ExportScope.allData();
        }

        @Override
        protected LocalDateTime currentTime() {
            return LocalDateTime.of(2026, 6, 29, 9, 45, 30);
        }

        @Override
        public List<ExportColumn> columns(TestQuery query) {
            return List.of();
        }

        @Override
        public List<?> queryRows(TestQuery query) {
            return List.of();
        }
    }

    private record TestQuery(boolean selected) {
    }
}
