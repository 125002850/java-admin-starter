package com.demo.file.app;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.demo.file.controller.dto.DeleteFileReqDTO;
import com.demo.file.controller.dto.FetchDirectUploadCredentialReqDTO;
import com.demo.file.controller.dto.FetchDirectUploadCredentialRspDTO;
import com.demo.file.controller.dto.FetchTempUrlBatchReqDTO;
import com.demo.file.controller.dto.FetchTempUrlBatchRspDTO;
import com.demo.file.controller.dto.FetchTempUrlItemRspDTO;
import com.demo.file.controller.dto.FetchTempUrlReqDTO;
import com.demo.file.controller.dto.FetchTempUrlRspDTO;
import com.demo.file.controller.dto.StoredFileRspDTO;
import com.demo.file.controller.dto.UploadFileReqDTO;
import com.demo.file.service.DirectUploadCredential;
import com.demo.file.service.FileService;
import com.demo.file.service.StoredFile;
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
