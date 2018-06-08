package software.amazon.awssdk.services.s3;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class Issue460Repro {
    private static final String BUCKET_NAME = "dongietest";
    private S3AsyncClient s3;

    @Before
    public void methodSetup() {
        s3 = S3AsyncClient.builder()
                .credentialsProvider(ProfileCredentialsProvider.create("java-integ-test"))
                .build();
    }
    @Test
    public void test() {
        final PutObjectRequest r = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key("test-key")
                .contentLength(1L)
                .build();
        s3.putObject(r, AsyncRequestBody.fromBytes("abcd".getBytes(StandardCharsets.UTF_8))).join();

        final PutObjectRequest r2 = r.toBuilder()
                .key("test-key-2")
                .build();

        s3.putObject(r2, AsyncRequestBody.fromBytes("abcd".getBytes(StandardCharsets.UTF_8))).join();

        final HeadObjectRequest r3 = HeadObjectRequest.builder().bucket(BUCKET_NAME).key("test-key").build();
        assertThat(s3.headObject(r3).join().contentLength()).isEqualTo(1L);

        final HeadObjectRequest r4 = r3.toBuilder().key("test-key-2").build();
        assertThat(s3.headObject(r4).join().contentLength()).isEqualTo(1L);
    }
}
