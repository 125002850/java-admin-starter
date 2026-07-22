package com.oigit.admin.file.infra.provider.qiniu;

import com.oigit.admin.core.exception.BizException;
import com.oigit.admin.file.config.FileStorageProperties;
import com.oigit.admin.file.enums.FileErrorCode;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;

@Component
@ConditionalOnProperty(prefix = "platform.file.storage", name = "type", havingValue = "qiniu")
public class DefaultQiniuOperations implements QiniuOperations {

    private final Auth auth;
    private final UploadManager uploadManager;
    private final BucketManager bucketManager;

    public DefaultQiniuOperations(FileStorageProperties fileStorageProperties) {
        FileStorageProperties.Qiniu qiniu = fileStorageProperties.getQiniu();
        validateQiniuConfig(qiniu);

        this.auth = Auth.create(qiniu.getAccessKey(), qiniu.getSecretKey());
        Configuration configuration = Configuration.create(resolveRegion(qiniu.getRegion()));
        configuration.connectTimeout = qiniu.getConnectTimeout();
        configuration.readTimeout = qiniu.getReadTimeout();
        configuration.writeTimeout = qiniu.getWriteTimeout();

        this.uploadManager = new UploadManager(configuration);
        this.bucketManager = new BucketManager(auth, configuration);
    }

    @Override
    public void upload(InputStream inputStream, String objectKey, String uploadToken, String contentType) throws QiniuException {
        uploadManager.put(inputStream, objectKey, uploadToken, null, contentType);
    }

    @Override
    public void delete(String bucketName, String objectKey) throws QiniuException {
        bucketManager.delete(bucketName, objectKey);
    }

    @Override
    public String createUploadToken(String bucketName, String objectKey, long expireSeconds) {
        return auth.uploadToken(bucketName, objectKey, expireSeconds, null);
    }

    @Override
    public String createPrivateDownloadUrl(String originUrl, long expireSeconds) {
        return auth.privateDownloadUrl(originUrl, expireSeconds);
    }

    private void validateQiniuConfig(FileStorageProperties.Qiniu qiniu) {
        if (qiniu == null
                || !StringUtils.hasText(qiniu.getAccessKey())
                || !StringUtils.hasText(qiniu.getSecretKey())
                || !StringUtils.hasText(qiniu.getBucketName())
                || !StringUtils.hasText(qiniu.getBaseUrl())
                || !StringUtils.hasText(qiniu.getUploadHost())
                || !StringUtils.hasText(qiniu.getRegion())) {
            throw new BizException(FileErrorCode.INVALID_STORAGE_CONFIG);
        }
    }

    private Region resolveRegion(String region) {
        return switch (region) {
            case "huadong" -> Region.huadong();
            case "huabei" -> Region.huabei();
            case "huanan" -> Region.huanan();
            case "beimei" -> Region.beimei();
            case "xinjiapo" -> Region.xinjiapo();
            default -> throw new BizException(FileErrorCode.INVALID_STORAGE_CONFIG, "未知七牛区域: " + region);
        };
    }
}
