/*
 * Copyright 2011-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.apigateway.mockservice;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.AWSCredentialsProvider;
import software.amazon.awssdk.auth.AWSStaticCredentialsProvider;
import software.amazon.awssdk.auth.BasicAWSCredentials;
import software.amazon.awssdk.services.apigateway.mockservice.MyService;
import software.amazon.awssdk.services.apigateway.mockservice.MyServiceClientBuilder;
import software.amazon.awssdk.services.apigateway.mockservice.model.GetNoauthScalarsRequest;

public class ServiceApiKeyComponentTest {
    private static final UrlMatchingStrategy URI = urlMatching(".*");

    @Rule
    public WireMockRule mockServer = new WireMockRule(wireMockConfig().dynamicPort());

    @Before
    public void setUp() {
        stubFor(any(URI).willReturn(aResponse()));
    }

    @Test
    public void canSpecifyAnAPIKeyOnClient() {
        String apiKey = randomAlphanumeric(20);
        MyService service = createServiceBuilder().apiKey(apiKey).build();

        service.getNoauthScalars(new GetNoauthScalarsRequest());

        verify(WireMockExtensions.anyRequestedFor(URI).withHeader("x-api-key", equalTo(apiKey)));
    }

    @Test
    public void noApiKeyIsSentIfNotSpecified() {
        MyService service = createServiceBuilder().build();

        service.getNoauthScalars(new GetNoauthScalarsRequest());

        verify(WireMockExtensions.anyRequestedFor(URI).withoutHeader("x-api-key"));
    }

    private MyServiceClientBuilder createServiceBuilder() {
        return MyService.builder()
                .endpoint("http://localhost:" + mockServer.port())
                .iamCredentials(createCredentialsProvider());
    }

    private AWSCredentialsProvider createCredentialsProvider() {
        String key = randomAlphanumeric(10), secret = randomAlphanumeric(10);
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials(key, secret));
    }
}
