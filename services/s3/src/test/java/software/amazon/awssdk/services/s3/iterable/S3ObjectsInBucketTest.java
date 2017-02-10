package software.amazon.awssdk.services.s3.iterable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;



public class S3ObjectsInBucketTest extends S3ObjectsTestCommon {

    @Before
    public void setUp() throws Exception {
        s3Objects = S3Objects.inBucket(s3, "my-bucket");
    }

    @Test
    public void testStoresBucketName() throws Exception {
        assertEquals("my-bucket", s3Objects.getBucketName());
    }

    @Test
    public void testUsesNullPrefix() throws Exception {
        assertNull(s3Objects.getPrefix());
    }

}
