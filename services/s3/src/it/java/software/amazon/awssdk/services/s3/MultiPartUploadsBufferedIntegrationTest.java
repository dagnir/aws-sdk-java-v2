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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.ResetException;
import software.amazon.awssdk.SDKGlobalConfiguration;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.internal.Constants;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AccessControlList;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.BucketVersioningConfiguration;
import software.amazon.awssdk.services.s3.model.CannedAccessControlList;
import software.amazon.awssdk.services.s3.model.CanonicalGrantee;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.EmailAddressGrantee;
import software.amazon.awssdk.services.s3.model.Grant;
import software.amazon.awssdk.services.s3.model.GroupGrantee;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.MultipartUpload;
import software.amazon.awssdk.services.s3.model.MultipartUploadListing;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PartETag;
import software.amazon.awssdk.services.s3.model.PartListing;
import software.amazon.awssdk.services.s3.model.PartSummary;
import software.amazon.awssdk.services.s3.model.Permission;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.SetBucketVersioningConfigurationRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResult;
import software.amazon.awssdk.services.s3.transfer.TransferManager;
import software.amazon.awssdk.test.util.RandomInputStream;
import software.amazon.awssdk.test.util.UnreliableRandomInputStream;

/** Integration tests for the low level Multipart operations but using BufferedInputStream. */
@Category(S3Categories.Slow.class)
public class MultiPartUploadsBufferedIntegrationTest extends S3IntegrationTestBase {

    private static final long CONTENT_LENGTH = 10 * Constants.MB;
    private final String bucketName = "java-sdk-mp-upload-" + System.currentTimeMillis();
    private final String RIDIRECT_LOCATION = "/redirecting...";
    private String uploadId;

    /** Releases all resources created in these tests */
    @After
    public void tearDown() throws Exception {
        // Clear the system property by setting to blank
        System.clearProperty(SDKGlobalConfiguration.DEFAULT_S3_STREAM_BUFFER_SIZE);
        try {
            new TransferManager(s3).abortMultipartUploads(bucketName, new Date());
        } catch (Exception e) {
        }

        try {
            deleteBucketAndAllContents(bucketName);
        } catch (Exception e) {
        }
    }

    /** Tests that a multipart upload can be created, listed and aborted. */
    @Test
    public void testAborted() throws Exception {
        s3.createBucket(bucketName);
        waitForBucketCreation(bucketName);
        uploadId = initiateMultipartRequest(bucketName, "key");
        uploadParts(bucketName, uploadId);

        listMultipartUploads(bucketName);
        listMultipartUploads(bucketName, uploadId);
        listParts(bucketName, uploadId);

        abortMultipartUpload(bucketName, uploadId);
    }

    @Test(expected = ResetException.class)
    public void testResetFailure() throws Exception {
        s3.createBucket(bucketName);
        waitForBucketCreation(bucketName);
        uploadId = initiateMultipartRequest(bucketName, "key");
        // The content length is much bigger than the default reset buffer size
        s3.uploadPart(new UploadPartRequest()
                              .withBucketName(bucketName)
                              .withInputStream(new BufferedInputStream(new UnreliableRandomInputStream(CONTENT_LENGTH)))
                              .withKey("key")
                              .withPartNumber(1)
                              .withPartSize(CONTENT_LENGTH)
                              .withUploadId(uploadId));
    }

