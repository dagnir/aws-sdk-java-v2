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
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.ASCII_HIGH;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.ASCII_LOW;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class CryptoTestUtilsTest {
    @Test
    public void generateRandomAsciiFile() throws IOException {
        int size = 1000;
        File f = CryptoTestUtils.generateRandomAsciiFile(size);
        assertTrue("Unexpected file size: " + f.length(), f.length() == size);
        byte[] bytes = FileUtils.readFileToByteArray(f);
        for (byte b : bytes) {
            if (b < ASCII_LOW && b != '\n' || b > ASCII_HIGH) {
                fail("Unexpected byte " + b);
            }
        }
    }
}
