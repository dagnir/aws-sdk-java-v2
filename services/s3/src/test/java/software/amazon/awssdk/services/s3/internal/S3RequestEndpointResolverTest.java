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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.runtime.endpoint.ServiceEndpointBuilder;
import software.amazon.awssdk.services.s3.AmazonS3Client;

public class S3RequestEndpointResolverTest {

    private static final String ENDPOINT = "s3.us-east-1.amazonaws.com";
    private static final String BUCKET_NAME = "some-bucket";
    private static final String KEY = "some-key";
    private static final String KEY_WITH_SPACES = "key with spaces";

    private Request<String> request;

    @Mock
    private ServiceEndpointBuilder endpointBuilder;

    private static URI toHttpsUri(String endpoint) {
        try {
            return new URI("https://" + endpoint);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setup() {
        request = new DefaultRequest<String>(AmazonS3Client.S3_SERVICE_NAME);
        MockitoAnnotations.initMocks(this);
        Mockito.when(endpointBuilder.getServiceEndpoint()).thenReturn(toHttpsUri(ENDPOINT));
        Mockito.when(endpointBuilder.withRegion(Mockito.any(Region.class))).thenReturn(endpointBuilder);
    }

    @Test
    public void resolveRequestEndpoint_HostStyleAddressing_ReturnsValidEndpoint() {
        S3RequestEndpointResolver endpointResolver = new S3RequestEndpointResolver(endpointBuilder, false, BUCKET_NAME,
                                                                                   KEY);
        endpointResolver.resolveRequestEndpoint(request);
        assertHostStyleAddressing(ENDPOINT, BUCKET_NAME, KEY);
    }

    @Test
    public void resolveRequestEndpoint_HostStyleAddressingKeyWithSpaces_UrlEncodesResourcePath() {
        S3RequestEndpointResolver endpointResolver = new S3RequestEndpointResolver(endpointBuilder,
                                                                                   false,
                                                                                   BUCKET_NAME,
                                                                                   KEY_WITH_SPACES);
        endpointResolver.resolveRequestEndpoint(request);
        assertHostStyleAddressing(ENDPOINT, BUCKET_NAME, "key%20with%20spaces");
    }

    @Test(expected = AmazonClientException.class)
    public void
    resolveRequestEndpoint_UnknownRegion_HostStyleAddressing_throwsException() {
        S3RequestEndpointResolver endpointResolver = new S3RequestEndpointResolver(endpointBuilder, false, BUCKET_NAME,
                                                                                   KEY);
        endpointResolver.resolveRequestEndpoint(request, "unknown-region");
    }

    @Test
    public void resolveRequestEndpoint_InvalidDnsBucketName_UsesPathStyleAddressing() {
        String pathStyleBucketName = "invalidForVirtualAddressing";
        S3RequestEndpointResolver endpointResolver = new S3RequestEndpointResolver(endpointBuilder, false,
                                                                                   pathStyleBucketName, KEY);
        endpointResolver.resolveRequestEndpoint(request);
        assertPathStyleAddressing(ENDPOINT, pathStyleBucketName, KEY);
    }

    @Test
    public void resolveRequestEndpoint_ForcedPathStyleAddressing_UsesPathStyleAddressing() {
        S3RequestEndpointResolver endpointResolver = new S3RequestEndpointResolver(endpointBuilder, true, BUCKET_NAME,
                                                                                   KEY);
        endpointResolver.resolveRequestEndpoint(request);
        assertPathStyleAddressing(ENDPOINT, BUCKET_NAME, KEY);
    }

    @Test
    public void resolveRequestEndpoint_ForcedPathStyleAddressingNoKey_UsesPathStyleAddressing() {
        S3RequestEndpointResolver endpointResolver = new S3RequestEndpointResolver(endpointBuilder, true, BUCKET_NAME,
                                                                                   null);
        endpointResolver.resolveRequestEndpoint(request);
        // Key was null so we expect just a trailing slash after the bucket name in the resource
        // path
        assertPathStyleAddressing(ENDPOINT, BUCKET_NAME, "");
    }

    @Test
    public void resolveRequestEndpoint_ForcedPathStyleKeyWithSpaces_UsesPathStyleAddressing() {
        S3RequestEndpointResolver endpointResolver =
                new S3RequestEndpointResolver(endpointBuilder, true, BUCKET_NAME, KEY_WITH_SPACES);
        endpointResolver.resolveRequestEndpoint(request);
        // Key was null so we expect just a trailing slash after the bucket name in the resource
        // path
        assertPathStyleAddressing(ENDPOINT, BUCKET_NAME, "key%20with%20spaces");
    }

    @Test
    public void resolveRequestEndpoint_WithNewRegion_ChangesRegionOnEndpointBuilder() {
        S3RequestEndpointResolver endpointResolver = new S3RequestEndpointResolver(endpointBuilder, true, BUCKET_NAME,
                                                                                   null);
        final Region region = Region.getRegion(Regions.EU_CENTRAL_1);
        endpointResolver.resolveRequestEndpoint(request, region.toString());
        // The real assertion here is that we expect the region to change on the endpoint builder
        Mockito.verify(endpointBuilder).withRegion(region);
    }

    @Test
    public void resolveRequestEndpoint_NullBucketNameWithPathStyleAddressing_HasNullResourcePath() {
        S3RequestEndpointResolver endpointResolver = new S3RequestEndpointResolver(endpointBuilder, true, null, null);
        endpointResolver.resolveRequestEndpoint(request);
        assertNull(request.getResourcePath());
    }

    @Test
    public void resolveRequestEndpoint_HostNameIsIp_DoesNotUseVirtualAddressing() {
        final String ipAddress = "10.1.1.1";
        S3RequestEndpointResolver endpointResolver = new S3RequestEndpointResolver(endpointBuilder, false, BUCKET_NAME,
                                                                                   KEY);
        Mockito.when(endpointBuilder.getServiceEndpoint()).thenReturn(toHttpsUri(ipAddress));
        endpointResolver.resolveRequestEndpoint(request);
        assertPathStyleAddressing(ipAddress, BUCKET_NAME, KEY);
    }

    @Test
    public void resolveRequestEndpoint_HostStyleAddressing_KeyNameHasSlash_PrependsAnotherSlash() {
        final String keyWithLeadingSlash = "/someKey";
        S3RequestEndpointResolver endpointResolver = new S3RequestEndpointResolver(endpointBuilder, false, BUCKET_NAME,
                                                                                   keyWithLeadingSlash);
        endpointResolver.resolveRequestEndpoint(request);
        assertHostStyleAddressing(ENDPOINT, BUCKET_NAME, "/" + keyWithLeadingSlash);
    }

    @Test
    public void isValidIpV4Address_NullString_ReturnsFalse() {
        assertFalse(S3RequestEndpointResolver.isValidIpV4Address(null));
    }

    @Test
    public void isValidIpV4Address_EmptyString_ReturnsFalse() {
        assertFalse(S3RequestEndpointResolver.isValidIpV4Address(""));
    }

    @Test
    public void isValidIpV4Address_HostName_ReturnsFalse() {
        assertFalse(S3RequestEndpointResolver.isValidIpV4Address("s3.amazonaws.com"));
    }

    @Test
    public void isValidIpV4Address_ValidIp_ReturnsTrue() {
        assertTrue(S3RequestEndpointResolver.isValidIpV4Address("10.10.10.10"));
    }

    @Test
    public void isValidIpV4Address_OctetOver255_ReturnsFalse() {
        assertFalse(S3RequestEndpointResolver.isValidIpV4Address("10.256.10.10"));
    }

    @Test
    public void isValidIpV4Address_OctetLessThanZero_ReturnsFalse() {
        assertFalse(S3RequestEndpointResolver.isValidIpV4Address("10.-1.10.10"));
    }

    @Test
    public void isValidIpV4Address_TooManyOctets_ReturnsFalse() {
        assertFalse(S3RequestEndpointResolver.isValidIpV4Address("10.10.10.10.10.10"));
    }

    private void assertHostStyleAddressing(String endpoint, String bucketName, String expectedKey) {
        assertEquals(toHttpsUri(bucketName + "." + endpoint), request.getEndpoint());
        assertEquals(expectedKey, request.getResourcePath());
    }

    private void assertPathStyleAddressing(String endpoint, String bucketName, String expectedKey) {
        assertEquals(toHttpsUri(endpoint), request.getEndpoint());
        assertEquals(bucketName + "/" + expectedKey, request.getResourcePath());
    }
}
