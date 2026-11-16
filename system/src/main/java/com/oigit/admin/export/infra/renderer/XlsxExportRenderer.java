package com.oigit.admin.export.infra.renderer;

import com.oigit.admin.core.export.model.ExportColumn;
import com.oigit.admin.core.export.model.ExportRenderRequest;
import com.oigit.admin.core.export.model.ExportSheet;
import com.oigit.admin.core.export.spi.ExportRenderer;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

/** 文本表格导出；编码、长数字和以等号开头的值均保留为文本。 */
@Component
public class XlsxExportRenderer implements ExportRenderer {

    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final int ROW_WINDOW_SIZE = 100;

    @Override
    public String fileType() { return "xlsx"; }

    @Override
    public String contentType() { return CONTENT_TYPE; }

    @Override
    public void render(ExportRenderRequest request, java.io.OutputStream output) throws IOException {
        try (var workbook = new SXSSFWorkbook(null, ROW_WINDOW_SIZE, true, false)) {
            if (request.getSheets() == null || request.getSheets().isEmpty()) {
                writeSheet(workbook, new ExportSheet("数据", request.getColumns(), request.getRowSource()), false);
            } else {
                for (ExportSheet sheet : request.getSheets()) writeSheet(workbook, sheet, true);
            }
            workbook.write(output);
        }
    }

    private void writeSheet(SXSSFWorkbook workbook, ExportSheet source, boolean analysisSheet) {
        var sheet = workbook.createSheet(source.name());
        List<ExportColumn> columns = source.columns() == null ? List.of() : source.columns().stream()
                .sorted(Comparator.comparing(column -> column.getOrder() == null
                        ? Integer.MAX_VALUE : column.getOrder())).toList();
        var header = sheet.createRow(0);
        var headerStyle = workbook.createCellStyle();
        var font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        for (int column = 0; column < columns.size(); column++) {
            var cell = header.createCell(column, CellType.STRING);
            cell.setCellValue(columns.get(column).getTitle());
            if (analysisSheet) {
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(column, 24 * 256);
            }
        }
        int index = 1;
        for (Object rowSource : source.rows() == null ? List.of() : source.rows()) {
            var row = sheet.createRow(index++);
            for (int column = 0; column < columns.size(); column++) {
                Object value = value(rowSource, columns.get(column).getField());
                if (analysisSheet && value instanceof Number number) {
                    row.createCell(column, CellType.NUMERIC).setCellValue(number.doubleValue());
                } else {
                    row.createCell(column, CellType.STRING).setCellValue(value == null ? "" : String.valueOf(value));
                }
            }
        }
        if (analysisSheet && !columns.isEmpty()) {
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, index - 1), 0, columns.size() - 1));
        }
    }

    private Object value(Object row, String field) {
        if (row == null || field == null) return null;
        if (row instanceof Map<?, ?> map) return map.get(field);
        var bean = new BeanWrapperImpl(row);
        return bean.isReadableProperty(field) ? bean.getPropertyValue(field) : null;
    }
}
