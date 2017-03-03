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

package utils.http;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Rule;
import software.amazon.awssdk.auth.BasicAwsCredentials;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.S3ClientOptions;
import software.amazon.awssdk.util.IOUtils;

/**
 * Base class for tests that use a WireMock server
 */
public abstract class S3WireMockTestBase {

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    public static String getExpectedMarshalledXml(String resourceName) {
        final InputStream stream = S3WireMockTestBase.class.getResourceAsStream("/resources/marshalling/" + resourceName);
        try {
            return IOUtils.toString(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(stream, null);
        }
    }

    protected String getEndpoint() {
        return "http://localhost:" + mockServer.port();
    }

    protected AmazonS3 buildClient() {
        AmazonS3Client s3 = new AmazonS3Client(new BasicAwsCredentials("akid", "skid"));
        s3.setEndpoint(getEndpoint());
        s3.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build());
        return s3;
    }

    /**
     * Stubs common S3 response headers
     */
    protected ResponseDefinitionBuilder stubS3ResponseCommon(
            ResponseDefinitionBuilder responseBuilder) {
        return responseBuilder.withHeader(Headers.REQUEST_ID, "36E5C81B8463E101")
                              .withHeader(Headers.EXTENDED_REQUEST_ID,
                                          "FJKdbo9Vbfb+MGbciAgKQ+Dy8mQ70rKNaz7PHvoCNKiZuh0OcKJd9Y9a6g8v1Oec")
                              .withHeader(Headers.CONTENT_TYPE, "application/xml");

    }
}
