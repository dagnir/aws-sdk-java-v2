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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.Constants.GB;
import static software.amazon.awssdk.services.s3.internal.Constants.MB;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.internal.AmazonS3TestClient;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyPartRequest;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.GetObjectMetadataRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.ObjectListing;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PartETag;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.services.s3.model.SseCustomerKey;
import software.amazon.awssdk.services.s3.transfer.Transfer.TransferState;
import software.amazon.awssdk.services.s3.transfer.model.CopyResult;
import software.amazon.awssdk.services.s3.transfer.model.UploadResult;
import software.amazon.awssdk.test.AwsTestBase;
import software.amazon.awssdk.test.util.RandomTempFile;

/**
 * Tests the copy operation functionality of the TransferManager. A mock S3
 * client is used for certain cases where copy request involves large files.
 */
@Category(S3Categories.Slow.class)
public class TransferManagerCopyIntegrationTest extends AwsTestBase {

    /** Source bucket name from where the Amazon S3 object is to be copied. */
    private static final String sourceBucketName = "java-sdk-src-bucket-tm-copy-"
                                                   + System.currentTimeMillis();

    /**
     * Name of the Amazon S3 object in the source bucket.
     */
    private static final String sourceKey =
            "java-sdk-src-key-tm-copy-" + System.currentTimeMillis();

    /** Name of the Amazon S3 encrypted object in the source bucket. */
    private static final String sourceKeyEncrypted = "java-sdk-src-Enckey-tm-copy-"
                                                     + System.currentTimeMillis();

    /** Destination bucket Name to where the Amazon S3 object in to be copied. */
    private static final String destinationBucketName = "java-sdk-dst-bucket-tm-copy-"
                                                        + System.currentTimeMillis();

    /**
     * Destination bucket Name to where the Amazon S3 object in to be copied.
     */
    private static final String fraSourceBucketName =
            "fra-java-sdk-dst-bucket-tm-copy-" + System.currentTimeMillis();

    /**
     * Name of the Amazon S3 object in the destination bucket.
     */
    private static final String destinationKey =
            "java-sdk-dst-key-tm-copy-" + System.currentTimeMillis();

    private static final String fraSourceKey =
            "fra-java-sdk-dst-key-tm-copy-" + System.currentTimeMillis();

    /**
     * Content length of the smaller file whose copy is carried out in a single
     * request.
     */
    private static final long contentLengthSingleChunkCopyFile = 200 * MB;

    /**
     * Content length of the larger file whose copy is carried out as multiple
     * part requests.
     */
    private static final long contentLengthMultipartCopyFile = 6 * GB;

    /** Transfer manager used for performing copy operation. */
    private static TransferManager tm = null;

    /** Amazon S3 client used for carrying out copy operations. */
    private static AmazonS3Client s3 = null;

    /** Encrypted Amazon S3 client used for uploading encrypted files. */
    private static AmazonS3 encS3 = null;

    private static AmazonS3Client fraS3;

    /** Mock Amazon S3 client used for carrying out large file copy operations. */
    private static AmazonS3 mockS3Client = null;

    /** Reference to the temporary file created in the file system. */
    private static File smallFile = null;

    private static String smallFileName = "java-sdk-tm-copy-smallFile-"
                                          + System.currentTimeMillis();

    private static SecretKey symmetricKey = null;

    @BeforeClass
    public static void setUp() throws AmazonServiceException,
                                      AmazonClientException, IOException, InterruptedException {

        setUpCredentials();
        generateSymmetricKeyForEncryptionClient();
        setupClients();
        initializeTransferManager();
        createTemporaryFiles();
        s3.createBucket(sourceBucketName);
        s3.createBucket(destinationBucketName);

        fraS3.createBucket(fraSourceBucketName);

        Upload upload = tm.upload(sourceBucketName, sourceKey, smallFile);
        UploadResult uploadResult = upload.waitForUploadResult();

        assertNotNull(uploadResult.getETag());
        encS3.putObject(sourceBucketName, sourceKeyEncrypted, smallFile);

    }

    @AfterClass
    public static void tearDown() throws AmazonServiceException,
                                         AmazonClientException {
        try {
            CryptoTestUtils.deleteBucketAndAllContents(s3, sourceBucketName);
            CryptoTestUtils.deleteBucketAndAllContents(s3, destinationBucketName);
            CryptoTestUtils.deleteBucketAndAllContents(fraS3, fraSourceBucketName);

            if (smallFile != null && smallFile.exists()) {
                smallFile.delete();
            }
        } catch (Exception e) {
            // Ignored or expected.
        }

    }

    private static void initializeTransferManager()
            throws FileNotFoundException, IllegalArgumentException, IOException {
        initializeTransferManager(50);
    }

