package com.learnapp.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MinioBucketPolicySupportTest {

    @Test
    void buildPublicReadPolicyShouldAllowAnonymousGetObjectOnBucketObjects() {
        String policy = MinioBucketPolicySupport.buildPublicReadPolicy("uploads");

        assertTrue(policy.contains("\"Action\": ["));
        assertTrue(policy.contains("\"s3:GetObject\""));
        assertTrue(policy.contains("\"arn:aws:s3:::uploads/*\""));
    }

    @Test
    void buildPublicReadPolicyShouldRejectBlankBucketName() {
        assertThrows(IllegalArgumentException.class, () -> MinioBucketPolicySupport.buildPublicReadPolicy("  "));
    }
}
