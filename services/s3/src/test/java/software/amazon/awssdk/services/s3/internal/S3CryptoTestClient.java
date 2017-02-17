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
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.services.kms.AWSKMSClient;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.EncryptionMaterialsProvider;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutInstructionFileRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.StaticEncryptionMaterialsProvider;

/**
 * Same functionality as {@link AmazonS3EncryptionClient} but is more resilient
 * to transient eventual consistency problem via retries with exponential
 * backoff.
 */
public class S3CryptoTestClient extends AmazonS3EncryptionClient {
    private static final int MAX_RETRY = 5;

    public S3CryptoTestClient(EncryptionMaterials encryptionMaterials) {
        super(encryptionMaterials);
    }

    public S3CryptoTestClient(
            EncryptionMaterialsProvider encryptionMaterialsProvider) {
        super(encryptionMaterialsProvider);
    }

    public S3CryptoTestClient(EncryptionMaterials encryptionMaterials,
                              CryptoConfiguration cryptoConfig) {
        super(encryptionMaterials, cryptoConfig);
    }

    public S3CryptoTestClient(
            EncryptionMaterialsProvider encryptionMaterialsProvider,
            CryptoConfiguration cryptoConfig) {
        super(encryptionMaterialsProvider, cryptoConfig);
    }

    public S3CryptoTestClient(AwsCredentials credentials,
                              EncryptionMaterials encryptionMaterials) {
        super(credentials, encryptionMaterials);
    }

    public S3CryptoTestClient(AwsCredentials credentials,
                              EncryptionMaterialsProvider encryptionMaterialsProvider) {
        super(credentials, encryptionMaterialsProvider);
    }

    public S3CryptoTestClient(
            AwsCredentialsProvider credentialsProvider,
            EncryptionMaterialsProvider encryptionMaterialsProvider) {
        super(credentialsProvider, encryptionMaterialsProvider);
    }

    public S3CryptoTestClient(AwsCredentials credentials,
                              EncryptionMaterials encryptionMaterials,
                              CryptoConfiguration cryptoConfig) {
        super(credentials, encryptionMaterials, cryptoConfig);
    }

    public S3CryptoTestClient(AWSKMSClient kms, AwsCredentials credentials,
                              EncryptionMaterials encryptionMaterials,
                              CryptoConfiguration cryptoConfig) {
        super(kms, new AwsStaticCredentialsProvider(credentials),
              new StaticEncryptionMaterialsProvider(encryptionMaterials),
              new ClientConfiguration(),
              cryptoConfig,
              null);
    }

    public S3CryptoTestClient(AwsCredentials credentials,
                              EncryptionMaterialsProvider encryptionMaterialsProvider,
                              CryptoConfiguration cryptoConfig) {
        super(credentials, encryptionMaterialsProvider, cryptoConfig);
    }

    public S3CryptoTestClient(
            AwsCredentialsProvider credentialsProvider,
            EncryptionMaterialsProvider encryptionMaterialsProvider,
            CryptoConfiguration cryptoConfig) {
        super(credentialsProvider, encryptionMaterialsProvider, cryptoConfig);
    }

    public S3CryptoTestClient(AwsCredentials credentials,
                              EncryptionMaterials encryptionMaterials,
                              ClientConfiguration clientConfig, CryptoConfiguration cryptoConfig) {
        super(credentials, encryptionMaterials, clientConfig, cryptoConfig);
    }

    public S3CryptoTestClient(AWSKMSClient kms, AwsCredentials credentials,
                              EncryptionMaterialsProvider encryptionMaterialsProvider,
                              ClientConfiguration clientConfig, CryptoConfiguration cryptoConfig) {
        super(kms,
              new AwsStaticCredentialsProvider(credentials),
              encryptionMaterialsProvider,
              clientConfig,
              cryptoConfig, null);
    }

    public S3CryptoTestClient(AwsCredentials credentials,
                              EncryptionMaterialsProvider encryptionMaterialsProvider,
                              ClientConfiguration clientConfig, CryptoConfiguration cryptoConfig) {
        super(credentials, encryptionMaterialsProvider, clientConfig,
              cryptoConfig);
    }

    public S3CryptoTestClient(
            AwsCredentialsProvider credentialsProvider,
            EncryptionMaterialsProvider kekMaterialsProvider,
            ClientConfiguration clientConfig,
            CryptoConfiguration cryptoConfig) {
        super(credentialsProvider, kekMaterialsProvider, clientConfig, cryptoConfig);
    }

    public S3CryptoTestClient(
            AwsCredentialsProvider credentialsProvider,
            EncryptionMaterialsProvider kekMaterialsProvider,
            ClientConfiguration clientConfig,
            CryptoConfiguration cryptoConfig,
            RequestMetricCollector requestMetricCollector) {
        super(credentialsProvider, kekMaterialsProvider, clientConfig, cryptoConfig, requestMetricCollector);
    }

    public S3CryptoTestClient(
            AWSKMSClient kms,
            AwsCredentialsProvider credentialsProvider,
            EncryptionMaterialsProvider kekMaterialsProvider,
            ClientConfiguration clientConfig,
            CryptoConfiguration cryptoConfig,
            RequestMetricCollector requestMetricCollector) {
        super(kms, credentialsProvider, kekMaterialsProvider, clientConfig, cryptoConfig, requestMetricCollector);
    }


    @Override
    public S3Object getObject(GetObjectRequest req) {
        for (int i = 0; ; i++) {
            try {
                return super.getObject(req);
            } catch (RuntimeException ex) {
                if (i >= MAX_RETRY) {
                    throw ex;
                }
                if (ex instanceof SecurityException) {
                    final String msg = ex.getMessage();
                    if (!msg.contains("Instruction file not found")) {
                        throw ex;
                    }
                    printAndPause(i, ex);
                } else if (ex instanceof AmazonClientException) {
                    printAndPause(i, ex);
                } else {
                    throw ex;
                }
            }
        }
    }

    @Override
    public ObjectMetadata getObject(GetObjectRequest req, File dest) {
        for (int i = 0; ; i++) {
            try {
                return super.getObject(req, dest);
            } catch (RuntimeException ex) {
                if (i >= MAX_RETRY) {
                    throw ex;
                }
                if (ex instanceof SecurityException) {
                    final String msg = ex.getMessage();
                    if (!msg.contains("Instruction file not found")) {
                        throw ex;
                    }
                    printAndPause(i, ex);
                } else if (ex instanceof AmazonClientException) {
                    printAndPause(i, ex);
                } else {
                    throw ex;
                }
            }
        }
    }

    private void printAndPause(int i, RuntimeException ex) {
        ex.printStackTrace();
        System.out.println("Retrying getObject " + (i + 1));
        try {
            Thread.sleep((1 << (i + 1)) * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PutObjectResult putInstructionFile(PutInstructionFileRequest req) {
        for (int i = 0; ; i++) {
            try {
                return super.putInstructionFile(req);
            } catch (RuntimeException ex) {
                if (i >= MAX_RETRY) {
                    throw ex;
                }
                if (ex instanceof IllegalArgumentException
                    || ex instanceof AmazonClientException) {
                    ex.printStackTrace();
                    System.out.println("Retrying putInstructionFile " + (i + 1));
                    try {
                        Thread.sleep((1 << (i + 1)) * 1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw ex;
                }
            }
        }
    }
}
