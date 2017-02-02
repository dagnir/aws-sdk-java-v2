/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.awssdk.services.s3;

import static software.amazon.awssdk.services.s3.internal.Constants.MB;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.Request;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.SSEAwsKeyManagementParams;
import software.amazon.awssdk.test.util.RandomTempFile;
import software.amazon.awssdk.util.AWSRequestMetrics;

public class SSEAWSKMSSigV4IntegrationTest extends S3IntegrationTestBase {

    /**
     * Bucket name in the standard region.
     */
    private static final String BUCKET_NAME = "java-sdk-kms-put-"
            + System.currentTimeMillis();

    /**
     * Bucket name in US_WEST region.
     */
    private static final String BUCKET_NAME_US_WEST = "java-sdk-kms-us-west-put-"
            + System.currentTimeMillis();

    private static final String FILE_NAME = "sdk-kms-put-file-"
            + System.currentTimeMillis();

    /**
     * Content Length of the file used for upload.
     */
    private static final long CONTENT_LENGTH = 2 * MB;

    /**
     * Reference to the file uploaded for testing.
     */
    private static File fileToUpload = null;

    /**
     * Name of the Amazon S3 Object.
     */
    private static String KEY = "key";

    /**
     * Client for the s3USWest region.
     */
    private static AmazonS3Client s3USWest;

    /**
     * Metric collector that collects the RequestCount metric.
     */
    private final RequestCountMetricCollector requestCountCollector = new RequestCountMetricCollector();

    private static final Log LOG = LogFactory.getLog(SSEAWSKMSSigV4IntegrationTest.class);

    @BeforeClass
    public static void setUp() throws Exception {
        S3IntegrationTestBase.setUp();
        s3.createBucket(BUCKET_NAME);
        fileToUpload = new RandomTempFile(FILE_NAME, CONTENT_LENGTH);
        s3USWest = new AmazonS3Client(credentials);
        s3USWest.configureRegion(Regions.US_WEST_1);
        s3USWest.createBucket(BUCKET_NAME_US_WEST);
    }

    @AfterClass
    public static void tearDown() {
        try {
            CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET_NAME);
            CryptoTestUtils.deleteBucketAndAllContents(s3USWest,
                    BUCKET_NAME_US_WEST);
            if (fileToUpload != null)
                fileToUpload.delete();
        } catch (Exception e) {
            LOG.error(
                    "Error in cleaning up the resources after test completion.",
                    e);
        }
    }

    /**
     * This test uses the standard endpoint(s3.amazonaws.com) to upload a Amazon
     * S3 object with KMS encryption. Since we don't know the region from the request,
     * we use HeadBucket/BucketRegionCache to find the region and
     * use it to create a SigV4 signer for signing.
     */
    @Test
    public void testClassicEndpointWithNoExplicitRegionSet() {
        final AmazonS3Client s3Client = new AmazonS3Client(credentials);

        PutObjectRequest request = new PutObjectRequest(BUCKET_NAME, KEY,
                fileToUpload).withSSEAwsKeyManagementParams(
                new SSEAwsKeyManagementParams()).withRequestMetricCollector(
                requestCountCollector);
        s3Client.putObject(request);
        assertTrue(requestCountCollector.getRequestCount() == 1);
    }

    /**
     * This test uses the standard endpoint(s3.amazonaws.com) however with an
     * explicit region set to US_EAST_1. We will not know what region to sign
     * even in this case as the endpoint doesn't have information to compute
     * region. This request will be signed with SigV2. We receive an KMS auth
     * error and retry with SigV4.
     */
    @Test
    public void testClassicEndpointWithExplicitRegionSet() {
        // us-east-1
        final AmazonS3Client s3Client = new AmazonS3Client(credentials);
        s3Client.configureRegion(Regions.US_EAST_1);

        PutObjectRequest request = new PutObjectRequest(BUCKET_NAME_US_WEST,
                KEY, fileToUpload).withSSEAwsKeyManagementParams(
                new SSEAwsKeyManagementParams()).withRequestMetricCollector(
                requestCountCollector);
        s3Client.putObject(request);
        assertTrue(requestCountCollector.getRequestCount() > 1);
    }

    /**
     * This test uses the region specific endpoint. We will know what region to
     * sign even from the endpoint. This request will be signed using SigV4.
     */
    @Test
    public void testNonClassicEndpoint() {
        final AmazonS3Client s3Client = new AmazonS3Client(credentials);
        s3Client.configureRegion(Regions.US_WEST_1);

        PutObjectRequest request = new PutObjectRequest(BUCKET_NAME_US_WEST,
                KEY, fileToUpload).withSSEAwsKeyManagementParams(
                new SSEAwsKeyManagementParams()).withRequestMetricCollector(
                requestCountCollector);
        s3Client.putObject(request);
        assertTrue(requestCountCollector.getRequestCount() == 1);
    }

    static class RequestCountMetricCollector extends RequestMetricCollector {

        private int requestCount;

        @Override
        public void collectMetrics(Request<?> request, Response<?> response) {

            requestCount = request.getAWSRequestMetrics().getTimingInfo()
                    .getCounter(AWSRequestMetrics.Field.RequestCount.name())
                    .intValue();
        }

        public int getRequestCount() {
            return requestCount;
        }
    }
}
