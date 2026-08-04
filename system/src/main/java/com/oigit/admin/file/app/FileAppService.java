package com.oigit.admin.file.app;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.file.domain.gateway.FileStorageGateway;
import com.oigit.admin.file.domain.model.DirectUploadCredential;
import com.oigit.admin.file.domain.model.StoredFile;
import com.oigit.admin.file.domain.service.FileObjectKeyPolicy;
import com.oigit.admin.file.dto.req.DeleteFileReqDTO;
import com.oigit.admin.file.dto.req.FetchDirectUploadCredentialReqDTO;
import com.oigit.admin.file.dto.req.FetchTempUrlBatchReqDTO;
import com.oigit.admin.file.dto.req.FetchTempUrlReqDTO;
import com.oigit.admin.file.dto.req.UploadFileReqDTO;
import com.oigit.admin.file.dto.rsp.FetchDirectUploadCredentialRspDTO;
import com.oigit.admin.file.dto.rsp.FetchTempUrlBatchRspDTO;
import com.oigit.admin.file.dto.rsp.FetchTempUrlItemRspDTO;
import com.oigit.admin.file.dto.rsp.FetchTempUrlRspDTO;
import com.oigit.admin.file.dto.rsp.StoredFileRspDTO;
import com.oigit.admin.file.enums.FileErrorCode;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class FileAppService {

    private final FileStorageGateway fileStorageGateway;
    private final FileObjectKeyPolicy fileObjectKeyPolicy;

    public FileAppService(FileStorageGateway fileStorageGateway, FileObjectKeyPolicy fileObjectKeyPolicy) {
        this.fileStorageGateway = fileStorageGateway;
        this.fileObjectKeyPolicy = fileObjectKeyPolicy;
    }

    public StoredFileRspDTO upload(
            InputStream inputStream,
            long size,
            String originalFilename,
            String contentType,
            UploadFileReqDTO reqDTO
    ) {
        StoredFile storedFile = store(
                inputStream,
                size,
                reqDTO.getBizPath(),
                reqDTO.getObjectKey(),
                originalFilename,
                contentType
        );
        return new StoredFileRspDTO(
                storedFile.getObjectKey(),
                storedFile.getOriginUrl(),
                storedFile.getFileName(),
                storedFile.getContentType(),
                storedFile.getSize()
        );
    }

    public StoredFile store(
            InputStream inputStream,
            long size,
            String bizPath,
            String objectKey,
            String originalFilename,
            String contentType
    ) {
        if (inputStream == null || size <= 0) {
            throw new BizException(FileErrorCode.EMPTY_FILE);
        }
        String resolvedObjectKey = fileObjectKeyPolicy.resolveObjectKey(bizPath, objectKey, originalFilename);
        try {
            return fileStorageGateway.upload(inputStream, resolvedObjectKey, contentType, size, originalFilename);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    public void delete(DeleteFileReqDTO reqDTO) {
        delete(reqDTO.getObjectKey());
    }

    public void delete(String objectKey) {
        fileStorageGateway.delete(fileObjectKeyPolicy.normalizeObjectKey(objectKey));
    }

    public FetchTempUrlRspDTO fetchTempUrl(FetchTempUrlReqDTO reqDTO) {
        return new FetchTempUrlRspDTO(reqDTO.getObjectKey(), fetchTempUrl(reqDTO.getObjectKey()));
    }

    public String fetchTempUrl(String objectKey) {
        try {
            return fileStorageGateway.buildTempUrl(fileObjectKeyPolicy.normalizeObjectKey(objectKey));
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(FileErrorCode.TEMP_URL_GENERATE_FAILED);
        }
    }

    public FetchTempUrlBatchRspDTO batchFetchTempUrls(FetchTempUrlBatchReqDTO reqDTO) {
        LinkedHashSet<String> objectKeys = new LinkedHashSet<>(reqDTO.getObjectKeys());
        List<FetchTempUrlItemRspDTO> items = new ArrayList<>(objectKeys.size());
        for (String objectKey : objectKeys) {
            items.add(new FetchTempUrlItemRspDTO(objectKey, fetchTempUrl(objectKey)));
        }
        return new FetchTempUrlBatchRspDTO(items);
    }

    public FetchDirectUploadCredentialRspDTO fetchDirectUploadCredential(FetchDirectUploadCredentialReqDTO reqDTO) {
        String objectKey = fileObjectKeyPolicy.resolveObjectKey(
                reqDTO.getBizPath(),
                reqDTO.getObjectKey(),
                null
        );
        DirectUploadCredential credential = fileStorageGateway.fetchDirectUploadCredential(objectKey)
                .orElseThrow(() -> new BizException(FileErrorCode.DIRECT_UPLOAD_NOT_SUPPORTED));
        return new FetchDirectUploadCredentialRspDTO(
                credential.getProvider(),
                credential.getCredential(),
                credential.getObjectKey(),
                credential.getOriginUrl(),
                credential.getUploadHost()
        );
    }

    public InputStream openDownloadStream(String objectKey) {
        try {
            return fileStorageGateway.openStream(fileObjectKeyPolicy.normalizeObjectKey(objectKey));
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(FileErrorCode.FILE_DOWNLOAD_FAILED);
        }
    }
}
