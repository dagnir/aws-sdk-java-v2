package software.amazon.awssdk.services.s3.internal.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.security.KeyPair;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.SimpleMaterialProvider;
import software.amazon.awssdk.util.StringMapBuilder;

public class ContentCryptoMaterialTest {
    private final SecretKey key = CryptoTestUtils.getTestSecretKey();

    @BeforeClass
    public static void setup() {
        CryptoRuntime.enableBouncyCastle();
    }

    @Test
    public void testCreate() throws Exception {
        Map<String, String> md = new StringMapBuilder()
                .put("key1", "value1").build();
        for (CryptoMode mode: CryptoMode.values()) {
            S3CryptoScheme scheme = S3CryptoScheme.from(mode);
            EncryptionMaterials kek = new EncryptionMaterials(key)
                    .addDescriptions(md);
            ContentCryptoMaterial ccm =
                ContentCryptoMaterial.create(key, new byte[16], kek, scheme, null, null, null);
            assertEquals(scheme.getKeyWrapScheme().getKeyWrapAlgorithm(key),
                    ccm.getKeyWrappingAlgorithm());
            assertEquals(scheme.getContentCryptoScheme(), ccm.getContentCryptoScheme());
            assertEquals(md, ccm.getKEKMaterialsDescription());
        }
        for (CryptoMode mode: CryptoMode.values()) {
            S3CryptoScheme scheme = S3CryptoScheme.from(mode);
            KeyPair kekNew = CryptoTestUtils.getTestKeyPair();
            EncryptionMaterials km = new EncryptionMaterials(kekNew).addDescriptions(md);
            ContentCryptoMaterial ccm =
                    ContentCryptoMaterial.create(key, new byte[16], km, scheme, null, null, null);
            assertEquals(scheme.getKeyWrapScheme().getKeyWrapAlgorithm(kekNew.getPublic()),
                    ccm.getKeyWrappingAlgorithm());
            assertEquals(scheme.getContentCryptoScheme(), ccm.getContentCryptoScheme());
            assertEquals(md, ccm.getKEKMaterialsDescription());
        }
    }

    @Test
    public void testReCreate() throws Exception {
        SimpleMaterialProvider smp = new SimpleMaterialProvider();
        for (CryptoMode mode: CryptoMode.values()) {
            S3CryptoScheme scheme = S3CryptoScheme.from(mode);
            EncryptionMaterials km = new EncryptionMaterials(key)
                    .addDescriptions(new StringMapBuilder()
                            .put("scheme", scheme.toString())
                            .put("key", "old")
                            .build());
            smp.addMaterial(km);
            ContentCryptoMaterial ccm =
                ContentCryptoMaterial.create(key, new byte[16], km, scheme, null, null, null);
            assertEquals(scheme.getKeyWrapScheme().getKeyWrapAlgorithm(key),
                    ccm.getKeyWrappingAlgorithm());
            assertEquals(scheme.getContentCryptoScheme(), ccm.getContentCryptoScheme());
            assertEquals(km.getMaterialsDescription(), ccm.getKEKMaterialsDescription());
            Map<String, String> newMD = new StringMapBuilder()
                    .put("scheme", scheme.toString())
                    .put("key", "new")
                    .build();
            KeyPair kekNew = CryptoTestUtils.getTestKeyPair();
            smp.addMaterial(new EncryptionMaterials(kekNew).addDescriptions(newMD));
            ContentCryptoMaterial ccmNew = ccm.recreate(newMD, smp, scheme, null, null, null);
            assertEquals(scheme.getKeyWrapScheme().getKeyWrapAlgorithm(kekNew.getPublic()),
                    ccmNew.getKeyWrappingAlgorithm());
            assertEquals(scheme.getContentCryptoScheme(), ccmNew.getContentCryptoScheme());
            assertEquals(newMD, ccmNew.getKEKMaterialsDescription());
            assertFalse(ccm.getKEKMaterialsDescription().equals(
                    ccmNew.getKEKMaterialsDescription()));
        }
    }
}
