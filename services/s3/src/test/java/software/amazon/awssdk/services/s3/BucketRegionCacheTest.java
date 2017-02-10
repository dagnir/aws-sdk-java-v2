/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.net.URI;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.regions.Regions;

public class BucketRegionCacheTest {

    private static final String FAKE_BUCKET_NAME = "fake-bucket-name";
    private final String REGION_ENDPOINT = "s3.eu-central-1.amazonaws.com";

    @Test
    public void testBucketRegionCacheWithRegionSpecificEndpoint_ReturnsRegionEndpoint() {
        AmazonS3Client s3 = new AmazonS3Client();
        s3.setEndpoint(REGION_ENDPOINT);

        URI uri = s3.resolveServiceEndpoint(FAKE_BUCKET_NAME);
        Assert.assertEquals(REGION_ENDPOINT, uri.getHost());
    }

    @Test
    public void testBucketRegionCacheWithSignerOverrideSet_ReturnsEndpointConfigured() {
        AmazonS3Client s3 = new AmazonS3Client();
        s3.setEndpoint(REGION_ENDPOINT);
        s3.setSignerRegionOverride("AWSS4SignerType");
        URI uri = s3.resolveServiceEndpoint(FAKE_BUCKET_NAME);
        Assert.assertEquals(REGION_ENDPOINT, uri.getHost());
    }

    @Test
    public void testBucketRegionCacheWithRegionSet_ReturnsEndpointConfigured() {
        AmazonS3Client s3 = new AmazonS3Client();
        s3.configureRegion(Regions.EU_CENTRAL_1);

        URI uri = s3.resolveServiceEndpoint(FAKE_BUCKET_NAME);
        Assert.assertEquals(REGION_ENDPOINT, uri.getHost());
    }

    @Test
    public void testBucketRegionCacheWithBucketRegionNotInRegionXML_ReturnsEndpointConfigured() {
        final String futureEndpoint = "s3-future-region.amazonaws.com";
        AmazonS3Client s3 = new AmazonS3Client();
        s3.setEndpoint(futureEndpoint);
        URI uri = s3.resolveServiceEndpoint(FAKE_BUCKET_NAME);
        Assert.assertEquals(futureEndpoint, uri.getHost());
    }
}
