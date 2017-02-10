/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.net.URI;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.function.SdkPredicate;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.retry.internal.AuthRetryParameters;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.Headers;

public class S3V4AuthErrorRetryStrategyTest {

    private static final SdkPredicate<AmazonServiceException> ALWAYS_TRUE_PREDICATE = getPredicate(true);
    private static final SdkPredicate<AmazonServiceException> ALWAYS_FALSE_PREDICATE = getPredicate(false);
    private static final String REGION = Regions.EU_CENTRAL_1.toString();
    private static final String BUCKET_NAME = "somebucket";

    private Request<String> request;
    private HttpResponse httpResponse;
    private S3V4AuthErrorRetryStrategy retryStrategy;

    @Mock
    private S3RequestEndpointResolver endpointResolver;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.when(endpointResolver.getBucketName()).thenReturn(BUCKET_NAME);
        request = new DefaultRequest<String>(AmazonS3Client.S3_SERVICE_NAME);
        request.setEndpoint(new URI("http://s3.amazonaws.com"));
        httpResponse = new HttpResponse(null, null);
        retryStrategy = new S3V4AuthErrorRetryStrategy(endpointResolver, ALWAYS_TRUE_PREDICATE);
    }

    @Test
    public void isRetryablePredicateReturnsFalse_ReturnsNullToSignifyNoRetry() {
        retryStrategy = new S3V4AuthErrorRetryStrategy(endpointResolver, ALWAYS_FALSE_PREDICATE);
        assertNull(retryStrategy.shouldRetryWithAuthParam(request, httpResponse, null));
    }

    @Test
    public void isRetryable_RegionHeaderPresent_ReturnsRedirectToRegionEndpoint() {
        httpResponse.addHeader(Headers.S3_SERVING_REGION, REGION);

        AuthRetryParameters authParams = retryStrategy.shouldRetryWithAuthParam(request, httpResponse, null);

        Mockito.verify(endpointResolver).resolveRequestEndpoint(request, REGION);
        assertThat(authParams.getSignerForRetry(), Matchers.instanceOf(AWSS3V4Signer.class));
        assertEquals(REGION, ((AWSS3V4Signer) authParams.getSignerForRetry()).getRegionName());
    }

    @Test
    public void isRetryable_RegionHeaderMissing_ReturnsRedirectToS3External() {
        AuthRetryParameters authParams = retryStrategy.shouldRetryWithAuthParam(request, httpResponse, null);

        assertThat(authParams.getSignerForRetry(), Matchers.instanceOf(AWSS3V4Signer.class));
        assertEquals(Regions.US_EAST_1.getName(), ((AWSS3V4Signer) authParams
                .getSignerForRetry()).getRegionName());
        assertEquals("https://" + BUCKET_NAME + ".s3-external-1.amazonaws.com", authParams.getEndpointForRetry()
                .toString());
    }

    @Test(expected = AmazonClientException.class)
    public void isRetryable_RegionHeaderMissing_BucketNotVirtuallyAddressable_ThrowsAmazonClientException() {
        // Bucket name with upper case letters is not virtually addressable so this should fail
        Mockito.when(endpointResolver.getBucketName()).thenReturn("invalidBucketName");
        retryStrategy.shouldRetryWithAuthParam(request, httpResponse, null);
    }

    private static <T> SdkPredicate<T> getPredicate(final boolean toReturn) {
        return new SdkPredicate<T>() {
            @Override
            public boolean test(T t) {
                return toReturn;
            }
        };
    }

}
