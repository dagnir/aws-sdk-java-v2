package software.amazon.awssdk.services.s3.internal.crypto;

import software.amazon.awssdk.services.s3.model.CryptoMode;

public class IssuesJAVA423DefaultCrytoIntegrationTest extends IssuesJAVA423IntegrationTestBase {
    @Override
    protected CryptoMode cryptoMode() {
        return null;
    }
}
