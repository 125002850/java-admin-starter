package com.demo.file.infra.provider;

import com.demo.file.service.DirectUploadCredential;

public interface DirectUploadCapable {

    DirectUploadCredential fetchDirectUploadCredential(String objectKey);
}
