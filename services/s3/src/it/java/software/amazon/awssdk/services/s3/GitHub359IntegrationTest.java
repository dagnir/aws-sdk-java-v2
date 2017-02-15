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

package software.amazon.awssdk.services.s3;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.PartETag;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

// https://github.com/aws/aws-sdk-java/issues/359
public class GitHub359IntegrationTest extends S3IntegrationTestBase {
    private String bucketName = CryptoTestUtils.tempBucketName(GitHub359IntegrationTest.class);
    /** The key of the object these tests will create, test on and delete */
    private String key = "key";

    @Before
    public void setup() {
        s3.createBucket(bucketName);
    }

    @After
    public void tearDown() {
        try {
            CryptoTestUtils.deleteBucketAndAllContents(s3, bucketName);
        } catch (Exception ex) {
            LogFactory.getLog(getClass()).warn("", ex);
        }
    }

    @Test(expected = AmazonClientException.class)
    public void test() throws Exception {
        byte[] data = new byte[122880000];
        InputStream inputStream = new ByteArrayInputStream(data);
        long partSize = 10 * 1024 * 1024;

        List<PartETag> partETags = new ArrayList<PartETag>();

        // Step 1: Initialize.
        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, key);

        InitiateMultipartUploadResult initResponse =
                s3.initiateMultipartUpload(initRequest);


        try {
            // Step 2: Upload parts.
            long filePosition = 0;
            for (int i = 1; filePosition < data.length; i++) {
                // Last part can be less than partSize. Adjust part size.
                partSize = Math.min(partSize, (data.length - filePosition));

                // Create request to upload a part.
                UploadPartRequest uploadRequest = new UploadPartRequest()
                        .withBucketName(bucketName).withKey(key)
                        .withUploadId(initResponse.getUploadId()).withPartNumber(i)
                        .withFileOffset(filePosition)
                        .withInputStream(inputStream)
                        .withPartSize(partSize);

                // Upload part and add response to our list.
                partETags.add(s3.uploadPart(uploadRequest).getPartETag());
                System.out.println("Uploaded part " + i);
                filePosition += partSize;
            }

            // Step 3: Complete.
            CompleteMultipartUploadRequest compRequest = new
                    CompleteMultipartUploadRequest(bucketName,
                                                   key,
                                                   initResponse.getUploadId(),
                                                   partETags);

            s3.completeMultipartUpload(compRequest);
        } catch (Exception e) {
            LogFactory.getLog(getClass()).debug("", e);
            Assert.assertTrue(e.getMessage().startsWith("Unable to position the currentPosition"));
            s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, key, initResponse.getUploadId()));
            throw e;
        }
        System.out.println("Written " + data.length);
    }
}
