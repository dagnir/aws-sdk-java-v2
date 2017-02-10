package software.amazon.awssdk.services.s3.iterable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.model.ListVersionsRequest;

public class S3VersionsWithPrefixTest extends S3VersionsTestCommon {

    @Before
    public void setUp() throws Exception {
        s3Versions = S3Versions.withPrefix(s3, "my-bucket", "photos/");
    }

    @Test
    public void testSetsBucket() throws Exception {
        assertEquals("my-bucket", s3Versions.getBucketName());
    }

    @Test
    public void testSetsPrefix() throws Exception {
        assertEquals("photos/", s3Versions.getPrefix());
    }

    @Test
    public void testSetsS3Client() throws Exception {
        assertSame(s3, s3Versions.getS3());
    }

    @Test
    public void testSetsPrefixOnRequest() throws Exception {
        s3Versions.iterator().hasNext();

        ArgumentCaptor<ListVersionsRequest> listCaptor = ArgumentCaptor.forClass(ListVersionsRequest.class);
        verify(s3).listVersions(listCaptor.capture());
        assertEquals("photos/", listCaptor.getValue().getPrefix());
    }

}
