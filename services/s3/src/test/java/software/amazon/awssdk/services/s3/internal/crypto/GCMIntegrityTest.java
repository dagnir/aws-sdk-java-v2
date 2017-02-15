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

import java.io.InputStream;
import javax.crypto.Cipher;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.test.util.ConstantInputStream;

public class GCMIntegrityTest {

    @BeforeClass
    public static void setup() {
        CryptoRuntime.enableBouncyCastle();
    }

    @Test(expected = SecurityException.class)
    public void negative() throws Exception {
        InputStream is = new CipherLiteInputStream(new ConstantInputStream(100,
                                                                           (byte) 'F'), CryptoTestUtils.createTestCipherLite(
                Cipher.DECRYPT_MODE, ContentCryptoScheme.AES_GCM));
        IOUtils.toString(is);
    }

}
