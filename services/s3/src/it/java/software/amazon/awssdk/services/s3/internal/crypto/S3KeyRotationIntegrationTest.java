package software.amazon.awssdk.services.s3.internal.crypto;

import static software.amazon.awssdk.services.s3.internal.crypto.ContentCryptoScheme.AES_CTR;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.awsTestCredentials;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.valueOf;
import static software.amazon.awssdk.services.s3.model.CryptoMode.EncryptionOnly;
import static software.amazon.awssdk.services.s3.model.CryptoMode.StrictAuthenticatedEncryption;
import static software.amazon.awssdk.services.s3.model.InstructionFileId.DEFAULT_INSTRUCTION_FILE_SUFFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.internal.AmazonS3TestClient;
import software.amazon.awssdk.services.s3.internal.S3CryptoTestClient;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoMode;
import software.amazon.awssdk.services.s3.model.CryptoStorageMode;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.EncryptedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.InstructionFileId;
import software.amazon.awssdk.services.s3.model.PutInstructionFileRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3ObjectId;
import software.amazon.awssdk.services.s3.model.SimpleMaterialProvider;
import software.amazon.awssdk.test.util.IndexValues;
import software.amazon.awssdk.util.StringMapBuilder;

@Category(S3Categories.Slow.class)
public class S3KeyRotationIntegrationTest implements Headers {
    private static final String TEST_BUCKET = CryptoTestUtils.tempBucketName(S3KeyRotationIntegrationTest.class);
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

