package software.amazon.awssdk.kms.utils;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.auth.AWSCredentials;
import software.amazon.awssdk.regions.Regions;

/**
 * Caches the ids of both a non default KMS key reserved for s3 integration testing and the default
 * s3 KMS key (alias/aws/s3). Cache is per region. Currently Key IDs are eagerly fetched in the
 * constructor
 */
public class KmsTestKeyCache {

    private static Map<Regions, KmsTestKeyCache> instances = new HashMap<Regions, KmsTestKeyCache>();

    private final String nonDefaultKeyId;
    private final String s3DefaultKeyId;

    /**
     * Retrieve the instance of KmsKeyCache for the given region
     * 
     * @param region
     *            AWS region to retrieve cache for
     * @param awsCredentials
     *            Credentials of test account being used
     * @return Singleton KmsKeyCache instance for given region
     */
    public synchronized static KmsTestKeyCache getInstance(Regions region, AWSCredentials awsCredentials) {
        if (!instances.containsKey(region)) {
            instances.put(region, new KmsTestKeyCache(region, awsCredentials));
        }
        return instances.get(region);
    }

    public KmsTestKeyCache(Regions region, AWSCredentials awsCredentials) {
        KmsClientTestExtensions kmsClient = new KmsClientTestExtensions(awsCredentials);
        kmsClient.configureRegion(region);
        this.nonDefaultKeyId = kmsClient.getNonDefaultKeyId();
        this.s3DefaultKeyId = kmsClient.getDefaultS3KeyId();
    }

    /**
     * @return A non default (i.e. non service default) KMS key to be used for integration testing.
     *         Creates one if it doesn't already exist.
     */
    public String getNonDefaultKeyId() {
        return this.nonDefaultKeyId;
    }

    /**
     * @return The S3 Default KMS key, with alias = alias/aws/s3
     */
    public String getDefaultS3KeyId() {
        return this.s3DefaultKeyId;
    }
}