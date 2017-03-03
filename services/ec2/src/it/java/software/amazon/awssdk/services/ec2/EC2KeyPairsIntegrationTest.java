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

package software.amazon.awssdk.services.ec2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.ec2.model.CreateKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.DeleteKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsResult;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairResult;
import software.amazon.awssdk.services.ec2.model.KeyPair;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;

/**
 * Integration tests for the EC2 KeyPair operations.
 *
 * @author fulghum@amazon.com
 */
public class EC2KeyPairsIntegrationTest extends EC2IntegrationTestBase {

    /** KeyPair name for all tests to share. */
    private static String testKeyPairName = "keypair-integ-test-" + System.currentTimeMillis();

    /**
     * Ensures that any EC2 resources are correctly released after the tests.
     */
    @AfterClass
    public static void tearDown() {
        if (testKeyPairName != null) {
            deleteKeyPair(testKeyPairName);
        }
    }

    /**
     * Deletes the specified key pair.
     *
     * @param name
     *            The name of the key pair to delete.
     */
    private static void deleteKeyPair(String name) {
        ec2.deleteKeyPair(new DeleteKeyPairRequest(name));
    }

    /*
     * Individual Tests
     */

    /**
     * Runs the individual KeyPair tests in a specific order, so that we hit all
     * keypair operations, and so that tests that require data from earlier
     * tests run in the correct order.
     */
    @Test
    public void testKeyPairOperations() throws Exception {
        testCreateKeyPair();
        testImportKeyPair();
        testDescribeKeyPairs();
        testDeleteKeyPair();
    }

    /**
     * Tests that we can correctly create a new key pair.
     */
    private void testCreateKeyPair() {
        assertFalse(doesKeyPairExist(testKeyPairName));

        KeyPair keyPair = ec2.createKeyPair(new CreateKeyPairRequest()
                                                    .withKeyName(testKeyPairName)
                                           ).getKeyPair();

        assertEquals(testKeyPairName, keyPair.getKeyName());
        assertTrue(keyPair.getKeyFingerprint().length() > 10);
        assertTrue(keyPair.getKeyMaterial().length() > 20);

        assertTrue(doesKeyPairExist(testKeyPairName));
    }

    /**
     * Tests that we can correctly import a key pair.
     */
    private void testImportKeyPair() throws Exception {
        String keyName = "import-key-test-" + System.currentTimeMillis();
        String keyMaterial = loadResource("software/amazon/awssdk/services/ec2/public-key.txt");

        ImportKeyPairResult result = ec2.importKeyPair(
                new ImportKeyPairRequest(keyName, keyMaterial));
        assertEquals(keyName, result.getKeyName());
        assertNotNull(result.getKeyFingerprint());

        ec2.deleteKeyPair(new DeleteKeyPairRequest(keyName));
    }

    /**
     * Tests that we can find the keypair we previously created.
     */
    private void testDescribeKeyPairs() {

        // List all
        DescribeKeyPairsResult result = ec2.describeKeyPairs();
        Map<String, KeyPairInfo> keyPairMap = convertKeyPairListToMap(result.getKeyPairs());
        KeyPairInfo key = keyPairMap.get(testKeyPairName);
        assertNotNull(key);
        assertTrue(key.getKeyFingerprint().length() > 10);

        // List by filter
        List<KeyPairInfo> keyPairs = ec2.describeKeyPairs(
                new DescribeKeyPairsRequest()
                        .withFilters(new Filter()
                                             .withName("key-name")
                                             .withValues(testKeyPairName)
                                    )
                                                         ).getKeyPairs();
        assertEquals(1, keyPairs.size());
        assertEquals(testKeyPairName, keyPairs.get(0).getKeyName());
    }


    /*
     * Test Helper Methods
     */

    /**
     * Tests that we can delete the key pair we created earlier.
     */
    private void testDeleteKeyPair() {
        assertTrue(doesKeyPairExist(testKeyPairName));

        deleteKeyPair(testKeyPairName);

        assertFalse(doesKeyPairExist(testKeyPairName));
        testKeyPairName = null;
    }

    /**
     * Returns true if the specified key pair name exists.
     *
     * @param name
     *            The key pair name to test.
     *
     * @return True if a key pair already exists with the specified name,
     *         otherwise false.
     */
    private boolean doesKeyPairExist(String name) {
        try {
            DescribeKeyPairsResult result =
                    ec2.describeKeyPairs(new DescribeKeyPairsRequest()
                                                 .withKeyNames(name));
            return result.getKeyPairs().size() > 0;

        } catch (AmazonServiceException ase) {
            if ("InvalidKeyPair.NotFound".equals(ase.getErrorCode())) {
                return false;
            }
            throw ase;
        }
    }

    /**
     * Converts the specified list of key pairs to to a map of key pairs, keyed
     * by the key pair name.
     *
     * @param keys
     *            The key pairs to convert to a map.
     *
     * @return A map of the specified key pairs, keyed by the key pair name.
     */
    private Map<String, KeyPairInfo> convertKeyPairListToMap(List<KeyPairInfo> keys) {
        Map<String, KeyPairInfo> keyPairsByName = new HashMap<String, KeyPairInfo>();

        for (KeyPairInfo key : keys) {
            keyPairsByName.put(key.getKeyName(), key);
        }

        return keyPairsByName;
    }

}
