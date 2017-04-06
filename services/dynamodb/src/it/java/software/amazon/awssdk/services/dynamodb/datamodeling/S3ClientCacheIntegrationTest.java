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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.BasicAwsCredentials;
import software.amazon.awssdk.auth.DefaultAwsCredentialsProviderChain;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.AmazonS3ClientBuilder;
import software.amazon.awssdk.services.s3.model.Region;
import software.amazon.awssdk.services.s3.transfer.TransferManager;

public class S3ClientCacheIntegrationTest {
    private AwsCredentials credentials;

    @Before
    public void setUp() {
        credentials = new BasicAwsCredentials("mock", "mock");
    }

    @Test
    public void testClientReuse() {
        S3ClientCache s3cc = new S3ClientCache(credentials);

        TransferManager tmEast = s3cc.getTransferManager(Region.US_Standard);
        AmazonS3 s3East = s3cc.getClient(Region.US_Standard);

        assertNotNull(tmEast);
        assertNotNull(s3East);
        assertSame(s3East, tmEast.getAmazonS3Client());

        assertSame(s3East, s3cc.getClient(Region.US_Standard));
        assertSame(tmEast, s3cc.getTransferManager(Region.US_Standard));
    }

    @Test
    public void testUserProvidedClients() {
        S3ClientCache s3cc = new S3ClientCache(credentials);
        AmazonS3Client s3East1 = new AmazonS3Client(credentials);
        s3East1.setRegion(Region.US_Standard.toAwsRegion());
        AmazonS3Client s3West1 = new AmazonS3Client(credentials);
        s3West1.setRegion(Region.US_West.toAwsRegion());
        AmazonS3Client s3West2 = new AmazonS3Client(credentials);
        s3West2.setRegion(Region.US_West_2.toAwsRegion());

        s3cc.useClient(s3East1);
        s3cc.useClient(s3West1);
        s3cc.useClient(s3West2);

        TransferManager tmEast1 = s3cc.getTransferManager(Region.US_Standard);
        TransferManager tmWest1 = s3cc.getTransferManager(Region.US_West);
        TransferManager tmWest2 = s3cc.getTransferManager(Region.US_West_2);

        assertNotSame(tmEast1, tmWest1);
        assertNotSame(tmEast1, tmWest2);
        assertNotSame(tmWest1, tmWest2);

        assertSame(s3cc.getClient(Region.US_Standard), tmEast1.getAmazonS3Client());
        assertSame(s3cc.getClient(Region.US_West), tmWest1.getAmazonS3Client());
        assertSame(s3cc.getClient(Region.US_West_2), tmWest2.getAmazonS3Client());
    }

    @Test
    public void testS3ClientCacheWithRegionString() {
        S3ClientCache s3cc = new S3ClientCache(credentials);
        AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion("us-east-2")
                                           .withCredentials(new DefaultAwsCredentialsProviderChain()).build();
        s3cc.useClient(s3);

        TransferManager tm = s3cc.getTransferManager("us-east-2");

        assertSame(s3cc.getClient("us-east-2"), s3);
        assertSame(s3cc.getTransferManager("us-east-2"), tm);
    }

    @Test
    public void testReplaceClient() {
        S3ClientCache s3cc = new S3ClientCache(credentials);

        TransferManager tmEast1 = s3cc.getTransferManager(Region.US_Standard);
        assertNotNull(tmEast1);

        AmazonS3Client newS3East = new AmazonS3Client(credentials);
        newS3East.setRegion(Region.US_Standard.toAwsRegion());
        s3cc.useClient(newS3East); // should remove old TM

        TransferManager tmEast2 = s3cc.getTransferManager(Region.US_Standard);
        assertNotNull(tmEast2);
        assertNotSame(tmEast2, tmEast1);
        assertNotSame(tmEast2.getAmazonS3Client(), tmEast1.getAmazonS3Client());
    }

    @Test
    public void testBadClientCache() {
        S3ClientCache s3cc = new S3ClientCache(credentials);
        AmazonS3Client notAnAWSEndpoint = new AmazonS3Client(credentials);
        notAnAWSEndpoint.setEndpoint("i.am.an.invalid.aws.endpoint.com");
        try {
            s3cc.useClient(notAnAWSEndpoint);
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("No valid region has been specified. Unable to return region name"));
            return;
        }

        fail("Expected exception to be thrown");
    }

    @Test
    public void testNonExistantRegion() {
        S3ClientCache s3cc = new S3ClientCache(credentials);
        AmazonS3Client notAnAWSEndpoint = new AmazonS3Client(credentials);
        notAnAWSEndpoint.setEndpoint("s3.mordor.amazonaws.com");
        try {
            s3cc.useClient(notAnAWSEndpoint);
        } catch (IllegalStateException e) {
            assertEquals("No valid region has been specified. Unable to return region name", e.getMessage());
            return;
        }

        fail("Expected IllegalStateException to be thrown");
    }
}
