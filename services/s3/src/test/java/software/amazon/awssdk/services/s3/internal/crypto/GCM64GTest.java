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

import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.runTimeConsumingTests;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.test.util.ConstantInputStream;

public class GCM64GTest {
    private static final long MAX_BYTES = ContentCryptoScheme.MAX_GCM_BYTES;
    private static final long TEN_MEG = (1L << 20) * 10;
    private static final long GIG = (1L << 30);

    @BeforeClass
    public static void setup() {
        CryptoRuntime.enableBouncyCastle();
    }

    // Took 5,819 seconds to run
    @Test
    public void positive() throws Exception {
        if (runTimeConsumingTests()) {
            doTest(MAX_BYTES, false);
        } else {
            System.out.println("Please set the environment variable, export RUN_TIME_CONSUMING_TESTS=true, to run the positive test");
        }
    }

    // Took 4,809 seconds to run
    @Test
    public void negative() throws Exception {
        if (runTimeConsumingTests()) {
            doTest(MAX_BYTES + 1, true);
        } else {
            System.out.println("Please set the environment variable, export RUN_TIME_CONSUMING_TESTS=true, to run the negative test");
        }
    }

    private void doTest(long size, boolean shouldFail) throws Exception {
        long start = System.nanoTime();
        CipherLite cl = CryptoTestUtils.createTestCipherLite(
                Cipher.ENCRYPT_MODE, ContentCryptoScheme.AES_GCM);
        System.out.println("ContentCryptoScheme.MAX_GCM_BYTES="
                           + ContentCryptoScheme.MAX_GCM_BYTES + ", MAX_BYTES="
                           + MAX_BYTES + ", size=" + size + ", TEN_MEG=" + TEN_MEG
                           + ", GIG=" + GIG);
        InputStream is = new CipherLiteInputStream(new ConstantInputStream(
                size, (byte) 'G'), cl);
        long count = 0;
        long gigs = 0;
        byte[] buf = new byte[2 << 10];
        try {
            int len;
            while ((len = is.read(buf)) != -1) {
                count += len;
                if (count % GIG == 0) {
                    System.out.println();
                    System.out.println(++gigs + "GB GCM encrypted");
                } else if (count % TEN_MEG == 0) {
                    System.out.print(".");
                }
            }
            if (shouldFail) {
                fail();
            }
        } catch (SecurityException ex) {
            if (shouldFail) {
                ex.printStackTrace(System.out);
            } else {
                throw ex;
            }
        } finally {
            is.close();
            System.out.println("took: "
                               + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start)
                               + "s");
        }
    }
}
