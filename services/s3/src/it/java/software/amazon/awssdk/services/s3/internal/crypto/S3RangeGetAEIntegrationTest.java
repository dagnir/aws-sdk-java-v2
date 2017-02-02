package software.amazon.awssdk.services.s3.internal.crypto;

import software.amazon.awssdk.services.s3.model.CryptoMode;

public class S3RangeGetAEIntegrationTest extends S3RangeGetAEIntegrationTestBase {
    @Override
    protected CryptoMode cryptoMode() {
        return CryptoMode.AuthenticatedEncryption;
    }
}
