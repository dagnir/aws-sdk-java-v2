/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3.internal;

import java.io.File;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.services.kms.KMSClient;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Same functionality as {@link AmazonS3Client} but is more resilient to
 * transient eventual consistency problem via retries with exponential backoff.
 */
public class AmazonS3TestClient extends AmazonS3Client {
    private static final int MAX_RETRY = 4;

    public AmazonS3TestClient() {
        super();
    }

    public AmazonS3TestClient(AwsCredentials awsCredentials) {
        super(awsCredentials);
    }

    public AmazonS3TestClient(KMSClient kms, AwsCredentials awsCredentials) {
        super(awsCredentials);
    }

    public AmazonS3TestClient(AwsCredentials awsCredentials, LegacyClientConfiguration clientConfiguration) {
        super(awsCredentials, clientConfiguration);
    }

    public AmazonS3TestClient(AwsCredentialsProvider credentialsProvider) {
        super(credentialsProvider);
    }

    public AmazonS3TestClient(AwsCredentialsProvider credentialsProvider,
                              LegacyClientConfiguration clientConfiguration) {
        super(credentialsProvider, clientConfiguration);
    }

    public AmazonS3TestClient(AwsCredentialsProvider credentialsProvider,
                              LegacyClientConfiguration clientConfiguration,
                              RequestMetricCollector requestMetricCollector) {
        super(credentialsProvider, clientConfiguration, requestMetricCollector);
    }

    public AmazonS3TestClient(LegacyClientConfiguration clientConfiguration) {
        super(clientConfiguration);
    }

    @Override
    public S3Object getObject(GetObjectRequest req) {
        for (int i = 0; ; i++) {
            try {
                return super.getObject(req);
            } catch (AmazonClientException ex) {
                if (i >= MAX_RETRY) {
                    throw ex;
                }
                ex.printStackTrace();
                System.out.println("Retrying getObject " + (i + 1));
                try {
                    Thread.sleep((1 << (i + 1)) * 1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public ObjectMetadata getObject(GetObjectRequest req, File dest) {
        for (int i = 0; ; i++) {
            try {
                return super.getObject(req, dest);
            } catch (AmazonClientException ex) {
                if (i >= MAX_RETRY) {
                    throw ex;
                }
                ex.printStackTrace();
                System.out.println("Retrying getObject " + (i + 1));
                try {
                    Thread.sleep((1 << (i + 1)) * 1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
