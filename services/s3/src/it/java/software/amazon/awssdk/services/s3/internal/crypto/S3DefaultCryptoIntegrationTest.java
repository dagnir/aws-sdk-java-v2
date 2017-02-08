package software.amazon.awssdk.services.s3.internal.crypto;

import org.junit.experimental.categories.Category;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.model.CryptoMode;

/**
 * Integration tests for the Amazon S3 V2 Encryption Client running in default mode.
 */
@Category(S3Categories.Slow.class)
public class S3DefaultCryptoIntegrationTest extends S3CryptoIntegrationTestBase {
    @Override
    protected final CryptoMode cryptoMode() {
        return null; // default behavior should be v1
    }
}
