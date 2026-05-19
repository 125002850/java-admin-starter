package com.demo.file.app;

import com.demo.file.controller.dto.DeleteFileReqDTO;
import com.demo.file.controller.dto.FetchDirectUploadCredentialReqDTO;
import com.demo.file.controller.dto.FetchDirectUploadCredentialRspDTO;
import com.demo.file.controller.dto.FetchTempUrlReqDTO;
import com.demo.file.controller.dto.FetchTempUrlRspDTO;
import com.demo.file.controller.dto.StoredFileRspDTO;
import com.demo.file.controller.dto.UploadFileReqDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileAppService {

    public StoredFileRspDTO upload(MultipartFile file, UploadFileReqDTO reqDTO) {
        String objectKey = resolveObjectKey(reqDTO.getObjectKey());
        return new StoredFileRspDTO(
                objectKey,
                "/local-files/" + objectKey,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );
    }

    public void delete(DeleteFileReqDTO reqDTO) {
        // Task 1 only exposes the endpoint contract; provider implementation arrives in Task 2.
    }

    public FetchTempUrlRspDTO fetchTempUrl(FetchTempUrlReqDTO reqDTO) {
        return new FetchTempUrlRspDTO(reqDTO.getObjectKey(), "/local-files/" + reqDTO.getObjectKey());
    }

    public FetchDirectUploadCredentialRspDTO fetchDirectUploadCredential(FetchDirectUploadCredentialReqDTO reqDTO) {
        String objectKey = resolveObjectKey(reqDTO.getObjectKey());
        return new FetchDirectUploadCredentialRspDTO(
                "stub",
                "stub-credential",
                objectKey,
                "/local-files/" + objectKey,
                "http://localhost/upload"
        );
    }

    private String resolveObjectKey(String objectKey) {
        if (StringUtils.hasText(objectKey)) {
            return objectKey;
        }
        return "task-1/placeholder-object";
    }
}
