package software.amazon.awssdk.services.s3.transfer;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.model.GetObjectMetadataRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;

/**
 * Unit tests for {@link TransferManager} pause and resume functionality.
 */
public class TransferManagerPauseAndResumeTest {
    private static final String BUCKET = "bucket";
    private static final String KEY = "key";

    private AmazonS3 s3;
    private TransferManager tm;

    @Before
    public void testSetUp() {
        s3 = mock(AmazonS3.class);
        tm = new TransferManager(s3);
    }

    @Test
    public void testSetsIfUnmodifiedSinceHeaderSinglePart() throws InterruptedException, IOException {
        final Date modifiedTime = new Date(12345L);
        ObjectMetadata md = new ObjectMetadata();
        md.setLastModified(modifiedTime);

        when(s3.getObjectMetadata(any(GetObjectMetadataRequest.class))).thenReturn(md);

        PersistableDownload dl = new PersistableDownload(BUCKET, KEY, "version", null, null, false,
                "test", null, modifiedTime.getTime());
        tm.resumeDownload(dl).waitForCompletion();

        ArgumentCaptor<GetObjectRequest> getRequest = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3).getObject(getRequest.capture());

        Date unmodifiedSinceConstraint = getRequest.getValue().getUnmodifiedSinceConstraint();
        assertThat(unmodifiedSinceConstraint, is(equalTo(modifiedTime)));
    }
}
