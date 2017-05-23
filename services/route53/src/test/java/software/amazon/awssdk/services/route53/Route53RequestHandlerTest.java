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

import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.AmazonWebServiceResponse;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.services.route53.internal.Route53IdRequestHandler;
import software.amazon.awssdk.services.route53.model.CreateHostedZoneResult;
import software.amazon.awssdk.services.route53.model.CreateReusableDelegationSetResult;
import software.amazon.awssdk.services.route53.model.DelegationSet;
import software.amazon.awssdk.services.route53.model.GetHostedZoneResult;
import software.amazon.awssdk.services.route53.model.GetReusableDelegationSetResult;
import software.amazon.awssdk.services.route53.model.ListReusableDelegationSetsResult;

/**
 * Unit test for request handler customization of delegation set id's
 */
@ReviewBeforeRelease("This test appears to be wrong, see comment on line 80")
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

        DelegationSet delegationSet = DelegationSet.builder().id(delegationSetId).build();

        CreateHostedZoneResult createResult = CreateHostedZoneResult.builder()
                .delegationSet(delegationSet)
                .build();

        afterResponse(requestHandler, createResult);

        assertEquals(createResult.delegationSet().id(), id);

        CreateReusableDelegationSetResult createResuableResult = CreateReusableDelegationSetResult.builder()
                .delegationSet(delegationSet)
                .build();

        afterResponse(requestHandler, createResuableResult);

        assertEquals(createResuableResult.delegationSet().id(), id);

        GetHostedZoneResult getZoneResult = GetHostedZoneResult.builder()
                .delegationSet(delegationSet)
                .build();

        afterResponse(requestHandler, getZoneResult);

        // This assert works, but only because of the other operations the are sequenced before this, that modify the id.
        assertEquals(getZoneResult.delegationSet().id(), id);

        GetReusableDelegationSetResult getResuableResult = GetReusableDelegationSetResult.builder()
                .delegationSet(delegationSet)
                .build();

        afterResponse(requestHandler, getResuableResult);

        assertEquals(getResuableResult.delegationSet().id(), id);

        ListReusableDelegationSetsResult listResult = ListReusableDelegationSetsResult.builder()
                .delegationSets(delegationSet)
                .build();

        afterResponse(requestHandler, listResult);

        assertEquals(listResult.delegationSets().get(0).id(), id);

        delegationSet = delegationSet.toBuilder().id(id).build();

        createResult = CreateHostedZoneResult.builder()
                .delegationSet(delegationSet)
                .build();

        afterResponse(requestHandler, createResult);

        assertEquals(createResult.delegationSet().id(), id);
    }

    private void afterResponse(RequestHandler2 requestHandler2, Object responseObject) {
        AmazonWebServiceResponse<Object> resp = new AmazonWebServiceResponse<>();
        resp.setResult(responseObject);
        requestHandler2.afterResponse(null, new Response<>(resp, null));
    }
}