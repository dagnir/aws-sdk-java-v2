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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class S3CryptoModuleBaseTest {
    @Test
    public void getAdjustedCryptoRange_Invalid() {
        long[] range = {1, 0};
        long[] result = S3CryptoModuleBase.getAdjustedCryptoRange(range);
        assertNull(result);
    }

    @Test
    public void getAdjustedCryptoRange_MaxLong() {
        long[] range = {0, Long.MAX_VALUE};
        long[] result = S3CryptoModuleBase.getAdjustedCryptoRange(range);
        assertTrue(0 == result[0]);
        assertTrue(String.valueOf(result[1]), Long.MAX_VALUE == result[1]);
    }

    @Test
    public void getAdjustedCryptoRange_0To15() {
        for (int i = 0; i < 16; i++) {
            long[] range = {0, i};
            long[] result = S3CryptoModuleBase.getAdjustedCryptoRange(range);
            assertTrue(0 == result[0]);
            assertTrue(String.valueOf(result[1]), 32 == result[1]);
        }
    }

    @Test
    public void getAdjustedCryptoRange_1To15() {
        for (int i = 0; i < 16; i++) {
            long[] range = {i, i};
            long[] result = S3CryptoModuleBase.getAdjustedCryptoRange(range);
            assertTrue(0 == result[0]);
            assertTrue(String.valueOf(result[1]), 32 == result[1]);
        }
    }

    @Test
    public void getAdjustedCryptoRange_16() {
        long[] range = {0, 16};
        long[] result = S3CryptoModuleBase.getAdjustedCryptoRange(range);
        assertTrue(0 == result[0]);
        assertTrue(String.valueOf(result[1]), 48 == result[1]);
    }
}
