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

package software.amazon.awssdk.services.cloudfront;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import org.junit.BeforeClass;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionRequest;
import software.amazon.awssdk.services.cloudfront.model.GetDistributionResult;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.model.ObjectListing;
import software.amazon.awssdk.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.test.AwsTestBase;

/**
 * Base class for CloudFront integration tests.
 */
public abstract class IntegrationTestBase extends AwsTestBase {

    /** Shared CloudFront client for all tests to use. */
    protected static CloudFrontClient cloudfront;

    /** Shared S3 client for all tests to use. */
    protected static AmazonS3Client s3;

    /**
     * Loads the AWS account info for the integration tests and creates an
     * AutoScaling client for tests to use.
     */
    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        cloudfront = CloudFrontClient.builder().withCredentials(new AwsStaticCredentialsProvider(credentials)).build();
        s3 = new AmazonS3Client(credentials);
    }

    /**
     * Polls the test distribution until it moves into the "Deployed" state, or
     * throws an exception and gives up after waiting too long.
     *
     * @param distributionId
     *            The distribution to delete
     */
    protected static void waitForDistributionToDeploy(String distributionId) throws Exception {
        int timeoutInMinutes = 20;
        long startTime = System.currentTimeMillis();
        while (true) {
            GetDistributionResult getDistributionResult = cloudfront.getDistribution(new GetDistributionRequest()
                                                                                             .withId(distributionId));
            String status = getDistributionResult.getDistribution().getStatus();
            System.out.println(status);
            if (status.equalsIgnoreCase("Deployed")) {
                return;
            }

            if ((System.currentTimeMillis() - startTime) > (1000 * 60 * timeoutInMinutes)) {
                throw new RuntimeException("Waited " + timeoutInMinutes
                                           + " minutes for distribution to be deployed, but never happened");
            }

            Thread.sleep(1000 * 20);
        }
    }

    /**
     * Deletes all objects in the specified bucket, and then deletes the bucket.
     *
     * @param bucketName
     *            The bucket to empty and delete.
     */
    protected static void deleteBucketAndAllContents(String bucketName) {
        ObjectListing objectListing = s3.listObjects(bucketName);

        while (true) {
            for (Iterator iterator = objectListing.getObjectSummaries().iterator(); iterator.hasNext(); ) {
                S3ObjectSummary objectSummary = (S3ObjectSummary) iterator.next();
                s3.deleteObject(bucketName, objectSummary.getKey());
            }

            if (objectListing.isTruncated()) {
                objectListing = s3.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }

        s3.deleteBucket(bucketName);
    }

}
