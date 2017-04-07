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

package software.amazon.awssdk.services.s3.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;
import org.junit.Test;
import software.amazon.awssdk.services.s3.UploadObjectObserver;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.utils.IoUtils;

public class MultiFileOutputStreamTest {

    @Test
    public void testMultiFiles() throws IOException {
        long size = 11 << 20;
        File tmpfile = CryptoTestUtils.generateRandomAsciiFile(size);
        String prefix = "MultiFileOutputStreamTest.part";
        File root = new File("/tmp");
        MultiFileOutputStream os = new MultiFileOutputStream(root, prefix);
        assertEquals(prefix, os.getNamePrefix());
        assertEquals(root, os.getRoot());
        os.init(new UploadObjectObserver() {
            // no-op observer
            @Override
            public void onPartCreate(PartCreationEvent event) {
            }
        }, os.getPartSize(), os.getDiskLimit());
        IoUtils.copy(new FileInputStream(tmpfile), os);

        assertTrue(Math.ceil(1.0 * size / os.getPartSize()) == os
                .getNumFilesWritten());
        assertTrue(size == os.getTotalBytesWritten());

        final int files = os.getNumFilesWritten();
        for (int i = 0; i < files; i++) {
            File f = new File(root, prefix + "." + (i + 1));
            if (i < (files - 1)) {
                assertTrue(os.getPartSize() == f.length());
            } else {
                assertTrue(size % os.getPartSize() == f.length());
            }
            f.delete();
        }
    }

    @Test
    public void testRandom() throws IOException {
        MultiFileOutputStream os = new MultiFileOutputStream();
        os.init(new UploadObjectObserver() {
            // no-op observer
            @Override
            public void onPartCreate(PartCreationEvent event) {
            }
        }, os.getPartSize(), os.getDiskLimit());
        int size = new Random().nextInt(22 << 20);
        File tmpfile = CryptoTestUtils.generateRandomAsciiFile(size);
        String prefix = os.getNamePrefix();
        File root = os.getRoot();
        IoUtils.copy(new FileInputStream(tmpfile), os);

        assertTrue(Math.ceil(1.0 * size / os.getPartSize()) == os
                .getNumFilesWritten());
        assertTrue(size == os.getTotalBytesWritten());

        final int files = os.getNumFilesWritten();
        for (int i = 0; i < files; i++) {
            File f = new File(root, prefix + "." + (i + 1));
            System.out.println(f);
            if (i < (files - 1)) {
                assertTrue(os.getPartSize() == f.length());
            } else {
                assertTrue(size % os.getPartSize() == f.length());
            }
            f.delete();
        }
    }

    @Test(expected = IOException.class)
    public void testIOException() throws IOException {
        MultiFileOutputStream os = new MultiFileOutputStream();
        os.close();
        os.write(0);
    }

    @Test
    public void testClose() throws IOException {
        MultiFileOutputStream os = new MultiFileOutputStream();
        os.close();
        assertTrue(os.getNumFilesWritten() == 0);
        assertTrue(os.getTotalBytesWritten() == 0);
    }

    @Test
    public void testWriteZeroBytes() throws IOException {
        MultiFileOutputStream os = new MultiFileOutputStream();
        os.init(new UploadObjectObserver() {
            // no-op observer
            @Override
            public void onPartCreate(PartCreationEvent event) {
            }
        }, os.getPartSize(), os.getDiskLimit());
        os.write(new byte[0]);
        assertTrue(os.getNumFilesWritten() == 0);
        os.write(new byte[0], 0, 0);
        assertTrue(os.getNumFilesWritten() == 0);
        os.write(0);
        assertTrue(os.getNumFilesWritten() == 1);
        assertFalse(os.isClosed());
        os.close();
        assertTrue(os.isClosed());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testIndexOutOfBoundsException() throws IOException {
        MultiFileOutputStream os = new MultiFileOutputStream();
        os.write(new byte[1], 0, -1);
        fail();
        os.close(); // keep the IDE happy
    }
}
