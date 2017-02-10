package software.amazon.awssdk.services.s3.internal.crypto;

import static software.amazon.awssdk.services.s3.model.CryptoMode.StrictAuthenticatedEncryption;

import software.amazon.awssdk.services.s3.internal.S3Direct;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.EncryptionMaterialsProvider;

public class S3CryptoModuleAEStrictTest extends S3CryptoModuleAETest {
    @Override
    protected S3CryptoModuleBase<?> createS3CryptoModule(S3Direct s3,
            EncryptionMaterialsProvider provider,
            CryptoConfiguration cryptoConfig) {
        return new S3CryptoModuleAE(s3, provider, 
                cryptoConfig.clone()
                    .withCryptoMode(StrictAuthenticatedEncryption)
                    .readOnly());
    }
}
