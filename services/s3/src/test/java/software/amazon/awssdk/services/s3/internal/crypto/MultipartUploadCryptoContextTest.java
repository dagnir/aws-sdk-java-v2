package software.amazon.awssdk.services.s3.internal.crypto;

import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;

public class MultipartUploadCryptoContextTest {

    @Test(expected=IllegalArgumentException.class)
    public void testBadPartNumber() {
        MultipartUploadCryptoContext ctx = new MultipartUploadCryptoContext(
                "bucket", "key", null);
        ctx.beginPartUpload(-1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testBadPartNumber0() {
        MultipartUploadCryptoContext ctx = new MultipartUploadCryptoContext(
                "bucket", "key", null);
        ctx.beginPartUpload(0);
    }

    @Test(expected=AmazonClientException.class)
    public void testBadRetry() {
        MultipartUploadCryptoContext ctx = new MultipartUploadCryptoContext(
                "bucket", "key", null);
        ctx.beginPartUpload(1);
        ctx.beginPartUpload(1);
    }

    @Test
    public void testRetry() {
        MultipartUploadCryptoContext ctx = new MultipartUploadCryptoContext(
                "bucket", "key", null);
        ctx.beginPartUpload(1);
        ctx.endPartUpload();
        ctx.beginPartUpload(1);
    }

    @Test(expected=AmazonClientException.class)
    public void testOutOfSync() {
        MultipartUploadCryptoContext ctx = new MultipartUploadCryptoContext(
                "bucket", "key", null);
        ctx.beginPartUpload(1);
        ctx.endPartUpload();
        ctx.beginPartUpload(3);
    }

    @Test
    public void testInSync() {
        MultipartUploadCryptoContext ctx = new MultipartUploadCryptoContext(
                "bucket", "key", null);
        ctx.beginPartUpload(1);
        ctx.endPartUpload();
        ctx.beginPartUpload(2);
    }
}
