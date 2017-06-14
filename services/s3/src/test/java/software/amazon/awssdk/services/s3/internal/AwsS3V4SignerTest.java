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

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.auth.internal.SignerConstants.X_AMZ_CONTENT_SHA256;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Random;
import org.junit.Test;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.ResetException;
import software.amazon.awssdk.auth.AwsChunkedEncodingInputStream;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.handlers.AwsHandlerKeys;
import software.amazon.awssdk.http.DefaultSdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.internal.AmazonWebServiceRequestAdapter;
import software.amazon.awssdk.runtime.io.SdkBufferedInputStream;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.request.S3HandlerContextKeys;
import software.amazon.awssdk.test.util.RandomInputStream;
import software.amazon.awssdk.util.StringInputStream;

public class AwsS3V4SignerTest {

    @Test
    public void testGetContentLength_Failure() throws IOException {
        InputStream content = new SdkBufferedInputStream(new RandomInputStream(Constants.getStreamBufferSize() * 2));
        try {
            AwsS3V4Signer.getContentLength(requestBuilder()
                                                   .content(content)
                                                   .httpMethod(SdkHttpMethod.POST));
            fail();
        } catch (ResetException expected) {
            // Expected.
        }
    }

    private DefaultSdkHttpFullRequest.Builder requestBuilder() {
        return DefaultSdkHttpFullRequest
                .builder()
                .handlerContext(AwsHandlerKeys.REQUEST_CONFIG, RequestConfig.NO_OP);
    }

    @Test
    public void testGetContentLength_ExactBufferSize() throws IOException {
        final int size = Constants.getStreamBufferSize();
        long len = AwsS3V4Signer.getContentLength(
                requestBuilder()
                        .content(new SdkBufferedInputStream(new RandomInputStream(size - 1)))
                        .httpMethod(SdkHttpMethod.POST));
        assertTrue("size=" + size + ", len=" + len, len == size - 1);
    }

    @Test
    public void testGetContentLength_InsufficientBufferSize() throws IOException {
        final int size = Constants.getStreamBufferSize();
        try {
            AwsS3V4Signer.getContentLength(requestBuilder()
                                                   .content(new SdkBufferedInputStream(new RandomInputStream(size)))
                                                   .httpMethod(SdkHttpMethod.POST));
            fail();
        } catch (ResetException expected) {
            // Expected.
        }
    }

    @Test
    public void testGetContentLength_RandomBufferSize() throws IOException {
        final int size = new Random().nextInt(Constants.getStreamBufferSize());
        long len = AwsS3V4Signer.getContentLength(
                requestBuilder()
                        .content(new SdkBufferedInputStream(new RandomInputStream(size)))
                        .httpMethod(SdkHttpMethod.POST));
        assertTrue("size=" + size + ", len=" + len, len == size);
    }

