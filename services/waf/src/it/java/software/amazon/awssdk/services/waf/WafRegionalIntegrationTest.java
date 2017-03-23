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

package software.amazon.awssdk.services.waf;

import org.junit.Test;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.waf.model.WAFNonexistentItemException;
import software.amazon.awssdk.services.waf.model.ListResourcesForWebACLRequest;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

public class WafRegionalIntegrationTest extends AwsIntegrationTestBase {

    /**
     * Calls an operation specific to WAF Regional. If we get a modeled exception back then we called the
     * right service.
     */
    @Test(expected = WAFNonexistentItemException.class)
    public void smokeTest() {
        final WAFRegionalClient client = WAFRegionalClient.builder()
                                                                 .withCredentials(new AwsStaticCredentialsProvider(getCredentials()))
                                                                 .withRegion(Regions.US_WEST_2)
                                                                 .build();

        client.listResourcesForWebACL(new ListResourcesForWebACLRequest().withWebACLId("foo"));
    }
}
