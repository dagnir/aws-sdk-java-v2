package software.amazon.awssdk.services.s3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.event.ProgressEventType;
import software.amazon.awssdk.event.ProgressListener.ExceptionReporter;
import software.amazon.awssdk.event.ProgressTracker;
import software.amazon.awssdk.event.SDKProgressPublisher;
import software.amazon.awssdk.event.request.Progress;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.test.util.ProgressListenerWithEventCodeVerification;
import software.amazon.awssdk.test.util.RandomInputStream;

public class S3RequestProgressIntegrationTest extends S3IntegrationTestBase {
    private static final String bucketName = "s3-request-progress-integ-test-"
            + new Date().getTime();
    private static final String KEY = "key";
    private static final long CONTENT_LENGTH = 10L * 1024L * 1024L;

    /** Releases all resources created by this test */
    @After
    public void tearDown() {
        try {s3.deleteObject(bucketName, KEY);} catch (Exception e) {}
        try {s3.deleteBucket(bucketName);} catch (Exception e) {}
    }

    /**
     * Tests that the user-specified progress listener is properly notified with
     * all the request/response progress event code.
     */
    @Test
    public void testProgressEventNotification() throws AmazonServiceException, AmazonClientException, IOException {
        s3.createBucket(bucketName);

        /* PutObject */

        PutObjectRequest putRequest = generatePutObjectRequest(bucketName, KEY, CONTENT_LENGTH);
        ProgressListenerWithEventCodeVerification verifier = new ProgressListenerWithEventCodeVerification(
                ProgressEventType.TRANSFER_STARTED_EVENT,
                ProgressEventType.CLIENT_REQUEST_STARTED_EVENT,
                ProgressEventType.HTTP_REQUEST_STARTED_EVENT,
                ProgressEventType.HTTP_REQUEST_COMPLETED_EVENT,
                ProgressEventType.HTTP_RESPONSE_STARTED_EVENT,
                ProgressEventType.HTTP_RESPONSE_COMPLETED_EVENT,
                ProgressEventType.CLIENT_REQUEST_SUCCESS_EVENT,
                ProgressEventType.TRANSFER_COMPLETED_EVENT);
        ExceptionReporter listener = ExceptionReporter.wrap(verifier);
        putRequest.setGeneralProgressListener(listener);

        s3.putObject(putRequest);

        waitTillListenerCallbacksComplete();

        /* GetObject */

        listener.throwExceptionIfAny();
        verifier.reset();

        GetObjectRequest getRequest = new GetObjectRequest(bucketName, KEY);
        getRequest.setGeneralProgressListener(listener);
        File tmpfile = File.createTempFile("s3-request-progress-integ-test-", "");
        tmpfile.deleteOnExit();
        s3.getObject(getRequest, tmpfile);

        try {
            SDKProgressPublisher.waitTillCompletion();
        } catch (InterruptedException e) {
            Assert.fail("Interrupted when waiting for the progress listener callbacks to return. "
                    + e.getMessage());
        } catch (ExecutionException e) {
            Assert.fail("Error when executing the progress listner callbacks. "
                    + e.getCause().getMessage());
        }
        listener.throwExceptionIfAny();
    }

    @Test
    public void testRequestCycleProgressReporting() throws AmazonServiceException, AmazonClientException, IOException {
        s3.createBucket(bucketName);

        /* PubObject */

        ProgressTracker putTracker = new ProgressTracker();
        PutObjectRequest putRequest = 
                generatePutObjectRequest(bucketName, KEY, CONTENT_LENGTH)
                .withGeneralProgressListener(putTracker)
                ;
        s3.putObject(putRequest);
        Progress putProgress = putTracker.getProgress();

        Assert.assertTrue(putProgress.getRequestContentLength() == CONTENT_LENGTH);
        Assert.assertTrue(putProgress.getRequestBytesTransferred() == CONTENT_LENGTH);
        // No response payload for PutObject
        Assert.assertTrue(putProgress.getResponseContentLength() == -1);
        Assert.assertTrue(putProgress.getResponseBytesTransferred() == 0);


        /* GetObject */

        ProgressTracker getTracker = new ProgressTracker();
        GetObjectRequest getRequest = new GetObjectRequest(bucketName, KEY)
            .withGeneralProgressListener(getTracker)
            ;
        File tmpfile = File.createTempFile("s3-request-progress-integ-test-", "");
        tmpfile.deleteOnExit();
        s3.getObject(getRequest, tmpfile);
        Progress getProgress = getTracker.getProgress();

        // TODO: the runtime layer sets the "TotalBytesInRequest" according to
        // the content-length header value in the request. Most of the S3 APIs
        // using GET/HEAD verb do not have any payload in the request, in which
        // case the content-length is not specified by the s3 client and
        // "TotalBytesInRequest" would therefore remain unknown(-1). To fix
        // this, we probably should explicitly set "Content-Length:0" in such
        // scenario.
        Assert.assertTrue(getProgress.getRequestContentLength() == -1);
        Assert.assertTrue(getProgress.getRequestBytesTransferred() == 0);
        Assert.assertTrue(getProgress.getResponseContentLength() == CONTENT_LENGTH);
        Assert.assertEquals((Long)getProgress.getResponseContentLength(),
                            (Long)getProgress.getResponseBytesTransferred());

    }

    private static PutObjectRequest generatePutObjectRequest(String bucketName, String key, long contentLength) {
        InputStream input = new RandomInputStream(contentLength);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        return new PutObjectRequest(bucketName, key, input, metadata);
    }

    private static void waitTillListenerCallbacksComplete() {
        try {
            SDKProgressPublisher.waitTillCompletion();
        } catch (InterruptedException e) {
            Assert.fail("Interrupted when waiting for the progress listener callbacks to return. "
                    + e.getMessage());
        } catch (ExecutionException e) {
            Assert.fail("Error when executing the progress listner callbacks. "
                    + e.getCause().getMessage());
        }
    }
}
