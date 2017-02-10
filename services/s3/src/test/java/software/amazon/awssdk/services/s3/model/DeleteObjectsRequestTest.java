package software.amazon.awssdk.services.s3.model;

import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest.KeyVersion;

public class DeleteObjectsRequestTest {

    @Test
    public void test() {
        DeleteObjectsRequest req = new DeleteObjectsRequest("bucketName");
        List<KeyVersion> keys = req.getKeys();
        assertTrue(keys.size() == 0);
        keys.add(new KeyVersion("1"));
        assertTrue(req.getKeys().size() == 1);
    }
}
 