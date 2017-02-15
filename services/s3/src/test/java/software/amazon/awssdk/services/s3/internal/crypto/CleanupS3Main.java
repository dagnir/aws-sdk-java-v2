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

import java.io.File;
import java.io.IOException;
import java.util.List;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.auth.PropertiesCredentials;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.Bucket;

public class CleanupS3Main {
    /** Returns the test AWS credential. */
    private static AWSCredentials awsCredentials() throws IOException {
        return new PropertiesCredentials(new File(
                "/Users/hchar/.aws/awsTestAccount.properties"));
        //    "/Users/hchar/.aws/awsTestAccount.properties-glacier"));
    }

    public static void main(String[] args) throws Exception {
        AmazonS3Client s3 = new AmazonS3Client(awsCredentials());
        List<Bucket> list = s3.listBuckets();
        for (Bucket b : list) {
            String name = b.getName();
            if (!name.startsWith("hanson") && !name.startsWith("hchar")) {
                try {
                    CryptoTestUtils.deleteBucketAndAllContents(s3, name);
                } catch (AmazonS3Exception ex) {
                    System.err.println("bucket: " + name);
                    ex.printStackTrace(System.err);
                }
            }
        }
    }
}