    private static void initializeTransferManager(int threadPoolSize)
            throws FileNotFoundException, IllegalArgumentException, IOException {

        tm = new TransferManager(s3,
                                 (ThreadPoolExecutor) Executors
                                         .newFixedThreadPool(threadPoolSize));
        TransferManagerConfiguration configuration = new TransferManagerConfiguration();
        configuration.setMinimumUploadPartSize(10 * MB);
        configuration.setMultipartUploadThreshold(100 * MB);
        configuration.setMultipartCopyThreshold(20 * MB);
        tm.setConfiguration(configuration);
    }

    private static void createTemporaryFiles() throws IOException {
        smallFile = new RandomTempFile(smallFileName,
                                       contentLengthSingleChunkCopyFile);
    }

    private static void generateSymmetricKeyForEncryptionClient() {
        KeyGenerator generator;
        try {
            generator = KeyGenerator.getInstance("AES");
            generator.init(128, new SecureRandom());
            symmetricKey = generator.generateKey();
        } catch (Exception e) {
            fail("Unable to generate symmetric key: " + e.getMessage());
        }
    }

    private static void setupClients() throws FileNotFoundException,
                                              IllegalArgumentException, IOException {
        s3 = new AmazonS3Client(credentials);
        s3.configureRegion(Regions.US_EAST_1);
        mockS3Client = new AmazonS3MockClient(contentLengthMultipartCopyFile);

        encS3 = new AmazonS3EncryptionClient(credentials, new EncryptionMaterials(symmetricKey));

        fraS3 = new AmazonS3Client(credentials);
        fraS3.configureRegion(Regions.EU_CENTRAL_1);
    }

    private static void deleteContentsOfBucket(AmazonS3 client, String bucketName) {
        ObjectListing objectListing = client.listObjects(bucketName);

        while (true) {
            for (Iterator iterator = objectListing.getObjectSummaries()
                                                  .iterator(); iterator.hasNext(); ) {
                S3ObjectSummary objectSummary = (S3ObjectSummary) iterator
                        .next();
                client.deleteObject(bucketName, objectSummary.getKey());
            }

            if (objectListing.isTruncated()) {
                objectListing = client.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
    }

    protected static SecretKey generateSecretKey() {
        /*
         * TODO: Change this test to extend from TransferManagerTestBase so that
         *       we can pick up this function from there.
         *       I started that change, but the tests were failing because of
         *       some other duplicated setup/teardown functionality.
         */
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256, new SecureRandom());
            return generator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Unable to generate key", e);
        }
    }

    @After
    public void afterEveryTest() {
        deleteContentsOfBucket(s3, destinationBucketName);
        deleteContentsOfBucket(fraS3, fraSourceBucketName);
    }

    /**
     * Tests by copying a small file from source to destination. Checks the
     * content of source and destination to be same. Also checks the metadata of
     * the source and the destination.
     */
    @Test
    public void testCopySmallSizeFile() throws Exception {
        Copy result = tm.copy(new CopyObjectRequest(sourceBucketName,
                                                    sourceKey, destinationBucketName, destinationKey));
        result.waitForCopyResult();

        S3Object s3Object = s3.getObject(destinationBucketName, destinationKey);

        ObjectMetadata metadataSource = s3.getObjectMetadata(sourceBucketName,
                                                             sourceKey);
        ObjectMetadata metadataDestination = s3.getObjectMetadata(
                destinationBucketName, destinationKey);

        assertFileEqualsStream(smallFile, s3Object.getObjectContent());
        assertEquals(Long.valueOf(metadataSource.getContentLength()),
                     Long.valueOf(metadataDestination.getContentLength()));
        assertEquals(metadataSource.getContentMD5(),
                     metadataDestination.getContentMD5());
        assertEquals(metadataSource.getContentType(),
                     metadataDestination.getContentType());
        assertEquals(metadataSource.getContentEncoding(),
                     metadataDestination.getContentEncoding());
    }

    /**
     * Copy an object from eu-central-1 into us-east-1. When doing a cross region copy we must
     * provide a client for the source region so that we can get the object metadata.
     */
    @Test
    public void copyCrossRegion() {
        fraS3.putObject(fraSourceBucketName, fraSourceKey, smallFile);
        tm.copy(new CopyObjectRequest(fraSourceBucketName, fraSourceKey, sourceBucketName,
                                      "cross-region-copy-key-" + System.currentTimeMillis()), fraS3,
                null);
    }

