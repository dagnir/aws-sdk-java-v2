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

package software.amazon.awssdk.services.s3;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToXml;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.GlacierJobParameters;
import software.amazon.awssdk.services.s3.model.RestoreObjectRequest;
import software.amazon.awssdk.services.s3.model.Tier;
import utils.http.S3WireMockTestBase;


public class RestoreObjectTest extends S3WireMockTestBase {

    private static final String BUCKET_NAME = "some-bucket";

    private static final String KEY = "some-key";

    private AmazonS3 s3;

    @Before
    public void setUp() throws Exception {
        s3 = buildClient();
    }

    @Test
    public void restoreObjectWithTier() {
        try {
            s3.restoreObject(new RestoreObjectRequest(BUCKET_NAME, KEY, 1)
                                     .withGlacierJobParameters(new GlacierJobParameters().withTier(Tier.Bulk)));
        } catch (Exception expected) {
        }

        verify(postRequestedFor(urlEqualTo(String.format("/%s/%s?restore", BUCKET_NAME, KEY)))
                       .withRequestBody(equalToXml(getExpectedMarshalledXml("RestoreObjectWithTier.xml"))));
    }

}
