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

package software.amazon.awssdk.services.route53;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import software.amazon.awssdk.services.route53.internal.Route53IdRequestHandler;
import software.amazon.awssdk.services.route53.model.CreateHostedZoneResult;
import software.amazon.awssdk.services.route53.model.CreateReusableDelegationSetResult;
import software.amazon.awssdk.services.route53.model.DelegationSet;
import software.amazon.awssdk.services.route53.model.GetHostedZoneResult;
import software.amazon.awssdk.services.route53.model.GetReusableDelegationSetResult;
import software.amazon.awssdk.services.route53.model.ListReusableDelegationSetsResult;

/**
 * Unit test for request handler customization of delegation set id's
 *
 */
public class Route53RequestHandlerTest {

    private static final String delegationPrefix = "delegationset";

    private static final String id = "delegationSetId";

    private static final String delegationSetId = "/" + delegationPrefix + "/"
                                                  + id;

    /**
     * Tests if the request handler strips the delegation set prefixes. Asserts
     * that the result object has prefix removed.
     */
    @Test
    public void testDelegationSetPrefixRemoval() {

        Route53IdRequestHandler requestHandler = new Route53IdRequestHandler();

        DelegationSet delegationSet = new DelegationSet();
        delegationSet.setId(delegationSetId);

        CreateHostedZoneResult createResult = new CreateHostedZoneResult()
                .withDelegationSet(delegationSet);

        requestHandler.afterResponse(null, createResult, null);

        assertEquals(createResult.getDelegationSet().getId(), id);

        CreateReusableDelegationSetResult createResuableResult = new CreateReusableDelegationSetResult()
                .withDelegationSet(delegationSet);

        requestHandler.afterResponse(null, createResuableResult, null);

        assertEquals(createResuableResult.getDelegationSet().getId(), id);

        GetHostedZoneResult getZoneResult = new GetHostedZoneResult()
                .withDelegationSet(delegationSet);

        requestHandler.afterResponse(null, getZoneResult, null);

        assertEquals(getZoneResult.getDelegationSet().getId(), id);

        GetReusableDelegationSetResult getResuableResult = new GetReusableDelegationSetResult()
                .withDelegationSet(delegationSet);

        requestHandler.afterResponse(null, getResuableResult, null);

        assertEquals(getResuableResult.getDelegationSet().getId(), id);

        ListReusableDelegationSetsResult listResult = new ListReusableDelegationSetsResult()
                .withDelegationSets(delegationSet);

        requestHandler.afterResponse(null, listResult, null);

        assertEquals(listResult.getDelegationSets().get(0).getId(), id);

        delegationSet.setId(id);

        createResult = new CreateHostedZoneResult()
                .withDelegationSet(delegationSet);

        requestHandler.afterResponse(null, createResult, null);

        assertEquals(createResult.getDelegationSet().getId(), id);

    }
}
