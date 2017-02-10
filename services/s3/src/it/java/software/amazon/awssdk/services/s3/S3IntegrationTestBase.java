package software.amazon.awssdk.services.s3;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import org.junit.BeforeClass;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.PropertiesCredentials;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.internal.AmazonS3TestClient;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AccessControlList;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CanonicalGrantee;
import software.amazon.awssdk.services.s3.model.DeleteVersionRequest;
import software.amazon.awssdk.services.s3.model.EmailAddressGrantee;
import software.amazon.awssdk.services.s3.model.GetObjectMetadataRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.Grant;
import software.amazon.awssdk.services.s3.model.Grantee;
import software.amazon.awssdk.services.s3.model.GroupGrantee;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListVersionsRequest;
import software.amazon.awssdk.services.s3.model.ObjectListing;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.Permission;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.services.s3.model.S3VersionSummary;
import software.amazon.awssdk.services.s3.model.VersionListing;
import software.amazon.awssdk.test.AWSTestBase;
import software.amazon.awssdk.test.util.RandomInputStream;
import software.amazon.awssdk.test.util.RandomTempFile;

/**
 * Base class for S3 integration tests. Loads AWS credentials from a properties
 * file and creates an S3 client for callers to use.
 *
 * @author fulghum@amazon.com
 */
public class S3IntegrationTestBase extends AWSTestBase {

    private static final Random RANDOM = new Random();

    private static final int POLL_INTERVAL = 1000;

    protected static final String AWS_DR_TOOLS_EMAIL_ADDRESS = "aws-dr-tools-test@amazon.com";
    protected static final String AWS_DR_TOOLS_ACCT_ID = "d25639fbe9c19cd30a4c0f43fbf00e2d3f96400a9aa8dabfbbebe19069d1a5df";
    protected static final String AWS_DR_ECLIPSE_ACCT_ID = "4088ec1197b563ffc5e4a4920c1e34bd7c91c511933b752383684cd6c9ed29ab";

    /** The S3 client for all tests to use */
    protected static AmazonS3Client s3;
    protected static AmazonS3Client euS3;
    protected static AmazonS3Client cnS3;
    protected static AmazonS3Client s3gamma;

    /** Android Directory, once set RandomTempFile will use it to create files. */
    public static File androidRootDir;

    /**
     * Loads the AWS account info for the integration tests and creates an S3
     * client for tests to use.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();

        AWSCredentials cnCredentials = new PropertiesCredentials(
            new File(new File(System.getProperty("user.home")),
                     ".aws/bjsTestAccount.properties"));

        s3 = new AmazonS3TestClient(credentials);
        euS3 = new AmazonS3TestClient(credentials);
        euS3.configureRegion(Regions.EU_WEST_1);
        cnS3 = new AmazonS3TestClient(cnCredentials);
        cnS3.setEndpoint("s3.cn-north-1.amazonaws.com.cn");
        AWSCredentials s3GammaCredentials = new PropertiesCredentials(
                new File(new File(System.getProperty("user.home")),
                         ".aws/awsTestAccount.properties_s3gamma"));
        s3gamma = new AmazonS3Client(s3GammaCredentials);
        s3gamma.setEndpoint("http://s3.aws-master.amazon.com");
        s3gamma.setSignerRegionOverride("us-east-1");
    }

    /*
     * Test Helper Methods
     */
     protected static File getRandomTempFile( String filename, long contentLength ) throws Exception {
        if ( androidRootDir == null ) {
            return new RandomTempFile(filename, contentLength);
        }
        else {
            return new RandomTempFile(androidRootDir, filename, contentLength);
        }
    }

