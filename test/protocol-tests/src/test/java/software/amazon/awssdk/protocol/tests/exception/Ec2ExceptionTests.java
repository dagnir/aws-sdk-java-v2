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

package software.amazon.awssdk.protocol.tests.exception;

import static org.junit.Assert.assertEquals;
import static util.exception.ExceptionTestUtils.stub404Response;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsStaticCredentialsProvider;
import software.amazon.awssdk.auth.BasicAwsCredentials;
import software.amazon.awssdk.client.builder.AwsClientBuilder.EndpointConfiguration;
import software.amazon.awssdk.services.protocolec2.ProtocolEc2Client;
import software.amazon.awssdk.services.protocolec2.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolec2.model.ProtocolEc2ClientException;

public class Ec2ExceptionTests {
    private static final String PATH = "/";

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolEc2Client client;

    @Before
    public void setupClient() {
        client = ProtocolEc2Client.builder()
                                  .withCredentials(new AwsStaticCredentialsProvider(new BasicAwsCredentials("akid", "skid")))
                                  .withEndpointConfiguration(new EndpointConfiguration("http://localhost:" + wireMock.port(),
                                                                                       "us-east-1"))
                                  .build();
    }

    @Test
    public void unmodeledException_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH,
                        "<Response><Errors><Error><Code>UnmodeledException</Code></Error></Errors></Response>");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void emptyErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH, "");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    @Test
    public void malformedErrorResponse_UnmarshalledIntoBaseServiceException() {
        stub404Response(PATH, "THIS ISN'T XML");
        assertThrowsServiceBaseException(this::callAllTypes);
    }

    private void callAllTypes() {
        client.allTypes(new AllTypesRequest());
    }

    private void assertThrowsServiceBaseException(Runnable runnable) {
        try {
            runnable.run();
        } catch (ProtocolEc2ClientException e) {
            assertEquals(ProtocolEc2ClientException.class, e.getClass());
        }
    }
}
