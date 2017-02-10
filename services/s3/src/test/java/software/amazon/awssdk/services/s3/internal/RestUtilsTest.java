package software.amazon.awssdk.services.s3.internal;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.Request;

public class RestUtilsTest {
    @Test
    public void testNullHeaderValue() {
        Request<?> request = new DefaultRequest<Object>("s3");
        request.addHeader("x-amz-test", null);

        String canonicalString = RestUtils.makeS3CanonicalString(
                "PUT", "/bucket/key", request, null);

        Assert.assertEquals(
                "PUT\n\n\nx-amz-test:\n/bucket/key",
                canonicalString);
    }
}
