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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.awsTestCredentials;

import java.io.File;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.event.ProgressEvent;
import software.amazon.awssdk.event.ProgressEventType;
import software.amazon.awssdk.event.SyncProgressListener;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResult;

/**
 * Test when the TransferManager is garbage collected before the put operation
 * has completed.
 */
public class S3ClientGCIntegrationTest {
    private static final String TEST_BUCKET = CryptoTestUtils
            .tempBucketName(S3ClientGCIntegrationTest.class);
    private static boolean WAIT_INSIDE = true;
    /**
     * True to clean up the temp S3 objects created during test; false
     * otherwise.
     */
    private static boolean cleanup = true;

    @BeforeClass
    public static void setup() throws Exception {
        AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
        CryptoTestUtils.tryCreateBucket(s3, TEST_BUCKET);
        s3.shutdown();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (cleanup) {
            AmazonS3Client s3 = new AmazonS3Client(awsTestCredentials());
            CryptoTestUtils.deleteBucketAndAllContents(s3, TEST_BUCKET);
            s3.shutdown();
        }
    }

    @Test
    public void testWaitInside() throws IOException, InterruptedException {
        Listener listener = new Listener();
        Runner runner = uploadLargeFile(listener, WAIT_INSIDE);
        assertFalse(listener.hasFailed());
        assertNotNull(runner.getResult());
    }

    @Test
    public void testWaitOutside() throws IOException, InterruptedException {
        Listener listener = new Listener();
        Runner runner = uploadLargeFile(listener, !WAIT_INSIDE);
        for (; ; ) {
            System.err.println("triggering GC");
            System.gc();
            if (runner.getResult() != null) {
                assertFalse(listener.hasFailed());
                assertNotNull(runner.getResult());
                return;
            }
            Thread.sleep(5000);
        }
    }

    public Runner uploadLargeFile(Listener listener, boolean waitInside) throws IOException, InterruptedException {
        File file = CryptoTestUtils.generateRandomAsciiFile(3 * 1024 * 1024);  // 3MB
        final PutObjectRequest req = new PutObjectRequest(TEST_BUCKET, "somekey", file)
                .withGeneralProgressListener(listener);
        Runner runner = new Runner(awsTestCredentials(), req);
        new Thread(runner).start();
        if (waitInside) {
            for (; ; ) {
                System.err.println("triggering GC");
                System.gc();
                if (runner.getResult() != null) {
                    return runner;
                }
                Thread.sleep(5000);
            }
        }
        Thread.sleep(1000);
        return runner;
    }

    private static class Runner implements Runnable {
        private final PutObjectRequest req;
        private final AwsCredentials cred;
        private PutObjectResult result;

        Runner(AwsCredentials cred, PutObjectRequest req) {
            this.cred = cred;
            this.req = req;
        }

        public PutObjectResult getResult() {
            return result;
        }

        @Override
        public void run() {
            AmazonS3Client s3 = new AmazonS3Client(cred);
            result = s3.putObject(req);
            s3.shutdown();
        }
    }

    private static class Listener extends SyncProgressListener {
        private boolean failed = true;

        public boolean hasFailed() {
            return failed;
        }

        @Override
        public void progressChanged(ProgressEvent progressEvent) {
            if (progressEvent.getEventType() == ProgressEventType.HTTP_RESPONSE_COMPLETED_EVENT) {
                System.err.println(progressEvent);
                failed = false;
            } else {
                System.out.println(progressEvent);
            }
        }
    }
}
