import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.Assert;

import software.amazon.awssdk.auth.profile.ProfileCredentialsProvider;
import software.amazon.awssdk.services.s3.AmazonS3;
import software.amazon.awssdk.services.s3.AmazonS3EncryptionClient;
import software.amazon.awssdk.services.s3.model.EncryptionMaterials;
import software.amazon.awssdk.services.s3.model.ListVersionsRequest;
import software.amazon.awssdk.services.s3.model.ObjectListing;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.services.s3.model.S3VersionSummary;
import software.amazon.awssdk.services.s3.model.StaticEncryptionMaterialsProvider;
import software.amazon.awssdk.services.s3.model.VersionListing;

/**
 * Demo code - not included for release purposes.
 */
public class S3ClientSideEncryptionWithSymmetricMasterKey {
    private static final String masterKeyDir = System.getProperty("java.io.tmpdir");
    private static final String bucketName = UUID.randomUUID() + "-"
            + DateTimeFormat.forPattern("yyMMdd-hhmmss").print(new DateTime());
    private static final String objectKey = UUID.randomUUID().toString();

    public static void main(String[] args) throws Exception {
        SecretKey mySymmetricKey = GenerateSymmetricMasterKey
                .loadSymmetricAESKey(masterKeyDir, "AES");

        EncryptionMaterials encryptionMaterials = new EncryptionMaterials(
                mySymmetricKey);

        AmazonS3EncryptionClient encryptionClient = new AmazonS3EncryptionClient(
                new ProfileCredentialsProvider(),
                new StaticEncryptionMaterialsProvider(encryptionMaterials));
        // Create the bucket
        encryptionClient.createBucket(bucketName);

        // Upload object using the encryption client.
        byte[] plaintext = "Hello World, S3 Client-side Encryption Using Asymmetric Master Key!"
                .getBytes();
        System.out.println("plaintext's length: " + plaintext.length);
        encryptionClient.putObject(new PutObjectRequest(bucketName, objectKey,
                new ByteArrayInputStream(plaintext), new ObjectMetadata()));

        // Download the object.
        S3Object downloadedObject = encryptionClient.getObject(bucketName,
                objectKey);
        byte[] decrypted = IOUtils.toByteArray(downloadedObject
                .getObjectContent());
        
        // Verify same data.
        Assert.assertTrue(Arrays.equals(plaintext, decrypted));
        deleteBucketAndAllContents(encryptionClient);
    }

    private static void deleteBucketAndAllContents(AmazonS3 client) {
        System.out.println("Deleting S3 bucket: " + bucketName);
        ObjectListing objectListing = client.listObjects(bucketName);

        while (true) {
            for ( Iterator<?> iterator = objectListing.getObjectSummaries().iterator(); iterator.hasNext(); ) {
                S3ObjectSummary objectSummary = (S3ObjectSummary) iterator.next();
                client.deleteObject(bucketName, objectSummary.getKey());
            }

            if (objectListing.isTruncated()) {
                objectListing = client.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        };
        VersionListing list = client.listVersions(new ListVersionsRequest().withBucketName(bucketName));
        for ( Iterator<?> iterator = list.getVersionSummaries().iterator(); iterator.hasNext(); ) {
            S3VersionSummary s = (S3VersionSummary)iterator.next();
            client.deleteVersion(bucketName, s.getKey(), s.getVersionId());
        }
        client.deleteBucket(bucketName);
    }
}
