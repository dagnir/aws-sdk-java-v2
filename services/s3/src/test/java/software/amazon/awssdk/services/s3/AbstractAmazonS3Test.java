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

package software.amazon.awssdk.services.s3;

import java.io.File;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * This test class exists because we want to make sure we update
 * AbstractAmazonS3 every time we introduce a method in AmazonS3 interface.
 */
public class AbstractAmazonS3Test {

    private static final String BUCKET_NAME = "foo-bucket";

    static class TestAmazonS3 extends AbstractAmazonS3 {

        @Override
        public Bucket createBucket(CreateBucketRequest createBucketRequest)
                throws AmazonClientException, AmazonServiceException {
            return new Bucket(createBucketRequest.getBucketName());
        }

    }

    @Test
    public void testAbstractClient_ImplementedMethodExpectedToReturnSuccess() {
        final TestAmazonS3 s3 = new TestAmazonS3();
        Bucket b = s3.createBucket(new CreateBucketRequest(BUCKET_NAME));
        Assert.assertEquals(BUCKET_NAME, b.getName());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAbstractClient_UnimplementedMethodExpectedToThrowException() {
        final TestAmazonS3 s3 = new TestAmazonS3();
        s3.putObject(new PutObjectRequest(BUCKET_NAME, "key", (File)null));
    }
}
