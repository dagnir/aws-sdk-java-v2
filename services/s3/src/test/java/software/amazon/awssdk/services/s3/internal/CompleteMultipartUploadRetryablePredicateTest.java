/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import software.amazon.awssdk.function.SdkPredicate;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;

public class CompleteMultipartUploadRetryablePredicateTest {

    private SdkPredicate<AmazonS3Exception> predicate = new CompleteMultipartUploadRetryablePredicate();

    @Test
    public void test_NullException_ReturnsFalse() {
        assertFalse(predicate.test(null));
    }

    @Test
    public void nullErrorMessage_ReturnsFalse() {
        assertFalse(predicate.test(new AmazonS3Exception(null)));
    }

    @Test
    public void nullErrorCode_ReturnsFalse() {
        AmazonS3Exception ase = new AmazonS3Exception("NonNull message");
        ase.setErrorCode(null);
        assertFalse(predicate.test(ase));
    }

    @Test
    public void matchingErrorCode_NonMatchingErrorMessage_ReturnsFalse() {
        AmazonS3Exception ase = new AmazonS3Exception("This message doesn't match");
        ase.setErrorCode("InvalidRequest");
        assertFalse(predicate.test(ase));
    }

    @Test
    public void nonMatchingErrorCode_MatchingErrorMessage_ReturnsFalse() {
        AmazonS3Exception ase = new AmazonS3Exception("We encountered an internal error. Please try again.");
        ase.setErrorCode("CodeDoesNotMatch");
        assertFalse(predicate.test(ase));
    }

    @Test
    public void matchingErrorCode_MatchingErrorMessage_ReturnsTrue() {
        AmazonS3Exception ase = new AmazonS3Exception("We encountered an internal error. Please try again.");
        ase.setErrorCode("InternalError");
        assertTrue(predicate.test(ase));
    }
}
