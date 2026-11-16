package com.oigit.admin.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oigit.admin.core.export.model.ExportColumn;
import com.oigit.admin.core.export.model.ExportRenderRequest;
import com.oigit.admin.core.export.model.ExportSheet;
import com.oigit.admin.export.infra.renderer.XlsxExportRenderer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.zip.ZipFile;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XlsxExportRendererTests {

    @TempDir
    Path directory;

    @Test
    void streamingWorkbookPreservesTextNumbersHeadersAndFilters() throws Exception {
        var columns = List.of(new ExportColumn("code", "编码", 0), new ExportColumn("count", "数量", 1));
        Iterable<Map<String, Object>> rows = () -> IntStream.range(0, 250)
                .mapToObj(index -> Map.<String, Object>of("code", index == 249 ? "=1+1" : "000001",
                        "count", index)).iterator();
        var request = new ExportRenderRequest();
        request.setFileName("关联.xlsx");
        request.setSheets(List.of(new ExportSheet("关联明细", columns, rows)));
        try (var file = new XlsxExportRenderer().render(request);
                var input = file.openInputStream();
                var workbook = new XSSFWorkbook(input)) {
            var sheet = workbook.getSheet("关联明细");
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(251);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("000001");
            assertThat(sheet.getRow(250).getCell(0).getStringCellValue()).isEqualTo("=1+1");
            assertThat(sheet.getRow(250).getCell(0).getCellType()).isEqualTo(CellType.STRING);
            assertThat(sheet.getRow(250).getCell(1).getNumericCellValue()).isEqualTo(249);
            assertThat(workbook.getFontAt(sheet.getRow(0).getCell(0).getCellStyle().getFontIndex()).getBold()).isTrue();
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
            assertThat(sheet.getCTWorksheet().getAutoFilter().getRef()).isEqualTo("A1:B251");
        }
    }

    @Test
    void largeWorkbookFitsIn96MiBAndCleansTemporaryFiles() throws Exception {
        var log = directory.resolve("probe.log");
        var output = directory.resolve("large.xlsx");
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx96m", "-Djava.io.tmpdir=" + directory, "-cp",
                System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                XlsxExportRendererTests.class.getName(), output.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertThat(process.waitFor(60, TimeUnit.SECONDS)).as("小堆导出应及时完成").isTrue();
            assertThat(process.exitValue()).withFailMessage("小堆导出失败：%s", Files.readString(log)).isZero();
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
        int rows = 0;
        int cells = 0;
        try (var zip = new ZipFile(output.toFile());
                var input = zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml"))) {
            var xml = XMLInputFactory.newFactory().createXMLStreamReader(input);
            try {
                while (xml.hasNext()) {
                    if (xml.next() == XMLStreamConstants.START_ELEMENT) {
                        if ("row".equals(xml.getLocalName())) rows++;
                        if ("c".equals(xml.getLocalName())) cells++;
                    }
                }
            } finally {
                xml.close();
            }
        }
        assertThat(rows).isEqualTo(100_001);
        assertThat(cells).isEqualTo(100_001 * 17);
        try (var files = Files.walk(directory)) {
            assertThat(files.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).toList())
                    .containsExactlyInAnyOrder("large.xlsx", "probe.log");
        }
    }

    /** 独立 JVM 限制堆大小，验证实际导出行为而非仅断言选用了某个 POI 类。 */
    public static void main(String[] args) throws Exception {
        var columns = IntStream.range(0, 17).mapToObj(index -> new ExportColumn("c" + index, "列" + index, index))
                .toList();
        Iterable<Map<String, Object>> rows = () -> IntStream.range(0, 100_000).mapToObj(index -> {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int column = 0; column < 17; column++) row.put("c" + column, "关联数据-" + index + "-" + column);
            return row;
        }).iterator();
        var request = new ExportRenderRequest();
        request.setFileName("large.xlsx");
        request.setSheets(List.of(new ExportSheet("关联明细", columns, rows)));
        try (var file = new XlsxExportRenderer().render(request); var input = file.openInputStream()) {
            Files.copy(input, Path.of(args[0]));
        }

        // 写入失败及 OOM 退出路径也应释放工作簿和已生成的临时文件。
        for (boolean outOfMemory : List.of(false, true)) {
            Iterable<Object> brokenRows = () -> new Iterator<>() {
                @Override
                public boolean hasNext() { return true; }

                @Override
                public Object next() {
                    if (outOfMemory) throw new OutOfMemoryError("simulated export OOM");
                    throw new IllegalStateException("simulated query failure");
                }
            };
            request.setSheets(List.of(new ExportSheet("关联明细", columns, brokenRows)));
            assertThatThrownBy(() -> new XlsxExportRenderer().render(request))
                    .isInstanceOf(outOfMemory ? OutOfMemoryError.class : IllegalStateException.class);
        }
    }
}
