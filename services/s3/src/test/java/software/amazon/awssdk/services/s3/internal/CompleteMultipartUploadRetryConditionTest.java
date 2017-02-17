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

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.function.SdkPredicate;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;

public class CompleteMultipartUploadRetryConditionTest {

    @Test
    public void notAnAmazonS3Exception_ReturnsFalse() {
        CompleteMultipartUploadRetryCondition retryCondition = new
                CompleteMultipartUploadRetryCondition();
        Assert.assertFalse(retryCondition.shouldRetry(null,
                                                      new AmazonClientException("null"), 0));
        Assert.assertFalse(retryCondition.shouldRetry(null, new
                AmazonServiceException("null"), 0));
    }

    @Test
    public void alwaysFalsePredicate_ReturnsFalse() {
        SdkPredicate<AmazonS3Exception> predicate = new SdkPredicate<AmazonS3Exception>() {
            @Override
            public boolean test(AmazonS3Exception e) {
                return false;
            }
        };
        CompleteMultipartUploadRetryCondition retryCondition = new CompleteMultipartUploadRetryCondition(predicate, 0);
        Assert.assertFalse(retryCondition.shouldRetry(null,
                                                      new AmazonS3Exception("null"), 0));

    }

    @Test(expected = IllegalArgumentException.class)
    public void nullPredicate_throwsException() {
        new CompleteMultipartUploadRetryCondition(null, 0);
    }

    @Test
    public void retryAttemptsExhausted_ReturnsFalse() {
        AmazonS3Exception s3Exception = new AmazonS3Exception("Please try again");
        s3Exception.setErrorCode("InternalError");

        CompleteMultipartUploadRetryCondition retryCondition = new CompleteMultipartUploadRetryCondition();
        Assert.assertFalse(retryCondition.shouldRetry(null,
                                                      s3Exception, 4));
    }

    @Test
    public void maxRetryAttemptsSetToZero_ReturnsFalse() {
        AmazonS3Exception s3Exception = new AmazonS3Exception("Please try again");
        s3Exception.setErrorCode("InternalError");

        CompleteMultipartUploadRetryCondition retryCondition = new CompleteMultipartUploadRetryCondition(
                new CompleteMultipartUploadRetryablePredicate(), 0);
        Assert.assertFalse(retryCondition.shouldRetry(null,
                                                      s3Exception, 1));
    }

    @Test
    public void validRetryableExceptionRetriesNotExhausted_ReturnsTrue() {
        AmazonS3Exception s3Exception = new AmazonS3Exception("Please try again");
        s3Exception.setErrorCode("InternalError");

        CompleteMultipartUploadRetryCondition retryCondition = new CompleteMultipartUploadRetryCondition(
                new CompleteMultipartUploadRetryablePredicate(), 4);
        Assert.assertFalse(retryCondition.shouldRetry(null,
                                                      s3Exception, 1));
    }
}
