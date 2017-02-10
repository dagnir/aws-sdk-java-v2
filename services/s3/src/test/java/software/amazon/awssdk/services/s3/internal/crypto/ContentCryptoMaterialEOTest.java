package software.amazon.awssdk.services.s3.internal.crypto;

import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.services.s3.internal.crypto.ContentCryptoScheme.AES_CBC;
import static software.amazon.awssdk.services.s3.model.ExtraMaterialsDescription.NONE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.EncryptionMaterialsAccessor;
import software.amazon.awssdk.util.json.Jackson;

public class ContentCryptoMaterialEOTest {
    private final SecretKey key = CryptoTestUtils.getTestSecretKey();

    private void doTestMatDesEO(Map<String, String> kekMatDesc,
            EncryptionMaterialsAccessor accessor) throws Exception {
        Cipher cipher = Cipher.getInstance(key.getAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, key);
        // wrapping the test key by itself is totally insecure, but this is just
        // a test so convenience wins
        byte[] encrypted = cipher.doFinal(key.getEncoded());
        ContentCryptoMaterial cekMaterial = new ContentCryptoMaterial(
                kekMatDesc, encrypted, cipher.getAlgorithm(),
                CryptoTestUtils.createTestCipherLite(Cipher.ENCRYPT_MODE, AES_CBC));
        String json = cekMaterial.toJsonString(CryptoMode.EncryptionOnly);
        @SuppressWarnings("unchecked")
        Map<String, String> map = Jackson.fromJsonString(json, Map.class);
        ContentCryptoMaterial cekm2 = ContentCryptoMaterial
            .fromInstructionFile(map, accessor,
                    null, // security provider
                    null, // range
                    NONE, // supplemental material descriptions
                    false,// key-wrap expected
                    null  // KMS client
            );
        String json2 = cekm2.toJsonString(CryptoMode.EncryptionOnly);
        assertEquals(json, json2);
    }

    @Test
    public void testNonEmptyMaterialDescription() throws Exception {
        Map<String, String> kekMatDesc = new HashMap<String, String>();
        kekMatDesc.put("Foo", "Bar");
        kekMatDesc.put("Hello", "World");
        doTestMatDesEO(kekMatDesc,
                new EncryptionMaterialsAccessor() {
                    @Override
                    public EncryptionMaterials getEncryptionMaterials(
                            Map<String, String> materialsDescription) {
                        assertEquals("Bar", materialsDescription.get("Foo"));
                        assertEquals("World", materialsDescription.get("Hello"));
                        return new EncryptionMaterials(key);
                    }
                });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEmptyMaterialDescription() throws Exception {
        doTestMatDesEO(Collections.EMPTY_MAP,
                new EncryptionMaterialsAccessor() {
                    @Override
                    public EncryptionMaterials getEncryptionMaterials(
                            Map<String, String> materialsDescription) {
                        return new EncryptionMaterials(key);
                    }
                });
    }

    @Test
    public void testNullMaterialDescription() throws Exception {
        doTestMatDesEO(null,
                new EncryptionMaterialsAccessor() {
                    @Override
                    public EncryptionMaterials getEncryptionMaterials(
                            Map<String, String> materialsDescription) {
                        return new EncryptionMaterials(key);
                    }
                });
    }
}
