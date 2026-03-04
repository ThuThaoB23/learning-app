package com.learnapp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MinioPropertiesTest {

    @Test
    void getObjectBaseUrlShouldPreferConfiguredPublicUrl() {
        MinioProperties properties = new MinioProperties();
        properties.setUrl("http://minio:9000");
        properties.setPublicUrl(" https://minio.backtofuture.baby/ ");

        assertEquals("https://minio.backtofuture.baby", properties.getObjectBaseUrl());
    }

    @Test
    void getObjectBaseUrlShouldFallBackToInternalUrlWhenPublicUrlIsBlank() {
        MinioProperties properties = new MinioProperties();
        properties.setUrl("http://localhost:9000/");
        properties.setPublicUrl("   ");

        assertEquals("http://localhost:9000", properties.getObjectBaseUrl());
    }

    @Test
    void getObjectBaseUrlShouldReturnNullWhenNoUrlIsConfigured() {
        MinioProperties properties = new MinioProperties();

        assertNull(properties.getObjectBaseUrl());
    }
}
