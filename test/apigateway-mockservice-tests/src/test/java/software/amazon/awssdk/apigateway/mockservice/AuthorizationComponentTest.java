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

package software.amazon.awssdk.apigateway.mockservice;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static software.amazon.awssdk.apigateway.mockservice.WireMockExtensions.anyRequestedFor;

import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.AWSCredentialsProvider;
import software.amazon.awssdk.auth.AWSStaticCredentialsProvider;
import software.amazon.awssdk.auth.BasicAWSCredentials;
import software.amazon.awssdk.opensdk.protect.auth.RequestSignerNotFoundException;
import software.amazon.awssdk.services.apigateway.mockservice.MyService;
import software.amazon.awssdk.services.apigateway.mockservice.MyServiceClientBuilder;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutCustomauthScalarsRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutIamauthScalarsRequest;
import software.amazon.awssdk.services.apigateway.mockservice.model.PutNoauthScalarsRequest;

public class AuthorizationComponentTest {

    private static final UrlMatchingStrategy URI = urlMatching(".*");

    @Rule
    public WireMockRule mockServer = new WireMockRule(wireMockConfig().dynamicPort());

    @Before
    public void setUp() {
        stubFor(any(URI).willReturn(aResponse()));
    }

    @Test
    public void canProviderCustomTokenGeneratorToSignRequests() {
        String authToken = randomAlphanumeric(20);
        MyService service = createServiceBuilder()
                .signer(r -> authToken)
                .build();

        service.putCustomauthScalars(new PutCustomauthScalarsRequest());

        verify(anyRequestedFor(URI).withHeader("Authorization", equalTo(authToken)));
    }

    @Test
    public void signersRegistryIsImmutableAfterClientCreation() {
        String correctToken = "correct", wrongToken = "mutated";
        MyServiceClientBuilder builder = createServiceBuilder()
                .signer(r -> correctToken);
        MyService service = builder.build();
        builder.signer(r -> wrongToken);

        service.putCustomauthScalars(new PutCustomauthScalarsRequest());

        verify(anyRequestedFor(URI).withHeader("Authorization", equalTo(correctToken)));
    }

    @Test
    public void noAuthorizerRequiredForNonAuthorizedRequests() {
        MyService service = createServiceBuilder().build();

        service.putNoauthScalars(new PutNoauthScalarsRequest());

        verify(anyRequestedFor(URI).withoutHeader("Authorization"));
    }

    @Test
    public void awsIamSignerIsIncludedWhenCredentialsAreSpecified() {
        String key = randomAlphanumeric(10), secret = randomAlphanumeric(10);
        AWSCredentialsProvider provider = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(key, secret));
        MyService service = createServiceBuilder().iamCredentials(provider).build();

        service.putIamauthScalars(new PutIamauthScalarsRequest());

        verify(anyRequestedFor(URI).withHeader("Authorization", containing(key)));
    }

    @Test(expected = RequestSignerNotFoundException.class)
    public void exceptionIsRaisedIfSignerNotSuppliedForCustomauthorizedRequest() {
        MyService service = createServiceBuilder().build();
        service.putCustomauthScalars(new PutCustomauthScalarsRequest());
    }

    @Test(expected = RequestSignerNotFoundException.class)
    public void exceptionIsRaisedIfCredentialsNotSuppliedToIamSignedRequest() {
        MyService service = createServiceBuilder().build();
        service.putIamauthScalars(new PutIamauthScalarsRequest());
    }

    private MyServiceClientBuilder createServiceBuilder() {
        return MyService.builder().endpoint("http://localhost:" + mockServer.port());
    }
}
