/*
 * Copyright 2012-2017 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.awssdk.services.glacier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import software.amazon.awssdk.util.IOUtils;
import org.junit.Test;

import software.amazon.awssdk.services.glacier.internal.TreeHashInputStream;
import software.amazon.awssdk.test.AWSTestBase;
import software.amazon.awssdk.test.util.RandomTempFile;
import software.amazon.awssdk.util.BinaryUtils;

/**
 * Tests of the TreeHashInputStream class.
 */
public class TreeHashInputStreamTest extends AWSTestBase {

    private static final int MB = 1024 * 1024;

    @Test
    public void testOnebyteFile() throws Exception {
        File f = new RandomTempFile("tree-hash-input-stream-test", 1);
        assertTrue(1 == f.length());

        String checksum = computeSHA256Hash(f);
        String treeHash = TreeHashGenerator.calculateTreeHash(f);

        assertEquals(checksum, treeHash);
    }

    @Test
    public void testOneMegabyteFile() throws Exception {
        File f = new RandomTempFile("tree-hash-input-stream-test", MB);
        assertEquals(MB, (int)f.length());

        String checksum = computeSHA256Hash(f);
        String treeHash = TreeHashGenerator.calculateTreeHash(f);

        assertEquals(checksum, treeHash);

        TreeHashInputStream treeHashInputStream = new TreeHashInputStream(new FileInputStream(f));
        while (treeHashInputStream.read() > -1);
        treeHashInputStream.close();

        assertEquals(1, treeHashInputStream.getChecksums().size());
        assertEquals(checksum, BinaryUtils.toHex(treeHashInputStream.getChecksums().get(0)));
    }

    @Test
    public void testOneAndAHalfMegabyteFile() throws Exception {
        int fileLength = MB + MB / 2;
        File f = new RandomTempFile("tree-hash-input-stream-test", fileLength);
        assertEquals(fileLength, (int)f.length());

        byte[] fileBytes = IOUtils.toByteArray(new FileInputStream(f));
        byte[] firstMeg = new byte[MB];
        System.arraycopy(fileBytes, 0, firstMeg, 0, MB);
        byte[] secondMeg = new byte[fileBytes.length - MB];
        System.arraycopy(fileBytes, MB, secondMeg, 0, secondMeg.length);

        String firstMegChecksum = computeSHA256Hash(firstMeg);
        String secondMegChecksum = computeSHA256Hash(secondMeg);


        TreeHashInputStream treeHashInputStream = new TreeHashInputStream(new FileInputStream(f));
        while (treeHashInputStream.read() > -1);
        treeHashInputStream.close();

        assertEquals(2, treeHashInputStream.getChecksums().size());
        assertEquals(firstMegChecksum, BinaryUtils.toHex(treeHashInputStream.getChecksums().get(0)));
        assertEquals(secondMegChecksum, BinaryUtils.toHex(treeHashInputStream.getChecksums().get(1)));
    }

    @Test
    public void testCloseInputStream() throws Exception {
        File f = new RandomTempFile("tree-hash-input-stream-test", 1);
        FileInputStream fileInputStream = new FileInputStream(f);

        TreeHashGenerator.calculateTreeHash(fileInputStream);

        try {
            fileInputStream.read();
            fail();
        } catch (IOException e) {
            assertEquals("Stream closed".toLowerCase(), e.getMessage().toLowerCase());
        } finally {
            software.amazon.awssdk.util.IOUtils.closeQuietly(fileInputStream, null);
        }
    }

    private byte[] computeSHA256Hash(InputStream input) throws NoSuchAlgorithmException, IOException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[16384];
            int bytesRead = -1;
            while ( (bytesRead = input.read(buffer, 0, buffer.length)) != -1 ) {
                messageDigest.update(buffer, 0, bytesRead);
            }
            return messageDigest.digest();
        } finally {
            software.amazon.awssdk.util.IOUtils.closeQuietly(input, null);
        }
    }

    private String computeSHA256Hash(byte[] data) throws NoSuchAlgorithmException, IOException {
        BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data));
        return BinaryUtils.toHex(computeSHA256Hash(bis));
    }

    private String computeSHA256Hash(File file) throws NoSuchAlgorithmException, IOException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        return BinaryUtils.toHex(computeSHA256Hash(bis));
    }

}
