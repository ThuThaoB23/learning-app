package com.learnapp.config;

public final class MinioBucketPolicySupport {

    private MinioBucketPolicySupport() {
    }

    public static String buildPublicReadPolicy(String bucket) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("Bucket name must not be blank");
        }

        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {
                        "AWS": [
                          "*"
                        ]
                      },
                      "Action": [
                        "s3:GetObject"
                      ],
                      "Resource": [
                        "arn:aws:s3:::%s/*"
                      ]
                    }
                  ]
                }
                """.formatted(bucket.trim());
    }
}
