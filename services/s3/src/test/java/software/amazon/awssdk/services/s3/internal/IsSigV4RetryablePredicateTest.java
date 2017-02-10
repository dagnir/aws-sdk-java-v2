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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.function.SdkPredicate;

public class IsSigV4RetryablePredicateTest {

    private SdkPredicate<AmazonServiceException> predicate = new IsSigV4RetryablePredicate();

    @Test
    public void test_NullException_ReturnsFalse() {
        assertFalse(predicate.test(null));
    }

    @Test
    public void nullErrorMessage_ReturnsFalse() {
        assertFalse(predicate.test(new AmazonServiceException(null)));
    }

    @Test
    public void nullErrorCode_ReturnsFalse() {
        AmazonServiceException ase = new AmazonServiceException("NonNull message");
        ase.setErrorCode(null);
        assertFalse(predicate.test(ase));
    }

    @Test
    public void matchingErrorCode_NonMatchingErrorMessage_ReturnsFalse() {
        AmazonServiceException ase = new AmazonServiceException("This message doesn't match");
        ase.setErrorCode("InvalidRequest");
        assertFalse(predicate.test(ase));
    }

    @Test
    public void nonMatchingErrorCode_MatchingErrorMessage_ReturnsFalse() {
        AmazonServiceException ase = new AmazonServiceException("----Please use AWS4-HMAC-SHA256.-----");
        ase.setErrorCode("CodeDoesNotMatch");
        assertFalse(predicate.test(ase));
    }

    @Test
    public void matchingErrorCode_MatchingErrorMessage_ReturnsTrue() {
        AmazonServiceException ase = new AmazonServiceException("----Please use AWS4-HMAC-SHA256.-----");
        ase.setErrorCode("InvalidRequest");
        assertTrue(predicate.test(ase));
    }
}
