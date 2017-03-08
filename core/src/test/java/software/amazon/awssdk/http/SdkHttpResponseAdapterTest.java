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

package software.amazon.awssdk.http;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.internal.http.settings.HttpClientSettings;
import software.amazon.awssdk.util.Crc32ChecksumValidatingInputStream;
import software.amazon.awssdk.util.StringInputStream;

@RunWith(MockitoJUnitRunner.class)
public class SdkHttpResponseAdapterTest {

    private final Request<Void> request = new DefaultRequest<>("foo");

    @Mock
    private HttpClientSettings httpSettings;

    @Test
    public void adapt_SingleHeaderValue_AdaptedCorrectly() throws Exception {
        SimpleSdkHttpResponse httpResponse = SimpleSdkHttpResponse.builder()
                .header("FooHeader", "headerValue")
                .build();

        HttpResponse adapted = adapt(httpResponse);

        assertThat(adapted.getHeader("FooHeader"), equalTo("headerValue"));
    }

    @Test
    public void adapt_StatusTextAndStatusCode_AdaptedCorrectly() throws Exception {
        SimpleSdkHttpResponse httpResponse = SimpleSdkHttpResponse.builder()
                .statusText("OK")
                .statusCode(200)
                .build();

        HttpResponse adapted = adapt(httpResponse);

        assertThat(adapted.getStatusText(), equalTo("OK"));
        assertThat(adapted.getStatusCode(), equalTo(200));
    }

    @Test
    public void adapt_InputStreamWithNoGzipOrCrc32_NotWrappedWhenAdapted() throws UnsupportedEncodingException {
        InputStream content = new StringInputStream("content");
        SimpleSdkHttpResponse httpResponse = SimpleSdkHttpResponse.builder()
                .content(content)
                .build();

        HttpResponse adapted = adapt(httpResponse);

        assertThat(adapted.getContent(), equalTo(content));
    }

    @Test
    public void adapt_InputStreamWithCrc32Header_WrappedWithValidatingStream() throws UnsupportedEncodingException {
        InputStream content = new StringInputStream("content");
        SimpleSdkHttpResponse httpResponse = SimpleSdkHttpResponse.builder()
                .header("x-amz-crc32", "1234")
                .content(content)
                .build();

        HttpResponse adapted = adapt(httpResponse);

        assertThat(adapted.getContent(), instanceOf(Crc32ChecksumValidatingInputStream.class));
    }

    @Test
    public void adapt_InputStreamWithGzipEncoding_WrappedWithDecompressingStream() throws UnsupportedEncodingException {
        InputStream content = getClass().getResourceAsStream("/resources/compressed_json_body.gz");
        SimpleSdkHttpResponse httpResponse = SimpleSdkHttpResponse.builder()
                .header("Content-Encoding", "gzip")
                .content(content)
                .build();

        HttpResponse adapted = adapt(httpResponse);

        assertThat(adapted.getContent(), instanceOf(GZIPInputStream.class));
    }

    @Test
    public void adapt_CalculateCrcFromCompressed_WrapsWithCrc32ThenGzip() throws UnsupportedEncodingException {
        InputStream content = getClass().getResourceAsStream("/resources/compressed_json_body.gz");
        SimpleSdkHttpResponse httpResponse = SimpleSdkHttpResponse.builder()
                .header("Content-Encoding", "gzip")
                .header("x-amz-crc32", "1234")
                .content(content)
                .build();

        when(httpSettings.calculateCRC32FromCompressedData()).thenReturn(true);
        HttpResponse adapted = adapt(httpResponse);

        assertThat(adapted.getContent(), instanceOf(GZIPInputStream.class));
    }

    @Test(expected = UncheckedIOException.class)
    public void adapt_InvalidGzipContent_ThrowsException() throws UnsupportedEncodingException {
        InputStream content = new StringInputStream("this isn't GZIP");
        SimpleSdkHttpResponse httpResponse = SimpleSdkHttpResponse.builder()
                .header("Content-Encoding", "gzip")
                .content(content)
                .build();

        HttpResponse adapted = adapt(httpResponse);

        assertThat(adapted.getContent(), instanceOf(GZIPInputStream.class));
    }

    private HttpResponse adapt(SimpleSdkHttpResponse httpResponse) {
        return SdkHttpResponseAdapter.adapt(httpSettings, request, httpResponse);
    }

    public static class SimpleSdkHttpResponse implements SdkHttpResponse {

        private final Map<String, List<String>> headers;
        private final InputStream content;
        private final String statusText;
        private final int statusCode;

        private SimpleSdkHttpResponse(Builder builder) {
            this.headers = builder.headers;
            this.content = builder.content;
            this.statusText = builder.statusText;
            this.statusCode = builder.statusCode;
        }

        @Override
        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        @Override
        public List<String> getHeaderValues(String headerName) {
            return headers.get(headerName);
        }

        @Override
        public InputStream getContent() {
            return content;
        }

        @Override
        public String getStatusText() {
            return statusText;
        }

        @Override
        public int getStatusCode() {
            return statusCode;
        }


        /**
         * @return Builder instance to construct a {@link SimpleSdkHttpResponse}.
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for a {@link SimpleSdkHttpResponse}.
         */
        public static final class Builder {

            private final Map<String, List<String>> headers = new HashMap<>();
            private InputStream content;
            private String statusText;
            private int statusCode;

            private Builder() {
            }

            public Builder header(String headerName, String... values) {
                this.headers.put(headerName, Arrays.asList(values));
                return this;
            }

            public Builder content(InputStream content) {
                this.content = content;
                return this;
            }

            public Builder statusText(String statusText) {
                this.statusText = statusText;
                return this;
            }

            public Builder statusCode(int statusCode) {
                this.statusCode = statusCode;
                return this;
            }

            /**
             * @return An immutable {@link SimpleSdkHttpResponse} object.
             */
            public SimpleSdkHttpResponse build() {
                return new SimpleSdkHttpResponse(this);
            }
        }
    }

}
