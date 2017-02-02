package software.amazon.awssdk.services.s3.internal.crypto;

import org.junit.experimental.categories.Category;

import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.model.CryptoMode;

/**
 * Integration tests for the Amazon S3 Encryption Client running in V2 mode.
 */
@Category(S3Categories.Slow.class)
public class S3AEIntegrationTest extends S3CryptoIntegrationTestBase {
    @Override
    protected final CryptoMode cryptoMode() {
        return CryptoMode.AuthenticatedEncryption;
    }
}
