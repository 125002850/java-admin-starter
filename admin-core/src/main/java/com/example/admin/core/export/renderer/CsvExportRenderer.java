package com.example.admin.core.export.renderer;

import com.example.admin.core.export.model.ExportColumn;
import com.example.admin.core.export.model.ExportRenderRequest;
import com.example.admin.core.export.model.RenderedExportFile;
import com.example.admin.core.export.spi.ExportRenderer;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class CsvExportRenderer implements ExportRenderer {

    private static final String FILE_TYPE = "csv";
    private static final String CONTENT_TYPE = "text/csv;charset=UTF-8";
    private static final String UTF8_BOM = "\uFEFF";

    @Override
    public String fileType() {
        return FILE_TYPE;
    }

    @Override
    public RenderedExportFile render(ExportRenderRequest request) {
        List<ExportColumn> orderedColumns = new ArrayList<>(request.getColumns());
        orderedColumns.sort(Comparator.comparing(column -> column.getOrder() == null ? Integer.MAX_VALUE : column.getOrder()));

        StringBuilder builder = new StringBuilder(UTF8_BOM);
        builder.append(toCsvLine(orderedColumns.stream().map(ExportColumn::getTitle).toList())).append("\r\n");
        for (Object row : request.getRows()) {
            List<String> values = new ArrayList<>(orderedColumns.size());
            for (ExportColumn column : orderedColumns) {
                values.add(stringify(resolveValue(row, column.getField())));
            }
            builder.append(toCsvLine(values)).append("\r\n");
        }

        byte[] content = builder.toString().getBytes(StandardCharsets.UTF_8);
        RenderedExportFile file = new RenderedExportFile();
        file.setFileName(request.getFileName());
        file.setFileType(FILE_TYPE);
        file.setContentType(CONTENT_TYPE);
        file.setContent(content);
        file.setFileSize(content.length);
        return file;
    }

    private Object resolveValue(Object row, String field) {
        if (row == null || field == null) {
            return null;
        }
        if (row instanceof Map<?, ?> map) {
            return map.get(field);
        }
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(row);
        if (!beanWrapper.isReadableProperty(field)) {
            return null;
        }
        return beanWrapper.getPropertyValue(field);
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String toCsvLine(List<String> values) {
        return values.stream().map(this::escape).reduce((left, right) -> left + "," + right).orElse("");
    }

    private String escape(String value) {
        String normalized = value == null ? "" : value;
        boolean needQuote = normalized.contains(",")
                || normalized.contains("\"")
                || normalized.contains("\r")
                || normalized.contains("\n");
        if (!needQuote) {
            return normalized;
        }
        return "\"" + normalized.replace("\"", "\"\"") + "\"";
    }
}
