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

package software.amazon.awssdk.services.glacier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.auth.BasicAwsCredentials;
import software.amazon.awssdk.client.builder.AwsClientBuilder;
import software.amazon.awssdk.services.glacier.model.ListVaultsRequest;

/**
 * Glacier has a customization to default accountId to '-' (which indicates the current account) if not provided.
 */
public class AccountIdDefaultValueTest {

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    private GlacierClient glacier;

    @Before
    public void setup() {
        glacier = GlacierClient.builder()
                .withCredentials(new AwsStaticCredentialsProvider(new BasicAwsCredentials("akid", "skid")))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(getEndpoint(), "us-west-2"))
                .build();
    }

    private String getEndpoint() {
        return "http://localhost:" + mockServer.port();
    }

    @Test
    public void noAccountIdProvided_DefaultsToHyphen() {
        stubFor(any(urlMatching(".*"))
                        .willReturn(aResponse()
                                            .withStatus(200)
                                            .withBody("{}")));
        glacier.listVaults(ListVaultsRequest.builder_().build_());
        verify(getRequestedFor(urlEqualTo("/-/vaults")));
    }

    @Test
    public void accountIdProvided_DoesNotChangeValue() {
        stubFor(any(urlMatching(".*"))
                        .willReturn(aResponse()
                                            .withStatus(200)
                                            .withBody("{}")));
        glacier.listVaults(ListVaultsRequest.builder_().accountId("1234").build_());
        verify(getRequestedFor(urlEqualTo("/1234/vaults")));
    }
}
