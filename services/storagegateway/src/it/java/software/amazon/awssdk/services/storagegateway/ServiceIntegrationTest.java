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

package software.amazon.awssdk.services.storagegateway;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.storagegateway.model.DeleteGatewayRequest;
import software.amazon.awssdk.services.storagegateway.model.InvalidGatewayRequestException;
import software.amazon.awssdk.services.storagegateway.model.ListGatewaysRequest;
import software.amazon.awssdk.services.storagegateway.model.ListGatewaysResult;
import software.amazon.awssdk.test.AwsTestBase;

/**
 * Tests service methods in storage gateway. Because of the non-trivial amount of set-up required,
 * this is more of a spot check than an exhaustive test.
 */
public class ServiceIntegrationTest extends AwsTestBase {

    private static StorageGatewayClient sg;

    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();
        sg = StorageGatewayClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).region(Regions.US_EAST_1.getName()).build();
    }

    @Test
    public void testListGateways() {
        ListGatewaysResult listGateways = sg.listGateways(new ListGatewaysRequest());
        assertNotNull(listGateways);
        assertThat(listGateways.getGateways().size(), greaterThanOrEqualTo(0));
    }

    @Test(expected = InvalidGatewayRequestException.class)
    public void deleteGateway_InvalidArn_ThrowsException() {
        sg.deleteGateway(new DeleteGatewayRequest().withGatewayARN("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
    }

    @Test(expected = AmazonServiceException.class)
    public void deleteGateway_NullArn_ThrowsAmazonServiceException() {
        sg.deleteGateway(new DeleteGatewayRequest());

    }

}
