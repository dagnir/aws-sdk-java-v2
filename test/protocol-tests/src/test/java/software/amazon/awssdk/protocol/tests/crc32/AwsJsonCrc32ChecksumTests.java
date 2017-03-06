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

package software.amazon.awssdk.protocol.tests.crc32;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.BasicAwsCredentials;
import software.amazon.awssdk.internal.StaticCredentialsProvider;
import software.amazon.awssdk.services.protocol.jsonrpc.AmazonProtocolJsonRpc;
import software.amazon.awssdk.services.protocol.jsonrpc.AmazonProtocolJsonRpcClient;
import software.amazon.awssdk.services.protocol.jsonrpc.model.AllTypesRequest;
import software.amazon.awssdk.services.protocol.jsonrpc.model.AllTypesResult;

public class AwsJsonCrc32ChecksumTests {
    @Rule
    public WireMockRule mockServer = new WireMockRule(WireMockConfiguration.wireMockConfig()
                                                              .port(0)
                                                              .fileSource(new SingleRootFileSource("src/test/resources")));

    private static final String JSON_BODY = "{\"StringMember\":\"foo\"}";
    private static final String JSON_BODY_GZIP = "compressed_json_body.gz";
    private static final String JSON_BODY_Crc32_CHECKSUM = "3049587505";
    private static final String JSON_BODY_GZIP_Crc32_CHECKSUM = "3023995622";

    private static final String JSON_BODY_EXTRA_DATA_GZIP = "compressed_json_body_with_extra_data.gz";
    private static final String JSON_BODY_EXTRA_DATA_GZIP_Crc32_CHECKSUM = "1561543715";

    private static final StaticCredentialsProvider FAKE_CREDENTIALS_PROVIDER = new StaticCredentialsProvider(
            new BasicAwsCredentials("foo", "bar"));

    @BeforeClass
    public static void setup() {
        BasicConfigurator.configure();
    }

