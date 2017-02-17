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

package software.amazon.awssdk.services.s3.region;

import java.io.File;
import java.io.IOException;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.test.util.RandomTempFile;

/**
 * Integration test to verify that we are able to send simple GET/PUT requests
 * to all the public s3 endpoints.
 */
@Category(S3Categories.Slow.class)
public class AllPublicEndpointsIntegrationTest extends S3IntegrationTestBase {

    private static final String KEY = "key";
    private static AmazonS3Client s3;
    private static File file;

    @BeforeClass
    public static void setup() throws IOException {
        setUpCredentials();
        s3 = new AmazonS3Client(credentials);
        file = new RandomTempFile("source", 100L);
    }

    @Test
    public void testRegionsWithValidCreds() throws IOException {
        String[] endpoints = {
            "s3.amazonaws.com",
            "s3-external-1.amazonaws.com",
            "s3-us-west-1.amazonaws.com",
            "s3-us-west-2.amazonaws.com",
            "s3-eu-west-1.amazonaws.com",
            "s3.eu-central-1.amazonaws.com",
            "s3-ap-southeast-1.amazonaws.com",
            "s3-ap-southeast-2.amazonaws.com",
            "s3-ap-northeast-1.amazonaws.com",
            "s3-sa-east-1.amazonaws.com",
        };

        for (String endpoint : endpoints) {
            testOperations(endpoint, s3);
        }
    }

    @Test
    public void testRegionsWithInvalidCreds() throws IOException {
        String[] endpoints = {
            "s3.cn-north-1.amazonaws.com.cn",
            "s3-us-gov-west-1.amazonaws.com",
            "s3-fips-us-gov-west-1.amazonaws.com"
        };

        for (String endpoint : endpoints) {
            try {
                testOperations(endpoint, s3);
            } catch (AmazonS3Exception ase) {
                Assert.assertEquals("InvalidAccessKeyId", ase.getErrorCode());
            }
        }
    }

    private void testOperations(String endpoint, AmazonS3Client s3) throws IOException {
        String bucket = "java-all-public-regions-test-" + System.currentTimeMillis();

        s3.setEndpoint(endpoint);

        if (!s3.doesBucketExist(bucket)) {
            s3.createBucket(bucket);
        }

        s3.putObject(bucket, KEY, file);

        S3Object object = s3.getObject(bucket, KEY);
        object.close();

        s3.deleteObject(bucket, KEY);

        s3.deleteBucket(bucket);
    }
}
