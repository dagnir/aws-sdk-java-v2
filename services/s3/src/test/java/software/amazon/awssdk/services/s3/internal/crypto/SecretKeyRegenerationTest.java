package software.amazon.awssdk.services.s3.internal.crypto;

import java.io.File;
import java.security.Provider;
import java.security.Security;
import javax.crypto.SecretKey;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.runtime.io.SdkFilterInputStream;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

/**
 * Used to test secret key regeneration.
 * 
 * See: https://github.com/aws/aws-sdk-android/issues/15
 */
public class SecretKeyRegenerationTest {

    @Test
    public void genKey() throws Exception {
        CryptoRuntime.enableBouncyCastle();
        Provider p = Security.getProvider(CryptoRuntime.BOUNCY_CASTLE_PROVIDER);
        // The probability of having all zeros in the left most byte of a
        // randomly generated secret key is a geometric distribution with
        // p=1/256. See: http://en.wikipedia.org/wiki/Geometric_distribution
        //
        // The probability of inducing at least one such edge case with 1000
        // iterations is 1-(255/256)^1000 = 98%. In contrast, 100 iterations 
        // would only have 32% chance of hitting such edge case 
        // so that's not good enough.
        for (int i=0; i < 1000; i++) {
            SecretKey secretKey = new MyS3CryptoModuleBase().generateCEK(
                    new EncryptionMaterials(CryptoTestUtils.getTestKeyPair()), p);
            Assert.assertTrue(32 == secretKey.getEncoded().length);
        }
    }

    private static class MyS3CryptoModuleBase extends
            S3CryptoModuleBase<MultipartUploadCryptoContext> {
        protected MyS3CryptoModuleBase() {
            super(null, null, null, new CryptoConfiguration()
                .withCryptoProvider(
                    Security.getProvider(CryptoRuntime.BOUNCY_CASTLE_PROVIDER))
                .readOnly());
        }

        @Override
        protected long ciphertextLength(long plaintextLength) {
            return 0;
        }

        @Override
        MultipartUploadCryptoContext newUploadContext(
                InitiateMultipartUploadRequest req,
                ContentCryptoMaterial cekMaterial) {
            return null;
        }

        @Override
        CipherLite cipherLiteForNextPart(
                MultipartUploadCryptoContext uploadContext) {
            return null;
        }

        @Override
        long computeLastPartSize(UploadPartRequest req) {
            return 0;
        }

        @Override
        SdkFilterInputStream wrapForMultipart(CipherLiteInputStream is,
                long partSize) {
            return null;
        }

        @Override
        void updateUploadContext(MultipartUploadCryptoContext uploadContext,
                SdkFilterInputStream is) {
        }

        @Override
        public S3Object getObjectSecurely(GetObjectRequest req) {
            return null;
        }

        @Override
        public ObjectMetadata getObjectSecurely(GetObjectRequest req, File dest) {
            return null;
        }
    };

}
