package software.amazon.awssdk.services.s3.internal.crypto;

import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.awsTestCredentials;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.deleteBucketAndAllContents;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.generateRandomAsciiFile;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tempBucketName;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tryCreateBucket;
import static software.amazon.awssdk.util.StringUtils.UTF8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.CryptoStorageMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.test.util.IndexValues;

@Category(S3Categories.Slow.class)
public class S3RangeGetCompatIntegrationTest {
    /**
     * True to clean up the temp S3 objects created during test; false
     * otherwise.
     */
    private static boolean cleanup = true;
    private static boolean get_only = false;

    private static final String TEST_BUCKET = tempBucketName(S3RangeGetCompatIntegrationTest.class);

    @BeforeClass
    public static void setup() throws Exception {
        AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
        tryCreateBucket(s3, TEST_BUCKET);
        s3.shutdown();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (cleanup) {
            AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
            deleteBucketAndAllContents(s3, TEST_BUCKET);
            s3.shutdown();
        }
    }
    

    private void doTestBackwardCompatibility(EncryptionMaterials kekMaterial,
            CryptoStorageMode storageMode, CryptoMode readMode, CryptoMode writeMode)
            throws Exception {
        CryptoConfiguration configRead = new CryptoConfiguration().withCryptoMode(readMode);

        AmazonS3EncryptionClient s3Reader = new AmazonS3EncryptionClient(
                awsTestCredentials(),
                kekMaterial,
                configRead
        );
        AmazonS3Client s3Writer = writeMode == CryptoMode.EncryptionOnly
                ? new AmazonS3EncryptionClient(
                        awsTestCredentials(), 
                        kekMaterial, 
                        new CryptoConfiguration().withStorageMode(storageMode))
                : new AmazonS3EncryptionClient(
                        awsTestCredentials(),
                        kekMaterial,
                        new CryptoConfiguration()
                            .withCryptoMode(writeMode)
                            .withStorageMode(storageMode)
        );
        // A S3 raw client used to inspect the raw data
        AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
        final int pt_size = 100;
        final String bucketName = TEST_BUCKET;
        String key;
        
        String plaintext = null;
        if (get_only) {
            key = "encrypted-140325-012844-v2.txt";    
        } else {
            String yymmdd_hhmmss = new SimpleDateFormat("yyMMdd-hhmmss").format(new Date());
            key = "encrypted-" + yymmdd_hhmmss + "-" + writeMode +".txt";
            System.err.println(bucketName + "/" + key);
            File file = generateRandomAsciiFile(pt_size);
            plaintext = FileUtils.readFileToString(file);
            System.err.println(plaintext);
            // upload file to s3 using v2
            s3Writer.putObject(bucketName, key, file);
        }
        int[][] test_ranges = {
                {1, 0},  // last 10 bytes
                {100, 9},  // last 10 bytes
                {0, 0},     // first byte
                {99, 99},   // last byte
                {99, 100},  // 1 past the last byte
                {0, 9},     // first 10 bytes
                {90, 99},   // last 10 bytes
                {90, 116},  // last 10 bytes
                {90, 120},  // last 10 bytes
        };
        for (int[] test_range: test_ranges) {
            int beginIndex = test_range[0];
            int endIndex = test_range[1];
            GetObjectRequest req = new GetObjectRequest(bucketName, key).withRange(beginIndex, endIndex);
            S3Object s3object;
            try {
                s3object = s3Reader.getObject(req);
                if (CryptoMode.StrictAuthenticatedEncryption.equals(readMode))
                    fail();
            } catch(SecurityException ex) {
                if (CryptoMode.StrictAuthenticatedEncryption.equals(readMode))
                    continue;
                else
                    throw ex;
            }
            long instanceLen = s3object.getObjectMetadata().getInstanceLength();
            assertTrue(pt_size <= instanceLen);
            byte[] retrieved = IOUtils.toByteArray(s3object.getObjectContent());
            int expectedLen;
            
            if (endIndex < beginIndex) {
                expectedLen = plaintext.length();
                beginIndex = 0;
            } else {
                expectedLen = Math.min(plaintext.length() - beginIndex,
                    Math.max(0, endIndex - beginIndex + 1));
            }
//            System.out.println("retrieved.length=" + retrieved.length);
            assertTrue(expectedLen == retrieved.length);
            if (retrieved.length > 0) {
                String result = new String(retrieved, UTF8);
                System.out.println(result);
                if (!get_only) {
                    String expected = plaintext.substring(beginIndex, beginIndex+expectedLen); 
                    assertEquals(expected, result);
                }
            }
        }
        if (cleanup) {
            s3.deleteObject(bucketName, key);
            if (storageMode == CryptoStorageMode.InstructionFile)
                s3.deleteObject(bucketName, key + ".instruction");
        }
        s3.shutdown();
        s3Reader.shutdown();
        s3Writer.shutdown();
    }

