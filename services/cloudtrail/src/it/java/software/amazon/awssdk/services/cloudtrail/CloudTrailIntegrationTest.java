package software.amazon.awssdk.services.cloudtrail;

import software.amazon.awssdk.services.cloudtrail.model.CreateTrailRequest;
import software.amazon.awssdk.services.cloudtrail.model.CreateTrailResult;
import software.amazon.awssdk.services.cloudtrail.model.DeleteTrailRequest;
import software.amazon.awssdk.services.cloudtrail.model.DescribeTrailsRequest;
import software.amazon.awssdk.services.cloudtrail.model.DescribeTrailsResult;
import software.amazon.awssdk.services.cloudtrail.model.StartLoggingRequest;
import software.amazon.awssdk.services.cloudtrail.model.StopLoggingRequest;
import software.amazon.awssdk.services.cloudtrail.model.Trail;
import software.amazon.awssdk.services.cloudtrail.model.UpdateTrailRequest;
import software.amazon.awssdk.services.cloudtrail.model.UpdateTrailResult;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.model.ListVersionsRequest;
import software.amazon.awssdk.services.s3.model.ObjectListing;
import software.amazon.awssdk.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.services.s3.model.S3VersionSummary;
import software.amazon.awssdk.services.s3.model.VersionListing;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CloudTrailIntegrationTest extends IntegrationTestBase {
    private static final String BUCKET_NAME =
            "aws-java-cloudtrail-integ-" + System.currentTimeMillis();
    private static final String TRAIL_NAME = "aws-java-trail-" + System.currentTimeMillis();
    /**
     * Path to the sample policy for this test
     */
    private static final String POLICY_FILE = "/software/amazon/awssdk/services/cloudtrail/samplePolicy.json";

    @BeforeClass
    public static void setUp() throws IOException {
        IntegrationTestBase.setUp();
        s3.createBucket(BUCKET_NAME);
    }

    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(s3, BUCKET_NAME);

        try {
            for (Trail trail : cloudTrail.describeTrails().getTrailList()) {
                cloudTrail.deleteTrail(new DeleteTrailRequest().withName(trail.getName()));
            }
        } catch (Exception e) {
        }
    }

    @Test
    public void testServiceOperations() throws IOException, InterruptedException {
        String policyText = IOUtils.toString(getClass().getResourceAsStream(POLICY_FILE));
        policyText = policyText.replace("@BUCKET_NAME@", BUCKET_NAME);
        System.out.println(policyText);
        s3.setBucketPolicy(BUCKET_NAME, policyText);

        Thread.sleep(1000 * 5);

        // create trail
        CreateTrailResult createTrailResult =
                cloudTrail.createTrail(new CreateTrailRequest()
                                               .withName(TRAIL_NAME)
                                               .withS3BucketName(BUCKET_NAME)
                                               .withIncludeGlobalServiceEvents(true));

        assertEquals(TRAIL_NAME, createTrailResult.getName());
        assertEquals(BUCKET_NAME, createTrailResult.getS3BucketName());
        assertNull(createTrailResult.getS3KeyPrefix());
        assertTrue(createTrailResult.getIncludeGlobalServiceEvents());

        // describe trail
        DescribeTrailsResult describeTrails = cloudTrail.describeTrails();
        assertTrue(describeTrails.getTrailList().size() > 0);

        describeTrails = cloudTrail
                .describeTrails(new DescribeTrailsRequest().withTrailNameList(TRAIL_NAME));
        assertTrue(describeTrails.getTrailList().size() == 1);
        Trail trail = describeTrails.getTrailList().get(0);

        assertEquals(TRAIL_NAME, trail.getName());
        assertEquals(BUCKET_NAME, trail.getS3BucketName());
        assertNull(trail.getS3KeyPrefix());
        assertTrue(trail.getIncludeGlobalServiceEvents());

        // update the trail
        UpdateTrailResult updateTrailResult =
                cloudTrail.updateTrail(new UpdateTrailRequest()
                                               .withName(TRAIL_NAME)
                                               .withS3BucketName(BUCKET_NAME)
                                               .withIncludeGlobalServiceEvents(false)
                                               .withS3KeyPrefix("123"));

        assertEquals(TRAIL_NAME, updateTrailResult.getName());
        assertEquals(BUCKET_NAME, updateTrailResult.getS3BucketName());
        assertEquals("123", updateTrailResult.getS3KeyPrefix());
        assertFalse(updateTrailResult.getIncludeGlobalServiceEvents());

        // start and stop the logging
        cloudTrail.startLogging(new StartLoggingRequest().withName(TRAIL_NAME));
        cloudTrail.stopLogging(new StopLoggingRequest().withName(TRAIL_NAME));

        // delete the trail
        cloudTrail.deleteTrail(new DeleteTrailRequest().withName(TRAIL_NAME));

        // try to get the deleted trail
        DescribeTrailsResult describeTrailResult = cloudTrail
                .describeTrails(new DescribeTrailsRequest().withTrailNameList(TRAIL_NAME));
        assertEquals(0, describeTrailResult.getTrailList().size());
    }

    public static void deleteBucketAndAllContents(AmazonS3 client, String bucketName) {
        System.out.println("Deleting S3 bucket: " + bucketName);
        ObjectListing objectListing = client.listObjects(bucketName);

        while (true) {
            for (Iterator<?> iterator = objectListing.getObjectSummaries().iterator(); iterator
                    .hasNext(); ) {
                S3ObjectSummary objectSummary = (S3ObjectSummary) iterator.next();
                client.deleteObject(bucketName, objectSummary.getKey());
            }

            if (objectListing.isTruncated()) {
                objectListing = client.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }

        VersionListing list = client
                .listVersions(new ListVersionsRequest().withBucketName(bucketName));
        for (Iterator<?> iterator = list.getVersionSummaries().iterator(); iterator.hasNext(); ) {
            S3VersionSummary s = (S3VersionSummary) iterator.next();
            client.deleteVersion(bucketName, s.getKey(), s.getVersionId());
        }
        client.deleteBucket(bucketName);
    }
}
