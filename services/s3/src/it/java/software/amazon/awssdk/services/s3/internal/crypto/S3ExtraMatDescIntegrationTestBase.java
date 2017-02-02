package software.amazon.awssdk.services.s3.internal.crypto;

import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.awsTestCredentials;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.valueOf;
import static software.amazon.awssdk.services.s3.model.CryptoMode.EncryptionOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.KeyWrapException;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.CryptoStorageMode;
import software.amazon.awssdk.services.s3.model.EncryptedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.PutInstructionFileRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3ObjectId;
import software.amazon.awssdk.services.s3.model.SimpleMaterialProvider;
import software.amazon.awssdk.util.ImmutableMapParameter;
import software.amazon.awssdk.util.StringMapBuilder;

public abstract class S3ExtraMatDescIntegrationTestBase implements Headers {
    private static final String TEST_BUCKET = CryptoTestUtils.tempBucketName(S3ExtraMatDescIntegrationTestBase.class);
    /**
     * True to clean up the temp S3 objects created during test; false
     * otherwise.
     */
    private static boolean cleanup = true;

    @BeforeClass
    public static void setup() throws Exception {
        AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
        CryptoTestUtils.tryCreateBucket(s3, TEST_BUCKET);
        s3.shutdown();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (cleanup) {
            AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
            CryptoTestUtils.deleteBucketAndAllContents(s3, TEST_BUCKET);
            s3.shutdown();
        }
    }

    protected abstract CryptoMode cryptoMode();

    private SimpleMaterialProvider createTestMaterialProvider() {
        SimpleMaterialProvider smp = new SimpleMaterialProvider()
            .withLatest(new EncryptionMaterials(
                new SecretKeySpec(new byte[32], "AES"))
            .addDescription("id", "1st_kek"))
            ;
        byte[] cek2 = new byte[32];
        Arrays.fill(cek2, (byte)~0);
        EncryptionMaterials kek2 =
            new EncryptionMaterials(new SecretKeySpec(cek2, "AES"))
            .addDescription("id", "2nd_kek")
            ;
        smp.addMaterial(kek2);
        byte[] cek3 = new byte[32];
        Arrays.fill(cek3, (byte)1);
        EncryptionMaterials kek3 =
                new EncryptionMaterials(new SecretKeySpec(cek3, "AES"))
                ; // omit the id deliberately so empty description will get stored in S3
        smp.addMaterial(kek3);
        EncryptionMaterials kek3_for_get =
                new EncryptionMaterials(new SecretKeySpec(cek3, "AES"))
                .addDescription("id", "3rd_kek")
                ;
        smp.addMaterial(kek3_for_get);
        return smp;
    }

    @Test
    public void testExtraMaterialDesc() throws Exception {
        String yymmdd_hhmmss = new SimpleDateFormat("yyMMdd-hhmmss").format(new Date());
        String bucketName = TEST_BUCKET, key = "encrypted-" + yymmdd_hhmmss;
        String v2key = key + "-v2.txt";
        System.err.println(key + "/" + bucketName);
        SimpleMaterialProvider smp = createTestMaterialProvider();
        assertTrue(smp.size() == 4);
        AmazonS3EncryptionClient s3v2 = new AmazonS3EncryptionClient(
                awsTestCredentials(),
                smp,
                new CryptoConfiguration()
                    .withStorageMode(CryptoStorageMode.InstructionFile)
                    .withCryptoMode(cryptoMode())
        );
        File file = CryptoTestUtils.generateRandomAsciiFile(100);
        final String plaintext = FileUtils.readFileToString(file);
        System.err.println(plaintext);
        s3v2.putObject(bucketName, v2key, file);
        S3Object s3object = s3v2.getObject(bucketName, v2key);
        assertEquals(plaintext, valueOf(s3object));

        S3ObjectId s3ObjectId = new S3ObjectId(bucketName, v2key);
        // Create the 2nd instruction file
        s3v2.putInstructionFile(new PutInstructionFileRequest(
                s3ObjectId, 
                new ImmutableMapParameter.Builder<String, String>()
                    .put("id", "2nd_kek")
                    .build(),
                "instruction.2"));
        // Retrieve object via the 2nd instruction file
        s3object = s3v2.getObject(new EncryptedGetObjectRequest(s3ObjectId)
                .withInstructionFileSuffix("instruction.2"));
        assertEquals(plaintext, valueOf(s3object));

        s3v2.putInstructionFile(new PutInstructionFileRequest(
                s3ObjectId, 
                new ImmutableMapParameter.Builder<String, String>()
//                    .put("id", "3rd_kek")
                    .build(),
                "instruction.3"));
        // Remove the empty material description to test the per-request
        // supplemental material description
        smp.removeMaterial(Collections.EMPTY_MAP);
        assertTrue(smp.size() == 3);
        // Retrieve object via the 3rd instruction file
        s3object = s3v2.getObject(
            new EncryptedGetObjectRequest(s3ObjectId)
                    .withExtraMaterialsDescription(
                        new StringMapBuilder()
                            .put("id", "3rd_kek")
                            .build())
                    .withInstructionFileSuffix("instruction.3"));
        assertEquals(plaintext, valueOf(s3object));

        try {
            s3object = s3v2.getObject(
                    new EncryptedGetObjectRequest(s3ObjectId)
                            .withKeyWrapExpected(true)
                            .withExtraMaterialsDescription(
                                new StringMapBuilder()
                                    .put("id", "3rd_kek")
                                    .build())
                            .withInstructionFileSuffix("instruction.3"));
            if (cryptoMode() == EncryptionOnly)
                fail();
        } catch(KeyWrapException ex) {
            if (cryptoMode() != EncryptionOnly)
                fail();
        }
    }
}
