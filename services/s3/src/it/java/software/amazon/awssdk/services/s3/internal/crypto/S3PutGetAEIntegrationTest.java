package software.amazon.awssdk.services.s3.internal.crypto;

import software.amazon.awssdk.services.s3.model.CryptoMode;

public class S3PutGetAEIntegrationTest extends S3PutGetAEIntegrationTestBase {
    @Override
    protected CryptoMode cryptoMode() {
        return CryptoMode.AuthenticatedEncryption;
    }
}
