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

package software.amazon.awssdk.services.swf;

import java.util.UUID;
import org.junit.Test;
import software.amazon.awssdk.services.simpleworkflow.model.DescribeDomainRequest;
import software.amazon.awssdk.services.simpleworkflow.model.RegisterDomainRequest;
import software.amazon.awssdk.services.simpleworkflow.model.UnknownResourceException;

/**
 * Integration tests for SWF
 */
public class SWFIntegrationTest extends IntegrationTestBase {

    /**
     * Simple smoke test to demonstrate a working client
     */
    @Test
    public void testCallReturningVoid() throws Exception {
        swf.registerDomain(new RegisterDomainRequest().withName(UUID.randomUUID().toString()).withDescription("blah")
                                                      .withWorkflowExecutionRetentionPeriodInDays("1"));
        System.out.println("Domain registered successfully!");
    }

    @Test(expected = UnknownResourceException.class)
    public void testCallThrowingFault() throws Exception {
        swf.describeDomain(new DescribeDomainRequest().withName(UUID.randomUUID().toString()));
    }

}
