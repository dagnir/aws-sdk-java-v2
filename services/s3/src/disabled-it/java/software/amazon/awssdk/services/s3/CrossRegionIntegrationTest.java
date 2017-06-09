package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.SDKGlobalConfiguration;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

/**
 * Validate the cross-region bucket discovery functionality of the S3 client.
 */
public class CrossRegionIntegrationTest extends AWSIntegrationTestBase {

    // The name of a bucket created for the purposes of cross-region testing
    private static final String TEST_CROSS_REGION_BUCKET_NAME = "cross-region-test-bucket-" + UUID.randomUUID();

    // The location of a bucket created for the purposes of cross-region testing
    private static final Regions TEST_CROSS_REGION_BUCKET_LOCATION = Regions.US_WEST_2;

    private static final Regions[] CROSS_REGION_REGIONS = new Regions[] {
            Regions.EU_CENTRAL_1,
            Regions.US_WEST_1
    };

    @BeforeClass
    public static void createBucket() {
        CreateBucketRequest request = new CreateBucketRequest(TEST_CROSS_REGION_BUCKET_NAME);
        AmazonS3 s3Client = createClientWithBuilder(TEST_CROSS_REGION_BUCKET_LOCATION, false);
        s3Client.createBucket(TEST_CROSS_REGION_BUCKET_NAME);
    }

    @AfterClass
    public static void deleteBucket() {
        DeleteBucketRequest request = new DeleteBucketRequest(TEST_CROSS_REGION_BUCKET_NAME);
        AmazonS3 s3Client = createClientWithBuilder(TEST_CROSS_REGION_BUCKET_LOCATION, false);
        s3Client.deleteBucket(request);
    }

    @Test
    public void bucketsCanBeCreated_when_createdFromOutOfRegionAndGlobalAccessEnabledWithBuilder() {
        assertBucketCanBeCreatedInRegion(createClientWithBuilder(Regions.US_WEST_2, true),
                                         Regions.US_WEST_1);
    }

    @Test
    public void bucketsCannotBeCreated_when_createdFromOutOfRegionAndGlobalAccessDisabledWithBuilder() {
        assertBucketCannotBeCreatedInRegion(createClientWithBuilder(Regions.US_WEST_2, false),
                                            Regions.US_WEST_1);
    }

    @Test
    public void bucketsCanBeCreated_when_createdwithDefaultClientAndNullRegion() {
        assertBucketCanBeCreatedInRegion(createClientWithoutBuilder(null), Regions.US_WEST_1);
    }

    @Test
    public void bucketsCannotBeCreated_when_createdWithDefaultClientAndNonNullRegion() {
        assertBucketCannotBeCreatedInRegion(createClientWithoutBuilder(Regions.US_WEST_2), Regions.US_WEST_1);
    }

    @Test
    public void bucketShouldBeFound_when_checkedWithinRegionWithoutGlobalAccessDisabled() {
        assertBucketFound(createClientWithBuilder(TEST_CROSS_REGION_BUCKET_LOCATION, false));
    }

    @Test
    public void bucketShouldBeFound_when_checkedWithinRegionWithGlobalAccessEnabled() {
        assertBucketFound(createClientWithBuilder(TEST_CROSS_REGION_BUCKET_LOCATION, true));
    }

    @Test
    public void bucketShouldBeFound_when_checkedWithGlobalAccessEnabledAndDisableImplicitCrossRegionProperty() {
        try {
            disableImplicitCrossRegionClients();
            assertBucketFound(createClientWithBuilder(Regions.US_EAST_1, true));
        }
        finally {
            enableImplicitCrossRegionClients();
        }
    }

    @Test
    public void bucketShouldBeCreated_when_createdWithinRegionWithRegionlessClientAndDisableImplicitCrossRegionProperty() {
        try {
            disableImplicitCrossRegionClients();
            assertBucketCanBeCreatedInRegion(createClientWithoutBuilder(Regions.US_WEST_1), Regions.US_WEST_1);
        }
        finally {
            enableImplicitCrossRegionClients();
        }
    }

