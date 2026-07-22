package com.oigit.admin.file.controller;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.oigit.admin.core.web.R;
import com.oigit.admin.file.app.FileAppService;
import com.oigit.admin.file.controller.dto.DeleteFileReqDTO;
import com.oigit.admin.file.controller.dto.FetchDirectUploadCredentialReqDTO;
import com.oigit.admin.file.controller.dto.FetchDirectUploadCredentialRspDTO;
import com.oigit.admin.file.controller.dto.FetchTempUrlBatchReqDTO;
import com.oigit.admin.file.controller.dto.FetchTempUrlBatchRspDTO;
import com.oigit.admin.file.controller.dto.FetchTempUrlReqDTO;
import com.oigit.admin.file.controller.dto.FetchTempUrlRspDTO;
import com.oigit.admin.file.controller.dto.StoredFileRspDTO;
import com.oigit.admin.file.controller.dto.UploadFileReqDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Validated
@RestController
@Tag(name = "文件存储", description = "文件上传、删除、临时访问地址与直传凭证相关接口")
@RequestMapping("/api/file/storage")
public class FileStorageController {

    private final FileAppService fileAppService;

    public FileStorageController(FileAppService fileAppService) {
        this.fileAppService = fileAppService;
    }

    @Operation(summary = "上传文件对象", description = "上传文件到当前启用的文件存储 provider", operationId = "uploadFileObject")
    @PostMapping(value = "/object/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<StoredFileRspDTO> upload(
            @Schema(type = "string", format = "binary")
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "业务路径", example = "avatar/user", required = true)
            @RequestParam("bizPath") String bizPath,
            @Parameter(description = "对象键，可选", example = "avatar/user/custom-key.png")
            @RequestParam(value = "objectKey", required = false) String objectKey
    ) {
        UploadFileReqDTO reqDTO = new UploadFileReqDTO();
        reqDTO.setBizPath(bizPath);
        reqDTO.setObjectKey(objectKey);
        return R.ok(fileAppService.upload(file, reqDTO));
    }

    @Operation(summary = "删除文件对象", description = "根据对象键删除文件", operationId = "deleteFileObject")
    @PostMapping("/object/delete")
    public R<Void> delete(@Valid @RequestBody DeleteFileReqDTO reqDTO) {
        fileAppService.delete(reqDTO);
        return R.ok();
    }

    @Operation(summary = "获取文件临时访问地址", description = "根据对象键获取临时访问地址")
    @PostMapping("/object/temp-url/fetch")
    public R<FetchTempUrlRspDTO> fetchTempUrl(@Valid @RequestBody FetchTempUrlReqDTO reqDTO) {
        return R.ok(fileAppService.fetchTempUrl(reqDTO));
    }

    @Operation(
            summary = "批量获取文件临时访问地址",
            description = "根据对象键列表批量获取临时访问地址，重复对象键会按首次出现顺序去重",
            operationId = "batchFetchFileObjectTempUrls"
    )
    @PostMapping("/object/temp-url/batch-fetch")
    public R<FetchTempUrlBatchRspDTO> batchFetchTempUrls(@Valid @RequestBody FetchTempUrlBatchReqDTO reqDTO) {
        return R.ok(fileAppService.batchFetchTempUrls(reqDTO));
    }

    @Operation(summary = "获取直传凭证", description = "获取客户端直传所需的凭证与对象信息")
    @PostMapping("/direct-upload/credential/fetch")
    public R<FetchDirectUploadCredentialRspDTO> fetchDirectUploadCredential(
            @Valid @RequestBody FetchDirectUploadCredentialReqDTO reqDTO
    ) {
        return R.ok(fileAppService.fetchDirectUploadCredential(reqDTO));
    }
}
