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

package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.awsTestCredentials;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.deleteBucketAndAllContents;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tempBucketName;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tryCreateBucket;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AbortedException;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.test.util.ConstantInputStream;

public class S3InterruptIntegrationTest {
    private static final boolean DEBUG = false;
    private static final int DATA_SIZE = 30 * 1024 * 1024;
    private static final String TEST_BUCKET = tempBucketName(S3InterruptIntegrationTest.class);
    private static boolean cleanup = true;
    private static AmazonS3Client s3;

    @BeforeClass
    public static void setup() throws Exception {
        s3 = new AmazonS3Client(awsTestCredentials());
        tryCreateBucket(s3, TEST_BUCKET);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (cleanup) {
            AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
            deleteBucketAndAllContents(s3, TEST_BUCKET);
        }
        s3.shutdown();
    }

    // Interrupting thread doesn't really abort the upload early UNLESS
    // the content length is set
    @Test
    public void testUploadInterrupts() throws InterruptedException {
        InputStream is = new ConstantInputStream(DATA_SIZE, (byte) 'Z');
        ObjectMetadata omd = new ObjectMetadata();
        omd.setContentLength(DATA_SIZE);
        final PutObjectRequest req = new PutObjectRequest(TEST_BUCKET, "test",
                                                          is, omd);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    s3.putObject(req);
                    fail("PUT should have been aborted");
                } catch (AbortedException expected) {
                    // expected
                }
            }
        });
        t.start();
        do {
            t.join(100);
            if (t.isAlive()) {
                t.interrupt();
                debug("Upload thread interrupted");
            } else {
                debug("Upload thread joined");
            }
        } while (t.isAlive());
        debug("Upload done");
    }

    @Test
    public void testUploadInterruptsViaFuture() throws InterruptedException {
        InputStream is = new ConstantInputStream(DATA_SIZE, (byte) 'Z');
        ObjectMetadata omd = new ObjectMetadata();
        omd.setContentLength(DATA_SIZE);
        final PutObjectRequest req = new PutObjectRequest(TEST_BUCKET, "test",
                                                          is, omd);
        ExecutorService es = Executors.newSingleThreadExecutor();
        final Boolean[] success = {null};
        Future<?> f = es.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    s3.putObject(req);
                    synchronized (success) {
                        success[0] = Boolean.FALSE;
                    }
                    return;
                } catch (AbortedException expected) {
                    // expected
                }
                synchronized (success) {
                    success[0] = Boolean.TRUE;
                }
            }
        });
        Thread.sleep(100);
        f.cancel(true);
        while (success[0] == null) {
            Thread.sleep(2000);
        }
        es.shutdownNow();
        assertTrue("PUT via future should have been aborted", success[0].booleanValue());
        debug("Upload via future done");
    }

    @Test
    public void testDownloadInterrupts() throws InterruptedException,
                                                IOException {
        // Put an object to S3
        InputStream is = new ConstantInputStream(DATA_SIZE, (byte) 'Z');
        ObjectMetadata omd = new ObjectMetadata();
        omd.setContentLength(DATA_SIZE);
        final PutObjectRequest req = new PutObjectRequest(TEST_BUCKET, "test",
                                                          is, omd);
        s3.putObject(req);

        final File destfile = CryptoTestUtils.generateRandomAsciiFile(0);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    GetObjectRequest req = new GetObjectRequest(TEST_BUCKET,
                                                                "test");
                    s3.getObject(req, destfile);
                    fail("GET should have been aborted");
                } catch (AbortedException expected) {
                    // Ignored or expected.
                }
            }
        });
        t.start();
        do {
            t.join(100);
            if (t.isAlive()) {
                t.interrupt();
                debug("download thread interrupted");
            } else {
                debug("download thread joined");
            }
        } while (t.isAlive());
        debug("Download done");
    }

    @Test
    public void testDownloadInterruptsViaFuture() throws InterruptedException,
                                                         IOException {
        // Put an object to S3
        InputStream is = new ConstantInputStream(DATA_SIZE, (byte) 'Z');
        ObjectMetadata omd = new ObjectMetadata();
        omd.setContentLength(DATA_SIZE);
        final PutObjectRequest req = new PutObjectRequest(TEST_BUCKET, "test",
                                                          is, omd);
        s3.putObject(req);
        final File destfile = CryptoTestUtils.generateRandomAsciiFile(0);
        ExecutorService es = Executors.newSingleThreadExecutor();
        final Boolean[] success = {null};
        Future<?> f = es.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    GetObjectRequest req = new GetObjectRequest(TEST_BUCKET,
                                                                "test");
                    s3.getObject(req, destfile);
                    synchronized (success) {
                        success[0] = Boolean.FALSE;
                    }
                    return;
                } catch (AbortedException expected) {
                    // Ignored or expected.
                }
                synchronized (success) {
                    success[0] = Boolean.TRUE;
                }
            }
        });
        Thread.sleep(100);
        f.cancel(true);
        while (success[0] == null) {
            Thread.sleep(2000);
        }
        es.shutdownNow();
        assertTrue("GET should have been aborted", success[0].booleanValue());
        debug("Download via future done");
    }

    private void debug(Object o) {
        if (DEBUG) {
            System.err.println(String.valueOf(o));
        }
    }
}
