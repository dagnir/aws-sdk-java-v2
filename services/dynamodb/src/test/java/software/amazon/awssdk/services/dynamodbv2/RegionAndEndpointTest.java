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

package software.amazon.awssdk.services.dynamodbv2;

import junit.framework.Assert;
import org.junit.Test;
import software.amazon.awssdk.auth.AWS4Signer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.util.AwsHostNameUtils;

public class RegionAndEndpointTest {

    private static final String DYNAMODB = "dynamodb";
    private static final String US_WEST_2 = "us-west-2";
    private static final String CN_NORTH_1 = "cn-north-1";

    @Test
    public void testSetEndpoint_StandardEndpoint() {

        TestAmazonDynamoDBStreamsClient client = new TestAmazonDynamoDBStreamsClient();
        String endpoint = "streams.dynamodb.us-west-2.amazonaws.com";
        client.setEndpoint(endpoint);

        Assert.assertEquals(endpoint, client.getEndpointHost());

        AWS4Signer signer = client.getSigner();
        Assert.assertEquals(DYNAMODB, signer.getServiceName());
        // Current don't have a better way to verify the signer region
        Assert.assertEquals(US_WEST_2,
                            AwsHostNameUtils.parseRegionName(endpoint, signer.getServiceName()));
    }

    @Test
    public void testSetEndpoint_NonstandardEndpoint_RegionOverride() {

        TestAmazonDynamoDBStreamsClient client = new TestAmazonDynamoDBStreamsClient();
        String endpoint = "ddbstreams.test-cluster.amazonaws.com";
        client.setEndpoint(endpoint);
        client.setSignerRegionOverride("non-standard-region");

        Assert.assertEquals(endpoint, client.getEndpointHost());

        AWS4Signer signer = client.getSigner();
        Assert.assertEquals(DYNAMODB, signer.getServiceName());
        Assert.assertEquals("non-standard-region", signer.getRegionName());
    }

    @Test
    public void testSetRegion() {

        TestAmazonDynamoDBStreamsClient client = new TestAmazonDynamoDBStreamsClient();
        client.setRegion(Region.getRegion(Regions.US_WEST_2));

        Assert.assertEquals("streams.dynamodb.us-west-2.amazonaws.com", client.getEndpointHost());

        AWS4Signer signer = client.getSigner();
        Assert.assertEquals(DYNAMODB, signer.getServiceName());
        // Current don't have a better way to verify the signer region
        Assert.assertEquals(US_WEST_2,
                            AwsHostNameUtils.parseRegionName(client.getEndpointHost(), signer.getServiceName()));
    }

    @Test
    public void testSetRegion_BJS() {

        TestAmazonDynamoDBStreamsClient client = new TestAmazonDynamoDBStreamsClient();
        client.setRegion(Region.getRegion(Regions.CN_NORTH_1));

        Assert.assertEquals("streams.dynamodb.cn-north-1.amazonaws.com.cn", client.getEndpointHost());

        AWS4Signer signer = client.getSigner();
        Assert.assertEquals(DYNAMODB, signer.getServiceName());
        // Current don't have a better way to verify the signer region
        Assert.assertEquals(CN_NORTH_1,
                            AwsHostNameUtils.parseRegionName(client.getEndpointHost(), signer.getServiceName()));
    }

    /**
     * Subclass of AmazonDynamoDBStreamsClient that exposes some protected
     * fields that we need for testing.
     */
    static class TestAmazonDynamoDBStreamsClient extends AmazonDynamoDBStreamsClient {
        public AWS4Signer getSigner() {
            return (AWS4Signer) super.getSigner();
        }

        public String getEndpointHost() {
            return endpoint.getHost();
        }
    }

}
