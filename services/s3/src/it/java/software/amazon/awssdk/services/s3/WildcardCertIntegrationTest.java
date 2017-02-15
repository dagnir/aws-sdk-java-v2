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

import java.io.UnsupportedEncodingException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.AWSStaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.test.AWSIntegrationTestBase;
import software.amazon.awssdk.util.StringInputStream;

/**
 * S3 uses a wildcard certificate which is needed when using DNS style addressing with buckets that
 * have dots '.' in the name. I.E. my.bucket.name.s3.amazonaws.com. This test ensures strict host
 * name validation is disabled in the HTTP client for clients created with both the builder and the
 * constructors.
 */
public class WildcardCertIntegrationTest extends AWSIntegrationTestBase {

    private static final String BUCKET_NAME =
            "java-sdk.wildcard.cert.integration.test-" + System.currentTimeMillis();

    private static AmazonS3 S3;

    @BeforeClass
    public static void setup() {
        S3 = AmazonS3ClientBuilder.standard()
                                  .withRegion(Regions.US_WEST_2)
                                  .withCredentials(new AWSStaticCredentialsProvider(getCredentials()))
                                  .build();
        S3.createBucket(BUCKET_NAME);
    }

    @AfterClass
    public static void tearDown() {
        CryptoTestUtils.deleteBucketAndAllContents(S3, BUCKET_NAME);
    }

    @Test
    public void clientCreatedWithBuilder_DisablesStrictHostNameVerification() throws
                                                                              UnsupportedEncodingException {
        S3.putObject(
                new PutObjectRequest(BUCKET_NAME, "some-key.txt", new StringInputStream("content"),
                                     new ObjectMetadata()));
    }

    @Test
    public void clientCreatedWithConstructor_DisablesStrictHostNameVerification() throws
                                                                                  UnsupportedEncodingException {
        AmazonS3 constructorS3 = new AmazonS3Client(getCredentials());
        constructorS3.setRegion(Region.getRegion(Regions.US_WEST_2));
        constructorS3.putObject(
                new PutObjectRequest(BUCKET_NAME, "some-key.txt", new StringInputStream("content"),
                                     new ObjectMetadata()));
    }
}
