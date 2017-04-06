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

package software.amazon.awssdk.services.applicationdiscovery;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.services.applicationdiscovery.model.ConfigurationItemType;
import software.amazon.awssdk.services.applicationdiscovery.model.ListConfigurationsRequest;
import software.amazon.awssdk.services.applicationdiscovery.model.ListConfigurationsResult;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

public class ServiceIntegrationTest extends AwsIntegrationTestBase {

    private static ApplicationDiscoveryClient discoveryService;

    @BeforeClass
    public static void setUp() {
        discoveryService = ApplicationDiscoveryClient.builder()
                .withCredentials(new AwsStaticCredentialsProvider(getCredentials()))
                .build();
    }

    @Test
    public void testListOperation() {
        ListConfigurationsResult listResult = discoveryService.listConfigurations(
                new ListConfigurationsRequest().withConfigurationType(ConfigurationItemType.PROCESS));
        Assert.assertNotNull(listResult);
        Assert.assertNotNull(listResult.getConfigurations());
    }

}
