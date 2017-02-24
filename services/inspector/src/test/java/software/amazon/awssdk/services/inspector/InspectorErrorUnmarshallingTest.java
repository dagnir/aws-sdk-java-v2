package software.amazon.awssdk.services.inspector;/*
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

import software.amazon.awssdk.auth.BasicAwsCredentials;
import software.amazon.awssdk.services.inspector.model.AccessDeniedException;
import software.amazon.awssdk.services.inspector.model.ListRulesPackagesRequest;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;

public class InspectorErrorUnmarshallingTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private AmazonInspector inspector;

    @Before
    public void setup() {
        inspector = new AmazonInspectorClient(new BasicAwsCredentials("akid", "skid"));
        inspector.setEndpoint("http://localhost:" + wireMock.port());
    }

    /**
     * Some error shapes in Inspector define an errorCode member which clashes with the errorCode
     * defined in {@link com.amazonaws.AmazonServiceException}. We've customized the name of the
     * modeled error code so both can be used by customers. This test asserts that both are
     * unmarshalled correctly.
     */
    @Test
    public void errorCodeAndInspectorErrorCodeUnmarshalledCorrectly() {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse().withStatus(400).withBody(
                "{\"__type\":\"AccessDeniedException\",\"errorCode\": \"ACCESS_DENIED_TO_RULES_PACKAGE\", " +
                "\"Message\":\"User: arn:aws:iam::1234:user/no-perms is not authorized to perform: inspector:ListRulesPackages\"}")));

        try {
            inspector.listRulesPackages(new ListRulesPackagesRequest());
        } catch (AccessDeniedException e) {
            assertEquals("AccessDeniedException", e.getErrorCode());
            assertEquals("ACCESS_DENIED_TO_RULES_PACKAGE", e.getInspectorErrorCode());
        }
    }

}
