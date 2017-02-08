package software.amazon.awssdk.services.s3.internal.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.awsTestCredentials;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.bytesOf;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.generateRandomAsciiFile;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.getTestKeyPair;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.valueOf;
import static software.amazon.awssdk.util.StringUtils.UTF8;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.handlers.RequestHandler2;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.internal.AmazonS3TestClient;
import software.amazon.awssdk.services.s3.internal.S3CryptoTestClient;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.CryptoStorageMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.util.json.Jackson;

/**
 * Test the edge case of retrieving encrypted S3 object with no material description.
 */
public abstract class IssuesJAVA423IntegrationTestBase {
    private static final String TEST_BUCKET = CryptoTestUtils.tempBucketName(IssuesJAVA423IntegrationTestBase.class);
    /**
     * True to clean up the temp S3 objects created during test; false
     * otherwise.
     */
    private static boolean cleanup = true;

    @BeforeClass
    public static void setup() throws Exception {
        AmazonS3Client s3 = new AmazonS3TestClient(awsTestCredentials());
        CryptoTestUtils.tryCreateBucket(s3, TEST_BUCKET);
        s3.shutdown();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (cleanup) {
            AmazonS3Client s3 = new AmazonS3TestClient(awsTestCredentials());
            CryptoTestUtils.deleteBucketAndAllContents(s3, TEST_BUCKET);
            s3.shutdown();
        }
    }

    protected abstract CryptoMode cryptoMode();

    @Test
    public void testWihMetaDataRSA() throws Exception {
        EncryptionMaterials kekMaterial = new EncryptionMaterials(
                getTestKeyPair());
        doTestWithMetadata(kekMaterial);
    }

    @Test
    public void testWithMetadata() throws Exception {
        EncryptionMaterials kekMaterial = new EncryptionMaterials(
                new SecretKeySpec(new byte[16], "AES"));
        doTestWithMetadata(kekMaterial);
    }

    /**
     * Returns a S3 v1 client that removes empty material description so as
     * to test the edge case of retrieving encrypted S3 object with no material
     * description.
     */
    private AmazonS3EncryptionClient createS3v1Client(EncryptionMaterials kekMaterial, CryptoConfiguration config) throws IOException {
        AmazonS3EncryptionClient s3v1 = config == null 
            ? new S3CryptoTestClient(awsTestCredentials(), kekMaterial)
            : new S3CryptoTestClient(
                awsTestCredentials(),
                kekMaterial,
                config)
            ;
        // Add a handler to remove empty material description
        s3v1.addRequestHandler(new RequestHandler2() {
            @Override public void beforeRequest(Request<?> request) {
                Map<String,String> headers = request.getHeaders();
                String matdesc = headers.remove(Headers.S3_USER_METADATA_PREFIX
                        + Headers.MATERIALS_DESCRIPTION);
                if (matdesc != null)
                    assertEquals("{}", matdesc);
            }
            @Override public void afterResponse(Request<?> request, Response<?> response) {}
            @Override public void afterError(Request<?> request, Response<?> response, Exception e) {}
        });
        return s3v1;
    }

    private void doTestWithMetadata(EncryptionMaterials kekMaterial) throws Exception {
        String yymmdd_hhmmss = new SimpleDateFormat("yyMMdd-hhmmss").format(new Date());
        final String bucketName = TEST_BUCKET, key = "encrypted-" + yymmdd_hhmmss;
        String v1key = key + "-v1.txt";
        System.err.println(key + "/" + bucketName);

        AmazonS3EncryptionClient s3v1 = createS3v1Client(kekMaterial, null);
        // A S3 raw client used to inspect the raw data
        AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());

        File file = generateRandomAsciiFile(100);
        final String plaintext = FileUtils.readFileToString(file);
//        System.err.println(plaintext);

        // upload file to s3 using v1 and get it back
        s3v1.putObject(bucketName, v1key, file);
        // verify s3v1 is able to read back and decrypt the s3 object
        S3Object s3object = s3v1.getObject(bucketName, v1key);
        assertEquals(plaintext, valueOf(s3object));
        // Check the raw user metadata
        s3object = s3.getObject(bucketName, v1key);
        Map<String, String> map = s3object.getObjectMetadata().getUserMetadata();
        System.err.println(map.size() + ": " + map);
        assertNull(map.get(Headers.CRYPTO_CEK_ALGORITHM));
        assertNull(map.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));
        byte[] v1raw = bytesOf(s3object);
        assertFalse(Arrays.equals(v1raw, plaintext.getBytes(UTF8)));

        if (cleanup) {
            s3.deleteObject(bucketName, v1key);
        }
    }

    @Test
    public void testWithInstFileRSA() throws Exception {
        EncryptionMaterials kekMaterial = new EncryptionMaterials(
                getTestKeyPair());
        doTestWithInstFile(kekMaterial);
    }

    @Test
    public void testWithInstFile() throws Exception {
        EncryptionMaterials kekMaterial = new EncryptionMaterials(
                new SecretKeySpec(new byte[16], "AES"));
        doTestWithInstFile(kekMaterial);
    }

    public void doTestWithInstFile(EncryptionMaterials kekMaterial) throws Exception {
        String yymmdd_hhmmss = new SimpleDateFormat("yyMMdd-hhmmss").format(new Date());
        String bucketName = TEST_BUCKET, key = "encrypted-" + yymmdd_hhmmss;
        String v1key = key + "-v1.txt";
        System.err.println(key + "/" + bucketName);

        AmazonS3EncryptionClient s3v1 = createS3v1Client(kekMaterial,
                new CryptoConfiguration()
                    .withStorageMode(CryptoStorageMode.InstructionFile)
        );
        // A S3 raw client used to inspect the raw data
        AmazonS3Client s3 = new AmazonS3TestClient(awsTestCredentials());

        File file = CryptoTestUtils.generateRandomAsciiFile(100);
        final String plaintext = FileUtils.readFileToString(file);
        System.err.println(plaintext);

        // upload file to s3 using v1 and get it back
        s3v1.putObject(bucketName, v1key, file);
        // verify s3v1 is able to read back and decrypt the s3 object
        
        S3Object s3object = s3v1.getObject(bucketName, v1key);
        assertEquals(plaintext, valueOf(s3object));
        // Check the instruction file for v1 format
        s3object = s3v1.getObject(bucketName, v1key + ".instruction");
        String json = ContentCryptoMaterial.parseInstructionFile(s3object);
        @SuppressWarnings("unchecked")
        Map<String,String> imap = Jackson.fromJsonString(json, Map.class);
        assertNull(imap.get(Headers.CRYPTO_CEK_ALGORITHM));
        assertNull(imap.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));

        // Check the raw user metadata
        s3object = s3.getObject(bucketName, v1key);
        Map<String, String> map = s3object.getObjectMetadata().getUserMetadata();
        System.err.println(map.size() + ": " + map);
        assertNull(map.get(Headers.CRYPTO_CEK_ALGORITHM));
        assertNull(map.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));
        byte[] v1raw = bytesOf(s3object);
        assertFalse(Arrays.equals(v1raw, plaintext.getBytes(UTF8)));

        if (cleanup) {
            s3.deleteObject(bucketName, v1key);
            s3.deleteObject(bucketName, v1key + ".instruction");
        }
    }
}
