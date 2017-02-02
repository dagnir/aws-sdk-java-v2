package software.amazon.awssdk.services.s3.internal.crypto;

import org.junit.experimental.categories.Category;

import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.model.CryptoMode;

@Category(S3Categories.Slow.class)
public class S3MultipartUploadAEIntegrationTest extends S3MultipartUploadCryptoIntegrationTestBase {

    @Override
    protected CryptoMode cryptoMode() {
        return CryptoMode.AuthenticatedEncryption;
    }
 }
