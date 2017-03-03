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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import org.junit.Test;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.internal.Constants;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.SseCustomerKey;
import software.amazon.awssdk.services.s3.transfer.internal.TransferManagerUtils;

public class TransferManagerUtilsTest {

    private static final String BUCKET_NAME = "testBucketName";

    private static final String KEY = "key";

    private static AmazonS3 s3 = new AmazonS3Client();

    /**
     * Tests that we correctly calculate an upload's part size, and don't return
     * a part size that would result in breaking the upload into more than the
     * maximum allowed number of individual parts.
     */
    @Test
    public void testCalculateOptimalPartSize() throws Exception {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(120 * Constants.GB);
        PutObjectRequest request = new PutObjectRequest("bucketName", "key", new ByteArrayInputStream("foo".getBytes()),
                                                        metadata);

        long partSize = TransferManagerUtils.calculateOptimalPartSize(request, new TransferManagerConfiguration());
        double totalParts = (double) metadata.getContentLength() / (double) partSize;

        assertTrue(totalParts <= Constants.MAXIMUM_UPLOAD_PARTS);
    }

    /**
     * Tests isDownloadParallelizable when s3 client is null, an
     * IllegalArgumentException is returned.
     */
    @Test(expected = IllegalArgumentException.class)
    public void isDownloadParallelizableWithNullS3ClientThrowsAmazonClientException() {
        GetObjectRequest getObjectRequest = new GetObjectRequest(BUCKET_NAME, KEY);
        TransferManagerUtils.isDownloadParallelizable(null, getObjectRequest, 1);
    }

    /**
     * Tests isDownloadParallelizable when GetObjectRequest is null, an
     * IllegalArgumentException is returned.
     */
    @Test(expected = IllegalArgumentException.class)
    public void isDownloadParallelizableWithNullRequestThrowsAmazonClientException() {
        GetObjectRequest getObjectRequest = null;
        TransferManagerUtils.isDownloadParallelizable(s3, getObjectRequest, 1);
    }

    /**
     * Tests isDownloadParallelizable when s3 client is an instance of
     * AmazonS3Encryption, returns false.
     */
    @Test
    public void isDownloadParallelizableGivenEncryptedS3ClientReturnsFalse() {
        AmazonS3 s3EncryptionClient = new AmazonS3EncryptionClient(new EncryptionMaterials(new KeyPair(null, null)));
        GetObjectRequest getObjectRequest = new GetObjectRequest(BUCKET_NAME, KEY);
        assertTrue(!TransferManagerUtils.isDownloadParallelizable(s3EncryptionClient, getObjectRequest, 1));
    }

    /**
     * Tests isDownloadParallelizable when customer provides a server-side
     * encryption key in GetObjectRequest, returns true.
     */
    @Test
    public void isDownloadParallelizableGivenSSECustomerkeyInRequestReturnsFalse() {
        GetObjectRequest getObjectRequest = new GetObjectRequest(BUCKET_NAME, KEY)
                .withSSECustomerKey(new SseCustomerKey(" "));
        assertTrue(TransferManagerUtils.isDownloadParallelizable(s3, getObjectRequest, 1));
    }

    /**
     * Tests isDownloadParallelizable when range is set in GetObjectRequest,
     * returns false.
     */
    @Test
    public void isDownloadParallelizableGivenRangeInRequestReturnsFalse() {
        GetObjectRequest getObjectRequest = new GetObjectRequest(BUCKET_NAME, KEY).withRange(20);
        assertTrue(!TransferManagerUtils.isDownloadParallelizable(s3, getObjectRequest, 1));
    }

    /**
     * Tests isDownloadParallelizable when part count parameter is null, returns
     * false.
     */
    @Test
    public void isDownloadParallelizableGivenNullPartCountReturnsFalse() {
        GetObjectRequest getObjectRequest = new GetObjectRequest(BUCKET_NAME, KEY).withRange(20);
        assertTrue(!TransferManagerUtils.isDownloadParallelizable(s3, getObjectRequest, null));
    }

    /**
     * Tests isDownloadParallelizable when part count parameter is null, returns
     * false.
     */
    @Test
    public void isDownloadParallelizableGivenPartNumberReturnsFalse() {
        GetObjectRequest getObjectRequest = new GetObjectRequest(BUCKET_NAME, KEY).withPartNumber(1);
        assertTrue(!TransferManagerUtils.isDownloadParallelizable(s3, getObjectRequest, null));
    }

    /**
     * Tests isDownloadParallelizable when s3 not instance of
     * AmazonS3Encryption and range/partNumber not set in
     * GetObjectRequest and part count parameter is not null, then test returns
     * true.
     */
    @Test
    public void isDownloadParallelizableGivenArgumentsSatisfyingParallelDownloadConditionsReturnsTrue() {
        GetObjectRequest getObjectRequest = new GetObjectRequest(BUCKET_NAME, KEY);
        assertTrue(TransferManagerUtils.isDownloadParallelizable(s3, getObjectRequest, 4));
    }
}
