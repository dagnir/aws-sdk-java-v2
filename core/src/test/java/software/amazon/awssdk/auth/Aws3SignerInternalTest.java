/*
 * Copyright (c) 2016. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package software.amazon.awssdk.auth;

import software.amazon.awssdk.SignableRequest;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for the AWS3 signer implementation.
 */
public class Aws3SignerInternalTest {

    /**
     * A previously computed AWS3 HTTP authorization header from a Coral Explorer request.
     */
    private static final String EXPECTED_AUTHORIZATION_HEADER =
            "AWS3 AWSAccessKeyId=access,Algorithm=HmacSHA256," +
                    "SignedHeaders=Host;X-Amz-Date;X-Amz-Target," +
                    "Signature=ceuBBi+ulAGez7YKUWkrZRLga+L8hE1vi4M95aZVwCw=";

    /**
     * Shared signer for tests to use.
     */
    private Aws3Signer signer = new Aws3Signer();


    /**
     * Tests that we can calculate an AWS3 HTTP signature and compares it to a
     * previously computed signature.
     */
    @Test
    public void testHttpSigning() throws Exception {
        AwsCredentials credentials = new BasicAwsCredentials("access", "secret");
        SignableRequest<?> request = createMockRequest();

        // Override the date, so that we use the same date as
        // the previously
        signer.overrideDate("Tue, 15 Mar 2011 20:35:24 GMT");
        signer.sign(request, credentials);

        String authorization = request.getHeaders().get("X-Amzn-Authorization");
        assertEquals(EXPECTED_AUTHORIZATION_HEADER, authorization);
    }

    /**
     * Tests that if passed anonymous credentials, signer will not generate a signature
     */
    @Test
    public void testAnonymous() throws Exception {
        AwsCredentials credentials = new AnonymousAwsCredentials();
        SignableRequest<?> request = createMockRequest();

        // Override the date, so that we use the same date as
        // the previously
        signer.overrideDate("Tue, 15 Mar 2011 20:35:24 GMT");
        signer.sign(request, credentials);

        String authorization = request.getHeaders().get("X-Amzn-Authorization");
        assertNull(authorization);
    }

    private SignableRequest<?> createMockRequest() {
        return MockRequestBuilder.create()
                .withContent(new ByteArrayInputStream("{\"TableName\": \"foo\"}".getBytes()))
                .withPath("/")
                .withEndpoint("http://sdb-func7-3001.sea3:8080")
                .withTarget("com.amazon.bigbird.sharedtypes.BigBirdRequestRouterService.DescribeTable")
                .build();
    }


}