    @Test
    public void clientCalculatesCrc32FromCompressedData_WhenCrc32IsValid() {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                                                         .withStatus(200)
                                                         .withHeader("Content-Encoding", "gzip")
                                                         .withHeader("x-amz-crc32", JSON_BODY_GZIP_Crc32_CHECKSUM)
                                                         .withBodyFile(JSON_BODY_GZIP)));
        AmazonProtocolJsonRpc jsonRpc = new AmazonProtocolJsonRpcCrc32TestClient(FAKE_CREDENTIALS_PROVIDER,
                                                                                 new LegacyClientConfiguration().withGzip(true));
        jsonRpc.setEndpoint("http://localhost:" + mockServer.port());
        AllTypesResult result =
                jsonRpc.allTypes(new AllTypesRequest());
        Assert.assertEquals("foo", result.getStringMember());
    }

    /**
     * See https://github.com/aws/aws-sdk-java/issues/1018. With GZIP there is apparently a chance there can be some extra
     * stuff/padding beyond the JSON document. Jackson's JsonParser won't necessarily read this if it's able to close the JSON
     * object. After unmarshalling the response, the SDK should consume all the remaining bytes from the stream to ensure the
     * Crc32 calculated is accurate.
     */
    @Test
    public void clientCalculatesCrc32FromCompressedData_ExtraData_WhenCrc32IsValid() {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                                                         .withStatus(200)
                                                         .withHeader("Content-Encoding", "gzip")
                                                         .withHeader("x-amz-crc32", JSON_BODY_EXTRA_DATA_GZIP_Crc32_CHECKSUM)
                                                         .withBodyFile(JSON_BODY_EXTRA_DATA_GZIP)));
        AmazonProtocolJsonRpc jsonRpc = new AmazonProtocolJsonRpcCrc32TestClient(FAKE_CREDENTIALS_PROVIDER,
                                                                                 new LegacyClientConfiguration().withGzip(true));
        jsonRpc.setEndpoint("http://localhost:" + mockServer.port());
        AllTypesResult result =
                jsonRpc.allTypes(new AllTypesRequest());
        Assert.assertEquals("foo", result.getStringMember());
    }

    @Test(expected = AmazonClientException.class)
    public void clientCalculatesCrc32FromCompressedData_WhenCrc32IsInvalid_ThrowsException() {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                                                         .withStatus(200)
                                                         .withHeader("Content-Encoding", "gzip")
                                                         .withHeader("x-amz-crc32", JSON_BODY_Crc32_CHECKSUM)
                                                         .withBodyFile(JSON_BODY_GZIP)));
        AmazonProtocolJsonRpc jsonRpc = new AmazonProtocolJsonRpcCrc32TestClient(FAKE_CREDENTIALS_PROVIDER,
                                                                                 new LegacyClientConfiguration().withGzip(true));
        jsonRpc.setEndpoint("http://localhost:" + mockServer.port());
        jsonRpc.allTypes(new AllTypesRequest());
    }

    private static class AmazonProtocolJsonRpcCrc32TestClient extends AmazonProtocolJsonRpcClient {

        public AmazonProtocolJsonRpcCrc32TestClient(AwsCredentialsProvider credentialsProvider, LegacyClientConfiguration config) {
            super(credentialsProvider, config);
        }

        @Override
        public final boolean calculateCrc32FromCompressedData() {
            return true;
        }
    }

    @Test
    public void clientCalculatesCrc32FromDecompressedData_WhenCrc32IsValid() {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                                                         .withStatus(200)
                                                         .withHeader("Content-Encoding", "gzip")
                                                         .withHeader("x-amz-crc32", JSON_BODY_Crc32_CHECKSUM)
                                                         .withBodyFile(JSON_BODY_GZIP)));
        AmazonProtocolJsonRpc jsonRpc = new AmazonProtocolJsonRpcClient(FAKE_CREDENTIALS_PROVIDER,
                                                                        new LegacyClientConfiguration().withGzip(true));
        jsonRpc.setEndpoint("http://localhost:" + mockServer.port());
        AllTypesResult result =
                jsonRpc.allTypes(new AllTypesRequest());
        Assert.assertEquals("foo", result.getStringMember());
    }

    @Test(expected = AmazonClientException.class)
    public void clientCalculatesCrc32FromDecompressedData_WhenCrc32IsInvalid_ThrowsException() {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                                                         .withStatus(200)
                                                         .withHeader("Content-Encoding", "gzip")
                                                         .withHeader("x-amz-crc32", JSON_BODY_GZIP_Crc32_CHECKSUM)
                                                         .withBodyFile(JSON_BODY_GZIP)));
        AmazonProtocolJsonRpc jsonRpc = new AmazonProtocolJsonRpcClient(FAKE_CREDENTIALS_PROVIDER,
                                                                        new LegacyClientConfiguration().withGzip(true));
        jsonRpc.setEndpoint("http://localhost:" + mockServer.port());
        jsonRpc.allTypes(new AllTypesRequest());
    }

    @Test
    public void useGzipFalse_WhenCrc32IsValid() {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                                                         .withStatus(200)
                                                         .withHeader("x-amz-crc32", JSON_BODY_Crc32_CHECKSUM)
                                                         .withBody(JSON_BODY)));
        AmazonProtocolJsonRpc jsonRpc = new AmazonProtocolJsonRpcClient(FAKE_CREDENTIALS_PROVIDER,
                                                                        new LegacyClientConfiguration().withGzip(false));
        jsonRpc.setEndpoint("http://localhost:" + mockServer.port());
        AllTypesResult result =
                jsonRpc.allTypes(new AllTypesRequest());
        Assert.assertEquals("foo", result.getStringMember());
    }

    @Test(expected = AmazonClientException.class)
    public void useGzipFalse_WhenCrc32IsInvalid_ThrowException() {
        stubFor(post(urlEqualTo("/")).willReturn(aResponse()
                                                         .withStatus(200)
                                                         .withHeader("x-amz-crc32", JSON_BODY_GZIP_Crc32_CHECKSUM)
                                                         .withBody(JSON_BODY)));
        AmazonProtocolJsonRpc jsonRpc = new AmazonProtocolJsonRpcClient(FAKE_CREDENTIALS_PROVIDER,
                                                                        new LegacyClientConfiguration().withGzip(false));
        jsonRpc.setEndpoint("http://localhost:" + mockServer.port());
        jsonRpc.allTypes(new AllTypesRequest());
    }
}
