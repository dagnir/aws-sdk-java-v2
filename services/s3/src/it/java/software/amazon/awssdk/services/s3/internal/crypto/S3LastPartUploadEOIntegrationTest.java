package software.amazon.awssdk.services.s3.internal.crypto;

import software.amazon.awssdk.services.s3.model.CryptoMode;

public class S3LastPartUploadEOIntegrationTest extends S3LastPartUploadIntegrationTestBase {
    protected CryptoMode cryptoMode() {
        return CryptoMode.EncryptionOnly;
    }
}
