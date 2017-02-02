package software.amazon.awssdk.services.ets;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.BeforeClass;

import software.amazon.awssdk.services.elastictranscoder.AmazonElasticTranscoderClient;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.sns.AmazonSNSClient;
import software.amazon.awssdk.test.AWSTestBase;

public class IntegrationTestBase extends AWSTestBase {

    protected static AmazonElasticTranscoderClient ets;
    protected static AmazonS3Client s3;
    protected static AmazonSNSClient sns;

    @BeforeClass
    public static void setUp() throws FileNotFoundException, IOException {
        setUpCredentials();
        System.setProperty("software.amazon.awssdk.sdk.disableCertChecking", "true");
        ets = new AmazonElasticTranscoderClient(credentials);
      //  ets.setEndpoint("https://ets-beta.us-east-1.amazon.com", "elastictranscoder", "us-west-2");
        s3 = new AmazonS3Client(credentials);
        sns = new AmazonSNSClient(credentials);
    }
}
