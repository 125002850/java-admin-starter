package com.oigit.admin.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oigit.admin.core.export.model.*;
import com.oigit.admin.core.export.renderer.CsvExportRenderer;
import com.oigit.admin.core.export.spi.*;
import com.oigit.admin.core.query.support.DynamicQuerySummaryRenderer;
import com.oigit.admin.core.translation.TranslationEngine;
import com.oigit.admin.export.app.ExportExecutionAppService;
import com.oigit.admin.export.app.ExportRecordLifecycleAppService;
import com.oigit.admin.export.domain.model.ExportRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExportStreamingExecutionTests {
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void exportsEveryPageWithoutCallingTheFullRowsQueryAndReleasesTheTemporaryFile() throws Exception {
        ChunkedExportHandler<Map> handler = mock(ChunkedExportHandler.class);
        when(handler.queryType()).thenReturn(Map.class);
        when(handler.sceneCode()).thenReturn("test.chunked");
        when(handler.sceneName()).thenReturn("分批导出");
        var meta = new ExportMeta(); meta.setFileName("rows.csv"); meta.setFileType("csv");
        meta.setQuerySnapshotSummary("test");
        when(handler.buildMeta(any())).thenReturn(meta);
        when(handler.columns(any())).thenReturn(List.of(new ExportColumn("value", "值", 0)));
        doReturn(IntStream.range(0, 5000).mapToObj(index -> Map.of("value", "row-" + index)).toList())
                .when(handler).queryPage(any(), eq(1L), eq(5000L));
        doReturn(List.of(Map.of("value", "row-5000"))).when(handler).queryPage(any(), eq(2L), eq(5000L));
        var scenes = mock(ExportSceneRegistry.class); doReturn(handler).when(scenes).getRequired("test.chunked");
        var renderers = mock(ExportRendererRegistry.class); when(renderers.getRequired("csv")).thenReturn(new CsvExportRenderer());
        var lifecycle = mock(ExportRecordLifecycleAppService.class);
        doAnswer(call -> { call.<ExportRecord>getArgument(0).setId(100L); return null; }).when(lifecycle).createProcessingRecord(any());
        var content = new AtomicReference<String>(); var path = new AtomicReference<Path>();
        ExportFileSink sink = new ExportFileSink() {
            public void delete(String key) {}
            public ExportStoredFile store(RenderedExportFile file, ExportStoreRequest request) {
            try {
            path.set(file.getContentPath()); content.set(Files.readString(path.get()));
            var stored = new ExportStoredFile(); stored.setObjectKey("rows.csv"); stored.setStorageType("test");
            stored.setContentType(file.getContentType()); stored.setFileSize(file.getFileSize()); return stored;
            } catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
            }
        };
        var beans = new DefaultListableBeanFactory(); beans.registerSingleton("translation", new TranslationEngine(List.of()));
        var json = new ObjectMapper();
        var service = new ExportExecutionAppService(scenes, renderers, sink, lifecycle, json,
                new DynamicQuerySummaryRenderer(), Runnable::run, beans.getBeanProvider(TranslationEngine.class));
        service.submit("test.chunked", json.createObjectNode());
        assertThat(content.get()).contains("row-0", "row-5000");
        assertThat(content.get().lines().count()).isEqualTo(5002);
        assertThat(Files.exists(path.get())).isFalse();
        verify(handler, never()).queryRows(any());
        verify(lifecycle).markSuccess(eq(100L), eq("rows.csv"), anyString(), anyLong(), eq("test"));
    }
}
