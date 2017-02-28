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

package demo;

import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tempBucketName;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tryCreateBucket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

import software.amazon.awssdk.auth.profile.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.KMSEncryptionMaterialsProvider;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3KMSRegionDemo {

    public static void main(String[] args) throws IOException {
        String customerMasterKeyId = "fc763590-3758-4e2f-9da6-303ecbfe37eb";
        AmazonS3EncryptionClient s3 = new AmazonS3EncryptionClient(
                new ProfileCredentialsProvider(),
                new KMSEncryptionMaterialsProvider(customerMasterKeyId),
                new CryptoConfiguration().withKmsRegion(Regions.US_WEST_2))
            .withRegion(Region.getRegion(Regions.US_WEST_2));
        String bucket = tempBucketName(S3KMSRegionDemo.class.getSimpleName());
        tryCreateBucket(s3, bucket);
        byte[] plaintext = "Hello S3/KMS Client-side Encryption!".getBytes(Charset.forName("UTF-8"));
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(plaintext.length);
        PutObjectResult putResult = s3.putObject(bucket, "hello_s3_kms.txt", new ByteArrayInputStream(plaintext), metadata);
        System.out.println(putResult);
        S3Object s3object = s3.getObject(bucket, "hello_s3_kms.txt");
        System.out.println(IOUtils.toString(s3object.getObjectContent()));
        // deleteBucketAndAllContents(s3, bucket);
        s3.shutdown();
    }
}
