package com.oigit.admin.file.app;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

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
import com.oigit.admin.file.service.DirectUploadCredential;
import com.oigit.admin.file.service.FileService;
import com.oigit.admin.file.service.StoredFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileAppService {

    private final FileService fileService;

    public FileAppService(FileService fileService) {
        this.fileService = fileService;
    }

    public StoredFileRspDTO upload(MultipartFile file, UploadFileReqDTO reqDTO) {
        StoredFile storedFile = fileService.upload(file, reqDTO.getBizPath(), reqDTO.getObjectKey());
        return new StoredFileRspDTO(
                storedFile.getObjectKey(),
                storedFile.getOriginUrl(),
                storedFile.getFileName(),
                storedFile.getContentType(),
                storedFile.getSize()
        );
    }

    public void delete(DeleteFileReqDTO reqDTO) {
        fileService.delete(reqDTO.getObjectKey());
    }

    public FetchTempUrlRspDTO fetchTempUrl(FetchTempUrlReqDTO reqDTO) {
        return new FetchTempUrlRspDTO(reqDTO.getObjectKey(), fileService.fetchTempUrl(reqDTO.getObjectKey()));
    }

    public FetchTempUrlBatchRspDTO batchFetchTempUrls(FetchTempUrlBatchReqDTO reqDTO) {
        LinkedHashSet<String> objectKeys = new LinkedHashSet<>(reqDTO.getObjectKeys());
        List<FetchTempUrlItemRspDTO> items = new ArrayList<>(objectKeys.size());
        for (String objectKey : objectKeys) {
            items.add(new FetchTempUrlItemRspDTO(objectKey, fileService.fetchTempUrl(objectKey)));
        }
        return new FetchTempUrlBatchRspDTO(items);
    }

    public FetchDirectUploadCredentialRspDTO fetchDirectUploadCredential(FetchDirectUploadCredentialReqDTO reqDTO) {
        DirectUploadCredential directUploadCredential = fileService.fetchDirectUploadCredential(
                reqDTO.getBizPath(),
                reqDTO.getObjectKey()
        );
        return new FetchDirectUploadCredentialRspDTO(
                directUploadCredential.getProvider(),
                directUploadCredential.getCredential(),
                directUploadCredential.getObjectKey(),
                directUploadCredential.getOriginUrl(),
                directUploadCredential.getUploadHost()
        );
    }
}
