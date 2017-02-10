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

package software.amazon.awssdk.services.s3;

import java.util.HashMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.AmazonWebServiceResponse;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.services.s3.internal.S3ObjectResponseHandler;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.util.ImmutableMapParameter;

public class ContentLanguageUnitTest {

    private static final String CONTENT_LANGUAGE = "en";

    @Mock
    private static HttpResponse httpResponse;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void object_metadata_returns_contentlanguage_when_header_is_set_in_response() {
        Mockito.when(httpResponse.getHeaders()).thenReturn(ImmutableMapParameter.of
                (Headers.CONTENT_LANGUAGE,
                        CONTENT_LANGUAGE));

        try {
            AmazonWebServiceResponse<S3Object> response = new S3ObjectResponseHandler().handle
                    (httpResponse);
            ObjectMetadata metadata = response.getResult().getObjectMetadata();
            Assert.assertNotNull(metadata.getContentLanguage());
            Assert.assertEquals(CONTENT_LANGUAGE, metadata.getContentLanguage());

        } catch (Exception e) {
            Assert.fail("failed to parse object metadata from httpResponse " + e.getMessage());
        }

    }

    @Test
    public void object_metadata_returns_null_for_content_language_when_header_is_not_set_in_response
            () {
        Mockito.when(httpResponse.getHeaders()).thenReturn(new HashMap<String, String>());

        try {
            AmazonWebServiceResponse<S3Object> response = new S3ObjectResponseHandler().handle
                    (httpResponse);
            ObjectMetadata metadata = response.getResult().getObjectMetadata();
            Assert.assertNull(metadata.getContentLanguage());

        } catch (Exception e) {
            Assert.fail("failed to parse object metadata from httpResponse " + e.getMessage());
        }

    }


}
