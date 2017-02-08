package software.amazon.awssdk.services.s3;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.internal.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.internal.SkipMd5CheckStrategy;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.test.AWSIntegrationTestBase;
import software.amazon.awssdk.util.IOUtils;
import software.amazon.awssdk.util.StringInputStream;

public class SkipMd5CheckIntegrationTest extends AWSIntegrationTestBase {

    private static final String BUCKET = "skip-md5-check-integ-" + System.currentTimeMillis();
    private static final String GET_KEY = "some-key";
    private static final String PUT_KEY = "some-other-key";

    private static AmazonS3Client normalS3;
    private static AmazonS3Client md5TamperingS3;

    private static SkipMd5CheckStrategy mockStrategy;

    @BeforeClass
    public static void setupFixture()
            throws AmazonServiceException, AmazonClientException, UnsupportedEncodingException {
        mockStrategy = mock(SkipMd5CheckStrategy.class);
        normalS3 = new AmazonS3Client(getCredentials());
        normalS3.createBucket(BUCKET);
        normalS3.putObject(new PutObjectRequest(BUCKET, GET_KEY, getContent(), null));

        md5TamperingS3 = new AmazonS3Client(new StaticCredentialsProvider(getCredentials()), new ClientConfiguration(),
                null, mockStrategy);
        md5TamperingS3.addRequestHandler(new RequestHandler2() {
            @Override
            public HttpResponse beforeUnmarshalling(Request<?> request, HttpResponse httpResponse) {
                httpResponse.addHeader(Headers.ETAG, "faf42cafe0581aa18e4ea78a910b6d2b");
                return httpResponse;
            }
        });
    }

    @AfterClass
    public static void tearDown() {
        CryptoTestUtils.deleteBucketAndAllContents(normalS3, BUCKET);
    }

    @Before
    public void setup() {
        // Mock strategy is shared between tests so we need to reset to avoid interferring with
        // other tests
        reset(mockStrategy);
    }

    @Test
    public void getObject_Md5ValidationEnabled_ThrowsExceptionWhenMd5IsInvalid() throws IOException {
        GetObjectRequest getObjectRequest = new GetObjectRequest(BUCKET, GET_KEY);
        when(mockStrategy.skipClientSideValidation(eq(getObjectRequest), any(ObjectMetadata.class))).thenReturn(false);
        try {
            getMd5TamperedObject(getObjectRequest);
            fail();
        } catch (AmazonClientException expected) {
            assertIsMd5Exception(expected);
        }
    }

    @Test
    public void getObject_Md5ValidationDisabled_DoesNotThrowExceptionWhenMd5IsInvalid() throws IOException {
        GetObjectRequest getObjectRequest = new GetObjectRequest(BUCKET, GET_KEY);
        when(mockStrategy.skipClientSideValidation(eq(getObjectRequest), any(ObjectMetadata.class))).thenReturn(true);
        getMd5TamperedObject(getObjectRequest);
    }

    @Test
    public void putObject_Md5ValidationEnabled_ThrowsExceptionWhenMd5IsInvalid() throws UnsupportedEncodingException {
        PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET, PUT_KEY, getContent(), null);
        when(mockStrategy.skipClientSideValidationPerRequest(putObjectRequest)).thenReturn(false);
        try {
            md5TamperingS3.putObject(putObjectRequest);
            fail();
        } catch (AmazonClientException expected) {
            assertIsMd5Exception(expected);
        }
    }

    @Test
    public void putObject_Md5ValidationDisabled_DoesNotThrowExceptionWhenMd5IsInvalid()
            throws UnsupportedEncodingException {
        PutObjectRequest putObjectRequest = new PutObjectRequest(BUCKET, PUT_KEY, getContent(), null);
        when(mockStrategy.skipClientSideValidationPerRequest(putObjectRequest)).thenReturn(true);
        md5TamperingS3.putObject(putObjectRequest);
    }

    @Test
    public void uploadPart_Md5ValidationEnabled_ThrowsExceptionWhenMd5IsInvalid() throws UnsupportedEncodingException {
        UploadPartRequest uploadPartRequest = getUploadPartRequest();
        when(mockStrategy.skipClientSideValidationPerRequest(uploadPartRequest)).thenReturn(false);
        try {
            md5TamperingS3.uploadPart(uploadPartRequest);
        } catch (AmazonClientException expected) {
            assertIsMd5Exception(expected);
        }
    }

    @Test
    public void uploadPart_Md5ValidationDisabled_DoesNotThrowExceptionWhenMd5IsInvalid()
            throws UnsupportedEncodingException {
        UploadPartRequest uploadPartRequest = getUploadPartRequest();
        when(mockStrategy.skipClientSideValidationPerRequest(uploadPartRequest)).thenReturn(true);
        md5TamperingS3.uploadPart(uploadPartRequest);
    }

    @Test
    public void uploadPart_Md5ValidationDisabledPerResponse_DoesNotThrowExceptionWhenMd5IsInvalid()
            throws UnsupportedEncodingException {
        UploadPartRequest uploadPartRequest = getUploadPartRequest();
        when(mockStrategy.skipClientSideValidationPerUploadPartResponse(any(ObjectMetadata.class))).thenReturn(true);
        md5TamperingS3.uploadPart(uploadPartRequest);
    }

    @Test
    public void uploadPart_Md5ValidationDisabledPerRequestAndResponse_DoesNotThrowExceptionWhenMd5IsInvalid()
            throws UnsupportedEncodingException {
        UploadPartRequest uploadPartRequest = getUploadPartRequest();
        when(mockStrategy.skipClientSideValidationPerRequest(uploadPartRequest)).thenReturn(true);
        when(mockStrategy.skipClientSideValidationPerUploadPartResponse(any(ObjectMetadata.class))).thenReturn(true);
        md5TamperingS3.uploadPart(uploadPartRequest);
    }

    /**
     * Assert the exception is because of an MD5 validation failure
     */
    private void assertIsMd5Exception(AmazonClientException expected) {
        assertThat(expected.getMessage(), containsString("Unable to verify integrity of data"));
    }

    private static StringInputStream getContent() throws UnsupportedEncodingException {
        return new StringInputStream("content");
    }

    /**
     * Fetch the object from S3 using the MD5 tampering client and read to the end of the stream to
     * trigger (or not trigger) the MD5 validation
     */
    private void getMd5TamperedObject(GetObjectRequest getObjectRequest) throws IOException {
        S3Object s3Object = md5TamperingS3.getObject(getObjectRequest);
        // MD5 validation is triggered when content is read
        IOUtils.toString(s3Object.getObjectContent());
    }

    /**
     * Start a multipart upload and return an {@link UploadPartRequest} for the one and only part
     */
    private UploadPartRequest getUploadPartRequest() throws UnsupportedEncodingException {
        String uploadId = startMultipartUpload();
        InputStream content = getContent();
        UploadPartRequest uploadPartRequest = new UploadPartRequest().withBucketName(BUCKET).withKey(PUT_KEY)
                .withUploadId(uploadId).withPartNumber(1).withInputStream(content).withLastPart(true);
        return uploadPartRequest;
    }

    /**
     * Start a multipart upload
     * 
     * @return The UploadId
     */
    private String startMultipartUpload() {
        return normalS3.initiateMultipartUpload(new InitiateMultipartUploadRequest(BUCKET, PUT_KEY)).getUploadId();
    }
}