    private SimpleMaterialProvider createTestMaterialProvider(final boolean kekAes)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        SimpleMaterialProvider smp = new SimpleMaterialProvider() {
            @Override
            public EncryptionMaterials getEncryptionMaterials() {
                return getEncryptionMaterials(
                        new StringMapBuilder("id",
                        kekAes ? "from_kek_aes" : "from_kek_pub")
                        .build());
            }
        }.addMaterial(
                new EncryptionMaterials(CryptoTestUtils.getTestSecretKey())
                .addDescription("id", "from_kek_aes"))
                ;
        smp.addMaterial(
                new EncryptionMaterials(CryptoTestUtils.getTestKeyPair())
                .addDescription("id", "from_kek_pub"))
                ;
        smp.addMaterial(
                new EncryptionMaterials(CryptoTestUtils.generateSecretKey(
                    AES_CTR.getKeyGeneratorAlgorithm(),
                    AES_CTR.getKeyLengthInBits()))
                .addDescription("id", "to_kek_aes"))
                ;
        smp.addMaterial(
                new EncryptionMaterials(CryptoTestUtils.generateKeyPair("RSA", 1024))
                .addDescription("id", "to_kek_pub"))
                ;
        return smp;
    }

    @Test
    public void testKekRotation() throws Exception {
        CryptoMode[] cryptoModeFroms = CryptoMode.values();
        CryptoMode[] cryptoModeTos = CryptoMode.values();
        CryptoStorageMode[] storageModeFroms = CryptoStorageMode.values();
        CryptoStorageMode[] storageModeTos = CryptoStorageMode.values();
        String[] materialIdFroms = {"from_kek_aes", "from_kek_pub"};
        String[] materialIdTos = {"to_kek_aes", "to_kek_pub"};
        boolean[] kekAes = {true, false};

        IndexValues iv = new IndexValues(cryptoModeFroms.length,
                cryptoModeTos.length, storageModeFroms.length,
                storageModeTos.length, materialIdFroms.length,
                materialIdTos.length,
                kekAes.length);
        int testCaseIdx=0;
        for (int[] testcase : iv) {
            int i=0;
            System.err.println(Arrays.toString(testcase));
//            if (testCaseIdx >= 142) {
            doTestKekRotation(testCaseIdx, cryptoModeFroms[testcase[i++]],
                    cryptoModeTos[testcase[i++]],
                    storageModeFroms[testcase[i++]],
                    storageModeTos[testcase[i++]],
                    materialIdFroms[testcase[i++]],
                    materialIdTos[testcase[i++]],
                    kekAes[testcase[i++]]);
//            }
            testCaseIdx++;
        }
    }

    public void doTestKekRotation(int testCaseIdx,
            CryptoMode cryptoModeFrom,
            CryptoMode cryptoModeTo,
            CryptoStorageMode storageModeFrom,
            CryptoStorageMode storageModeTo,
            String materialIdFrom,
            String materialIdTo,
            boolean kekAes
    ) throws Exception {
        System.err.println("doTestWithInstFile cryptoModeFrom="
                + cryptoModeFrom + ", cryptoModeTo=" + cryptoModeTo
                + ", storageModeFrom=" + storageModeFrom + ", storageModeTo="
                + storageModeTo
                + ", materialIdFrom=" + materialIdFrom
                + ", materialIdTo=" + materialIdTo
                + ", kekAes=" + kekAes
                );
        final String bucketName = TEST_BUCKET;
        final String key = "encrypted-" + testCaseIdx;
        System.err.println(bucketName + "/" + key);
        SimpleMaterialProvider materialProvider = createTestMaterialProvider(kekAes);
        // A S3 raw client used to inspect the raw data
        AmazonS3Client s3 = new AmazonS3TestClient(awsTestCredentials());
        S3CryptoTestClient s3from = new S3CryptoTestClient(
                awsTestCredentials(),
                materialProvider,
                new CryptoConfiguration()
                    .withStorageMode(storageModeFrom)
                    .withCryptoMode(cryptoModeFrom)
                    .withIgnoreMissingInstructionFile(false)
        );
        S3CryptoTestClient s3to = new S3CryptoTestClient(
                awsTestCredentials(),
                materialProvider,
                new CryptoConfiguration()
                    .withStorageMode(storageModeTo)
                    .withCryptoMode(cryptoModeTo)
                    .withIgnoreMissingInstructionFile(false)
        );
        File file = CryptoTestUtils.generateRandomAsciiFile(100);
        final String plaintext = FileUtils.readFileToString(file);
        System.err.println(plaintext);
        s3from.putObject(bucketName, key, file);
        S3Object s3object = s3from.getObject(bucketName, key);
        assertEquals(plaintext, valueOf(s3object));
        S3ObjectId s3ObjectId = new S3ObjectId(bucketName, key);
        // Create a new instruction file
        Map<String,String> toMatDesc = new StringMapBuilder()
            .put("id", kekAes ? "to_kek_aes" : "to_kek_pub")
            .build();
        try {
            s3to.putInstructionFile(new PutInstructionFileRequest(
                    s3ObjectId,
                    toMatDesc,
                    InstructionFileId.DEFAULT_INSTRUCTION_FILE_SUFFIX));
            if (cryptoModeFrom == EncryptionOnly) {
                if (cryptoModeTo == StrictAuthenticatedEncryption)
                    fail();
            } else {    // AE lowed to EO is not allowed
                if (cryptoModeTo == EncryptionOnly)
                    fail();
            }
        } catch(SecurityException ex) {
            if (cryptoModeFrom == EncryptionOnly) {
                if (cryptoModeTo != StrictAuthenticatedEncryption)
                    throw ex;
            } else {
                if (cryptoModeTo != EncryptionOnly)
                    throw ex;
            }
            return; // skip the rest of this test
        }
        // Retrieve the object using the default mechanism
        s3object = s3to.getObject(new GetObjectRequest(s3ObjectId));
        assertEquals(plaintext, valueOf(s3object));

        // Retrieve the object via instruction file
        s3object = s3to.getObject(new EncryptedGetObjectRequest(s3ObjectId)
                .withInstructionFileSuffix(DEFAULT_INSTRUCTION_FILE_SUFFIX))
                ;
        assertEquals(plaintext, valueOf(s3object));

        if (cleanup) {
            try {
                s3from.deleteObject(new DeleteObjectRequest(bucketName, key));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            InstructionFileId ifid = s3ObjectId.instructionFileId(DEFAULT_INSTRUCTION_FILE_SUFFIX);
            try {
                s3.deleteObject(new DeleteObjectRequest(ifid.getBucket(), ifid.getKey()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        s3.shutdown();
        s3from.shutdown();
        s3to.shutdown();
    }
}
