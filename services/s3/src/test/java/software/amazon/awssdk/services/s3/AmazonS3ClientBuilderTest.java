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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.function.SdkFunction;
import software.amazon.awssdk.regions.AwsRegionProvider;
import software.amazon.awssdk.regions.Regions;

public class AmazonS3ClientBuilderTest {

    private MockClientFactory mockClientFactory;
    private AmazonS3ClientBuilder builder;

    @Before
    public void setup() {
        mockClientFactory = new MockClientFactory();
        builder = new AmazonS3ClientBuilder(mockClientFactory,
                                            new FindNothingAwsRegionProvider());
        builder.withRegion(Regions.US_WEST_2);
    }

    @Test
    public void noExplicitConfiguration_AllDefaultClientOptionsAreCorrect() {
        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertFalse(clientOptions.isAccelerateModeEnabled());
        assertFalse(clientOptions.isChunkedEncodingDisabled());
        assertFalse(clientOptions.isPathStyleAccess());
        assertFalse(clientOptions.isPayloadSigningEnabled());
    }

    @Test
    public void enablePayloadSigning_EnabledPayloadSigningInClient() {
        builder.enablePayloadSigning();
        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertTrue(clientOptions.isPayloadSigningEnabled());
    }

    @Test
    public void withPayloadSigningFalse_LeavesPayloadSigningDisabled() {
        builder.withPayloadSigningEnabled(false);
        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertFalse(clientOptions.isPayloadSigningEnabled());
    }

    @Test
    public void withPayloadSigningTrue_EnablesPayloadSigningInClient() {
        builder.withPayloadSigningEnabled(true);
        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertTrue(clientOptions.isPayloadSigningEnabled());
    }

    @Test
    public void disableChunkedEncoding_DisabledChunkedEncodingInClient() {
        builder.disableChunkedEncoding();
        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertTrue(clientOptions.isChunkedEncodingDisabled());
    }

    @Test
    public void withChunkedEncodingDisabledFalse_LeavesChunkedEncodingEnabledInClient() {
        builder.withChunkedEncodingDisabled(false);
        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertFalse(clientOptions.isChunkedEncodingDisabled());
    }

    @Test
    public void withChunkedEncodingDisabledTrue_DisablesChunkedEncodingInClient() {
        builder.withChunkedEncodingDisabled(true);
        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertTrue(clientOptions.isChunkedEncodingDisabled());
    }

    @Test
    public void enableAccelerateMode_EnablesAccelerateModeInClient() {
        builder.enableAccelerateMode();
        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertTrue(clientOptions.isAccelerateModeEnabled());
    }

    @Test
    public void withAccelerateModeEnabledFalse_LeavesAccelerateModeDisabled() {
        builder.withAccelerateModeEnabled(false);
        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertFalse(clientOptions.isAccelerateModeEnabled());
    }

    @Test
    public void withAccelerateModeEnabledTrue_EnablesAccelerateModeInClient() {
        builder.withAccelerateModeEnabled(true);
        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertTrue(clientOptions.isAccelerateModeEnabled());
    }

    @Test
    public void enablePathStyleAccess_EnablesPathStyleAccessInClient() {
        builder.enablePathStyleAccess();
        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertTrue(clientOptions.isPathStyleAccess());
    }

    @Test
    public void withPathStyleAccessFalse_LeavesPathStyleAccessDisabledInClient() {
        builder.withPathStyleAccessEnabled(false);
        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertFalse(clientOptions.isPathStyleAccess());
    }

    @Test
    public void withPathStyleAccessTrue_EnablesPathStyleAccessInClient() {
        builder.withPathStyleAccessEnabled(true);
        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertTrue(clientOptions.isPathStyleAccess());
    }

    @Test
    public void enableDualstack_EnablesDualstackInClient() {
        builder.enableDualstack();
        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertTrue(clientOptions.isDualstackEnabled());
    }

    @Test
    public void withDualstackEnabledFalse_LeavesDualstackDisabledInClient() {
        builder.withDualstackEnabled(false);
        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertFalse(clientOptions.isDualstackEnabled());
    }

    @Test
    public void withDualstackEnabledTrue_EnablesDualstackInClient() {
        builder.withDualstackEnabled(true);
        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertTrue(clientOptions.isDualstackEnabled());
    }

    @Test(expected = IllegalStateException.class)
    public void withCrossRegionDisabledAndNullRegion_ThrowsExceptionOnBuild() {
        builder.setRegion(null);
        builder.withForceGlobalBucketAccessEnabled(false);
        builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void withCrossRegionEnabledAndNullRegion_ThrowsExceptionOnBuild() {
        builder.setRegion(null);
        builder.withForceGlobalBucketAccessEnabled(true);
        builder.build();
    }

    @Test
    public void withCrossRegionDisabledAndDefinedRegion_DisablesCrossRegionInClient() {
        builder.setRegion(Regions.CN_NORTH_1.getName());
        builder.withForceGlobalBucketAccessEnabled(false);

        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertFalse(clientOptions.isForceGlobalBucketAccessEnabled());
        assertEquals(Regions.CN_NORTH_1.getName(), builder.build().getRegionName());
    }

    @Test
    public void withCrossRegionEnabledAndDefinedRegion_EnablesCrossRegionInClient() {
        builder.setRegion(Regions.CN_NORTH_1.getName());
        builder.withForceGlobalBucketAccessEnabled(true);

        final S3ClientOptions clientOptions = buildAndCaptureClientOptions();
        assertTrue(clientOptions.isForceGlobalBucketAccessEnabled());
        assertEquals(Regions.CN_NORTH_1.getName(), builder.build().getRegionName());
    }

    private S3ClientOptions buildAndCaptureClientOptions() {
        builder.build();
        return mockClientFactory.getCapturedParams().getS3ClientOptions();
    }

    private static class MockClientFactory implements
                                           SdkFunction<AmazonS3ClientParamsWrapper, AmazonS3> {
        private AmazonS3ClientParamsWrapper capturedParams;

        @Override
        public AmazonS3 apply(AmazonS3ClientParamsWrapper params) {
            this.capturedParams = params;
            return new AmazonS3Client(params);
        }

        public AmazonS3ClientParamsWrapper getCapturedParams() {
            return capturedParams;
        }
    }

    private static class FindNothingAwsRegionProvider extends AwsRegionProvider {
        @Override
        public String getRegion() throws AmazonClientException {
            // Imitates the buggy region provider chain that throws an exception instead of returning null when no
            // region is found.
            throw new SdkClientException("No region will ever be found.");
        }
    }
}
