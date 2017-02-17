/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3.internal.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.crypto.ContentCryptoScheme.AES_CBC;
import static software.amazon.awssdk.services.s3.internal.crypto.ContentCryptoScheme.AES_GCM;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.awsTestCredentials;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.valueOf;
import static software.amazon.awssdk.services.s3.model.CryptoMode.EncryptionOnly;
import static software.amazon.awssdk.services.s3.model.CryptoMode.StrictAuthenticatedEncryption;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.jmx.JmxInfoProviderSupport;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.Headers;
import software.amazon.awssdk.services.s3.KeyWrapException;
import software.amazon.awssdk.services.s3.categories.S3Categories;
import software.amazon.awssdk.services.s3.internal.AmazonS3TestClient;
import software.amazon.awssdk.services.s3.internal.S3CryptoTestClient;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
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
import software.amazon.awssdk.test.util.Memory;
import software.amazon.awssdk.util.Base16;
import software.amazon.awssdk.util.StringMapBuilder;
import software.amazon.awssdk.util.json.Jackson;

@Category(S3Categories.ReallySlow.class)
public class S3InstFileIntegrationTest implements Headers {

    private static final String TEST_BUCKET = CryptoTestUtils.tempBucketName(S3InstFileIntegrationTest.class);
    private static final Map<String, AmazonS3EncryptionClient> cryptoClientMap =
            new HashMap<String, AmazonS3EncryptionClient>();
    private static final SimpleMaterialProvider[] materialProviders = new SimpleMaterialProvider[2];
    /**
     * True to clean up the temp S3 objects created during test; false
     * otherwise.
     */
    private static boolean cleanup = true;
    private static JmxInfoProviderSupport jmx = new JmxInfoProviderSupport();
    private static AmazonS3Client s3;

    @BeforeClass
    public static void setup() throws Exception {
        System.err.println(Memory.poolSummaries());
        s3 = new AmazonS3TestClient(awsTestCredentials());
        CryptoTestUtils.tryCreateBucket(s3, TEST_BUCKET);
        materialProviders[0] = createTestMaterialProvider(true);
        materialProviders[1] = createTestMaterialProvider(false);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (cleanup) {
            CryptoTestUtils.deleteBucketAndAllContents(s3, TEST_BUCKET);
            s3.shutdown();
            for (AmazonS3EncryptionClient c : cryptoClientMap.values()) {
                c.shutdown();
            }
        }
    }


    private static SimpleMaterialProvider getTestMaterialProvider(
            final boolean kekAes) throws NoSuchAlgorithmException,
                                         InvalidKeySpecException {
        return materialProviders[kekAes ? 0 : 1];
    }

