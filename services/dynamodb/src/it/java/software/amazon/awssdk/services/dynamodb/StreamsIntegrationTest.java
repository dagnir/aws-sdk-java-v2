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

package software.amazon.awssdk.services.dynamodb;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.dynamodb.model.ListStreamsRequest;
import software.amazon.awssdk.test.AwsTestBase;

public class StreamsIntegrationTest extends AwsTestBase {

    private AmazonDynamoDBStreams streams;

    @Before
    public void setup() throws Exception {
        setUpCredentials();
        streams = new AmazonDynamoDBStreamsClient(credentials);
    }

    @Test
    public void testDefaultEndpoint() {
        streams.listStreams(new ListStreamsRequest().withTableName("foo"));
    }

    @Test
    public void testSetEndpoint() {
        streams.setEndpoint("streams.dynamodb.us-west-2.amazonaws.com");
        streams.listStreams(new ListStreamsRequest().withTableName("foo"));
    }

    @Test
    public void testSetRegion() {
        streams.setRegion(Region.getRegion(Regions.EU_WEST_1));
        streams.listStreams(new ListStreamsRequest().withTableName("foo"));
    }

}
