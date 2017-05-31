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

package software.amazon.awssdk.services.s3.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import software.amazon.awssdk.regions.Region;

public class DualstackEndpointBuilderTest {

    private static final Region US_EAST_1 = Region.US_EAST_1;
    private static final Region EU_CENTRAL_1 = Region.EU_CENTRAL_1;

    @Test
    public void getServiceEndpoint_S3StandardRegion_HttpsProtocol_RegionInConstructor() throws Exception {
        DualstackEndpointBuilder endpointBuilder = new DualstackEndpointBuilder("s3", "https", US_EAST_1);
        assertEquals("https://s3.dualstack.us-east-1.amazonaws.com", endpointBuilder.getServiceEndpoint().toString());
    }

    @Test
    public void getServiceEndpoint_S3StandardRegion_HttpProtocol_RegionInConstructor() throws Exception {
        DualstackEndpointBuilder endpointBuilder = new DualstackEndpointBuilder("s3", "http", US_EAST_1);
        assertEquals("http://s3.dualstack.us-east-1.amazonaws.com", endpointBuilder.getServiceEndpoint().toString());
    }

    @Test
    public void getServiceEndpoint_S3NonStandardRegion_HttpProtocol_RegionInConstructor() throws Exception {
        DualstackEndpointBuilder endpointBuilder = new DualstackEndpointBuilder("s3", "http", EU_CENTRAL_1);
        assertEquals("http://s3.dualstack.eu-central-1.amazonaws.com", endpointBuilder.getServiceEndpoint().toString());
    }
}
