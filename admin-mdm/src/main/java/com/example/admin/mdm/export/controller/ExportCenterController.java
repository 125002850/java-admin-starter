package com.example.admin.mdm.export.controller;

import com.example.admin.core.web.PageResult;
import com.example.admin.core.web.R;
import com.example.admin.mdm.export.app.ExportCenterAppService;
import com.example.admin.mdm.export.controller.dto.ExportBatchDownloadReqDTO;
import com.example.admin.mdm.export.controller.dto.ExportBatchDownloadRspDTO;
import com.example.admin.mdm.export.controller.dto.ExportDownloadRspDTO;
import com.example.admin.mdm.export.controller.dto.ExportRecordDeleteReqDTO;
import com.example.admin.mdm.export.controller.dto.ExportRecordIdReqDTO;
import com.example.admin.mdm.export.controller.dto.ExportRecordRspDTO;
import com.example.admin.mdm.export.controller.dto.ExportSubmitReqDTO;
import com.example.admin.mdm.export.controller.dto.ExportSubmitRspDTO;
import com.example.admin.mdm.export.controller.dto.query.ExportRecordDynamicPageReqDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Tag(name = "导出中心", description = "统一导出提交、查询、下载相关接口")
@RequestMapping("/api/mdm/export")
public class ExportCenterController {

    private final ExportCenterAppService exportCenterAppService;

    public ExportCenterController(ExportCenterAppService exportCenterAppService) {
        this.exportCenterAppService = exportCenterAppService;
    }

    @Operation(summary = "提交导出任务", description = "按导出场景编码提交一次同步导出", operationId = "submitExport")
    @PostMapping("/submit")
    public R<ExportSubmitRspDTO> submit(@Valid @RequestBody ExportSubmitReqDTO reqDTO) {
        return R.ok(exportCenterAppService.submit(reqDTO));
    }

    @Operation(summary = "分页查询我的导出记录", description = "按当前登录人返回其导出记录列表", operationId = "pageMyExportRecords")
    @PostMapping("/my/page")
    public R<PageResult<ExportRecordRspDTO>> pageMyExports(@Valid @RequestBody ExportRecordDynamicPageReqDTO reqDTO) {
        return R.ok(exportCenterAppService.pageMyExports(reqDTO));
    }

    @Operation(summary = "查询导出记录详情", description = "按导出记录ID查询详情", operationId = "detailExportRecord")
    @PostMapping("/detail")
    public R<ExportRecordRspDTO> detail(@Valid @RequestBody ExportRecordIdReqDTO reqDTO) {
        return R.ok(exportCenterAppService.detail(reqDTO.getRecordId()));
    }

    @Operation(summary = "获取导出下载地址", description = "校验导出记录后返回临时下载地址", operationId = "downloadExportRecord")
    @PostMapping("/download")
    public R<ExportDownloadRspDTO> download(@Valid @RequestBody ExportRecordIdReqDTO reqDTO) {
        return R.ok(exportCenterAppService.download(reqDTO.getRecordId()));
    }

    @Operation(summary = "批量下载导出记录", description = "将多条可下载导出记录打包为 zip 并返回临时下载地址", operationId = "batchDownloadExportRecords")
    @PostMapping("/download/batch")
    public R<ExportBatchDownloadRspDTO> batchDownload(@Valid @RequestBody ExportBatchDownloadReqDTO reqDTO) {
        return R.ok(exportCenterAppService.batchDownload(reqDTO));
    }

    @Operation(summary = "删除导出记录", description = "批量逻辑删除指定导出记录", operationId = "deleteExportRecord")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody ExportRecordDeleteReqDTO reqDTO) {
        exportCenterAppService.delete(reqDTO.getIds());
        return R.ok();
    }
}
