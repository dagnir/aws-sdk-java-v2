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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.newBouncyCastleProvider;

import java.security.Security;
import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoMode;

public class CryptoConfigurationTest {

    @AfterClass
    public static void after() throws Exception {
        CryptoRuntime.enableBouncyCastle();
        assertTrue(Security.addProvider(newBouncyCastleProvider()) == -1);
        // Only necessary in unit test when the same class loader is used across
        // multiple unit tests, like during brazil-build.
        CryptoRuntime.recheck();
        assertTrue(CryptoRuntime.isBouncyCastleAvailable());
        assertTrue(CryptoRuntime.isAesGcmAvailable());
    }

    @Test
    public void testBouncyCastleNotAvailable() {
        // trigger a one-time initialization
        CryptoRuntime.enableBouncyCastle();
        assertTrue(CryptoRuntime.isAesGcmAvailable());
        // remove BC explicitly
        Security.removeProvider("BC");
        // The cached value would still be true
        assertTrue(CryptoRuntime.isAesGcmAvailable());
        // Refresh cached value
        CryptoRuntime.recheckAesGcmAvailablility();
        assertFalse(CryptoRuntime.isAesGcmAvailable());
        CryptoConfiguration config = new CryptoConfiguration();
        try {
            config.withCryptoMode(CryptoMode.AuthenticatedEncryption);
            fail();
        } catch (UnsupportedOperationException expected) {
            // Ignored or expected.
        }
        // property is set despite the exception
        assertEquals(CryptoMode.AuthenticatedEncryption, config.getCryptoMode());
        config.setCryptoMode(null);
        try {
            config.setCryptoMode(CryptoMode.AuthenticatedEncryption);
            fail();
        } catch (UnsupportedOperationException expected) {
            // Ignored or expected.
        }
        // property is set despite the exception
        assertEquals(CryptoMode.AuthenticatedEncryption, config.getCryptoMode());
    }

    @Test
    public void testIgnoreMissingInstructionFile() {
        CryptoConfiguration c = new CryptoConfiguration();
        // default is true
        assertTrue(c.isIgnoreMissingInstructionFile());
        // change to false
        c.withIgnoreMissingInstructionFile(false);
        assertFalse(c.isIgnoreMissingInstructionFile());
        // change back to true
        c.setIgnoreMissingInstructionFile(true);
        assertTrue(c.isIgnoreMissingInstructionFile());
        // the cloned should also be true
        assertTrue(c.clone().readOnly().isIgnoreMissingInstructionFile());
        // change to false
        c.setIgnoreMissingInstructionFile(false);
        // the cloned should now be false
        assertFalse(c.readOnly().isIgnoreMissingInstructionFile());
    }

    @Test
    public void use_deprecated_methods_for_setting_kms_region() {
        CryptoConfiguration c = new CryptoConfiguration();
        // default is null
        assertNull(c.getKmsRegion());
        // change to AP_NORTHEAST_1
        c.withKmsRegion(Regions.AP_NORTHEAST_1);
        assertEquals(Regions.AP_NORTHEAST_1, c.getKmsRegion());
        // change back to null
        c.setKmsRegion(null);
        assertNull(c.getKmsRegion());
        // the cloned should also be null
        assertNull(c.clone().readOnly().getKmsRegion());
        // change to AP_NORTHEAST_1
        c.setKmsRegion(Regions.AP_NORTHEAST_1);
        // the cloned should now be false
        assertEquals(Regions.AP_NORTHEAST_1, c.clone().readOnly().getKmsRegion());
    }

    @Test
    public void set_retrieve_kms_region() {
        CryptoConfiguration c = new CryptoConfiguration();
        // default is null
        assertNull(c.getAwsKmsRegion());

        // change to AP_NORTHEAST_1
        c.setAwsKmsRegion(Region.getRegion(Regions.AP_NORTHEAST_1));
        // the cloned should now be false
        assertEquals(Regions.AP_NORTHEAST_1.getName(), c.clone().readOnly()
                                                        .getAwsKmsRegion().getName());

        // change back to null
        c.setAwsKmsRegion(null);
        assertNull(c.getAwsKmsRegion());
        // the cloned should also be null
        assertNull(c.clone().readOnly().getAwsKmsRegion());
        // change to AP_NORTHEAST_1
        c.setAwsKmsRegion(Region.getRegion(Regions.AP_NORTHEAST_1));
        // the cloned should now be false
        assertEquals(Regions.AP_NORTHEAST_1, c.clone().readOnly().getKmsRegion());
    }
}
