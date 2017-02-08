package software.amazon.awssdk.services.cloudformation;

import java.io.File;
import java.util.Iterator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import software.amazon.awssdk.auth.policy.Action;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.model.ObjectListing;
import software.amazon.awssdk.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.test.AWSTestBase;

/**
 * Base class for CloudFormation integration tests. Loads AWS credentials from a properties file and
 * creates a client for callers to use.
 */
public class CloudFormationIntegrationTestBase extends AWSTestBase {

    protected static AmazonCloudFormation cf;
    protected static String bucketName = "cloudformation-templates" + System.currentTimeMillis();
    protected static String template1 = "sampleTemplate";
    protected static String templateForCloudFormationIntegrationTests = "templateForCloudFormationIntegrationTests";
    protected static String templateForStackIntegrationTests = "templateForStackIntegrationTests";
    protected static String templateUrlForCloudFormationIntegrationTests = "https://s3.amazonaws.com/" + bucketName
            + "/" + templateForCloudFormationIntegrationTests;
    protected static String templateUrlForStackIntegrationTests = "https://s3.amazonaws.com/" + bucketName + "/"
            + templateForStackIntegrationTests;
    protected static AmazonS3Client s3;

    /**
     * Loads the AWS account info for the integration tests and creates an S3 client for tests to
     * use.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();
        cf = new AmazonCloudFormationClient(credentials);
        cf.setEndpoint("https://cloudformation.ap-northeast-1.amazonaws.com");
        s3 = new AmazonS3Client(credentials);
        s3.createBucket(bucketName);
        s3.putObject(bucketName, templateForCloudFormationIntegrationTests, new File("tst/"
                + templateForCloudFormationIntegrationTests));
        s3.putObject(bucketName, templateForStackIntegrationTests, new File("tst/" + templateForStackIntegrationTests));
    }

    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(bucketName);
        s3.shutdown();
    }

    /**
     * Deletes all objects in the specified bucket, and then deletes the bucket.
     *
     * @param bucketName
     *            The bucket to empty and delete.
     */
    protected static void deleteBucketAndAllContents(String bucketName) {
        ObjectListing objectListing = s3.listObjects(bucketName);

        while (true) {
            for (Iterator<S3ObjectSummary> iterator = objectListing.getObjectSummaries().iterator(); iterator.hasNext();) {
                S3ObjectSummary objectSummary = (S3ObjectSummary) iterator.next();
                s3.deleteObject(bucketName, objectSummary.getKey());
            }

            if (objectListing.isTruncated()) {
                objectListing = s3.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
        ;

        s3.deleteBucket(bucketName);
    }

    /**
     * An auxiliary class to help instantiate the action object.
     */
    protected static class NamedAction implements Action {

        private String actionName;

        public NamedAction(String actionName) {
            this.actionName = actionName;
        }

        public String getActionName() {
            return actionName;
        }
    }

}
