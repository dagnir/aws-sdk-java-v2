package software.amazon.awssdk.services.s3.internal;

import java.io.File;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.AWSCredentialsProvider;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.services.kms.AWSKMSClient;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Same functionality as {@link AmazonS3Client} but is more resilient to
 * transient eventual consistency problem via retries with exponential backoff.
 */
public class AmazonS3TestClient extends AmazonS3Client {
    private static final int MAX_RETRY = 4;

    public AmazonS3TestClient() {
        super();
    }

    public AmazonS3TestClient(AWSCredentials awsCredentials) {
        super(awsCredentials);
    }

    public AmazonS3TestClient(AWSKMSClient kms, AWSCredentials awsCredentials) {
        super(awsCredentials);
    }

    public AmazonS3TestClient(AWSCredentials awsCredentials, ClientConfiguration clientConfiguration) {
        super(awsCredentials, clientConfiguration);
    }

    public AmazonS3TestClient(AWSCredentialsProvider credentialsProvider) {
        super(credentialsProvider);
    }

    public AmazonS3TestClient(AWSCredentialsProvider credentialsProvider,
            ClientConfiguration clientConfiguration) {
        super(credentialsProvider, clientConfiguration);
    }

    public AmazonS3TestClient(AWSCredentialsProvider credentialsProvider,
            ClientConfiguration clientConfiguration,
            RequestMetricCollector requestMetricCollector) {
        super(credentialsProvider, clientConfiguration, requestMetricCollector);
    }

    public AmazonS3TestClient(ClientConfiguration clientConfiguration) {
        super(clientConfiguration);
    }

    @Override
    public S3Object getObject(GetObjectRequest req) {
        for (int i = 0;;i++) {
            try {
                return super.getObject(req);
            } catch(AmazonClientException ex) {
                if (i >= MAX_RETRY)
                    throw ex;
                ex.printStackTrace();
                System.out.println("Retrying getObject " + (i+1));
                try {
                    Thread.sleep((1<<(i+1))*1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public ObjectMetadata getObject(GetObjectRequest req, File dest) {
        for (int i = 0;;i++) {
            try {
                return super.getObject(req, dest);
            } catch(AmazonClientException ex) {
                if (i >= MAX_RETRY)
                    throw ex;
                ex.printStackTrace();
                System.out.println("Retrying getObject " + (i+1));
                try {
                    Thread.sleep((1<<(i+1))*1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
