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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import org.junit.Test;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.auth.AnonymousAWSCredentials;
import software.amazon.awssdk.auth.Signer;
import software.amazon.awssdk.auth.internal.AWS4SignerRequestParams;
import software.amazon.awssdk.auth.internal.SignerConstants;
import software.amazon.awssdk.http.HttpMethodName;
import software.amazon.awssdk.regions.RegionUtils;
import software.amazon.awssdk.services.s3.internal.AWSS3V4Signer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * A unit test for default singing methods in different regions.
 */
public class DefaultSigningMethodTest {

    private static final String FAKE_BUCKET = "fake-bucket";
    private static final String FAKE_KEY = "fake-key";
    private static Method CREATE_REQUEST;

    static {
        setUpInternalMethods();
    }

    private static void setUpInternalMethods() {
        try {
            CREATE_REQUEST = AmazonS3Client.class.getDeclaredMethod("createRequest",
                                                                    String.class, String.class, AmazonWebServiceRequest.class, HttpMethodName.class);
            CREATE_REQUEST.setAccessible(true);
        } catch (Exception e) {
            fail("Failed to set up the internal methods of AmazonS3Clinet" + e.getMessage());
        }
    }

    /**
     * Use reflection to call the private method "createSigner" of
     * AmazonS3Client to create the default signer based on a fake request.
     * Returns whether the created signer is in SigV4.
     */
    private static void assertSigV4WithRegion(AmazonS3Client s3, String expectedRegion) {
        Signer signer = invokeCreateSigner(s3);
        assertTrue(signer instanceof AWSS3V4Signer);
        assertEquals(expectedRegion, invokeExtractRegionName(s3, (AWSS3V4Signer) signer));
        testSignAnonymously(s3);
    }

    private static Request<?> createFakeGetObjectRequest(AmazonS3Client s3) {
        try {
            GetObjectRequest fakeRequest = new GetObjectRequest(FAKE_BUCKET, FAKE_KEY);
            Request<?> fakeGetObjectRequest = (Request<?>) CREATE_REQUEST.invoke(s3, FAKE_BUCKET, FAKE_KEY, fakeRequest, HttpMethodName.GET);

            return fakeGetObjectRequest;
        } catch (Exception e) {
            fail("Exception when calling the private \"createRequest\" method. " + e.getMessage());
            return null;
        }
    }

    private static Signer invokeCreateSigner(AmazonS3Client s3) {
        Request<?> fakeGetObjectRequest = createFakeGetObjectRequest(s3);

        return s3.createSigner(fakeGetObjectRequest, FAKE_BUCKET, FAKE_KEY);
    }

    private static String invokeExtractRegionName(AmazonS3Client s3, AWSS3V4Signer signer) {
        try {
            AWS4SignerRequestParams signerParams = new AWS4SignerRequestParams(
                    createFakeGetObjectRequest(s3), signer.getOverriddenDate(),
                    signer.getRegionName(), signer.getServiceName(),
                    SignerConstants.AWS4_SIGNING_ALGORITHM);
            return signerParams.getRegionName();
        } catch (Exception e) {
            fail("Exception when calling the private \"extractRegionName\" method on AWS4Signer. " + e.getMessage());
            return null;
        }
    }

    private static void testSignAnonymously(AmazonS3Client s3) {
        Request<?> fakeGetObjectRequest = createFakeGetObjectRequest(s3);
        Signer signer = s3.createSigner(fakeGetObjectRequest, FAKE_BUCKET, FAKE_KEY);
        signer.sign(fakeGetObjectRequest, new AnonymousAWSCredentials());
    }

    /**
     * Tests that BJS endpoint always defaults to SigV4.
     */
    @Test
    public void testBJSDefaultSigning() {
        AmazonS3Client s3 = new AmazonS3Client();
        s3.setEndpoint("s3.cn-north-1.amazonaws.com.cn");
        assertSigV4WithRegion(s3, "cn-north-1");

        // Using any of the system props should not affect the default
        System.setProperty("software.amazon.awssdk.services.s3.enforceV4", "true");
        assertSigV4WithRegion(s3, "cn-north-1");
        System.setProperty("software.amazon.awssdk.services.s3.enableV4", "true");
        assertSigV4WithRegion(s3, "cn-north-1");
    }

    /**
     * Tests the behavior when using S3 standard endpoint without providing
     * a region or region override.
     */
    @Test
    public void testStandardEndpointWithoutRegionOverride() {
        AmazonS3Client s3 = new AmazonS3Client();
        s3.setEndpoint("s3.amazonaws.com");
        // When standard endpoint is used and region cannot be determined,
        // a SigV4 signer with "us-east-1" is used for signing.
        assertSigV4WithRegion(s3, "us-east-1");
    }

    /*
     * Test utility functions
     */

    /**
     * Tests the behavior when using S3 standard endpoint with explicit
     * region.
     */
    @Test
    public void testStandardEndpointWithRegionOverride() {
        AmazonS3Client s3 = new AmazonS3Client();
        // Explicitly setting a regionName will default to SigV4
        s3.setEndpoint("s3-us-west-1.amazonaws.com");
        assertSigV4WithRegion(s3, "us-west-1");

        s3.setEndpoint("s3-eu-west-1.amazonaws.com");
        assertSigV4WithRegion(s3, "eu-west-1");
    }

    /**
     * Tests the behavior when using S3 standard endpoint with explicit
     * region.
     */
    @Test
    public void testStandardEndpointWithSetRegion() {
        AmazonS3Client s3 = new AmazonS3Client();
        s3.setRegion(RegionUtils.getRegion("us-east-1"));

        // Default to SigV4
        assertSigV4WithRegion(s3, "us-east-1");
    }

    /**
     * Tests the behavior when both an explicit region and a region override
     * have been provided.
     */
    @Test
    public void testSetRegionAndRegionOverride() {
        AmazonS3Client s3 = new AmazonS3Client();
        s3.setRegion(RegionUtils.getRegion("us-east-1"));

        s3.setEndpoint("s3-eu-west-1.amazonaws.com");
        assertSigV4WithRegion(s3, "eu-west-1");
    }

    /**
     * Tests that other endpoints always defaults to SigV4.
     */
    @Test
    public void testOtherRegionDefaultSigning() {
        testSigV4OnlyRegionDefaultSigning("s3-external-1.amazonaws.com", "us-east-1");
        testSigV4OnlyRegionDefaultSigning("s3-us-west-2.amazonaws.com", "us-west-2");
        testSigV4OnlyRegionDefaultSigning("s3-us-west-1.amazonaws.com", "us-west-1");
        testSigV4OnlyRegionDefaultSigning("s3-eu-west-1.amazonaws.com", "eu-west-1");
        testSigV4OnlyRegionDefaultSigning("s3-ap-southeast-1.amazonaws.com", "ap-southeast-1");
        testSigV4OnlyRegionDefaultSigning("s3-ap-southeast-2.amazonaws.com", "ap-southeast-2");
        testSigV4OnlyRegionDefaultSigning("s3-ap-northeast-1.amazonaws.com", "ap-northeast-1");
        testSigV4OnlyRegionDefaultSigning("s3-sa-east-1.amazonaws.com", "sa-east-1");
    }

    private void testSigV4OnlyRegionDefaultSigning(String endpoint, String expectedRegionName) {
        // Since it is a get request, all non us standard regions would be default to SigV4
        AmazonS3Client s3 = new AmazonS3Client();
        s3.setEndpoint(endpoint);
        assertSigV4WithRegion(s3, expectedRegionName);
    }
}
