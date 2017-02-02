package software.amazon.awssdk.services.s3.internal.crypto;

import software.amazon.awssdk.services.s3.model.CryptoMode;

public class S3CryptoKMSAEIntegrationTest extends S3CryptoKMSIntegrationTestBase {
    
    protected CryptoMode cryptoMode() {
        return CryptoMode.AuthenticatedEncryption;
    }
}