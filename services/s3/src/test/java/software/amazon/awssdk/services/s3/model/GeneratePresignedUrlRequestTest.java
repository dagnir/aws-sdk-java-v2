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

package software.amazon.awssdk.services.s3.model;

import org.junit.Test;

public class GeneratePresignedUrlRequestTest {

    @Test
    public void nullKey() {
        new GeneratePresignedUrlRequest("bucket", null).rejectIllegalArguments();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullBucket() {
        new GeneratePresignedUrlRequest(null, "key").rejectIllegalArguments();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullMethod() {
        new GeneratePresignedUrlRequest("bucket", "key").withMethod(null)
                                                        .rejectIllegalArguments();
    }

    @Test
    public void defaultGetMethod() {
        new GeneratePresignedUrlRequest("bucket", "key").rejectIllegalArguments();
    }

    @Test
    public void sse() {
        new GeneratePresignedUrlRequest("bucket", "key")
                .withSseAlgorithm(SseAlgorithm.getDefault())
                .rejectIllegalArguments();
    }

    @Test
    public void sse_c() {
        new GeneratePresignedUrlRequest("bucket", "key")
                .withSseCustomerKeyAlgorithm(SseAlgorithm.getDefault())
                .rejectIllegalArguments();
    }

    @Test(expected = IllegalArgumentException.class)
    public void sse_and_sse_c() {
        new GeneratePresignedUrlRequest("bucket", "key")
                .withSseCustomerKeyAlgorithm(SseAlgorithm.getDefault())
                .withSseAlgorithm(SseAlgorithm.getDefault())
                .rejectIllegalArguments();

    }

    @Test(expected = IllegalArgumentException.class)
    public void kmsCmkId_and_sse_c() {
        new GeneratePresignedUrlRequest("bucket", "key")
                .withSseCustomerKeyAlgorithm(SseAlgorithm.getDefault())
                .withKmsCmkId("kms_cmk_id")
                .rejectIllegalArguments();

    }

    @Test(expected = IllegalArgumentException.class)
    public void sse_kms_cmkId_only() {
        new GeneratePresignedUrlRequest("bucket", "key")
                .withKmsCmkId("kms_cmk_id")
                .rejectIllegalArguments();
    }

    // This case is valid as S3 would use the default KMS CMK ID.
    @Test
    public void sse_kms_without_cmkId() {
        new GeneratePresignedUrlRequest("bucket", "key")
                .withSseAlgorithm(SseAlgorithm.KMS)
                .rejectIllegalArguments();
    }

    public void sse_kms() {
        new GeneratePresignedUrlRequest("bucket", "key")
                .withSseAlgorithm(SseAlgorithm.KMS)
                .withKmsCmkId("kms_cmk_id")
                .rejectIllegalArguments();

    }

    @Test(expected = IllegalArgumentException.class)
    public void sse_c_kms() {
        new GeneratePresignedUrlRequest("bucket", "key")
                .withSseCustomerKeyAlgorithm(SseAlgorithm.KMS)
        ;
    }
}
