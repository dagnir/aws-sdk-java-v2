package software.amazon.awssdk.services.s3.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Test;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestClientOptions;
import software.amazon.awssdk.RequestClientOptions.Marker;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.BasicAWSCredentials;
import software.amazon.awssdk.event.ProgressEvent;
import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.event.SyncProgressListener;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.services.s3.UploadObjectObserver;
import software.amazon.awssdk.services.s3.internal.MultiFileOutputStream;

public class PutObjectRequestTest {

    @Test
    public void clonePutObjectRequest() {
        File file = new File("somefile");
        doTestClone(new PutObjectRequest("bucket", "key", file), "bucket", "key", file);
    }

    @Test
    public void cloneEncryptedPutObjectRequest() {
        File file = new File("somefile");
        // null material description
        EncryptedPutObjectRequest clone = (EncryptedPutObjectRequest) doTestClone(
                new EncryptedPutObjectRequest("bucket", "key", file),
                "bucket", "key", file);
        assertNull(clone.getMaterialsDescription());

        // non-null material description
        Map<String,String> md = new HashMap<String, String>();
        md.put("foo", "bar");
        md = Collections.unmodifiableMap(md);
        clone = (EncryptedPutObjectRequest) doTestClone(
                new EncryptedPutObjectRequest("bucket", "key", file).withMaterialsDescription(md),
                "bucket", "key", file);
        assertEquals(clone.getMaterialsDescription(), md);
        assertNotSame(clone.getMaterialsDescription(), md);
    }

    @Test
    public void cloneUploadObjectRequest() {
        File file = new File("somefile");
        // null material description
        UploadObjectRequest clone = (UploadObjectRequest) doTestClone(
                new UploadObjectRequest("bucket", "key", file),
                "bucket", "key", file);
        assertNull(clone.getMaterialsDescription());

        // non-null material description
        Map<String,String> md = new HashMap<String, String>();
        md.put("foo", "bar");
        md = Collections.unmodifiableMap(md);
        final ExecutorService es = Executors.newSingleThreadExecutor();
        final MultiFileOutputStream multiFileOutputStream = new MultiFileOutputStream();
        final UploadObjectObserver uploadObjectObserver = new UploadObjectObserver();

        final UploadObjectRequest from = new UploadObjectRequest("bucket", "key", file)
            .withMaterialsDescription(md)
            .withDiskLimit(1 << 30)
            .withPartSize(10 << 20)
            .withExecutorService(es)
            .withMultiFileOutputStream(multiFileOutputStream)
            .withUploadObjectObserver(uploadObjectObserver)
            ;
        clone = (UploadObjectRequest) doTestClone(from, "bucket", "key", file);
        assertEquals(clone.getMaterialsDescription(), md);
        assertNotSame(clone.getMaterialsDescription(), md);
        assertTrue(clone.getDiskLimit() == (1 << 30));
        assertTrue(clone.getPartSize() == (10 << 20));
        assertSame(es, clone.getExecutorService());
        assertSame(multiFileOutputStream, clone.getMultiFileOutputStream());
        assertSame(uploadObjectObserver, clone.getUploadObjectObserver());
    }

    /**
     * Returns the cloned object.
     */
    private AbstractPutObjectRequest doTestClone(AbstractPutObjectRequest from, String bucket, String key, File file) {
        assertNull(from.getAccessControlList());
        assertEquals("bucket", from.getBucketName());
        assertNull(from.getCannedAcl());
        assertEquals(file, from.getFile());
        assertNull(from.getInputStream());
        assertNull(from.getRedirectLocation());
        assertNull(from.getStorageClass());
        verifyBaseBeforeCopy(from);

        // Fill it up - the base
        final ProgressListener listener = new SyncProgressListener() {
            @Override
            public void progressChanged(ProgressEvent progressEvent) {
            }
        };
        final AWSCredentials credentials = new BasicAWSCredentials("accesskey",
                "accessid");
        final RequestMetricCollector collector = new RequestMetricCollector() {
            @Override
            public void collectMetrics(Request<?> request, Response<?> response) {
            }
        };

        from.setGeneralProgressListener(listener);
        from.setRequestCredentials(credentials);
        from.setRequestMetricCollector(collector);
        from.putCustomRequestHeader("k1", "v1");
        from.putCustomRequestHeader("k2", "v2");
        from.getRequestClientOptions().setReadLimit(1234);

        AccessControlList accessControlList = new AccessControlList();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.addUserMetadata("um_k1", "um_v1");
        metadata.addUserMetadata("um_k2", "um_v2");
        SSECustomerKey sseKey = new SSECustomerKey(new byte[32]);

        // Fill it up - the rest
        from.setAccessControlList(accessControlList);
        from.setCannedAcl(CannedAccessControlList.Private);
        from.setInputStream(System.in);
        from.setMetadata(metadata);
        from.setSSECustomerKey(sseKey);
        from.setRedirectLocation("redirectLocation");
        from.setStorageClass(StorageClass.Standard);

        assertSame(metadata, from.getMetadata());

        // Clone it
        AbstractPutObjectRequest to = from.clone();
        verifyBaseAfterCopy(listener, credentials, collector, from, to);

        // Verify the rest
        assertSame(accessControlList, to.getAccessControlList());
        assertSame(CannedAccessControlList.Private, to.getCannedAcl());
        assertSame(System.in, to.getInputStream());

        assertNotSame(from.getMetadata(), to.getMetadata());
        ObjectMetadata toOMD = to.getMetadata();
        assertEquals("um_v1", toOMD.getUserMetaDataOf("um_k1"));
        assertEquals("um_v2", toOMD.getUserMetaDataOf("um_k2"));

        assertSame(sseKey, to.getSSECustomerKey());
        assertEquals("redirectLocation", to.getRedirectLocation());
        assertEquals(StorageClass.Standard.toString(), to.getStorageClass());

        return to;
    }

    public static void verifyBaseBeforeCopy(final AmazonWebServiceRequest to) {
        assertNull(to.getCustomRequestHeaders());
        assertSame(ProgressListener.NOOP, to.getGeneralProgressListener());
        assertNull(to.getRequestCredentials());
        assertNull(to.getRequestMetricCollector());

        assertTrue(RequestClientOptions.DEFAULT_STREAM_BUFFER_SIZE == to
                .getReadLimit());
        RequestClientOptions toOptions = to.getRequestClientOptions();
        assertNull(toOptions.getClientMarker(Marker.USER_AGENT));
        assertTrue(RequestClientOptions.DEFAULT_STREAM_BUFFER_SIZE == toOptions
                .getReadLimit());
    }

    private static void verifyBaseAfterCopy(final ProgressListener listener,
            final AWSCredentials credentials,
            final RequestMetricCollector collector,
            final AmazonWebServiceRequest from, final AmazonWebServiceRequest to) {
        RequestClientOptions toOptions;
        Map<String, String> headers = to.getCustomRequestHeaders();
        assertTrue(2 == headers.size());
        assertEquals("v1", headers.get("k1"));
        assertEquals("v2", headers.get("k2"));
        assertSame(listener, to.getGeneralProgressListener());
        assertSame(credentials, to.getRequestCredentials());
        assertSame(collector, to.getRequestMetricCollector());

        assertTrue(1234 == to.getReadLimit());
        toOptions = to.getRequestClientOptions();
        assertEquals(
                from.getRequestClientOptions().getClientMarker(
                        Marker.USER_AGENT),
                toOptions.getClientMarker(Marker.USER_AGENT));
        assertTrue(1234 == toOptions.getReadLimit());
    }
}
