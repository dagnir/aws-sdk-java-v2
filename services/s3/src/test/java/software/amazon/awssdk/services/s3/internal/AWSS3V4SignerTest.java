package software.amazon.awssdk.services.s3.internal;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.auth.internal.SignerConstants.X_AMZ_CONTENT_SHA256;

import java.io.IOException;
import java.net.URI;
import java.util.Random;
import org.junit.Test;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.ResetException;
import software.amazon.awssdk.auth.AwsChunkedEncodingInputStream;
import software.amazon.awssdk.auth.BasicAWSCredentials;
import software.amazon.awssdk.runtime.io.SdkBufferedInputStream;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.request.S3HandlerContextKeys;
import software.amazon.awssdk.test.util.RandomInputStream;
import software.amazon.awssdk.util.StringInputStream;

public class AWSS3V4SignerTest {

    @Test
    public void testGetContentLength_Failure() throws IOException {
        DefaultRequest<?> req = new DefaultRequest<Object>(AmazonWebServiceRequest.NOOP,
                                                           "testGetContentLength");
        req.setContent(new SdkBufferedInputStream(
                new RandomInputStream(Constants.getStreamBufferSize() * 2)));
        try {
            AWSS3V4Signer.getContentLength(req);
            fail();
        } catch (ResetException expected) {
        }
    }

    @Test
    public void testGetContentLength_ExactBufferSize() throws IOException {
        DefaultRequest<?> req = new DefaultRequest<Object>(AmazonWebServiceRequest.NOOP,
                                                           "testGetContentLength");
        final int size = Constants.getStreamBufferSize();
        req.setContent(new SdkBufferedInputStream(new RandomInputStream(size - 1)));
        long len = AWSS3V4Signer.getContentLength(req);
        assertTrue("size=" + size + ", len=" + len, len == size - 1);
    }

    @Test
    public void testGetContentLength_InsufficientBufferSize() throws IOException {
        DefaultRequest<?> req = new DefaultRequest<Object>(AmazonWebServiceRequest.NOOP,
                                                           "testGetContentLength");
        final int size = Constants.getStreamBufferSize();
        req.setContent(new SdkBufferedInputStream(new RandomInputStream(size)));
        try {
            AWSS3V4Signer.getContentLength(req);
            fail();
        } catch (ResetException expected) {
        }
    }

    @Test
    public void testGetContentLength_RandomBufferSize() throws IOException {
        DefaultRequest<?> req = new DefaultRequest<Object>(AmazonWebServiceRequest.NOOP,
                                                           "testGetContentLength");
        final int size = new Random().nextInt(Constants.getStreamBufferSize());
        req.setContent(new SdkBufferedInputStream(new RandomInputStream(size)));
        long len = AWSS3V4Signer.getContentLength(req);
        assertTrue("size=" + size + ", len=" + len, len == size);
    }

