package software.amazon.awssdk.services.cloudfront.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;

public class PEMTest {

    @Test
    public void test() throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("test-keypair.pem");
        List<PEMObject> pos = PEM.readPEMObjects(is);
        is.close();
        assertTrue(pos.size() == 2);
        PEMObject o1 = pos.get(0);
        assertEquals(PEMObjectType.PRIVATE_KEY_PKCS1, o1.getPEMObjectType());
        PEMObject o2 = pos.get(1);
        assertEquals(PEMObjectType.PUBLIC_KEY_X509, o2.getPEMObjectType());
    }

}
