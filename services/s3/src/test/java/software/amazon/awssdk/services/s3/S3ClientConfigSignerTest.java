package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.junit.Test;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.auth.NoOpSigner;
import software.amazon.awssdk.auth.Signer;
import software.amazon.awssdk.services.s3.internal.AWSS3V4Signer;
import software.amazon.awssdk.services.s3.internal.Constants;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class S3ClientConfigSignerTest {

    @Test
    public void clientConfigWithoutSigner() throws IOException {
        AmazonS3Client client = createTestClient(new ClientConfiguration());
        DefaultRequest<GetObjectRequest> req =
                new DefaultRequest<GetObjectRequest>(new GetObjectRequest("bucket", "key"),
                Constants.S3_SERVICE_DISPLAY_NAME);
        req.setEndpoint(client.getEndpoint());
        final Signer signer = client.createSigner(req, "bucket", "key");
        client.shutdown();
        assertTrue(signer instanceof AWSS3V4Signer);
    }

    @Test
    public void clientConfigWithSigner() throws IOException {
        AmazonS3Client client = createTestClient(
            new ClientConfiguration().withSignerOverride("NoOpSignerType"));
        DefaultRequest<GetObjectRequest> req =
                new DefaultRequest<GetObjectRequest>(new GetObjectRequest("bucket", "key"),
                Constants.S3_SERVICE_DISPLAY_NAME);
        req.setEndpoint(client.getEndpoint());
        final Signer signer = client.createSigner(req, "bucket", "key");
        client.shutdown();
        assertTrue(signer instanceof NoOpSigner);
    }

    private AmazonS3Client createTestClient(ClientConfiguration config) throws IOException {
        AmazonS3Client client = new AmazonS3Client(config);
        client.setEndpoint("s3.nonstandard.test.endpoint");
        return client;
    }

}
