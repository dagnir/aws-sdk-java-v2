/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.cloudsearchdomain;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.BasicAwsCredentials;
import software.amazon.awssdk.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import software.amazon.awssdk.services.cloudsearchdomain.model.SearchRequest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Unit tests for {@link SearchRequest}.
 */
public class SearchRequestUnitTest {
    private static final AwsCredentials CREDENTIALS = new BasicAwsCredentials("access", "secret");

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    private AmazonCloudSearchDomainClient searchClient;

    @Before
    public void testSetup() {
        searchClient = new AmazonCloudSearchDomainClient(CREDENTIALS);
    }

    /**
     * Test that search requests use POST instead of (the also supported) GET.
     * @throws IOException
     */
    @Test
    public void testPOSTUsedForSearchRequest() throws IOException {
        stubFor(post(urlMatching("/.*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("{\"status\":{\"rid\":\"fooBar\",\"time-ms\":7},\"hits\":{\"found\":0,\"start\":0,\"hit\":[]}}")));

        searchClient.setEndpoint("http://localhost:" + wireMockRule.port());
        searchClient.search(new SearchRequest().withQuery("Lord of the Rings"));

        verify(postRequestedFor(urlMatching("/.*")).withRequestBody(equalTo("format=sdk&pretty=true&q=Lord+of+the+Rings")));
    }
}
