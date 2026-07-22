package com.oigit.admin.core.export.renderer;

import com.oigit.admin.core.export.model.ExportColumn;
import com.oigit.admin.core.export.model.ExportRenderRequest;
import com.oigit.admin.core.export.spi.ExportRenderer;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
    public String contentType() {
        return CONTENT_TYPE;
    }

    @Override
    public void render(ExportRenderRequest request, OutputStream outputStream) throws IOException {
        List<ExportColumn> orderedColumns = new ArrayList<>(request.getColumns());
        orderedColumns.sort(Comparator.comparing(column -> column.getOrder() == null ? Integer.MAX_VALUE : column.getOrder()));

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        writer.write(UTF8_BOM);
        writeCsvLine(writer, orderedColumns.stream().map(ExportColumn::getTitle).toList());
        for (Object row : request.getRows()) {
            List<String> values = new ArrayList<>(orderedColumns.size());
            for (ExportColumn column : orderedColumns) {
                values.add(stringify(resolveValue(row, column.getField())));
            }
            writeCsvLine(writer, values);
        }
        writer.flush();
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

    private void writeCsvLine(BufferedWriter writer, List<String> values) throws IOException {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                writer.write(',');
            }
            writer.write(escape(values.get(index)));
        }
        writer.write("\r\n");
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
