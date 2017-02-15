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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.s3.internal.DigestValidationInputStream;
import software.amazon.awssdk.services.s3.util.StreamUtils;
import software.amazon.awssdk.util.Md5Utils;


public class DigestValidationInputStreamTest {

    private final static String INPUT_STRING = "Jason bought a house";

    @Test
    public void testDigestValidationInputStream() throws NoSuchAlgorithmException, IOException {
        // Consuming the stream with bad server side hash should trigger exception.
        InputStream in = new ByteArrayInputStream(INPUT_STRING.getBytes());
        MessageDigest digest = MessageDigest.getInstance("MD5");
        DigestValidationInputStream digestValidationInputStream = new DigestValidationInputStream(in, digest, "fake".getBytes());
        try {
            StreamUtils.consumeInputStream(digestValidationInputStream);
            fail();
        } catch (AmazonClientException e) {

        }
        digestValidationInputStream.close();

        // Close before consuming the stream should not trigger exception.
        in = new ByteArrayInputStream(INPUT_STRING.getBytes());
        digest = MessageDigest.getInstance("MD5");
        digestValidationInputStream = new DigestValidationInputStream(in, digest, "fake".getBytes());
        digestValidationInputStream.close();


        // Give the correct expected
        in = new ByteArrayInputStream(INPUT_STRING.getBytes());
        byte[] expectedHash = Md5Utils.computeMD5Hash(in);
        in.close();
        in = new ByteArrayInputStream(INPUT_STRING.getBytes());
        digest = MessageDigest.getInstance("MD5");
        digestValidationInputStream = new DigestValidationInputStream(in, digest, expectedHash);
        StreamUtils.consumeInputStream(digestValidationInputStream);
        // Read multiple times at the end of the input stream
        assertEquals(-1, digestValidationInputStream.read());
        assertEquals(-1, digestValidationInputStream.read());
        digestValidationInputStream.close();

    }

}
