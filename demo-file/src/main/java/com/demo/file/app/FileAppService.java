package com.demo.file.app;

import com.demo.file.controller.dto.DeleteFileReqDTO;
import com.demo.file.controller.dto.FetchDirectUploadCredentialReqDTO;
import com.demo.file.controller.dto.FetchDirectUploadCredentialRspDTO;
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
