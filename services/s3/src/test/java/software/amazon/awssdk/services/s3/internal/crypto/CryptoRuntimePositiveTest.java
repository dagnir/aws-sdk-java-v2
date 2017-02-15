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

import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.newBouncyCastleProvider;

import java.security.Security;
import org.junit.BeforeClass;
import org.junit.Test;

public class CryptoRuntimePositiveTest {

    @BeforeClass
    public static void touchBouncyCastle() throws Exception {
        CryptoRuntime.enableBouncyCastle();
        assertTrue(Security.addProvider(newBouncyCastleProvider()) == -1);
        // Only necessary in unit test when the same class loader is used across
        // multiple unit tests, like during brazil-build.
        CryptoRuntime.recheck();
    }

    @Test
    public void isAesGcmAvailable() {
        assertTrue(CryptoRuntime.isBouncyCastleAvailable());
        assertTrue(CryptoRuntime.isAesGcmAvailable());
        assertTrue(CryptoRuntime.isAesGcmAvailable());
    }

    @Test
    public void isRsaKeyWrappingAvailable() {
        assertTrue(CryptoRuntime.isBouncyCastleAvailable());
        assertTrue(CryptoRuntime.isRsaKeyWrapAvailable());
        assertTrue(CryptoRuntime.isRsaKeyWrapAvailable());
    }
}