    @Test
    public void bucketShouldBeFound_when_checkedCrossRegionWithGlobalAccessEnabled() {
        for(Regions region : CROSS_REGION_REGIONS) {
            assertBucketFound(createClientWithBuilder(region, true));
        }
    }

    @Test
    public void bucketShouldNotBeFound_when_checkedCrossRegionWithGlobalAccessDisabled() {
        for(Regions region : CROSS_REGION_REGIONS) {
            assertBucketNotFound(createClientWithBuilder(region, false));
        }
    }

    @Test
    public void bucketShouldBeFound_when_checkedCrossRegionWithRegionlessNonBuilderClient() {
        assertBucketFound(createClientWithoutBuilder(null));
    }

    @Test
    public void bucketShouldNotBeFound_When_checkedCrossRegionWithNonBuilderClient() {
        for(Regions region : CROSS_REGION_REGIONS) {
            assertBucketNotFound(createClientWithoutBuilder(region));
        }
    }

    /**
     * Validate that the cross-region bucket is found with the given client.
     */
    private void assertBucketFound(AmazonS3 client) {
        try {
            client.getBucketPolicy(TEST_CROSS_REGION_BUCKET_NAME);
        }
        catch(AmazonS3Exception e) {
            e.printStackTrace();
            fail("Bucket not found in region " + client.getRegionName());
        }
    }

    /**
     * Validate that the cross-region bucket is NOT found with the given client.
     */
    private void assertBucketNotFound(AmazonS3 client) {
        try {
            client.getBucketPolicy(TEST_CROSS_REGION_BUCKET_NAME);
            fail("Bucket found in region " + client.getRegionName());
        }
        catch(AmazonS3Exception e) {
            // Expected - not found
        }
    }

    private void assertBucketCanBeCreatedInRegion(AmazonS3 client, Regions region) {
        assertTrue(canBucketBeCreatedInRegion(client, region));
    }

    private void assertBucketCannotBeCreatedInRegion(AmazonS3 client, Regions region) {
        assertFalse(canBucketBeCreatedInRegion(client, region));
    }

    private boolean canBucketBeCreatedInRegion(AmazonS3 client, Regions region) {
        String bucketName = "cross-region-create-test-" + UUID.randomUUID();
        CreateBucketRequest request = new CreateBucketRequest(bucketName, region.getName());

        try {
            client.createBucket(request);
        }
        catch (SdkClientException e) {
            System.out.println("Error creating bucket. This may be expected.");
            e.printStackTrace();
            // Bucket could not be created.
            return false;
        }

        // Bucket was created, so clean up.
        try {
            client.deleteBucket(bucketName);
        }
        catch (SdkClientException e) {
            System.err.println("Bucket was successfully created, but couldn't be cleaned up: " + bucketName);
            e.printStackTrace();
        }

        return true;
    }

    private String disableImplicitCrossRegionClients() {
        return System.setProperty(SDKGlobalConfiguration.DISABLE_S3_IMPLICIT_GLOBAL_CLIENTS_SYSTEM_PROPERTY, "true");
    }

    private String enableImplicitCrossRegionClients() {
        return System.clearProperty(SDKGlobalConfiguration.DISABLE_S3_IMPLICIT_GLOBAL_CLIENTS_SYSTEM_PROPERTY);
    }

    /**
     * Create a client using the client builder, the provided region and cross-region functionality flag.
     */
    private static AmazonS3 createClientWithBuilder(Regions region, boolean enableGlobalBucketAccess) {
        return AmazonS3Client.builder()
                                    .withRegion(region)
                                    .credentialsProvider(new StaticCredentialsProvider(getCredentials()))
                                    .withForceGlobalBucketAccessEnabled(enableGlobalBucketAccess)
                                    .build();
    }

    /**
     * Create a client using the client constructor and the provided region.
     */
    private static AmazonS3 createClientWithoutBuilder(Regions region) {
        AmazonS3 client = new AmazonS3Client(getCredentials());
        if(region != null) {
            client.setRegion(Region.getRegion(region));
        }
        return client;
    }
}