    private static SimpleMaterialProvider createTestMaterialProvider(final boolean kekAes)
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
                        .addDescription("id", "from_kek_aes"));
        smp.addMaterial(
                new EncryptionMaterials(CryptoTestUtils.getTestKeyPair())
                        .addDescription("id", "from_kek_pub"))
        ;
        smp.addMaterial(
                new EncryptionMaterials(CryptoTestUtils.getTestSecretKey1())
                        .addDescription("id", "to_kek_aes"))
        ;
        smp.addMaterial(
                new EncryptionMaterials(CryptoTestUtils.getTestKeyPair1())
                        .addDescription("id", "to_kek_pub"))
        ;
        return smp;
    }

    private static AmazonS3EncryptionClient getAmazonS3EncryptionClient(
            boolean kekAes, CryptoStorageMode storageMode, CryptoMode cryptoMode)
            throws NoSuchAlgorithmException, InvalidKeySpecException,
                   UnsupportedOperationException, IOException {
        final String key = kekAes + "," + storageMode + "," + cryptoMode;
        AmazonS3EncryptionClient c = cryptoClientMap.get(key);
        if (c == null) {
            SimpleMaterialProvider materialProvider = getTestMaterialProvider(kekAes);
            c = new S3CryptoTestClient(
                    awsTestCredentials(),
                    materialProvider,
                    new ClientConfiguration().withConnectionTtl(1),
                    new CryptoConfiguration()
                            .withStorageMode(storageMode)
                            .withCryptoMode(cryptoMode)
                            .withIgnoreMissingInstructionFile(false)
            );
            cryptoClientMap.put(key, c);
        }
        return c;
    }

    private long fdInfo() {
        long[] fdInfo = jmx.getFileDecriptorInfo();
        return fdInfo[0];
    }

    @Test
    public void testWithInstFile() throws Exception {
        CryptoMode[] cryptoModeFroms = CryptoMode.values();
        CryptoMode[] cryptoModeTos = CryptoMode.values();
        CryptoStorageMode[] storageModeFroms = CryptoStorageMode.values();
        CryptoStorageMode[] storageModeTos = CryptoStorageMode.values();
        String[] materialIdFroms = {"from_kek_aes", "from_kek_pub"};
        String[] materialIdTos = {"to_kek_aes", "to_kek_pub"};
        boolean[] keyWrapExpected = {true, false};
        boolean[] useMatDesc = {true, false};
        boolean[] kekAes = {true, false};

        /*
         * Generates a catesian product of all possible combinations of the
         * parameter values defined above. This results in a total of 3^2*2^7 =
         * 1,152 test cases. Empirically this generated some interesting
         * edge cases never conceived, and uncover a few bugs!
         */
        IndexValues iv = new IndexValues(cryptoModeFroms.length,
                                         cryptoModeTos.length, storageModeFroms.length,
                                         storageModeTos.length, materialIdFroms.length,
                                         materialIdTos.length, keyWrapExpected.length,
                                         useMatDesc.length, kekAes.length);
        System.out.println("Beginning open fd: " + fdInfo());
        int testCaseIdx = 0;
        System.err.println("testCaseIdx=" + testCaseIdx + ", "
                           + Memory.heapSummary());
        for (int[] testcase : iv) {
            int i = 0;
            System.err.println("testcase: " + Arrays.toString(testcase));
            // Uncomment the if statement to test   a specific test case
            try {
                doTestWithInstFile(testCaseIdx, cryptoModeFroms[testcase[i++]],
                                   cryptoModeTos[testcase[i++]],
                                   storageModeFroms[testcase[i++]],
                                   storageModeTos[testcase[i++]],
                                   materialIdFroms[testcase[i++]],
                                   materialIdTos[testcase[i++]],
                                   keyWrapExpected[testcase[i++]],
                                   useMatDesc[testcase[i++]],
                                   kekAes[testcase[i++]]);
                System.out.println("testCaseIdx=" + testCaseIdx + ", open fd=" + fdInfo());
            } catch (AmazonS3Exception ex) {
                ex.printStackTrace(System.err);
                System.err.println("Ignoring " + ex.getMessage());
            }
            System.err.println("testCaseIdx=" + testCaseIdx + ", "
                               + Memory.heapSummary());
            testCaseIdx++;
        }
    }

    public void doTestWithInstFile(int testCaseIdx,
                                   CryptoMode cryptoModeFrom,
                                   CryptoMode cryptoModeTo,
                                   CryptoStorageMode storageModeFrom,
                                   CryptoStorageMode storageModeTo,
                                   String materialIdFrom,
                                   String materialIdTo,
                                   boolean keyWrapExpected,
                                   boolean useMatDesc,
                                   boolean kekAes
                                  ) throws Exception {
        final String msg = "doTestWithInstFile cryptoModeFrom="
                           + cryptoModeFrom + ", cryptoModeTo=" + cryptoModeTo
                           + ", storageModeFrom=" + storageModeFrom + ", storageModeTo="
                           + storageModeTo
                           + ", materialIdFrom=" + materialIdFrom
                           + ", materialIdTo=" + materialIdTo
                           + ", keyWrapExpected=" + keyWrapExpected
                           + ", useMatDesc=" + useMatDesc
                           + ", kekAes=" + kekAes;
        System.err.println(msg);
        final String bucketName = TEST_BUCKET;
        final String key = "encrypted-" + testCaseIdx;
        System.err.println(bucketName + "/" + key);
        SimpleMaterialProvider materialProvider = getTestMaterialProvider(kekAes);
        // A S3 raw client used to inspect the raw data
        AmazonS3EncryptionClient s3from = getAmazonS3EncryptionClient(kekAes, storageModeFrom, cryptoModeFrom);
        AmazonS3EncryptionClient s3to = getAmazonS3EncryptionClient(kekAes, storageModeTo, cryptoModeTo);
        File file = CryptoTestUtils.generateRandomAsciiFile(100);
        byte[] orig = FileUtils.readFileToByteArray(file);
        System.err.println(new String(orig, "UTF-8"));
        s3from.putObject(bucketName, key, file);
        final long fd_begin = fdInfo();
        decryptAndCompare(s3from,
                          new GetObjectRequest(bucketName, key),
                          orig, msg);
        final long fd_after_get = fdInfo();
        S3ObjectId s3ObjectId = new S3ObjectId(bucketName, key);
        // Create a new instruction file
        Map<String, String> toMatDesc = new StringMapBuilder()
                .put("id", kekAes ? "to_kek_aes" : "to_kek_pub")
                .build();
        final String suffix = "instruction.2";
        final long fd_after_put_ifile;
        try {
            if (useMatDesc) { // use material description for the new kek
                s3to.putInstructionFile(new PutInstructionFileRequest(s3ObjectId, toMatDesc, suffix));
            } else {    // use encryption material directly for the new kek
                s3to.putInstructionFile(new PutInstructionFileRequest(
                        s3ObjectId,
                        materialProvider.getEncryptionMaterials(toMatDesc),
                        suffix));
            }
            fd_after_put_ifile = fdInfo();
            if (cryptoModeFrom == EncryptionOnly) {
                if (cryptoModeTo == StrictAuthenticatedEncryption) {
                    fail();
                }
            } else {    // AE lowed to EO is not allowed
                if (cryptoModeTo == EncryptionOnly) {
                    fail();
                }
            }
        } catch (SecurityException ex) {
            if (cryptoModeFrom == EncryptionOnly) {
                if (cryptoModeTo != StrictAuthenticatedEncryption) {
                    throw ex;
                }
            } else {
                if (cryptoModeTo != EncryptionOnly) {
                    throw ex;
                }
            }
            return; // skip the rest of this test
        }
        // Check the 2nd instruction file
        InstructionFileId ifid = s3ObjectId.instructionFileId(suffix);
        S3Object ifile = s3.getObject(new GetObjectRequest(ifid.getBucket(), ifid.getKey()));
        String json = valueOf(ifile);
        final long fd_after_get_ifile = fdInfo();
        long fd_after_get2 = 0;
        verifyInstructionFile(cryptoModeFrom, cryptoModeTo, kekAes, json);
        // Retrieve object via the 2nd instruction file
        try {
            decryptAndCompare(s3to,
                              new EncryptedGetObjectRequest(s3ObjectId)
                                      .withInstructionFileSuffix(suffix)
                                      .withKeyWrapExpected(keyWrapExpected), orig, msg);
            fd_after_get2 = fdInfo();
            if (keyWrapExpected && cryptoModeTo == EncryptionOnly) {
                fail();
            }
        } catch (KeyWrapException ex) {
            if (keyWrapExpected && cryptoModeTo != EncryptionOnly) {
                fail();
            }
        }

        if (cleanup) {
            deleteObjects(bucketName, key, s3from, ifid);
        }
        final long fd_after_delete = fdInfo();

        System.err.printf("fd_begin=%d, fd_after_get=%d, fd_after_put_ifile=%d, fd_after_get_ifile=%d, fd_after_get2=%d, fd_after_delete=%d\n",
                          fd_begin, fd_after_get, fd_after_put_ifile,
                          fd_after_get_ifile, fd_after_get2, fd_after_delete);
    }

    private void decryptAndCompare(AmazonS3 s3, GetObjectRequest req,
                                   byte[] expected, String msg) throws InterruptedException,
                                                                       IOException {
        S3Object obj = s3.getObject(req);
        byte[] actual = IOUtils.toByteArray(obj.getObjectContent());
        if (Arrays.equals(expected, actual)) {
            return;
        }
        String errmsg = "inconsistent plaintext. actual="
                        + Base16.encodeAsString(actual) + ", expected="
                        + Base16.encodeAsString(expected) + ", msg=" + msg;
        System.err.println(errmsg);
        throw new AssertionError(errmsg);
    }

    private void verifyInstructionFile(CryptoMode cryptoModeFrom,
                                       CryptoMode cryptoModeTo, boolean kekAes, String json) {
        @SuppressWarnings("unchecked")
        Map<String, String> imap = Jackson.fromJsonString(json, Map.class);
        if (cryptoModeTo == EncryptionOnly) {
            assertNull(imap.get(Headers.CRYPTO_CEK_ALGORITHM));
            assertNull(imap.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));
        } else {
            if (cryptoModeFrom == EncryptionOnly) {
                assertEquals(AES_CBC.getCipherAlgorithm(),
                             imap.get(Headers.CRYPTO_CEK_ALGORITHM));
            } else {
                assertEquals(AES_GCM.getCipherAlgorithm(),
                             imap.get(Headers.CRYPTO_CEK_ALGORITHM));
            }
            if (kekAes) {
                assertEquals(S3KeyWrapScheme.AESWrap,
                             imap.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));
            } else {
                assertEquals(
                        S3KeyWrapScheme.RSA_ECB_OAEPWithSHA256AndMGF1Padding,
                        imap.get(Headers.CRYPTO_KEYWRAP_ALGORITHM));
            }
        }
    }

    private void deleteObjects(final String bucketName, final String key,
                               AmazonS3EncryptionClient s3from,
                               InstructionFileId ifid) {
        try {
            s3from.deleteObject(new DeleteObjectRequest(bucketName, key));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            s3.deleteObject(new DeleteObjectRequest(ifid.getBucket(), ifid.getKey()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
