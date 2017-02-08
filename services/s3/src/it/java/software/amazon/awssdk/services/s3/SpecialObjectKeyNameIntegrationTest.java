package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.services.s3.util.SpecialObjectKeyNameGenerator;

/** Tests that S3 could handle special key names. **/
public class SpecialObjectKeyNameIntegrationTest extends S3IntegrationTestBase {
    
    private static final String bucketName = "special-key-test-bucket-" + new Date().getTime();
    private static final String OBJECT_CONTENTS = "Special key name test";

    @BeforeClass
    public static void createBucket() {
        CreateBucketRequest request = new CreateBucketRequest(bucketName);
        Bucket bucket = s3.createBucket(request);
        assertNotNull(bucket);
        assertEquals(bucketName, bucket.getName());
    }
    
    /**
     * Put objects with all the special keys, and also check that the key names are not mistakenly tampered.
     */
    @Test
    public void testPutAllSpecialKeys() {
        List<String> allSpecialKeyNames = SpecialObjectKeyNameGenerator.initAllSpecialKeyNames();
        for (String specialKey : allSpecialKeyNames) {
            testPutObject(specialKey);
        }
    }
    
    @AfterClass
    public static void tearDown() {
        if (bucketName != null)
            deleteBucketAndAllContents(bucketName);
    }

    private void testPutObject(String specialKeyName) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(OBJECT_CONTENTS.getBytes().length);
        s3.putObject(bucketName, specialKeyName, new ByteArrayInputStream(OBJECT_CONTENTS.getBytes()), metadata);
        
        List<S3ObjectSummary> objects = s3.listObjects(bucketName).getObjectSummaries();
        for (S3ObjectSummary object : objects) {
            if (object.getKey().equals(specialKeyName)) {
                return;
            }
        }
        fail("Cannot find " + specialKeyName + " in the bucket.");
    }
}
