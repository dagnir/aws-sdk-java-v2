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

package software.amazon.awssdk.services.s3.transfer;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.kms.utils.KmsTestKeyCache;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.SSEAlgorithm;
import software.amazon.awssdk.util.Md5Utils;

/**
 * Tests overloads that take in an {@link ObjectMetadataProvider} correctly skip validation when
 * SSE-C or SSE-KMS is involved and not when SSE is involved.
 */
public class TransferManagerWithSseIntegrationTest extends TransferManagerTestBase {

    private static final String VIRTUAL_DIRECTORY = "ssekms";

    private List<File> files;

    @Before
    public void setup() throws Exception {
        initializeS3Client();
        initializeTransferManager();
        createTempFile(10);
        files = Arrays.asList(tempFile);
    }

    @Test
    public void transferManagerWithSse_ValidatesEtagClientSide() throws Exception {
        MultipleFileUpload upload = tm.uploadFileList(bucketName, VIRTUAL_DIRECTORY, directory, files,
                                                      new ObjectMetadataProvider() {
                                                          @Override
                                                          public void provideObjectMetadata(File file, ObjectMetadata metadata) {
                                                              metadata.setSSEAlgorithm(SSEAlgorithm.AES256.getAlgorithm());
                                                          }
                                                      });
        upload.waitForCompletion();
    }

    @Test
    public void transferManagerWithSseKmsDefault_DoesNotValidateEtagClientSide() throws Exception {
        MultipleFileUpload upload = tm.uploadFileList(bucketName, VIRTUAL_DIRECTORY, directory, files,
                                                      new ObjectMetadataProvider() {
                                                          @Override
                                                          public void provideObjectMetadata(File file, ObjectMetadata metadata) {
                                                              metadata.setSSEAlgorithm(SSEAlgorithm.KMS.getAlgorithm());
                                                          }
                                                      });
        upload.waitForCompletion();
    }

    @Test
    public void transferManagerWithSseKmsNonDefault_DoesNotValidateEtagClientSide() throws Exception {
        MultipleFileUpload upload = tm.uploadFileList(bucketName, VIRTUAL_DIRECTORY, directory, files,
                                                      new ObjectMetadataProvider() {
                                                          @Override
                                                          public void provideObjectMetadata(File file, ObjectMetadata metadata) {
                                                              metadata.setSSEAlgorithm(SSEAlgorithm.KMS.getAlgorithm());
                                                              metadata.setHeader(Headers.SERVER_SIDE_ENCRYPTION_AWS_KMS_KEYID,
                                                                                 KmsTestKeyCache.getInstance(Regions.US_EAST_1, credentials).getNonDefaultKeyId());
                                                          }
                                                      });
        upload.waitForCompletion();
    }

    @Test
    public void transferManagerWithSseC_DoesNotValidateEtagClientSide() throws Exception {
        MultipleFileUpload upload = tm.uploadFileList(bucketName, VIRTUAL_DIRECTORY, directory, files,
                                                      new ObjectMetadataProvider() {
                                                          @Override
                                                          public void provideObjectMetadata(File file, ObjectMetadata metadata) {
                                                              metadata.setHeader(Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM,
                                                                                 SSEAlgorithm.AES256.getAlgorithm());
                                                              byte[] customerKey = CryptoTestUtils.getTestSecretKey().getEncoded();
                                                              metadata.setHeader(Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY,
                                                                                 CryptoTestUtils.encodeBase64String(customerKey));
                                                              metadata.setHeader(Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5,
                                                                                 Md5Utils.md5AsBase64(customerKey));
                                                          }
                                                      });
        upload.waitForCompletion();
    }

}