    @Test
    public void chunkedEncodingExplicitlyEnabled_WrapsInputStreamForChunkedEncoding() throws
                                                                                      Exception {
        DefaultRequest<?> req = getMarshalledPutObjectRequest();
        req.addHandlerContext(S3HandlerContextKeys.IS_PAYLOAD_SIGNING_ENABLED, true);
        req.addHandlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED, false);
        AWSS3V4Signer signer = new AWSS3V4Signer();
        signer.setServiceName(mockServiceName());
        signer.sign(req, mockCredentials());
        assertThat(req.getContent(), instanceOf(AwsChunkedEncodingInputStream.class));
    }

    /**
     * If {@link S3HandlerContextKeys#IS_CHUNKED_ENCODING_DISABLED} is not set at all then the
     * default behavior should be to use chunked encoding
     */
    @Test
    public void chunkedEncodingNotDisabled_WrapsInputStreamForChunkedEncoding() throws Exception {
        DefaultRequest<?> req = getMarshalledPutObjectRequest();
        req.addHandlerContext(S3HandlerContextKeys.IS_PAYLOAD_SIGNING_ENABLED, Boolean.TRUE);
        AWSS3V4Signer signer = new AWSS3V4Signer();
        signer.setServiceName(mockServiceName());
        signer.sign(req, mockCredentials());
        assertThat(req.getContent(), instanceOf(AwsChunkedEncodingInputStream.class));
    }

    @Test
    public void chunkedEncodingExplicitlyDisabled_DoesNotWrapInputStream() throws Exception {
        DefaultRequest<?> req = getMarshalledPutObjectRequest();
        req.addHandlerContext(S3HandlerContextKeys.IS_PAYLOAD_SIGNING_ENABLED, Boolean.TRUE);
        req.addHandlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED, Boolean.TRUE);
        AWSS3V4Signer signer = new AWSS3V4Signer();
        signer.setServiceName(mockServiceName());
        signer.sign(req, mockCredentials());
        assertThat(req.getContent(), instanceOf(StringInputStream.class));
    }

    @Test
    public void chunkedEncoding_BodySigningEnabled() throws Exception {
        DefaultRequest<?> req = getMarshalledPutObjectRequest();
        final String contentSha = "ed7002b439e9ac845f22357d822bac1444730fbdb6016d3ec9432297b9ec9f73";
        req.addHandlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED, false);
        AWSS3V4Signer signer = new AWSS3V4Signer();
        signer.setServiceName(mockServiceName());
        signer.sign(req, mockCredentials());
        assertNotEquals(req.getHeaders().get(X_AMZ_CONTENT_SHA256), contentSha);
    }

    @Test
    public void chunkedEncoding_BodySigningExplicitlyEnabled() throws Exception {
        DefaultRequest<?> req = getMarshalledPutObjectRequest();
        req.addHandlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED, false);
        req.addHandlerContext(S3HandlerContextKeys.IS_PAYLOAD_SIGNING_ENABLED, true);
        AWSS3V4Signer signer = new AWSS3V4Signer();
        signer.setServiceName(mockServiceName());
        signer.sign(req, mockCredentials());
        assertNotEquals(req.getHeaders().get(X_AMZ_CONTENT_SHA256), "UNSIGNED-PAYLOAD");
    }

    @Test
    public void chunkedEncodingHttpProtocol_BodySigningEnabled() throws Exception {
        DefaultRequest<?> req = getMarshalledPutObjectRequest();
        req.setEndpoint(new URI("http://s3.amazonaws.com"));
        req.addHandlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED, false);
        AWSS3V4Signer signer = new AWSS3V4Signer();
        signer.setServiceName(mockServiceName());
        signer.sign(req, mockCredentials());
        assertNotEquals(req.getHeaders().get(X_AMZ_CONTENT_SHA256), "UNSIGNED-PAYLOAD");
    }

    @Test
    public void chunkedEncodingDisabled_BodySigningDisabled() throws Exception {
        DefaultRequest<?> req = getMarshalledPutObjectRequest();
        req.addHandlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED, true);
        AWSS3V4Signer signer = new AWSS3V4Signer();
        signer.setServiceName(mockServiceName());
        signer.sign(req, mockCredentials());
        assertEquals(req.getHeaders().get(X_AMZ_CONTENT_SHA256), "UNSIGNED-PAYLOAD");
    }

    @Test
    public void chunkedEncodingDisabled_BodySigningExplicitlyEnabled() throws Exception {
        DefaultRequest<?> req = getMarshalledPutObjectRequest();
        req.addHandlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED, true);
        req.addHandlerContext(S3HandlerContextKeys.IS_PAYLOAD_SIGNING_ENABLED, true);
        AWSS3V4Signer signer = new AWSS3V4Signer();
        signer.setServiceName(mockServiceName());
        signer.sign(req, mockCredentials());
        assertNotEquals(req.getHeaders().get(X_AMZ_CONTENT_SHA256), "UNSIGNED-PAYLOAD");
    }

    @Test
    public void chunkedEncodingDisabledHttpProtocol_BodySigningEnabled() throws Exception {
        DefaultRequest<?> req = getMarshalledPutObjectRequest();
        req.setEndpoint(new URI("http://s3.amazonaws.com"));
        req.addHandlerContext(S3HandlerContextKeys.IS_CHUNKED_ENCODING_DISABLED, true);
        AWSS3V4Signer signer = new AWSS3V4Signer();
        signer.setServiceName(mockServiceName());
        signer.sign(req, mockCredentials());
        assertNotEquals(req.getHeaders().get(X_AMZ_CONTENT_SHA256), "UNSIGNED-PAYLOAD");
    }

    private DefaultRequest<?> getMarshalledPutObjectRequest() throws Exception {
        StringInputStream content = new StringInputStream("content");
        PutObjectRequest putObjectRequest = new PutObjectRequest("some-bucket", "some-key", content,
                                                                 null);
        DefaultRequest<?> marshalledRequest = new DefaultRequest<Object>(putObjectRequest,
                                                                         AmazonS3Client.S3_SERVICE_NAME);
        marshalledRequest.setEndpoint(new URI("https://s3.amazonaws.com"));
        marshalledRequest.setContent(content);
        return marshalledRequest;
    }

    private BasicAWSCredentials mockCredentials() {
        return new BasicAWSCredentials("akid", "skid");
    }

    private String mockServiceName() {
        return "mockService";
    }
}
