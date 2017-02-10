package software.amazon.awssdk.services.s3.internal;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import org.junit.Test;
import software.amazon.awssdk.runtime.io.ResettableInputStream;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.transfer.internal.UploadPartRequestFactory;

public class UploadPartRequestFactoryTest {

    // https://github.com/aws/aws-sdk-java/issues/427
    @Test
    public void testFileInputStream() throws Exception {
        File file = CryptoTestUtils.generateRandomAsciiFile(1);
        FileInputStream fis = new FileInputStream(file);
        ObjectMetadata om = new ObjectMetadata();
        om.setContentLength(10*1024*1024*1024);
        PutObjectRequest req = new PutObjectRequest("bucket", "key", fis, om);
        final int readLimitOrig = req.getReadLimit();
        req.getRequestClientOptions().setReadLimit(readLimitOrig + 100);
        UploadPartRequestFactory f = new UploadPartRequestFactory(req, "1234", 5*1024*1024);
        UploadPartRequest upr = f.getNextUploadPartRequest();
        assertTrue(readLimitOrig + 100 == upr.getReadLimit());
        assertNull(upr.getFile());
        assertTrue(upr.getInputStream() instanceof InputSubstream);
        InputSubstream iss = (InputSubstream)upr.getInputStream();
        // Verifies the stream has unlimited mark-and-reset capabilities
        assertTrue(iss.getWrappedInputStream() instanceof ResettableInputStream);
    }

}