    @Test
    public void testBackwardCompatibility() throws Exception {
        EncryptionMaterials[] kekMaterials = {
                new EncryptionMaterials(CryptoTestUtils.getTestSecretKey()),
                new EncryptionMaterials(CryptoTestUtils.getTestKeyPair())
        };
        CryptoStorageMode[] storageModes = CryptoStorageMode.values();
        CryptoMode[] modes = CryptoMode.values();
        // Test out all combinations of storage modes, crypto modes for read
        // and write, and symmetric key vs PKI for kek
        /*
            readCryptoMode=EO, writeCryptoMode=EO, storageMode=InstructionFile, kekMaterial: SymmetricKey
            readCryptoMode=EO, writeCryptoMode=EO, storageMode=InstructionFile, kekMaterial: PKI
            readCryptoMode=EO, writeCryptoMode=AE, storageMode=InstructionFile, kekMaterial: SymmetricKey
            readCryptoMode=EO, writeCryptoMode=AE, storageMode=InstructionFile, kekMaterial: PKI
            readCryptoMode=AE, writeCryptoMode=EO, storageMode=InstructionFile, kekMaterial: SymmetricKey
            readCryptoMode=AE, writeCryptoMode=EO, storageMode=InstructionFile, kekMaterial: PKI
            readCryptoMode=AE, writeCryptoMode=AE, storageMode=InstructionFile, kekMaterial: SymmetricKey
            readCryptoMode=AE, writeCryptoMode=AE, storageMode=InstructionFile, kekMaterial: PKI
            readCryptoMode=EO, writeCryptoMode=EO, storageMode=ObjectMetadata, kekMaterial: SymmetricKey
            readCryptoMode=EO, writeCryptoMode=EO, storageMode=ObjectMetadata, kekMaterial: PKI
            readCryptoMode=EO, writeCryptoMode=AE, storageMode=ObjectMetadata, kekMaterial: SymmetricKey
            readCryptoMode=EO, writeCryptoMode=AE, storageMode=ObjectMetadata, kekMaterial: PKI
            readCryptoMode=AE, writeCryptoMode=EO, storageMode=ObjectMetadata, kekMaterial: SymmetricKey
            readCryptoMode=AE, writeCryptoMode=EO, storageMode=ObjectMetadata, kekMaterial: PKI
            readCryptoMode=AE, writeCryptoMode=AE, storageMode=ObjectMetadata, kekMaterial: SymmetricKey
            readCryptoMode=AE, writeCryptoMode=AE, storageMode=ObjectMetadata, kekMaterial: PKI
         */
        for (int[] indexes : new IndexValues(storageModes.length, modes.length,
                modes.length, kekMaterials.length)) {
            int i = 0;
            CryptoStorageMode storage = storageModes[indexes[i++]];
            CryptoMode readMode = modes[indexes[i++]];
            CryptoMode writeMode = modes[indexes[i++]];
            EncryptionMaterials kekMaterial = kekMaterials[indexes[i++]];
            System.err.println("readCryptoMode=" + readMode
                    + ", writeCryptoMode=" + writeMode
                    + ", storageMode=" + storage
                    + ", kekMaterial: "
                    + (kekMaterial.getKeyPair() == null ? "SymmetricKey"
                            : "PKI"));
            doTestBackwardCompatibility(kekMaterial, storage, readMode, writeMode);
        }
    }
}
