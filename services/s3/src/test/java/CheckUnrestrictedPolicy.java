import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import javax.crypto.KeyGenerator;
import org.junit.Test;

public class CheckUnrestrictedPolicy {

    @Test
    public void test() {
        boolean isUnlimitedSupported = false;
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES", "SunJCE");
            kgen.init(256);
            isUnlimitedSupported = true;
        } catch (NoSuchAlgorithmException e) {
            isUnlimitedSupported = false;
        } catch (NoSuchProviderException e) {
            isUnlimitedSupported = false;
        }
        System.out.println("isUnlimitedSupported=" + isUnlimitedSupported);
    }

}
