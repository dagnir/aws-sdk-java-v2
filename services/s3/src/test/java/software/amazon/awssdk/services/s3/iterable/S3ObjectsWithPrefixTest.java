package software.amazon.awssdk.services.s3.iterable;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;


public class S3ObjectsWithPrefixTest extends S3ObjectsTestCommon {

    @Before
    public void setUp() throws Exception {
        s3Objects = S3Objects.withPrefix(s3, "my-bucket", "photos/");
    }

    @Test
    public void testSetsBucket() throws Exception {
        assertEquals("my-bucket", s3Objects.getBucketName());
    }

    @Test
    public void testSetsPrefix() throws Exception {
        assertEquals("photos/", s3Objects.getPrefix());
    }

}
