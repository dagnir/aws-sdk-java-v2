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
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.auth.AWSCredentialsProvider;
import software.amazon.awssdk.auth.AWSStaticCredentialsProvider;
import software.amazon.awssdk.auth.BasicAWSCredentials;
import software.amazon.awssdk.services.protocol.restjson.AmazonProtocolRestJson;
import software.amazon.awssdk.services.protocol.restjson.AmazonProtocolRestJsonClient;
import software.amazon.awssdk.services.protocol.restjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocol.restjson.model.AllTypesResult;

public class RestJsonCRC32ChecksumTests {

    private static final String JSON_BODY = "{\"StringMember\":\"foo\"}";
    private static final String JSON_BODY_GZIP = "compressed_json_body.gz";
    private static final String JSON_BODY_CRC32_CHECKSUM = "3049587505";
    private static final String JSON_BODY_GZIP_CRC32_CHECKSUM = "3023995622";
    private static final String RESOURCE_PATH = "/2016-03-11/allTypes";
    private static final AWSCredentialsProvider FAKE_CREDENTIALS_PROVIDER = new AWSStaticCredentialsProvider(
            new BasicAWSCredentials("foo", "bar"));
    @Rule
    public WireMockRule mockServer = new WireMockRule(WireMockConfiguration.wireMockConfig()
                                                                           .port(0)
                                                                           .fileSource(new SingleRootFileSource("src/test/resources")));

    @BeforeClass
    public static void setup() {
        BasicConfigurator.configure();
    }

    @Test
    public void clientCalculatesCRC32FromCompressedData_WhenCRC32IsValid() {
        stubFor(post(urlEqualTo(RESOURCE_PATH)).willReturn(aResponse()
                                                                   .withStatus(200)
                                                                   .withHeader("Content-Encoding", "gzip")
                                                                   .withHeader("x-amz-crc32", JSON_BODY_GZIP_CRC32_CHECKSUM)
                                                                   .withBodyFile(JSON_BODY_GZIP)));
        AmazonProtocolRestJson client = new AmazonProtocolRestJsonCRC32TestClient(FAKE_CREDENTIALS_PROVIDER,
                                                                                  new ClientConfiguration().withGzip(true));
        client.setEndpoint("http://localhost:" + mockServer.port());
        AllTypesResult result =
                client.allTypes(new AllTypesRequest());
        Assert.assertEquals("foo", result.getStringMember());
    }

    @Test(expected = AmazonClientException.class)
    public void clientCalculatesCRC32FromCompressedData_WhenCRC32IsInvalid_ThrowsException() {
        stubFor(post(urlEqualTo(RESOURCE_PATH)).willReturn(aResponse()
                                                                   .withStatus(200)
                                                                   .withHeader("Content-Encoding", "gzip")
                                                                   .withHeader("x-amz-crc32", JSON_BODY_CRC32_CHECKSUM)
                                                                   .withBodyFile(JSON_BODY_GZIP)));
        AmazonProtocolRestJson client = new AmazonProtocolRestJsonCRC32TestClient(FAKE_CREDENTIALS_PROVIDER,
                                                                                  new ClientConfiguration().withGzip(true));
        client.setEndpoint("http://localhost:" + mockServer.port());
        client.allTypes(new AllTypesRequest());
    }

    @Test
    public void clientCalculatesCRC32FromDecompressedData_WhenCRC32IsValid() {
        stubFor(post(urlEqualTo(RESOURCE_PATH)).willReturn(aResponse()
                                                                   .withStatus(200)
                                                                   .withHeader("Content-Encoding", "gzip")
                                                                   .withHeader("x-amz-crc32", JSON_BODY_CRC32_CHECKSUM)
                                                                   .withBodyFile(JSON_BODY_GZIP)));
        AmazonProtocolRestJson client = new AmazonProtocolRestJsonClient(FAKE_CREDENTIALS_PROVIDER,
                                                                         new ClientConfiguration().withGzip(true));
        client.setEndpoint("http://localhost:" + mockServer.port());
        AllTypesResult result =
                client.allTypes(new AllTypesRequest());
        Assert.assertEquals("foo", result.getStringMember());
    }

    @Test(expected = AmazonClientException.class)
    public void clientCalculatesCRC32FromDecompressedData_WhenCRC32IsInvalid_ThrowsException() {
        stubFor(post(urlEqualTo(RESOURCE_PATH)).willReturn(aResponse()
                                                                   .withStatus(200)
                                                                   .withHeader("Content-Encoding", "gzip")
                                                                   .withHeader("x-amz-crc32", JSON_BODY_GZIP_CRC32_CHECKSUM)
                                                                   .withBodyFile(JSON_BODY_GZIP)));
        AmazonProtocolRestJson client = new AmazonProtocolRestJsonClient(FAKE_CREDENTIALS_PROVIDER,
                                                                         new ClientConfiguration().withGzip(true));
        client.setEndpoint("http://localhost:" + mockServer.port());
        client.allTypes(new AllTypesRequest());
    }

    @Test
    public void useGzipFalse_WhenCRC32IsValid() {
        stubFor(post(urlEqualTo(RESOURCE_PATH)).willReturn(aResponse()
                                                                   .withStatus(200)
                                                                   .withHeader("x-amz-crc32", JSON_BODY_CRC32_CHECKSUM)
                                                                   .withBody(JSON_BODY)));
        AmazonProtocolRestJson client = new AmazonProtocolRestJsonClient(FAKE_CREDENTIALS_PROVIDER,
                                                                         new ClientConfiguration().withGzip(false));
        client.setEndpoint("http://localhost:" + mockServer.port());
        AllTypesResult result =
                client.allTypes(new AllTypesRequest());
        Assert.assertEquals("foo", result.getStringMember());
    }

    @Test(expected = AmazonClientException.class)
    public void useGzipFalse_WhenCRC32IsInvalid_ThrowException() {
        stubFor(post(urlEqualTo(RESOURCE_PATH)).willReturn(aResponse()
                                                                   .withStatus(200)
                                                                   .withHeader("x-amz-crc32", JSON_BODY_GZIP_CRC32_CHECKSUM)
                                                                   .withBody(JSON_BODY)));
        AmazonProtocolRestJson client = new AmazonProtocolRestJsonClient(FAKE_CREDENTIALS_PROVIDER,
                                                                         new ClientConfiguration().withGzip(false));
        client.setEndpoint("http://localhost:" + mockServer.port());
        client.allTypes(new AllTypesRequest());
    }

    private static class AmazonProtocolRestJsonCRC32TestClient extends AmazonProtocolRestJsonClient {

        public AmazonProtocolRestJsonCRC32TestClient(AWSCredentialsProvider credentialsProvider, ClientConfiguration config) {
            super(credentialsProvider, config);
        }

        @Override
        public final boolean calculateCRC32FromCompressedData() {
            return true;
        }
    }

}
