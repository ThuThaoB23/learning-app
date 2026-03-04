package com.learnapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    private String url;
    private String publicUrl;
    private String accessKey;
    private String secretKey;
    private String bucket;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
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

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getObjectBaseUrl() {
        String configuredPublicUrl = trimToNull(publicUrl);
        if (configuredPublicUrl != null) {
            return normalizeBaseUrl(configuredPublicUrl);
        }

        String configuredUrl = trimToNull(url);
        if (configuredUrl != null) {
            return normalizeBaseUrl(configuredUrl);
        }

        return null;
    }

    private String normalizeBaseUrl(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
