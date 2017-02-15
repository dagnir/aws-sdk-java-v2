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

package software.amazon.awssdk.services.s3.internal.crypto;

import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;

public class MultipartUploadCryptoContextTest {

    @Test(expected = IllegalArgumentException.class)
    public void testBadPartNumber() {
        MultipartUploadCryptoContext ctx = new MultipartUploadCryptoContext(
                "bucket", "key", null);
        ctx.beginPartUpload(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadPartNumber0() {
        MultipartUploadCryptoContext ctx = new MultipartUploadCryptoContext(
                "bucket", "key", null);
        ctx.beginPartUpload(0);
    }

    @Test(expected = AmazonClientException.class)
    public void testBadRetry() {
        MultipartUploadCryptoContext ctx = new MultipartUploadCryptoContext(
                "bucket", "key", null);
        ctx.beginPartUpload(1);
        ctx.beginPartUpload(1);
    }

    @Test
    public void testRetry() {
        MultipartUploadCryptoContext ctx = new MultipartUploadCryptoContext(
                "bucket", "key", null);
        ctx.beginPartUpload(1);
        ctx.endPartUpload();
        ctx.beginPartUpload(1);
    }

    @Test(expected = AmazonClientException.class)
    public void testOutOfSync() {
        MultipartUploadCryptoContext ctx = new MultipartUploadCryptoContext(
                "bucket", "key", null);
        ctx.beginPartUpload(1);
        ctx.endPartUpload();
        ctx.beginPartUpload(3);
    }

    @Test
    public void testInSync() {
        MultipartUploadCryptoContext ctx = new MultipartUploadCryptoContext(
                "bucket", "key", null);
        ctx.beginPartUpload(1);
        ctx.endPartUpload();
        ctx.beginPartUpload(2);
    }
}
