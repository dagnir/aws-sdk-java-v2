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
package software.amazon.awssdk.services.s3;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.GetObjectMetadataRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import utils.http.S3WireMockTestBase;

/**
 * Unit tests for {@link AmazonS3#getObjectMetadata(GetObjectMetadataRequest)}.
 */
public class GetObjectMetadataTest extends S3WireMockTestBase {
    private static final String BUCKET = "test-bucket";

    private AmazonS3 s3;

    @Before
    public void methodSetUp() {
        s3 = buildClient();
    }

    @Test
    public void testRetrieveHeadersCaseInsensitive() {
        final String key = "foo.txt";
        final Map<String,String> headers = new HashMap<String,String>() {{
            put("foo", "1");
            put("bar", "2");
            put("baz", "3");
        }};

        ResponseDefinitionBuilder responseBuilder = new ResponseDefinitionBuilder();
        responseBuilder.withStatus(204);

        for (Map.Entry<String,String> e : headers.entrySet()) {
            responseBuilder.withHeader(e.getKey().toUpperCase(), e.getValue());
        }

        stubFor(head(urlEqualTo(buildPath(key)))
                .willReturn(responseBuilder));

        Map<String,Object> rawMd = s3.getObjectMetadata(BUCKET, key).getRawMetadata();

        for (String h : headers.keySet()) {
            assertThat((String) rawMd.get(h.toLowerCase()), equalTo(headers.get(h)));
        }
    }

    @Test
    public void testGetObjectReplicationStatus() {
        final String expectedStatus = "expected status";
        final String key = "foo.txt";
        stubFor(head(urlEqualTo(buildPath(key)))
                .willReturn(aResponse()
                    .withStatus(204)
                    .withHeader(Headers.OBJECT_REPLICATION_STATUS, expectedStatus)));
        ObjectMetadata md = s3.getObjectMetadata(BUCKET, key);
        assertThat(md.getReplicationStatus(), equalTo(expectedStatus));
    }

    private String buildPath(String objectKey) {
        return "/" + BUCKET + "/" + objectKey;
    }
}
