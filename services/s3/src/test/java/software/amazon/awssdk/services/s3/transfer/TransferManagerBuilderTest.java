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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.client.builder.ExecutorFactory;
import software.amazon.awssdk.function.SdkFunction;
import software.amazon.awssdk.services.s3.AmazonS3;

@RunWith(MockitoJUnitRunner.class)
public class TransferManagerBuilderTest {

    /**
     * Dummy threshold for various threshold related configuration in TransferManager.
     */
    private static final long DUMMY_THRESHOLD = 9001L;
    private MockTransferManagerFactory mockTransferManagerFactory;
    private TransferManagerBuilder builder;
    @Mock
    private AmazonS3 s3Client;

    @Before
    public void setup() {
        mockTransferManagerFactory = new MockTransferManagerFactory();
        builder = new TransferManagerBuilder(mockTransferManagerFactory);
        builder.withS3Client(s3Client);
    }

    @Test
    public void nonNullS3Client_TransferManagerBuildsSuccessfully() {
        final TransferManagerParams capturedParams = buildAndCaptureParams();
        assertEquals(s3Client, capturedParams.getS3Client());
    }

    @Test
    public void noExplicitConfiguration_ThreadPoolShutDownByDefault() {
        final TransferManagerParams capturedParams = buildAndCaptureParams();
        assertTrue(capturedParams.getShutDownThreadPools());
    }

    @Test
    public void withShutDownThreadPoolsFalse_ShutDownThreadPoolsIsFalseInParams() {
        builder.withShutDownThreadPools(false);
        final TransferManagerParams capturedParams = buildAndCaptureParams();
        assertFalse(capturedParams.getShutDownThreadPools());
    }

    @Test
    public void withShutDownThreadPoolsTrue_ShutDownThreadPoolsIsTrueInParams() {
        builder.withShutDownThreadPools(true);
        final TransferManagerParams capturedParams = buildAndCaptureParams();
        assertTrue(capturedParams.getShutDownThreadPools());
    }

    @Test
    public void noExplicitConfiguration_AllDefaultsAreSetCorrectly() {
        final TransferManagerConfiguration configuration = buildAndCaptureConfiguration();
        assertEquals(TransferManagerConfiguration.DEFAULT_MINIMUM_UPLOAD_PART_SIZE,
                     configuration.getMinimumUploadPartSize());
        assertEquals(TransferManagerConfiguration.DEFAULT_MINIMUM_COPY_PART_SIZE,
                     configuration.getMultipartCopyPartSize());
        assertEquals(TransferManagerConfiguration.DEFAULT_MULTIPART_COPY_THRESHOLD,
                     configuration.getMultipartCopyThreshold());
        assertEquals(TransferManagerConfiguration.DEFAULT_MULTIPART_UPLOAD_THRESHOLD,
                     configuration.getMultipartUploadThreshold());
    }

    @Test
    public void withMinimumUploadPartSize_CustomValueReflectedInConfiguration() {
        builder.withMinimumUploadPartSize(DUMMY_THRESHOLD);
        final TransferManagerConfiguration configuration = buildAndCaptureConfiguration();
        assertEquals(DUMMY_THRESHOLD, configuration.getMinimumUploadPartSize());
    }

    @Test
    public void withMultipartCopyPartSize_CustomValueReflectedInConfiguration() {
        builder.withMultipartCopyPartSize(DUMMY_THRESHOLD);
        final TransferManagerConfiguration configuration = buildAndCaptureConfiguration();
        assertEquals(DUMMY_THRESHOLD, configuration.getMultipartCopyPartSize());
    }

    @Test
    public void withMultipartCopyThreshold_CustomValueReflectedInConfiguration() {
        builder.withMultipartCopyThreshold(DUMMY_THRESHOLD);
        final TransferManagerConfiguration configuration = buildAndCaptureConfiguration();
        assertEquals(DUMMY_THRESHOLD, configuration.getMultipartCopyThreshold());
    }

    @Test
    public void withMultipartUploadThreshold_CustomValueReflectedInConfiguration() {
        builder.withMultipartUploadThreshold(DUMMY_THRESHOLD);
        final TransferManagerConfiguration configuration = buildAndCaptureConfiguration();
        assertEquals(DUMMY_THRESHOLD, configuration.getMultipartUploadThreshold());
    }

    @Test
    public void noExplicitConfiguration_TransferManagerUsesDefaultExecutor() {
        final TransferManagerParams capturedParams = buildAndCaptureParams();
        assertThat(capturedParams.getExecutorService(), instanceOf(ThreadPoolExecutor.class));
        assertEquals(10, ((ThreadPoolExecutor) capturedParams.getExecutorService())
                .getMaximumPoolSize());
    }

    @Test
    public void customExecutorFactoryProvided_ParamsContainsExecutorObtainedFromFactory() {
        builder.withExecutorFactory(new ExecutorFactory() {
            @Override
            public ExecutorService newExecutor() {
                return Executors.newFixedThreadPool(3);
            }
        });
        final TransferManagerParams capturedParams = buildAndCaptureParams();
        assertEquals(3, ((ThreadPoolExecutor) capturedParams.getExecutorService())
                .getMaximumPoolSize());
    }

    private TransferManagerParams buildAndCaptureParams() {
        builder.build();
        return mockTransferManagerFactory.getCapturedParams();
    }

    private TransferManagerConfiguration buildAndCaptureConfiguration() {
        builder.build();
        return mockTransferManagerFactory.getCapturedParams().getConfiguration();
    }

    /**
     * Mock factory that captures the params supplied to TransferManager creation for verification
     * in tests.
     */
    private static class MockTransferManagerFactory implements
                                                    SdkFunction<TransferManagerParams, TransferManager> {

        private TransferManagerParams capturedParams;

        @Override
        public TransferManager apply(TransferManagerParams params) {
            this.capturedParams = params;
            return new TransferManager(params);
        }

        public TransferManagerParams getCapturedParams() {
            return capturedParams;
        }
    }

}