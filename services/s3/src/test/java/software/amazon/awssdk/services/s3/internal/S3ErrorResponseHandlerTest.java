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

package software.amazon.awssdk.services.s3.internal;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.HttpResponseHandler;
import software.amazon.awssdk.util.StringInputStream;

public class S3ErrorResponseHandlerTest {

    private HttpResponse httpResponse;
    private S3ErrorResponseHandler errorResponseHandler;

    @Before
    public void setup() {
        httpResponse = new HttpResponse(new DefaultRequest<String>("s3"), null);
        errorResponseHandler = new S3ErrorResponseHandler();
    }

    @Test
    public void handle_NoContent_AllHeadersDumpedIntoHeaderMap() throws Exception {
        stubHeaders();
        AmazonServiceException ase = errorResponseHandler.handle(httpResponse);
        verifyHeaders(ase);
    }

    @Test
    public void handle_MalformedContent_AllHeadersDumpedIntoHeaderMap() throws Exception {
        httpResponse.setContent(new StringInputStream("lasdjflds;"));
        stubHeaders();
        AmazonServiceException ase = errorResponseHandler.handle(httpResponse);
        verifyHeaders(ase);
    }

    @Test
    public void handle_WellFormedContent_AllHeadersDumpedIntoHeaderMap() throws Exception {
        httpResponse.setContent(new StringInputStream(
                "<Error><Code>NoSuchKey</Code><Message>Foo msg</Message></Error>"));
        stubHeaders();
        AmazonServiceException ase = errorResponseHandler.handle(httpResponse);
        verifyHeaders(ase);

    }

    /**
     * Stub headers returned by service. Uses a well known header and a custom one.
     */
    private void stubHeaders() {
        httpResponse.addHeader(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER, "1234");
        httpResponse.addHeader("FooHeader", "FooValue");
    }

    /**
     * Verify that the stubbed headers were preserved in the header map.
     */
    private void verifyHeaders(AmazonServiceException ase) {
        assertThat(ase.getHttpHeaders(),
                   hasEntry(HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER, "1234"));
        assertThat(ase.getHttpHeaders(), hasEntry("FooHeader", "FooValue"));
    }

}