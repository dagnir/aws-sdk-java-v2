package software.amazon.awssdk.services.s3.model;

import org.junit.Test;

public class GeneratePresignedUrlRequestTest {

    @Test
    public void nullKey() {
        new GeneratePresignedUrlRequest("bucket", null).rejectIllegalArguments();
    }

    @Test(expected=IllegalArgumentException.class)
    public void nullBucket() {
        new GeneratePresignedUrlRequest(null, "key").rejectIllegalArguments();
    }

    @Test(expected=IllegalArgumentException.class)
    public void nullMethod() {
        new GeneratePresignedUrlRequest("bucket", "key").withMethod(null)
                .rejectIllegalArguments();
    }

    @Test
    public void defaultGetMethod() {
        new GeneratePresignedUrlRequest("bucket", "key").rejectIllegalArguments();
    }

    @Test
    public void sse() {
        new GeneratePresignedUrlRequest("bucket", "key")
            .withSSEAlgorithm(SSEAlgorithm.getDefault())
            .rejectIllegalArguments();
    }

    @Test
    public void sse_c() {
        new GeneratePresignedUrlRequest("bucket", "key")
            .withSSECustomerKeyAlgorithm(SSEAlgorithm.getDefault())
            .rejectIllegalArguments();
    }

    @Test(expected=IllegalArgumentException.class)
    public void sse_and_sse_c() {
        new GeneratePresignedUrlRequest("bucket", "key")
            .withSSECustomerKeyAlgorithm(SSEAlgorithm.getDefault())
            .withSSEAlgorithm(SSEAlgorithm.getDefault())
            .rejectIllegalArguments();
        ;
    }

    @Test(expected=IllegalArgumentException.class)
    public void kmsCmkId_and_sse_c() {
        new GeneratePresignedUrlRequest("bucket", "key")
            .withSSECustomerKeyAlgorithm(SSEAlgorithm.getDefault())
            .withKmsCmkId("kms_cmk_id")
            .rejectIllegalArguments();
        ;
    }

    @Test(expected=IllegalArgumentException.class)
    public void sse_kms_cmkId_only() {
        new GeneratePresignedUrlRequest("bucket", "key")
            .withKmsCmkId("kms_cmk_id")
            .rejectIllegalArguments();
    }

    // This case is valid as S3 would use the default KMS CMK ID.
    @Test
    public void sse_kms_without_cmkId() {
        new GeneratePresignedUrlRequest("bucket", "key")
            .withSSEAlgorithm(SSEAlgorithm.KMS)
            .rejectIllegalArguments();
    }

    public void sse_kms() {
        new GeneratePresignedUrlRequest("bucket", "key")
            .withSSEAlgorithm(SSEAlgorithm.KMS)
            .withKmsCmkId("kms_cmk_id")
            .rejectIllegalArguments();
        ;
    }

    @Test(expected=IllegalArgumentException.class)
    public void sse_c_kms() {
        new GeneratePresignedUrlRequest("bucket", "key")
            .withSSECustomerKeyAlgorithm(SSEAlgorithm.KMS)
            ;
    }
}
