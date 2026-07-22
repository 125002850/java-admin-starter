package com.oigit.admin.file.infra.provider;

import com.oigit.admin.file.service.DirectUploadCredential;

public interface DirectUploadCapable {

    DirectUploadCredential fetchDirectUploadCredential(String objectKey);
}
