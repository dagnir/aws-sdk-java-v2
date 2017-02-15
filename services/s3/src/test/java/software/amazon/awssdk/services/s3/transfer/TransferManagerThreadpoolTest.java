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

package software.amazon.awssdk.services.s3.transfer;

import static org.junit.Assert.assertFalse;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.Test;
import software.amazon.awssdk.services.s3.AmazonS3Client;

/**
 * Tests if the thread pool is shutdown properly as part of garbage collection.
 * Also tests if the thread pool shutdown is skipped when explicitly asked.
 */
public class TransferManagerThreadpoolTest {

    @Test(timeout = 60000)
    public void test() throws InterruptedException {
        final ThreadPoolExecutor threadPoolExecutor = createNewThreadPool();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                new TransferManager(new AmazonS3Client(), threadPoolExecutor,
                                    false);
            }
        });

        t.start();
        t.join();

        assertFalse(threadPoolExecutor.isShutdown());

        t = new Thread(new Runnable() {
            @Override
            public void run() {
                new TransferManager(new AmazonS3Client(), threadPoolExecutor);
            }
        });

        t.start();
        t.join();

        for (; ; ) {
            System.err.println("triggering GC explicitly.");
            System.gc();
            if (threadPoolExecutor.isShutdown()) {
                return;
            }
            Thread.sleep(5000);
        }

    }

    private ThreadPoolExecutor createNewThreadPool() {
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    }

}
