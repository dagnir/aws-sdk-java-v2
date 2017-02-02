package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.auth.PropertiesFileCredentialsProvider;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AccessControlList;
import software.amazon.awssdk.services.s3.model.CanonicalGrantee;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResult;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResult;
import software.amazon.awssdk.services.s3.model.GetObjectAclRequest;
import software.amazon.awssdk.services.s3.model.GetObjectMetadataRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.InitiateMultipartUploadResult;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PartListing;
import software.amazon.awssdk.services.s3.model.Permission;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResult;
import software.amazon.awssdk.test.util.RandomTempFile;

public class BucketRequesterPaysIntegrationTest extends S3IntegrationTestBase {

    /** S3 client for requester while the default S3 client s3 is for owner. */
    private static AmazonS3Client requester;

    /** The name of the Amazon S3 bucket used for testing requester pays options.*/
    private static final String REQUESTER_PAYS_BUCKET_NAME = "java-requester-pays-test-"+System.currentTimeMillis();

    /** The key used in these tests */
    private static final String KEY = "key";

    /** The file size of the file containing the test data uploaded to S3*/
    private static final long FILE_SIZE = 100000L;

    /** The file containing the test data uploaded to S3 */
    private static File file;

    private static final long SLEEP_TIME_IN_MILLIS = 3000;

    /** Additional Credentials file path used for Requester Pays testing*/
    private static final String REQUESTER_PAYS_CREDENTIALS_FILE_PATH = System.getProperty("user.home")
            + "/.aws/requsterPaysTestAccount.properties";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        S3IntegrationTestBase.setUp();

        s3.createBucket(REQUESTER_PAYS_BUCKET_NAME);
        file = new RandomTempFile("get-object-integ-test", FILE_SIZE);
        s3.putObject(REQUESTER_PAYS_BUCKET_NAME, KEY, file);

        // Sleep for a few seconds to make sure the test doesn't run before the future date
        Thread.sleep(1000 * 2);

        requester = new AmazonS3Client(
                new PropertiesFileCredentialsProvider(
                        REQUESTER_PAYS_CREDENTIALS_FILE_PATH));

        String id = requester.getS3AccountOwner().getId();

        // Setting the Acl for the requester for the newly created bucket.
        AccessControlList bucketAcl = s3.getBucketAcl(REQUESTER_PAYS_BUCKET_NAME);
        bucketAcl.grantPermission(new CanonicalGrantee(id), Permission.FullControl);
        s3.setBucketAcl(REQUESTER_PAYS_BUCKET_NAME, bucketAcl);

        // Checking if requester pays is disabled for the bucket.
        assertFalse(s3.isRequesterPaysEnabled(REQUESTER_PAYS_BUCKET_NAME));
        s3.enableRequesterPays(REQUESTER_PAYS_BUCKET_NAME);
        assertTrue(s3.isRequesterPaysEnabled(REQUESTER_PAYS_BUCKET_NAME));

        Thread.sleep(SLEEP_TIME_IN_MILLIS);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {

        // Disabling the requester pays for the bucket.
        s3.disableRequesterPays(REQUESTER_PAYS_BUCKET_NAME);
        // asserting that Requester Pays is disabled for the bucket.
        assertFalse(s3.isRequesterPaysEnabled(REQUESTER_PAYS_BUCKET_NAME));
        CryptoTestUtils.deleteBucketAndAllContents(s3, REQUESTER_PAYS_BUCKET_NAME);

        if ( file != null ) {
            file.delete();
        }
    }

    @Test
    public void testMultipartUpload() {
        InitiateMultipartUploadRequest initMultipartUploadRequest =
                new InitiateMultipartUploadRequest(REQUESTER_PAYS_BUCKET_NAME, KEY)
                    .withRequesterPays(true);
        InitiateMultipartUploadResult initMultipartUploadResult = requester.initiateMultipartUpload(initMultipartUploadRequest);
        assertTrue(initMultipartUploadResult.isRequesterCharged());

        String uploadId = initMultipartUploadResult.getUploadId();

        UploadPartResult uploadPartResult = requester.uploadPart(new UploadPartRequest()
                .withBucketName(REQUESTER_PAYS_BUCKET_NAME)
                .withKey(KEY)
                .withUploadId(uploadId)
                .withRequesterPays(true)
                .withPartNumber(1)
                .withFile(file));

        assertTrue(uploadPartResult.isRequesterCharged());

        PartListing partListing = requester.listParts(
                new ListPartsRequest(REQUESTER_PAYS_BUCKET_NAME, KEY, uploadId)
                .withRequesterPays(true));
        assertTrue(partListing.isRequesterCharged());

        CompleteMultipartUploadResult completeMultipartUploadResult = requester.completeMultipartUpload(
                new CompleteMultipartUploadRequest()
                .withBucketName(REQUESTER_PAYS_BUCKET_NAME)
                .withKey(KEY)
                .withUploadId(uploadId)
                .withRequesterPays(true)
                .withPartETags(uploadPartResult));

        assertTrue(completeMultipartUploadResult.isRequesterCharged());
    }

    @Test
    public void testPutAndDeleteObject() {
        final String PUT_OBJECT_KEY = "put-object-key";
        PutObjectResult putObjectResult = requester.putObject(
                new PutObjectRequest(REQUESTER_PAYS_BUCKET_NAME, PUT_OBJECT_KEY, file)
                .withRequesterPays(true));

        assertTrue(putObjectResult.isRequesterCharged());

        DeleteObjectsResult deleteObjectsResult = requester.deleteObjects(
                new DeleteObjectsRequest(REQUESTER_PAYS_BUCKET_NAME)
                .withKeys(PUT_OBJECT_KEY)
                .withRequesterPays(true));

        assertTrue(deleteObjectsResult.isRequesterCharged());
    }
    @Test
    public void testGetObject() throws InterruptedException {

        GetObjectRequest getObjectRequest = new GetObjectRequest(
                REQUESTER_PAYS_BUCKET_NAME, KEY, true);
        S3Object s3Object = requester.getObject(getObjectRequest);

        assertTrue(s3Object.isRequesterCharged());
    }

    @Test
    public void testHeadObject() {
        ObjectMetadata metadata = requester.getObjectMetadata(
                new GetObjectMetadataRequest(REQUESTER_PAYS_BUCKET_NAME, KEY)
                .withRequesterPays(true));

        assertTrue(metadata.isRequesterCharged());
    }

    @Test
    public void testCopyObject() {
        final String TARGET_KEY = "target-key";
        CopyObjectResult copyObjectResult = requester.copyObject(
                new CopyObjectRequest(REQUESTER_PAYS_BUCKET_NAME, KEY, REQUESTER_PAYS_BUCKET_NAME, TARGET_KEY)
                .withRequesterPays(true));

        assertTrue(copyObjectResult.isRequesterCharged());

        s3.deleteObject(REQUESTER_PAYS_BUCKET_NAME, TARGET_KEY);
    }

    @Test
    public void testGetObjectAcl() {
        AccessControlList acl = requester.getObjectAcl(
                new GetObjectAclRequest(REQUESTER_PAYS_BUCKET_NAME, KEY)
                .withRequesterPays(true));

        assertTrue(acl.isRequesterCharged());
    }
}
