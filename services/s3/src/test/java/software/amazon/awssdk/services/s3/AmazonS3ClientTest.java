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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.regions.RegionUtils;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.model.Region;

public class AmazonS3ClientTest {

    private static AmazonS3 s3;

    @Before
    public void setup() {
        s3 = AmazonS3Client.builder()
                .withRegion(Regions.US_EAST_1)
                .build();
    }

    @Test
    public void tt0051346531() {
        String strRegion = "eu-central-1";
        software.amazon.awssdk.regions.Region region = RegionUtils.getRegion(strRegion);
        String endpoint = region.getServiceEndpoint(AmazonS3Client.S3_SERVICE_NAME);
        s3.setEndpoint(endpoint);
        s3.getRegion();
    }

    @Test
    public void getRegionName_ReturnsRegion_When_SetRegion() {
        s3.setRegion(RegionUtils.getRegion("us-east-1"));
        assertThat(s3.getRegionName(), equalTo("us-east-1"));
    }

    @Test
    public void getRegionName_ReturnsRegion_When_SetEndpoint() {
        s3.setEndpoint("s3-us-east-1.amazonaws.com");
        assertThat(s3.getRegionName(), equalTo("us-east-1"));
    }

    @Test
    public void getRegionName_ReturnsRegion_WhenSetEndpointStandardFormat() {
        s3.setEndpoint("s3.us-east-2.amazonaws.com");
        assertThat(s3.getRegionName(), equalTo("us-east-2"));
    }

    @Test(expected = IllegalStateException.class)
    public void getRegionName_ThrowsIllegalStateException_When_InvalidRegion() {
        s3.setEndpoint("s3-mordor.amazonaws.com");
        s3.getRegionName();
    }

    @Test(expected = IllegalStateException.class)
    public void getRegionName_ThrowsIllegalStateException_When_InvalidRegionWithStandardFormat() {
        s3.setEndpoint("s3.mordor.amazonaws.com");
        s3.getRegionName();
    }
}
