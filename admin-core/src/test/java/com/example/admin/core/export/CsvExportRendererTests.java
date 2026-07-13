package com.example.admin.core.export;

import com.example.admin.core.export.model.ExportColumn;
import com.example.admin.core.export.model.ExportRenderRequest;
import com.example.admin.core.export.model.RenderedExportFile;
import com.example.admin.core.export.renderer.CsvExportRenderer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportRendererTests {

    @Test
    void render_should_output_csv_with_header_and_escaped_values() {
        CsvExportRenderer renderer = new CsvExportRenderer();
        ExportRenderRequest request = new ExportRenderRequest();
        request.setFileName("demo.csv");
        request.setColumns(List.of(
            new ExportColumn("name", "名称", 1),
            new ExportColumn("remark", "备注", 2)
        ));
        request.setRows(List.of(
            Map.of("name", "alpha", "remark", "plain"),
            Map.of("name", "beta", "remark", "a,b\"c")
        ));

        RenderedExportFile file = renderer.render(request);
        String csv = new String(file.getContent(), StandardCharsets.UTF_8);

        assertThat(file.getFileType()).isEqualTo("csv");
        assertThat(file.getContentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(csv).startsWith("\uFEFF");
        assertThat(csv).contains("名称,备注");
        assertThat(csv).contains("alpha,plain");
        assertThat(csv).contains("beta,\"a,b\"\"c\"");
    }
}