    /** Tests that a multipart upload can be created, listed and completed. */
    @Test
    public void testCompleted() throws Exception {
        s3.createBucket(bucketName);
        enableVersioning(bucketName);
        waitForBucketCreation(bucketName);

        AccessControlList acl = new AccessControlList();
        for (Permission permission : Permission.values()) {
            acl.grantPermission(new CanonicalGrantee(AWS_DR_ECLIPSE_ACCT_ID), permission);
            acl.grantPermission(GroupGrantee.AuthenticatedUsers, permission);
            acl.grantPermission(new EmailAddressGrantee(AWS_DR_TOOLS_EMAIL_ADDRESS), permission);
        }

        InitiateMultipartUploadResult initiateResult = s3.initiateMultipartUpload(new InitiateMultipartUploadRequest(
                bucketName, "key").withAccessControlList(acl).withStorageClass(StorageClass.ReducedRedundancy).withRedirectLocation(RIDIRECT_LOCATION));

        assertEquals(bucketName, initiateResult.getBucketName());
        assertEquals("key", initiateResult.getKey());
        assertNotEmpty(initiateResult.getUploadId());

        uploadId = initiateResult.getUploadId();

        List<PartETag> partETags = uploadParts(bucketName, uploadId);

        // Initiate two more uploads so we can test prefix/delimiter
        initiateMultipartRequest(bucketName, "key/foo/bar");
        initiateMultipartRequest(bucketName, "key/bar/baz");

        listMultipartUploads(bucketName);
        listMultipartUploads(bucketName, uploadId);
        listMultipartUploads(bucketName, "key/", "/");
        listParts(bucketName, uploadId);

        CompleteMultipartUploadResult completeMultipartUploadResult = s3.completeMultipartUpload(
                new CompleteMultipartUploadRequest(bucketName, "key", uploadId, partETags));
        assertNotEmpty(completeMultipartUploadResult.getBucketName());
        assertNotEmpty(completeMultipartUploadResult.getKey());
        assertNotEmpty(completeMultipartUploadResult.getETag());
        assertNotEmpty(completeMultipartUploadResult.getLocation());
        assertNotEmpty(completeMultipartUploadResult.getVersionId());

        AccessControlList aclRead = s3.getObjectAcl(bucketName, "key");
        assertEquals(15, aclRead.getGrantsAsList().size());

        Set<Grant> expectedGrants = translateEmailAclsIntoCanonical(acl);

        for (Grant expected : expectedGrants) {
            assertTrue("Didn't find expectd grant " + expected, aclRead.getGrantsAsList().contains(expected));
        }

        // Check we can get the redirect location back.
        S3Object object = s3.getObject(bucketName, "key");
        assertEquals(RIDIRECT_LOCATION, object.getRedirectLocation());
        assertEquals(bucketName, object.getBucketName());
        assertEquals("key", object.getKey());

        disableVersioning(bucketName);
    }

