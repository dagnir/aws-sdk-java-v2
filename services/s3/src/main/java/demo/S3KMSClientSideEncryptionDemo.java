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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import software.amazon.awssdk.auth.profile.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.KMSEncryptionMaterialsProvider;
import software.amazon.awssdk.services.s3.model.ListVersionsRequest;
import software.amazon.awssdk.services.s3.model.ObjectListing;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.services.s3.model.S3VersionSummary;
import software.amazon.awssdk.services.s3.model.VersionListing;
import software.amazon.awssdk.util.StringUtils;

public class S3KMSClientSideEncryptionDemo {

    public static void main(String[] args) throws IOException {
        String customerMasterKeyId = "a986ff87-7dcc-4726-8275-9356a465533a";
        AmazonS3EncryptionClient s3 = new AmazonS3EncryptionClient(
                new ProfileCredentialsProvider(),
                new KMSEncryptionMaterialsProvider(customerMasterKeyId))
            .withRegion(Region.getRegion(Regions.US_EAST_1));
        String bucket = tempBucketName(S3KMSClientSideEncryptionDemo.class.getSimpleName());
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

    public static void tryCreateBucket(AmazonS3 s3, String bucketName) {
        try {
            s3.createBucket(bucketName);
        } catch (Exception ex) {
            LogFactory.getLog(CryptoTestUtils.class).debug("", ex);
        }
        return;
    }

    static String tempBucketName(String prefix) {
        return StringUtils.lowerCase(prefix) + "-" + yyMMddhhmmss();
    }

    static String yyMMddhhmmss() {
        return DateTimeFormat.forPattern("yyMMdd-hhmmss").print(new DateTime());
    }

    static void deleteBucketAndAllContents(AmazonS3 client, String bucketName) {
        System.out.println("Deleting S3 bucket: " + bucketName);
        ObjectListing objectListing = client.listObjects(bucketName);

        while (true) {
            for (Iterator<?> iterator = objectListing.getObjectSummaries().iterator(); iterator.hasNext();) {
                S3ObjectSummary objectSummary = (S3ObjectSummary) iterator.next();
                client.deleteObject(bucketName, objectSummary.getKey());
            }

            if (objectListing.isTruncated()) {
                objectListing = client.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
        VersionListing list = client.listVersions(new ListVersionsRequest().withBucketName(bucketName));
        for (Iterator<?> iterator = list.getVersionSummaries().iterator(); iterator.hasNext(); ) {
            S3VersionSummary s = (S3VersionSummary) iterator.next();
            client.deleteVersion(bucketName, s.getKey(), s.getVersionId());
        }
        client.deleteBucket(bucketName);
    }
}
