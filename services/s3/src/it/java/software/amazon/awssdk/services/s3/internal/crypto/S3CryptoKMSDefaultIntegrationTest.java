package software.amazon.awssdk.services.s3.internal.crypto;

import software.amazon.awssdk.services.s3.model.CryptoMode;

public class S3CryptoKMSDefaultIntegrationTest extends S3CryptoKMSIntegrationTestBase {

    protected CryptoMode cryptoMode() {
        return null;
    }
}