    /** Tests server-side encryption */
    @Test
    public void testServerSideEncryption() throws Exception {
        s3.createBucket(bucketName);
        enableVersioning(bucketName);
        waitForBucketCreation(bucketName);
        InitiateMultipartUploadRequest initiateRequest = new InitiateMultipartUploadRequest(bucketName, "key")
                .withCannedACL(CannedAccessControlList.PublicRead).withStorageClass(StorageClass.ReducedRedundancy);
        initiateRequest.setObjectMetadata(new ObjectMetadata());
        initiateRequest.getObjectMetadata().setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
        InitiateMultipartUploadResult initiateResult = s3.initiateMultipartUpload(initiateRequest);

        assertEquals(bucketName, initiateResult.getBucketName());
        assertEquals("key", initiateResult.getKey());
        assertNotEmpty(initiateResult.getUploadId());

        uploadId = initiateResult.getUploadId();
        assertEquals("AES256", initiateResult.getSSEAlgorithm());
        List<PartETag> partETags1 = new ArrayList<PartETag>();
        System.setProperty(
                SDKGlobalConfiguration.DEFAULT_S3_STREAM_BUFFER_SIZE,
                String.valueOf(CONTENT_LENGTH + 1));
        UploadPartResult uploadPartResult = s3.uploadPart(new UploadPartRequest()
                                                                  .withBucketName(bucketName)
                                                                  .withInputStream(new BufferedInputStream(new UnreliableRandomInputStream(CONTENT_LENGTH)))
                                                                  .withKey("key")
                                                                  .withPartNumber(1)
                                                                  .withPartSize(CONTENT_LENGTH)
                                                                  .withUploadId(uploadId));
        assertEquals(1, uploadPartResult.getPartNumber());
        assertNotEmpty(uploadPartResult.getETag());
        assertEquals("AES256", uploadPartResult.getSSEAlgorithm());
        partETags1.add(new PartETag(uploadPartResult.getPartNumber(), uploadPartResult.getETag()));

        uploadPartResult = s3.uploadPart(new UploadPartRequest()
                                                 .withBucketName(bucketName)
                                                 .withInputStream(new BufferedInputStream(new RandomInputStream(CONTENT_LENGTH)))
                                                 .withKey("key")
                                                 .withPartNumber(2)
                                                 .withPartSize(CONTENT_LENGTH)
                                                 .withUploadId(uploadId));
        assertEquals(2, uploadPartResult.getPartNumber());
        assertNotEmpty(uploadPartResult.getETag());
        assertEquals("AES256", uploadPartResult.getSSEAlgorithm());
        partETags1.add(new PartETag(uploadPartResult.getPartNumber(), uploadPartResult.getETag()));

        List<PartETag> partETags = partETags1;

        listMultipartUploads(bucketName, uploadId);

        CompleteMultipartUploadResult completeMultipartUploadResult = s3.completeMultipartUpload(
                new CompleteMultipartUploadRequest(bucketName, "key", uploadId, partETags));

        assertNotEmpty(completeMultipartUploadResult.getBucketName());
        assertNotEmpty(completeMultipartUploadResult.getKey());
        assertNotEmpty(completeMultipartUploadResult.getETag());
        assertNotEmpty(completeMultipartUploadResult.getLocation());
        assertNotEmpty(completeMultipartUploadResult.getVersionId());
        assertEquals("AES256", completeMultipartUploadResult.getSSEAlgorithm());

        disableVersioning(bucketName);
    }

    /** Test error handling during CompleteMultipartUpload. */
    @Test
    public void testCompletionFailed() throws Exception {
        s3.createBucket(bucketName);
        waitForBucketCreation(bucketName);
        uploadId = initiateMultipartRequest(bucketName, "key");
        List<PartETag> partETags = uploadParts(bucketName, uploadId);

        listMultipartUploads(bucketName);
        listMultipartUploads(bucketName, uploadId);
        listParts(bucketName, uploadId);

        try {
            // fudge up an ETag to trigger an error response
            (partETags.get(1)).setETag((partETags.get(0)).getETag());
            s3.completeMultipartUpload(new CompleteMultipartUploadRequest(
                    bucketName, "key", uploadId, partETags));
            fail("Expected an AmazonS3Exception");
        } catch (AmazonS3Exception ase) {
            assertNotEmpty(ase.getErrorCode());
            assertNotEmpty(ase.getRequestId());
            assertNotEmpty(ase.getExtendedRequestId());
            assertNotEmpty(ase.getMessage());
        }
    }

    /** Tests error handling during UploadPart. */
    @Test
    public void testUploadError() throws Exception {
        s3.createBucket(bucketName);
        waitForBucketCreation(bucketName);
        uploadId = initiateMultipartRequest(bucketName, "key");
        uploadParts(bucketName, uploadId);
        System.setProperty(
                SDKGlobalConfiguration.DEFAULT_S3_STREAM_BUFFER_SIZE,
                String.valueOf(CONTENT_LENGTH + 1));
        try {
            s3.uploadPart(new UploadPartRequest()
                                  .withBucketName(bucketName)
                                  .withInputStream(new BufferedInputStream(new RandomInputStream(CONTENT_LENGTH)))
                                  .withKey("key")
                                  .withPartNumber(1)
                                  .withPartSize(CONTENT_LENGTH)
                                  .withMD5Digest("thisisn'tarealmd5")
                                  .withUploadId(uploadId));
            fail("Expected an AmazonServiceException");
        } catch (AmazonS3Exception ase) {
            assertNotEmpty(ase.getErrorCode());
            assertNotEmpty(ase.getRequestId());
            assertNotEmpty(ase.getExtendedRequestId());
            assertNotEmpty(ase.getMessage());
        }
    }


