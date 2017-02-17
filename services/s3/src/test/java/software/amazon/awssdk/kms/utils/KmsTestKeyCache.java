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

package software.amazon.awssdk.kms.utils;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.auth.AwsCredentials;
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

    public KmsTestKeyCache(Regions region, AwsCredentials awsCredentials) {
        KmsClientTestExtensions kmsClient = new KmsClientTestExtensions(awsCredentials);
        kmsClient.configureRegion(region);
        this.nonDefaultKeyId = kmsClient.getNonDefaultKeyId();
        this.s3DefaultKeyId = kmsClient.getDefaultS3KeyId();
    }

    /**
     * Retrieve the instance of KmsKeyCache for the given region
     *
     * @param region
     *            AWS region to retrieve cache for
     * @param awsCredentials
     *            Credentials of test account being used
     * @return Singleton KmsKeyCache instance for given region
     */
    public synchronized static KmsTestKeyCache getInstance(Regions region, AwsCredentials awsCredentials) {
        if (!instances.containsKey(region)) {
            instances.put(region, new KmsTestKeyCache(region, awsCredentials));
        }
        return instances.get(region);
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