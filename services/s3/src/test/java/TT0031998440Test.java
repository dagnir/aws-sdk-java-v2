import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.KeyPair;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import org.junit.Test;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;


public class TT0031998440Test {
    @Test
    public void test() throws Exception {
        // Generate a key pair
        KeyPair pair = CryptoTestUtils.generateKeyPair("RSA", 1024);
        // Generate a secret key
        SecretKey k = CryptoTestUtils.generateSecretKey("AES", 256);
        Cipher c = Cipher.getInstance(pair.getPublic().getAlgorithm());
        c.init(Cipher.ENCRYPT_MODE, pair.getPublic());
        // Encrypt the secret key with the public key
        byte[] ct = c.doFinal(k.getEncoded());
        Cipher d = Cipher.getInstance(pair.getPrivate().getAlgorithm());
        d.init(Cipher.DECRYPT_MODE, pair.getPrivate());
        // Decrypt the encrypted secret key with the private key
        byte[] pt = d.doFinal(ct);
        assertTrue(Arrays.equals(k.getEncoded(), pt));

        // Generate another key pair
        KeyPair pair2 = CryptoTestUtils.generateKeyPair("RSA", 1024);
        Cipher d2 = Cipher.getInstance(pair2.getPrivate().getAlgorithm());
        d2.init(Cipher.DECRYPT_MODE, pair2.getPrivate());
        try {
            // Decrypt the encrypted secret key with a key pair different than the one used to encrypt it 
            byte[] pt2 = d2.doFinal(ct);
            fail(); // should never succeeds
        } catch(BadPaddingException ex) {
            // Expected failure
            ex.printStackTrace(System.err);
        }
    }
}