    /**
     * Returns true if the specified ACL contains a grant for the specified
     * group grantee and permission.
     *
     * @param acl
     *            The AccessControlList to check.
     * @param expectedGrantee
     *            The grantee being searched for.
     * @param expectedPermission
     *            The grantee's permission being searched for.
     *
     * @return True if the specified ACL contains a grant for the specified
     *         group grantee and permission, otherwise false.
     */
    protected static boolean doesAclContainGroupGrant(
        AccessControlList acl, GroupGrantee expectedGrantee, Permission expectedPermission) {

        for (Grant grant: acl.getGrantsAsList()) {
            Grantee grantee = grant.getGrantee();
            Permission permission = grant.getPermission();

            if (grantee.equals(expectedGrantee)
                    && permission.equals(expectedPermission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Deletes all objects in the specified bucket, and then deletes the bucket.
     *
     * @param bucketName
     *            The bucket to empty and delete.
     */
    protected static void deleteBucketAndAllContents(String bucketName) {
        CryptoTestUtils.deleteBucketAndAllContents(s3, bucketName);
    }

    /**
     * Finds all buckets with the specified bucketPrefix, and deletes all objects in them and then
     * deletes these buckets.
     *
     * @param bucketPrefix The bucket prefix of those buckets to empty and delete.
     */
    protected static void deleteBucketAndAllContentsWithPrefix(String bucketPrefix) {
        for (Bucket bucket : s3.listBuckets()) {
            if (bucket.getName().startsWith(bucketPrefix)) {
                deleteBucketAndAllContents(bucket.getName());
            }
        }
    }

    /**
     * Asserts that the object stored in the specified bucket and key doesn't
     * exist at the specified version ID. If it does exist, this method will
     * fail the current test.
     *
     * @param bucketName
     *            The name of the bucket containing the object to test.
     * @param key
     *            The key under which the object is stored in the specified
     *            bucket.
     * @param versionId
     *            The version ID uniquely indicating a specific version of the
     *            object.
     */
    protected void assertObjectDoesntExist(String bucketName, String key, String versionId) throws Exception {
        try {
            // Sleep a few seconds to handle eventual consistency
            Thread.sleep(1000 * 5);
            s3.getObjectMetadata(new GetObjectMetadataRequest(bucketName, key, versionId));
            if (versionId != null) {
                fail("version '" + versionId + "' of object " + bucketName + "/" + key + " still exists");
            } else {
                fail("object " + bucketName + "/" + key + " still exists");
            }
        } catch (AmazonS3Exception ase) {
            /*
             * We expect a 404 indicating that the object version we requested
             * doesn't exist. If we get anything other than that, then we want
             * to let the exception keep going up the chain.
             */
            if (ase.getStatusCode() != 404) throw ase;
        }
    }

    /**
     * Asserts that the object stored in the specified bucket and key doesn't
     * exist. If it does exist, this method will fail the current test.
     *
     * @param bucketName
     *            The name of the bucket containing the object to test.
     * @param key
     *            The key under which the object is stored in the specified
     *            bucket.
     */
    protected void assertObjectDoesntExist(String bucketName, String key) throws Exception {
        assertObjectDoesntExist(bucketName, key, null);
    }

    /**
     * Deletes the specified bucket, including deleting all object versions in
     * the bucket.
     *
     * @param bucketName
     *            The name of the bucket to delete.
     */
    protected static void deleteBucketAndAllVersionedContents(String bucketName) throws Exception {
        // Delete all the versions in the bucket first
        Thread.sleep(1000 * 3);
        VersionListing versionListing = s3.listVersions(bucketName, null);
        do {
            for (java.util.Iterator iterator = versionListing.getVersionSummaries().iterator(); iterator.hasNext();) {
                S3VersionSummary version = (S3VersionSummary)iterator.next();

                try {
                    s3.deleteVersion(new DeleteVersionRequest(
                            bucketName, version.getKey(), version.getVersionId()));
                } catch (Exception e) {}
            }
            versionListing = s3.listNextBatchOfVersions(versionListing);
        } while (versionListing.isTruncated());

        Thread.sleep(1000 * 3);
        s3.deleteBucket(bucketName);
    }

    /**
     * Creates an object in the specified bucket under the specified key with
     * random data.
     *
     * @param bucketName
     *            The bucket in which to create the new object.
     * @param key
     *            The key under which to store the new object.
     *
     * @return The result of uploading the new object to Amazon S3.
     */
    protected PutObjectResult createObject(String bucketName, String key) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(123);
        return s3.putObject(bucketName, key,
                new RandomInputStream(metadata.getContentLength()), metadata);
    }

    /**
     * Creates an object and waits for it to exist, up to the given timeout.
     */
    protected PutObjectResult createObject(String bucketName, String key, int timeoutMillis) throws InterruptedException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(123);
        PutObjectResult putObject = s3.putObject(bucketName, key, new RandomInputStream(metadata.getContentLength()),
                metadata);
        int poll = 0;
        while ( !doesObjectExist(bucketName, key) && poll++ < timeoutMillis / POLL_INTERVAL ) {
            Thread.sleep(POLL_INTERVAL);
        }
        if ( poll >= timeoutMillis / POLL_INTERVAL ) {
            maxPollTimeExceeded();
        }
        return putObject;
    }

    public boolean doesObjectExist(String bucketName, String key) {
        return !s3.listObjects(new ListObjectsRequest(bucketName, key, null, null, null)).getObjectSummaries()
                  .isEmpty();
    }

    public boolean doesObjectExist(String bucketName, String key, String version) throws Exception {
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key, version);
        try {
            S3Object s3Object = s3.getObject(getObjectRequest);
            if (s3Object == null)
                return false;
            s3Object.getObjectContent().close();
        } catch ( AmazonServiceException ase ) {
            return false;
        }
        return true;
    }

    /**
     * Creates a bucket and waits for it to exist.
     */
    public void createBucket(String bucketName) throws InterruptedException {
        s3.createBucket(bucketName);
        int poll = 0;
        while ( !s3.doesBucketExist(bucketName) && poll++ < 60 ) {
            Thread.sleep(1000);
        }
        if ( poll >= 60 * 5 ) {
            maxPollTimeExceeded();
        }
    }

    protected static byte[] tempDataBuffer(int size) {
        byte[] temp = new byte[size];

        for ( int i = 0; i < size; i++ ) {
            temp[i] = (byte) (RANDOM.nextInt(125 - 32) + 32);
        }

        return temp;
    }

    public static void maxPollTimeExceeded() {
        throw new RuntimeException("Max poll time exceeded");
    }

    /**
     * S3 will always translate email address grantees into canonical ones when
     * we get the list back. This method performs this translation for the
     * aws-dr-tools-test account.
     */
    protected Set<Grant> translateEmailAclsIntoCanonical(AccessControlList acl) {
        Set<Grant> expectedGrants = new HashSet<Grant>();
        for ( Grant grant : acl.getGrantsAsList() ) {
            if ( grant.getGrantee() instanceof EmailAddressGrantee) {
                if ( grant.getGrantee().getIdentifier().equals(AWS_DR_TOOLS_EMAIL_ADDRESS) ) {
                    expectedGrants.add(new Grant(new CanonicalGrantee(AWS_DR_TOOLS_ACCT_ID), grant.getPermission()));
                } else {
                    throw new Error("Unrecognized email address " + grant.getGrantee());
                }
            } else {
                expectedGrants.add(grant);
            }
        }
        return expectedGrants;
    }

    /**
     * waiting a specific bucket created
     * When exceed the poll time, will throw Max poll time exceeded exception
     */
    protected static void waitForBucketCreation(String bucketName) throws Exception{
         long startTime = System.currentTimeMillis();
         long endTime = startTime + (30 * 60 * 1000);
         int hits=0;
        while ( System.currentTimeMillis() < endTime  ) {
                if(!s3.doesBucketExist(bucketName)) {
                     Thread.sleep(1000);
                     hits=0;
                }
                if(hits++ == 10)
                    return;
            }
            maxPollTimeExceeded();
    }

    protected interface EmptyBucketPredicate {
        boolean canDelete(S3ObjectSummary objectSummary);
        boolean canDelete(S3VersionSummary versionSummary);
    }

    protected static void emptyBucket(AmazonS3 s3client, String bucketName, EmptyBucketPredicate predicate) {
        ObjectListing objectListing = s3client.listObjects(bucketName);
        while (true) {
            for (Iterator<?> iterator = objectListing.getObjectSummaries().iterator(); iterator.hasNext(); ) {
                S3ObjectSummary objectSummary = (S3ObjectSummary) iterator.next();
                if (predicate.canDelete(objectSummary)) {
                    s3client.deleteObject(bucketName, objectSummary.getKey());
                }
            }

            if (objectListing.isTruncated()) {
                objectListing = s3client.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
        VersionListing list = s3client.listVersions(new ListVersionsRequest().withBucketName(bucketName));
        for ( Iterator<?> iterator = list.getVersionSummaries().iterator(); iterator.hasNext(); ) {
            S3VersionSummary s = (S3VersionSummary)iterator.next();
            if (predicate.canDelete(s)) {
                s3client.deleteVersion(bucketName, s.getKey(), s.getVersionId());
            }
        }
    }
}
