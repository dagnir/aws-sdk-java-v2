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

import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.getTestKeyPair;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.getTestSecretKey;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tempBucketName;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tryCreateBucket;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

import software.amazon.awssdk.auth.profile.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoStorageMode;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.InstructionFileId;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutInstructionFileRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3ObjectId;
import software.amazon.awssdk.services.s3.model.SimpleMaterialProvider;

public class S3KeyRotationDemo {
    public static void main(String[] args) throws Exception {
        // Configures a material provider for a client-managed master key v1.0
        SimpleMaterialProvider origMaterialProvider = 
            new SimpleMaterialProvider().withLatest(
                new EncryptionMaterials(getTestSecretKey())
                    .addDescription("version", "v1.0"));

        // Configure to use instruction file storage mode
        CryptoConfiguration config = new CryptoConfiguration()
            .withStorageMode(CryptoStorageMode.InstructionFile)
            .withIgnoreMissingInstructionFile(false);

        final AmazonS3EncryptionClient s3v1 = new AmazonS3EncryptionClient(
                new ProfileCredentialsProvider(),
                origMaterialProvider, config)
            .withRegion(Region.getRegion(Regions.US_EAST_1));

        String bucket = tempBucketName(S3KeyRotationDemo.class.getSimpleName());
        tryCreateBucket(s3v1, bucket);

        // Encrypts and saves the data under the name "sensitive_data.txt"
        // to S3. Under the hood, the client-managed master key v1.0 is used
        // to encrypt the randomly generated data key which gets automatically
        // saved in a separate  "instruction file".
        byte[] plaintext = "Hello S3 Client-side Key Rotation!"
                                .getBytes(Charset.forName("UTF-8"));
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(plaintext.length);
        PutObjectResult putResult = s3v1.putObject(bucket, "sensitive_data.txt",
                new ByteArrayInputStream(plaintext), metadata);
        System.out.println(putResult);

        // Retrieves and decrypts the data.
        S3Object s3object = s3v1.getObject(bucket, "sensitive_data.txt");
        System.out.println("Encrypt/Decrypt using v1.0 master key: "
                + IOUtils.toString(s3object.getObjectContent()));
        s3v1.shutdown();

        // Time to rotate to a new v2.0 mater key, but we still need access
        // to the v1.0 master key until the key rotation is complete.
        SimpleMaterialProvider materialProvider = 
            new SimpleMaterialProvider()
                .withLatest(new EncryptionMaterials(getTestKeyPair())
                                .addDescription("version", "v2.0"))
                .addMaterial(new EncryptionMaterials(getTestSecretKey())
                                .addDescription("version", "v1.0"));

        final AmazonS3EncryptionClient s3 = new AmazonS3EncryptionClient(
                new ProfileCredentialsProvider(),
                materialProvider, config)
            .withRegion(Region.getRegion(Regions.US_EAST_1));

        // Change the master key from v1.0 to v2.0.
        PutObjectResult result = s3.putInstructionFile(new PutInstructionFileRequest(
            new S3ObjectId(bucket, "sensitive_data.txt"),
            materialProvider.getEncryptionMaterials(), 
            InstructionFileId.DEFAULT_INSTRUCTION_FILE_SUFFIX));
        System.out.println(result);

        s3object = s3.getObject(bucket, "sensitive_data.txt");
        System.out.println("Client-managed master key rotated from v1.0 to v2.0: "
                + IOUtils.toString(s3object.getObjectContent()));
        s3.shutdown();
        // Key rotation success!

        // Once the key rotation is complete, access to only v2.0 master key is
        // necessary.
        SimpleMaterialProvider v2materialProvider =
            new SimpleMaterialProvider()
                .withLatest(new EncryptionMaterials(getTestKeyPair())
                                .addDescription("version", "v2.0"));
        final AmazonS3EncryptionClient s3v2 = new AmazonS3EncryptionClient(
                new ProfileCredentialsProvider(),
                v2materialProvider, config)
            .withRegion(Region.getRegion(Regions.US_EAST_1));

        s3object = s3v2.getObject(bucket, "sensitive_data.txt");
        System.out.println("Decrypt using v2.0 master key: "
                + IOUtils.toString(s3object.getObjectContent()));
        // Successfully retrieved and decrypted the data using
        // client-managed master key v2.0.
        s3v2.shutdown();
    }

}
