package software.amazon.awssdk.services.kinesisfirehose;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.auth.profile.ProfileCredentialsProvider;
import software.amazon.awssdk.services.kinesisfirehose.model.CreateDeliveryStreamRequest;
import software.amazon.awssdk.services.kinesisfirehose.model.ListDeliveryStreamsRequest;
import software.amazon.awssdk.services.kinesisfirehose.model.ListDeliveryStreamsResult;
import software.amazon.awssdk.services.kinesisfirehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.kinesisfirehose.model.PutRecordBatchResponseEntry;
import software.amazon.awssdk.services.kinesisfirehose.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesisfirehose.model.Record;
import software.amazon.awssdk.services.kinesisfirehose.model.S3DestinationConfiguration;
import software.amazon.awssdk.test.AWSTestBase;

public class ServiceIntegrationTest extends AWSTestBase {

    private static final String DEVLIVERY_STREAM_NAME = "java-sdk-delivery-stream-"
            + System.currentTimeMillis();
    private static final String FAKE_S3_BUCKET_ARN = "arn:aws:s3:::fake-s3-bucket-arn";
    private static final String FAKE_IAM_ROLE_ARN = "arn:aws:iam:::fake-iam-role-arn";

    private static AmazonKinesisFirehose firehose;


    @BeforeClass
    public static void setup() throws FileNotFoundException, IOException {
//        setUpCredentials();
//        firehose = new AmazonKinesisFirehoseClient(credentials);
//        s3 = new AmazonS3Client(credentials);

        // TODO: firehose can't whitelist our shared account at this point, so
        // for now we are using the test account provided by the firehose team
        ProfileCredentialsProvider firehostTestCreds = new ProfileCredentialsProvider(
                "firehose-test");
        firehose = new AmazonKinesisFirehoseClient(firehostTestCreds);
    }

    @AfterClass
    public static void tearDown() {
//        firehose.deleteDeliveryStream(new DeleteDeliveryStreamRequest()
//                .withDeliveryStreamName(DEVLIVERY_STREAM_NAME));
    }

//    @Test
    // Nope, can't make it work without full access to S3 and IAM
    public void testOperations() {

        // create delivery stream
        firehose.createDeliveryStream(new CreateDeliveryStreamRequest()
                .withDeliveryStreamName(DEVLIVERY_STREAM_NAME)
                .withS3DestinationConfiguration(
                        new S3DestinationConfiguration()
                                .withBucketARN(FAKE_S3_BUCKET_ARN)
                                .withRoleARN(FAKE_IAM_ROLE_ARN)));

        // put record
        String recordId = firehose.putRecord(new PutRecordRequest()
                .withDeliveryStreamName(DEVLIVERY_STREAM_NAME)
                .withRecord(new Record()
                        .withData(ByteBuffer.wrap(new byte[] { 0, 1, 2 }))
                        )
                 ).getRecordId();
        assertNotEmpty(recordId);

        // put record batch
        List<PutRecordBatchResponseEntry> entries = firehose.putRecordBatch(
                new PutRecordBatchRequest()
                    .withDeliveryStreamName(DEVLIVERY_STREAM_NAME)
                    .withRecords(
                            new Record().withData(ByteBuffer.wrap(new byte[] {0})),
                            new Record().withData(ByteBuffer.wrap(new byte[] {1}))
                             )
                ).getRequestResponses();
        assertEquals(2, entries.size());
        for (PutRecordBatchResponseEntry entry : entries) {
            if (entry.getErrorCode() == null) {
                assertNotEmpty(entry.getRecordId());
            } else {
                assertNotEmpty(entry.getErrorMessage());
            }
        }
    }

    @Test
    public void testListDeliveryStreams() {
        ListDeliveryStreamsResult result = firehose
                .listDeliveryStreams(new ListDeliveryStreamsRequest());
        assertNotNull(result.getDeliveryStreamNames());
        assertNotNull(result.getHasMoreDeliveryStreams());
    }

    @Test
    public void testCreateDeliveryStream_InvalidParameter() {
        try {
            firehose.createDeliveryStream(new CreateDeliveryStreamRequest());
            fail("ValidationException is expected.");
        } catch (AmazonServiceException ase) {
            assertEquals("ValidationException", ase.getErrorCode());
            assertNotEmpty(ase.getErrorMessage());
        }
    }

}