    /**
     * Tests that we can use the multipart copy process to copy an object using
     * either source or destination customer provided server-side encryption
     * keys.
     */
    @Test
    public void testCopyObjectWithSSECustomerKey() throws Exception {
        SseCustomerKey sseCustomerKey = new SseCustomerKey(generateSecretKey());

        CopyObjectRequest copyObjectRequest =
                new CopyObjectRequest(sourceBucketName, sourceKey,
                                      destinationBucketName, destinationKey)
                        .withDestinationSseCustomerKey(sseCustomerKey);
        Copy result = tm.copy(copyObjectRequest);
        result.waitForCopyResult();

        GetObjectMetadataRequest getObjectMetadataRequest =
                new GetObjectMetadataRequest(destinationBucketName, destinationKey)
                        .withSSECustomerKey(sseCustomerKey);
        ObjectMetadata metadata = s3.getObjectMetadata(getObjectMetadataRequest);
        assertNotNull(metadata.getSSECustomerAlgorithm());

        // Now try copying that encrypted object to an unencrypted location
        copyObjectRequest =
                new CopyObjectRequest(destinationBucketName, destinationKey,
                                      destinationBucketName, destinationKey)
                        .withSourceSseCustomerKey(sseCustomerKey);
        result = tm.copy(copyObjectRequest);
        result.waitForCopyResult();

        getObjectMetadataRequest =
                new GetObjectMetadataRequest(destinationBucketName, destinationKey);
        metadata = s3.getObjectMetadata(getObjectMetadataRequest);
        assertNull(metadata.getSSECustomerAlgorithm());
    }

    /**
     * Tests by copying a large file from source to destination. Checks if the
     * result has important information like ETag, destination bucket name, key.
     *
     * Requests are not sent to the S3 server in this test case. A mock S3
     * client is used that stubs responses for every request initiated in this
     * test case.
     */
    @Test
    public void testCopyLargeSizeFile() throws Exception {
        TransferManager tmLarge = new TransferManager(mockS3Client);
        TransferManagerConfiguration configuration = new TransferManagerConfiguration();
        configuration.setMultipartCopyPartSize(1 * GB);

        tmLarge.setConfiguration(configuration);

        Copy copy = null;
        try {
            copy = tmLarge.copy(new CopyObjectRequest(sourceBucketName,
                                                      sourceKey, destinationBucketName, destinationKey));
        } catch (Exception e) {
            fail(e.getMessage());
        }

        CopyResult result = copy.waitForCopyResult();

        assertNotNull(result);
        assertNotNull(result.getDestinationBucketName());
        assertNotNull(result.getDestinationKey());
        assertNotNull(result.getETag());
        assertEquals(destinationBucketName, result.getDestinationBucketName());
        assertEquals(destinationKey, result.getDestinationKey());
    }

    /**
     * Tests by copying a encrypted file from source to destination. Checks the
     * content of source and destination to be same. Also checks the metadata of
     * the source and the destination.
     *
     * Requests are not sent to the S3 server in this test case. A mock S3
     * client is used that stubs responses for every request initiated in this
     * test case.
     */
    @Test
    public void testCopyingEncryptedFile() throws AmazonServiceException,
                                                  AmazonClientException, InterruptedException {
        Copy result = tm.copy(new CopyObjectRequest(sourceBucketName,
                                                    sourceKeyEncrypted, destinationBucketName, destinationKey));

        result.waitForCopyResult();

        S3Object s3Object = encS3.getObject(destinationBucketName,
                                            destinationKey);
        ObjectMetadata metadataSource = encS3.getObjectMetadata(
                sourceBucketName, sourceKeyEncrypted);
        ObjectMetadata metadataDestination = encS3.getObjectMetadata(
                destinationBucketName, destinationKey);

        assertFileEqualsStream(smallFile, s3Object.getObjectContent());
        assertEquals(Long.valueOf(metadataSource.getContentLength()),
                     Long.valueOf(metadataDestination.getContentLength()));
        assertEquals(metadataSource.getContentMD5(),
                     metadataDestination.getContentMD5());
        assertEquals(metadataSource.getContentType(),
                     metadataDestination.getContentType());
        assertEquals(metadataSource.getContentEncoding(),
                     metadataDestination.getContentEncoding());
    }

    @Test
    public void testCopyStateChange() throws AmazonServiceException,
                                             AmazonClientException, InterruptedException {
        Copy copy = tm.copy(new CopyObjectRequest(sourceBucketName, sourceKey,
                                                  destinationBucketName, destinationKey));

        assertEquals(copy.isDone(), false);
        copy.waitForCompletion();
        assertEquals(copy.getState(), TransferState.Completed);
        assertEquals(copy.isDone(), true);

    }

    /**
     * A mock implementation of the Amazon S3 client.This class stubs the
     * necessary methods that are initiated in a copy operation.
     *
     */
    static class AmazonS3MockClient extends AmazonS3TestClient {

