package com.demo.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "platform.file.storage")
public class FileStorageProperties {

    private String type = "local";
    private String zoneId = "Asia/Shanghai";
    private Local local = new Local();
    private Qiniu qiniu = new Qiniu();
    private Minio minio = new Minio();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public Qiniu getQiniu() {
        return qiniu;
    }

    public void setQiniu(Qiniu qiniu) {
        this.qiniu = qiniu;
    }

    public Minio getMinio() {
        return minio;
    }

    public void setMinio(Minio minio) {
        this.minio = minio;
    }

    public static class Local {

        private String rootDir = System.getProperty("java.io.tmpdir") + "/java-demo/uploads";
        private String baseUrl = "/local-files";

        public String getRootDir() {
            return rootDir;
        }

        public void setRootDir(String rootDir) {
            this.rootDir = rootDir;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Qiniu {

        private String accessKey;
        private String secretKey;
        private String bucketName;
        private String baseUrl;
        private String uploadHost = "https://upload.qiniup.com";
        private boolean privateBucket;
        private long uploadTokenExpireSeconds = 600L;
        private long downloadUrlExpireSeconds = 1200L;
        private String region = "huadong";
        private int connectTimeout = 10;
        private int readTimeout = 30;
        private int writeTimeout;

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getUploadHost() {
            return uploadHost;
        }

        public void setUploadHost(String uploadHost) {
            this.uploadHost = uploadHost;
        }

        public boolean isPrivateBucket() {
            return privateBucket;
        }

        public void setPrivateBucket(boolean privateBucket) {
            this.privateBucket = privateBucket;
        }

        public long getUploadTokenExpireSeconds() {
            return uploadTokenExpireSeconds;
        }

        public void setUploadTokenExpireSeconds(long uploadTokenExpireSeconds) {
            this.uploadTokenExpireSeconds = uploadTokenExpireSeconds;
        }

        public long getDownloadUrlExpireSeconds() {
            return downloadUrlExpireSeconds;
        }

        public void setDownloadUrlExpireSeconds(long downloadUrlExpireSeconds) {
            this.downloadUrlExpireSeconds = downloadUrlExpireSeconds;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }

        public int getWriteTimeout() {
            return writeTimeout;
        }

        public void setWriteTimeout(int writeTimeout) {
            this.writeTimeout = writeTimeout;
        }
    }

    public static class Minio {

        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucketName;
        private String baseUrl;
        private boolean privateBucket = true;
        private long downloadUrlExpireSeconds = 1200L;
        private String region;
        private int connectTimeout = 10;
        private int readTimeout = 30;
        private int writeTimeout;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public boolean isPrivateBucket() {
            return privateBucket;
        }

        public void setPrivateBucket(boolean privateBucket) {
            this.privateBucket = privateBucket;
        }

        public long getDownloadUrlExpireSeconds() {
            return downloadUrlExpireSeconds;
        }

        public void setDownloadUrlExpireSeconds(long downloadUrlExpireSeconds) {
            this.downloadUrlExpireSeconds = downloadUrlExpireSeconds;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }

        public int getWriteTimeout() {
            return writeTimeout;
        }

        public void setWriteTimeout(int writeTimeout) {
            this.writeTimeout = writeTimeout;
        }
    }
}
