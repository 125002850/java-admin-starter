package com.example.admin.file.infra.provider;

import com.example.admin.file.service.DirectUploadCredential;

public interface DirectUploadCapable {

    DirectUploadCredential fetchDirectUploadCredential(String objectKey);
}