        private String uploadId = null;
        private String bucketName = null;
        private String key = null;

        private Map<Integer, CopyPartRequest> requestList = new ConcurrentHashMap<Integer, CopyPartRequest>();
        private long contentLength;
        private List<String> eTags = new ArrayList<String>();

        public AmazonS3MockClient(long contentLength) {
            this.contentLength = contentLength;
        }

        @Override
        public ObjectMetadata getObjectMetadata(
                GetObjectMetadataRequest getObjectMetadataRequest)
                throws AmazonClientException, AmazonServiceException {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentLength);
            return metadata;
        }

        @Override
        public CopyPartResult copyPart(CopyPartRequest copyPartRequest) {

            if (!(copyPartRequest.getDestinationBucketName().equals(bucketName))) {
                throw new AmazonServiceException(
                        "Bucket name in copy part request doesn't match the bucket name in InitiateMultipartUploadRequest. Bucket Name received : "
                        + copyPartRequest.getDestinationBucketName()
                        + ". Bucket Name expected : " + bucketName);
            }

            if (!(copyPartRequest.getDestinationKey().equals(key))) {
                throw new AmazonServiceException(
                        "Key in copy part request doesn't match the Key in InitiateMultipartUploadRequest. Key received : "
                        + copyPartRequest.getDestinationKey()
                        + ". Key expected : " + key);
            }

            if (!(copyPartRequest.getUploadId().equals(uploadId))) {
                throw new AmazonServiceException(
                        "UploadId in copy part request doesn't match the UploadId in InitiateMultipartUploadResult. Upload Id received : "
                        + copyPartRequest.getUploadId()
                        + ". Upload Id expected : " + uploadId);
            }

            String eTag = "eTag-" + System.currentTimeMillis();
            CopyPartResult result = new CopyPartResult();
            result.setETag(eTag);
            result.setPartNumber(copyPartRequest.getPartNumber());

            requestList.put(copyPartRequest.getPartNumber(), copyPartRequest);
            eTags.add(eTag);

            return result;
        }

        @Override
        public void abortMultipartUpload(
                AbortMultipartUploadRequest abortMultipartUploadRequest)
                throws AmazonClientException, AmazonServiceException {
            requestList.clear();
            uploadId = null;
            bucketName = null;
            key = null;
            contentLength = 0;
            eTags.clear();
        }

        @Override
        public CompleteMultipartUploadResult completeMultipartUpload(
                CompleteMultipartUploadRequest completeMultipartUploadRequest)
                throws AmazonClientException, AmazonServiceException {

            int noOfParts = requestList.size();
            int i = 0;
            CopyPartRequest request = null;
            CompleteMultipartUploadResult result = null;
            long lastByteOffset = -1;
            List<PartETag> eTagsList = completeMultipartUploadRequest
                    .getPartETags();

            while (++i <= noOfParts) {
                request = requestList.get(i);

                if (request.getFirstByte() != lastByteOffset + 1) {
                    throw new AmazonServiceException(
                            "Problem with offset calculation. Parts are not contiguous. Part Num : "
                            + i);
                } else {
                    lastByteOffset = request.getLastByte();
                }
            }

            if (lastByteOffset + 1 != contentLength) {
                throw new AmazonServiceException(
                        "Copy Part Request for certain parts are not received. Original Content Length : "
                        + contentLength
                        + " and last part offset is :"
                        + lastByteOffset);
            }

            if (eTags.size() != eTagsList.size()) {
                throw new AmazonServiceException(
                        "ETags Missing. No of ETags In Server : "
                        + eTags.size()
                        + ". No of ETags received from client : "
                        + eTagsList.size());
            }

            for (PartETag eTag : eTagsList) {
                if (!(eTags.contains(eTag.getETag()))) {
                    throw new AmazonServiceException(
                            "Additional ETag's Present. ETag : "
                            + eTag.getETag());
                }
            }

            result = new CompleteMultipartUploadResult();
            result.setBucketName(completeMultipartUploadRequest.getBucketName());
            result.setKey(completeMultipartUploadRequest.getKey());
            result.setETag("eTag-" + System.currentTimeMillis());

            return result;
        }

        @Override
        public InitiateMultipartUploadResult initiateMultipartUpload(
                InitiateMultipartUploadRequest initiateMultipartUploadRequest)
                throws AmazonClientException, AmazonServiceException {
            uploadId = "uploadId-" + System.currentTimeMillis();
            bucketName = initiateMultipartUploadRequest.getBucketName();
            key = initiateMultipartUploadRequest.getKey();

            InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();
            result.setBucketName(bucketName);
            result.setKey(key);
            result.setUploadId(uploadId);

            return result;
        }
    }
}