    /*
     * Private Helper Methods
     */

    private void enableVersioning(String bucketName) {
        s3.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(
                bucketName, new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED)));
    }

    private void disableVersioning(String bucketName) {
        s3.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(
                bucketName, new BucketVersioningConfiguration(BucketVersioningConfiguration.SUSPENDED)));
    }

    private void listParts(String bucketName, String uploadId) {
        PartListing listPartsResult = s3.listParts(new ListPartsRequest(bucketName, "key", uploadId)
                                                           .withMaxParts(100)
                                                           .withPartNumberMarker(new Integer(0))
                                                           .withEncodingType("url"));
        assertEquals(bucketName, listPartsResult.getBucketName());
        assertEquals("key", listPartsResult.getKey());
        assertEquals(100, listPartsResult.getMaxParts().intValue());
        assertEquals(0, listPartsResult.getPartNumberMarker().intValue());
        assertNotNull(listPartsResult.getNextPartNumberMarker());
        assertEquals(0, listPartsResult.getPartNumberMarker().intValue());
        assertEquals(StorageClass.ReducedRedundancy.toString(), listPartsResult.getStorageClass());
        assertEquals(uploadId, listPartsResult.getUploadId());
        assertTrue(listPartsResult.getParts().size() > 0);
        PartSummary part = listPartsResult.getParts().get(0);
        assertNotEmpty(part.getETag());
        assertNotNull(part.getLastModified());
        assertTrue(part.getPartNumber() > 0);
        assertEquals(CONTENT_LENGTH, part.getSize());

        assertNotNull(listPartsResult.getOwner());
        assertNotEmpty(listPartsResult.getOwner().getDisplayName());
        assertNotEmpty(listPartsResult.getOwner().getId());

        assertNotNull(listPartsResult.getInitiator());
        assertNotEmpty(listPartsResult.getInitiator().getDisplayName());
        assertNotEmpty(listPartsResult.getInitiator().getId());
        assertEquals("url", listPartsResult.getEncodingType());
    }

    private void listMultipartUploads(String bucketName, String prefix, String delimiter) {
        MultipartUploadListing listMultipartUploadsResult = s3.listMultipartUploads(
                new ListMultipartUploadsRequest(bucketName)
                        .withPrefix(prefix)
                        .withDelimiter(delimiter)
                        .withMaxUploads(100));
        assertEquals(bucketName, listMultipartUploadsResult.getBucketName());
        assertEquals(null, listMultipartUploadsResult.getKeyMarker());
        assertEquals(100, listMultipartUploadsResult.getMaxUploads());
        assertEquals(prefix, listMultipartUploadsResult.getPrefix());
        assertEquals(delimiter, listMultipartUploadsResult.getDelimiter());
        assertNull(listMultipartUploadsResult.getEncodingType());

        // assert that we have the two common prefixes we expect
        List<String> commonPrefixes = listMultipartUploadsResult.getCommonPrefixes();
        assertEquals(2, commonPrefixes.size());
        for (String commonPrefix : commonPrefixes) {
            assertNotEmpty(commonPrefix);
        }
    }

    private void listMultipartUploads(String bucketName, String uploadId) {
        // Test all the request parameters for ListMultipartUploads
        MultipartUploadListing listMultipartUploadsResult = s3.listMultipartUploads(
                new ListMultipartUploadsRequest(bucketName)
                        .withKeyMarker("key")
                        .withMaxUploads(100)
                        .withUploadIdMarker(uploadId));
        assertEquals(bucketName, listMultipartUploadsResult.getBucketName());
        assertEquals("key", listMultipartUploadsResult.getKeyMarker());
        assertEquals(100, listMultipartUploadsResult.getMaxUploads());
        assertEquals(uploadId, listMultipartUploadsResult.getUploadIdMarker());
        assertNull(listMultipartUploadsResult.getEncodingType());
    }

    private void listMultipartUploads(String bucketName) {
        // Now test some multipart upload data
        MultipartUploadListing listMultipartUploadsResult = s3.listMultipartUploads(
                new ListMultipartUploadsRequest(bucketName)
                        .withEncodingType("url"));
        assertEquals(bucketName, listMultipartUploadsResult.getBucketName());
        assertNotNull(listMultipartUploadsResult.getNextKeyMarker());
        assertNotNull(listMultipartUploadsResult.getNextUploadIdMarker());
        assertTrue(listMultipartUploadsResult.getMultipartUploads().size() > 0);
        MultipartUpload multiPartUpload = listMultipartUploadsResult.getMultipartUploads().get(0);
        assertNotNull(multiPartUpload.getInitiated());
        assertNotEmpty(multiPartUpload.getKey());
        assertNotEmpty(multiPartUpload.getStorageClass());
        assertNotEmpty(multiPartUpload.getUploadId());

        assertNotNull(multiPartUpload.getOwner());
        assertNotEmpty(multiPartUpload.getOwner().getDisplayName());
        assertNotEmpty(multiPartUpload.getOwner().getId());

        assertNotNull(multiPartUpload.getInitiator());
        assertNotEmpty(multiPartUpload.getInitiator().getDisplayName());
        assertNotEmpty(multiPartUpload.getInitiator().getId());

        // EncodingType parameter should be returned in the response
        assertEquals("url", listMultipartUploadsResult.getEncodingType());
    }

    private String initiateMultipartRequest(String bucketName, String key) {
        InitiateMultipartUploadResult initiateResult = s3.initiateMultipartUpload(
                new InitiateMultipartUploadRequest(bucketName, key)
                        .withCannedACL(CannedAccessControlList.PublicRead)
                        .withStorageClass(StorageClass.ReducedRedundancy));

        assertEquals(bucketName, initiateResult.getBucketName());
        assertEquals(key, initiateResult.getKey());
        assertNotEmpty(initiateResult.getUploadId());

        return initiateResult.getUploadId();
    }

    private List<PartETag> uploadParts(String bucketName, String uploadId) throws InterruptedException {
        List<PartETag> partETags = new ArrayList<PartETag>();
        System.setProperty(
                SDKGlobalConfiguration.DEFAULT_S3_STREAM_BUFFER_SIZE,
                String.valueOf(CONTENT_LENGTH + 1));
        UploadPartResult uploadPartResult = s3.uploadPart(new UploadPartRequest()
                                                                  .withBucketName(bucketName)
                                                                  .withInputStream(new BufferedInputStream(new RandomInputStream(CONTENT_LENGTH)))
                                                                  .withKey("key")
                                                                  .withPartNumber(1)
                                                                  .withPartSize(CONTENT_LENGTH)
                                                                  .withUploadId(uploadId));
        assertEquals(1, uploadPartResult.getPartNumber());
        assertNotEmpty(uploadPartResult.getETag());
        partETags.add(new PartETag(uploadPartResult.getPartNumber(), uploadPartResult.getETag()));

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setHeader("testing_upload_part_header", "testing_123");
        uploadPartResult = s3.uploadPart(new UploadPartRequest()
                                                 .withBucketName(bucketName)
                                                 .withInputStream(new BufferedInputStream(new UnreliableRandomInputStream(CONTENT_LENGTH)))
                                                 .withKey("key")
                                                 .withPartNumber(2)
                                                 .withPartSize(CONTENT_LENGTH)
                                                 .withUploadId(uploadId)
                                                 .withObjectMetadata(objectMetadata)
                                        );
        assertEquals(2, uploadPartResult.getPartNumber());
        assertNotEmpty(uploadPartResult.getETag());
        partETags.add(new PartETag(uploadPartResult.getPartNumber(), uploadPartResult.getETag()));

        return partETags;
    }

    private void abortMultipartUpload(String bucketName, String uploadId) {
        s3.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, "key", uploadId));
    }

}
