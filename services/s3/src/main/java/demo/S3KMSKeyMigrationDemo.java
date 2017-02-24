package demo;

import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.getTestSecretKey;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tempBucketName;
import static software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils.tryCreateBucket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.auth.profile.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.model.CryptoConfiguration;
import software.amazon.awssdk.services.s3.model.CryptoStorageMode;
import software.amazon.awssdk.services.s3.model.EncryptedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.InstructionFileId;
import software.amazon.awssdk.services.s3.model.KMSEncryptionMaterials;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutInstructionFileRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResult;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3ObjectId;
import software.amazon.awssdk.services.s3.model.SimpleMaterialProvider;

public class S3KMSKeyMigrationDemo {
    public static void main(String[] args) throws IOException {
        // Before KMS, existing S3 objects are encrypted using a client-managed 
        // master key
        SimpleMaterialProvider materialProviderOld = 
            new SimpleMaterialProvider().withLatest(
                    new EncryptionMaterials(getTestSecretKey()));
        AmazonS3EncryptionClient s3Old = new AmazonS3EncryptionClient(
                new ProfileCredentialsProvider(),
                materialProviderOld)
            .withRegion(Region.getRegion(Regions.US_EAST_1));
        
        String bucket = tempBucketName(S3KMSKeyMigrationDemo.class.getSimpleName());
        tryCreateBucket(s3Old, bucket);
        
        // Encrypts and saves the data under the name "sensitive_data.txt" to.
        // S3. Under the hood, the one-time data key is encrypted by the
        // client-side master key.
        byte[] plaintext = "Demo S3 Client-side Key Migration to AWS KMS!"
                .getBytes(Charset.forName("UTF-8"));
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(plaintext.length);
        PutObjectResult putResult = s3Old.putObject(bucket, "sensitive_data.txt",
                new ByteArrayInputStream(plaintext), metadata);
        System.out.println(putResult);

        S3Object s3object = s3Old.getObject(bucket, "sensitive_data.txt");
        System.out.println(IOUtils.toString(s3object.getObjectContent()));

        // Time to migrate to AWS KMS!
        

        // Assumption: created a KMS CMK

        // The migrating material provider uses the KMS-managed CMK for
        // encrypting all new S3 objects, and provides access to 
        // the old client-side master key
        String customerMasterKeyId = "a986ff87-7dcc-4726-8275-9356a465533a";
        SimpleMaterialProvider migratingMaterialProvider = 
            new SimpleMaterialProvider().withLatest(
                new KMSEncryptionMaterials(customerMasterKeyId))
                .addMaterial(new EncryptionMaterials(getTestSecretKey()));

        // Configure to use instruction file storage mode so that future S3 
        // objects can be easily key rotated
        CryptoConfiguration config = new CryptoConfiguration()
            .withStorageMode(CryptoStorageMode.InstructionFile)
            .withIgnoreMissingInstructionFile(false);
        AmazonS3EncryptionClient s3Migrate = new AmazonS3EncryptionClient(
                new ProfileCredentialsProvider(),
                migratingMaterialProvider, config)
            .withRegion(Region.getRegion(Regions.US_EAST_1));

        // Rotate the KEK from the original to a customer master key managed by
        // AWS KMS
        PutObjectResult result = s3Migrate.putInstructionFile(
            new PutInstructionFileRequest(
                new S3ObjectId(bucket, "sensitive_data.txt"),
                new KMSEncryptionMaterials(customerMasterKeyId), 
                InstructionFileId.DEFAULT_INSTRUCTION_FILE_SUFFIX));
        System.out.println(result);
        s3Migrate.shutdown();


        // Once key migration is complete, you can now exclusively use the
        // KMS-managed CMK.
        SimpleMaterialProvider kmsMaterialProvider = 
                new SimpleMaterialProvider().withLatest(
                    new KMSEncryptionMaterials(customerMasterKeyId));
        AmazonS3EncryptionClient s3New = new AmazonS3EncryptionClient(
                new ProfileCredentialsProvider(),
                kmsMaterialProvider, config)
            .withRegion(Region.getRegion(Regions.US_EAST_1));

        // Data key can be decrypted using the new KMS CMK  
        EncryptedGetObjectRequest getReq = 
            new EncryptedGetObjectRequest(bucket, "sensitive_data.txt")
                .withInstructionFileSuffix(
                    InstructionFileId.DEFAULT_INSTRUCTION_FILE_SUFFIX);
        s3object = s3New.getObject(getReq);
        System.out.println(IOUtils.toString(s3object.getObjectContent()));
        // Key rotation success!

        // The client-managed master key can still be used to decrypt the old 
        // ciphertext of the one-time data key persisted in the metadata
        s3object = s3Old.getObject(bucket, "sensitive_data.txt");
        System.out.println(IOUtils.toString(s3object.getObjectContent()));

        // However, any attempt to use the KMS CMK to decrypt the old ciphertext
        // of the data key in the metatdata would result in an error.
        try {
            s3object = s3New.getObject(bucket, "sensitive_data.txt");
            System.out.println(IOUtils.toString(s3object.getObjectContent()));
        } catch(AmazonClientException ex) {
            System.out.println(ex.getMessage());
        }
        s3Old.shutdown();
        s3New.shutdown();
    }

}