    @Test
    public void chunkedEncodingExplicitlyEnabled_WrapsInputStreamForChunkedEncoding() throws
                                                                                      Exception {
        SdkHttpFullRequest req = getMarshalledPutObjectRequest()
                .handlerContext(S3HandlerContextKeys.IS_PAYLOAD_SIGNING_ENABLED, true)
                .handlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED, false)
                .build();
        AwsS3V4Signer signer = new AwsS3V4Signer();
        signer.setServiceName(mockServiceName());
        final SdkHttpFullRequest signed = signer.sign(req, mockCredentials());
        assertThat(signed.getContent(), instanceOf(AwsChunkedEncodingInputStream.class));
    }

    /**
     * If {@link S3HandlerContextKeys#IS_CHUNKED_ENCODING_DISABLED} is not set at all then the
     * default behavior should be to use chunked encoding.
     */
    @Test
    public void chunkedEncodingNotDisabled_WrapsInputStreamForChunkedEncoding() throws Exception {
        SdkHttpFullRequest req = getMarshalledPutObjectRequest()
                .handlerContext(S3HandlerContextKeys.IS_PAYLOAD_SIGNING_ENABLED, Boolean.TRUE)
                .build();
        AwsS3V4Signer signer = new AwsS3V4Signer();
        signer.setServiceName(mockServiceName());
        final SdkHttpFullRequest signed = signer.sign(req, mockCredentials());
        assertThat(signed.getContent(), instanceOf(AwsChunkedEncodingInputStream.class));
    }

    @Test
    public void chunkedEncodingExplicitlyDisabled_DoesNotWrapInputStream() throws Exception {
        SdkHttpFullRequest req = getMarshalledPutObjectRequest()
                .handlerContext(S3HandlerContextKeys.IS_PAYLOAD_SIGNING_ENABLED, Boolean.TRUE)
                .handlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED, Boolean.TRUE)
                .build();
        AwsS3V4Signer signer = new AwsS3V4Signer();
        signer.setServiceName(mockServiceName());
        final SdkHttpFullRequest signed = signer.sign(req, mockCredentials());
        assertThat(signed.getContent(), instanceOf(StringInputStream.class));
    }

    @Test
    public void chunkedEncoding_BodySigningEnabled() throws Exception {
        final String contentSha = "ed7002b439e9ac845f22357d822bac1444730fbdb6016d3ec9432297b9ec9f73";
        SdkHttpFullRequest req = getMarshalledPutObjectRequest()
                .handlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED, false)
                .build();
        AwsS3V4Signer signer = new AwsS3V4Signer();
        signer.setServiceName(mockServiceName());
        final SdkHttpFullRequest signed = signer.sign(req, mockCredentials());
        assertHeaderNotEqual(signed, X_AMZ_CONTENT_SHA256, contentSha);
    }

    @Test
    public void chunkedEncoding_BodySigningExplicitlyEnabled() throws Exception {
        SdkHttpFullRequest req = getMarshalledPutObjectRequest()
                .handlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED, false)
                .handlerContext(S3HandlerContextKeys.IS_PAYLOAD_SIGNING_ENABLED, true)
                .build();
        AwsS3V4Signer signer = new AwsS3V4Signer();
        signer.setServiceName(mockServiceName());
        final SdkHttpFullRequest signed = signer.sign(req, mockCredentials());
        assertHeaderNotEqual(signed, X_AMZ_CONTENT_SHA256, "UNSIGNED-PAYLOAD");
    }

    @Test
    public void chunkedEncodingHttpProtocol_BodySigningEnabled() throws Exception {
        SdkHttpFullRequest req = getMarshalledPutObjectRequest()
                .endpoint(new URI("http://s3.amazonaws.com"))
                .handlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED, false)
                .build();
        AwsS3V4Signer signer = new AwsS3V4Signer();
        signer.setServiceName(mockServiceName());
        final SdkHttpFullRequest signed = signer.sign(req, mockCredentials());
        assertHeaderNotEqual(signed, X_AMZ_CONTENT_SHA256, "UNSIGNED-PAYLOAD");
    }

    @Test
    public void chunkedEncodingDisabled_BodySigningDisabled() throws Exception {
        SdkHttpFullRequest req = getMarshalledPutObjectRequest()
                .handlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED, true)
                .build();
        AwsS3V4Signer signer = new AwsS3V4Signer();
        signer.setServiceName(mockServiceName());
        final SdkHttpFullRequest signed = signer.sign(req, mockCredentials());
        assertHasHeader(signed, X_AMZ_CONTENT_SHA256, "UNSIGNED-PAYLOAD");
    }

    @Test
    public void chunkedEncodingDisabled_BodySigningExplicitlyEnabled() throws Exception {
        SdkHttpFullRequest req = getMarshalledPutObjectRequest()
                .handlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED, true)
                .handlerContext(S3HandlerContextKeys.IS_PAYLOAD_SIGNING_ENABLED, true)
                .build();
        AwsS3V4Signer signer = new AwsS3V4Signer();
        signer.setServiceName(mockServiceName());
        final SdkHttpFullRequest signed = signer.sign(req, mockCredentials());
        assertHeaderNotEqual(signed, X_AMZ_CONTENT_SHA256, "UNSIGNED-PAYLOAD");

    }

    @Test
    public void chunkedEncodingDisabledHttpProtocol_BodySigningEnabled() throws Exception {
        SdkHttpFullRequest req = getMarshalledPutObjectRequest()
                .endpoint(new URI("http://s3.amazonaws.com"))
                .handlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED, true)
                .build();
        AwsS3V4Signer signer = new AwsS3V4Signer();
        signer.setServiceName(mockServiceName());
        final SdkHttpFullRequest signed = signer.sign(req, mockCredentials());
        assertHeaderNotEqual(signed, X_AMZ_CONTENT_SHA256, "UNSIGNED-PAYLOAD");
    }

    private void assertHasHeader(SdkHttpFullRequest request, String headerName, String expectedValue) {
        assertThat(request.getValuesForHeader(headerName), hasItems(expectedValue));
    }

    private void assertHeaderNotEqual(SdkHttpFullRequest request, String headerName, String notEqualToValue) {
        assertThat(request.getValuesForHeader(headerName), not(hasItems(notEqualToValue)));
    }

    private SdkHttpFullRequest.Builder getMarshalledPutObjectRequest() throws Exception {
        StringInputStream content = new StringInputStream("content");
        PutObjectRequest request = new PutObjectRequest("bucket", "key", content, null);
        return requestBuilder()
                .endpoint(new URI("https://s3.amazonaws.com"))
                .content(content)
                .httpMethod(SdkHttpMethod.POST)
                .handlerContext(AwsHandlerKeys.REQUEST_CONFIG, new AmazonWebServiceRequestAdapter(request));
    }

    private AwsCredentials mockCredentials() {
        return new AwsCredentials("akid", "skid");
    }

    private String mockServiceName() {
        return "mockService";
    }
}
