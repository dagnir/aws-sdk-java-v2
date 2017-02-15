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

import java.net.InetAddress;
import java.net.URL;
import java.util.Date;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for IP address as the Amazon S3 endpoint.
 */
public class IpAddressAsEndpointIntegrationTest extends S3IntegrationTestBase {

    private static final String STANDARD_ENDPOINT = "https://s3.amazonaws.com";
    private String bucketName = "java-sdk-ip--address-integ-test-" + new Date().getTime();

    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty("software.amazon.awssdk.sdk.disableCertChecking", " ");
        S3IntegrationTestBase.setUp();
    }

    @After
    public void tearDown() {
        super.deleteBucketAndAllContents(bucketName);
    }

    @Test
    public void testIpAddressAsEndpoint() throws Exception {
        System.out.println(System.getProperty("software.amazon.awssdk.sdk.disableCertChecking"));

        URL url = new URL(STANDARD_ENDPOINT);
        String ip = InetAddress.getByName(url.getHost()).getHostAddress();
        s3.setEndpoint(ip);

        s3.createBucket(bucketName);
        waitForBucketCreation(bucketName);
    }

}
