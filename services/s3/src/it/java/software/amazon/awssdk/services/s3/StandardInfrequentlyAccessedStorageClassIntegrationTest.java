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

package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration.NoncurrentVersionTransition;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration.Rule;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration.Transition;
import software.amazon.awssdk.services.s3.model.BucketReplicationConfiguration;
import software.amazon.awssdk.services.s3.model.BucketVersioningConfiguration;
import software.amazon.awssdk.services.s3.model.ObjectListing;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.Region;
import software.amazon.awssdk.services.s3.model.ReplicationDestinationConfig;
import software.amazon.awssdk.services.s3.model.ReplicationRule;
import software.amazon.awssdk.services.s3.model.ReplicationRuleStatus;
import software.amazon.awssdk.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.services.s3.model.SetBucketVersioningConfigurationRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.util.StringUtils;

/**
 * Tests for the new Standard Infrequently Accessed storage class.
 * TODO Cleanup the test code when moving to mainline branch.
 */
public class StandardInfrequentlyAccessedStorageClassIntegrationTest extends S3IntegrationTestBase {

    private static final String KEY_PREFIX = "prefix";

    private static final String RULE_ID = "first-rule";
    private final static String BUCKET_NAME = "aws-java-sdk-standard-ia-test-" + System.currentTimeMillis();
    private final static String key = "key";
    private static final String DEST_BUCKET_NAME = "mvtargetbucketforreplication-" + System.currentTimeMillis();
    private static final String DEST_BUCKET_ARN = "arn:aws:s3:::"
                                                  + DEST_BUCKET_NAME;
    /** The time in days from  object's creation to its expiration used in the tests */
    private static final int EXPIRATION_IN_DAYS = 100;
    /** The time in days from an object's creation to the transition time(change the storage class to STANDARD_IA) used in the tests */
    private static final int STANDARD_IA_TRANSITION_TIME_IN_DAYS = 31;
    /** The time in days from an object's creation to the transition time(change the storage class to GLACIER) used in the tests */
    private static final int GLACIER_TRANSITION_TIME_IN_DAYS = 70;
    private static final Transition standardIATransition = new Transition()
            .withDays(STANDARD_IA_TRANSITION_TIME_IN_DAYS)
            .withStorageClass(StorageClass.StandardInfrequentAccess);
    private static final Transition glacierTransition = new Transition()
            .withDays(GLACIER_TRANSITION_TIME_IN_DAYS)
            .withStorageClass(StorageClass.Glacier);
    private static final NoncurrentVersionTransition standardIANoncurrentTransition = new NoncurrentVersionTransition()
            .withDays(STANDARD_IA_TRANSITION_TIME_IN_DAYS)
            .withStorageClass(StorageClass.StandardInfrequentAccess);
    private static final NoncurrentVersionTransition glacierNonCurrentTransition = new NoncurrentVersionTransition()
            .withDays(GLACIER_TRANSITION_TIME_IN_DAYS)
            .withStorageClass(StorageClass.Glacier);
    /**
     * ARN of the IAM role used for replication.
     */
    private static final String ROLE = "arn:aws:iam::pikc123456:role/abcdef";
    private static AmazonS3Client s3UsWest2 = null;
    private static AmazonS3Client euS3 = null;

    @BeforeClass
    public static void setUp() throws InterruptedException, IOException {
        setUpCredentials();
        s3UsWest2 = new AmazonS3Client(credentials);
        s3UsWest2.setRegion(software.amazon.awssdk.regions.Region
                                    .getRegion(Regions.US_WEST_2));

        euS3 = new AmazonS3Client(credentials);
        euS3.setRegion(software.amazon.awssdk.regions.Region
                               .getRegion(Regions.EU_WEST_1));

        s3UsWest2.createBucket(BUCKET_NAME, Region.US_West_2);
        euS3.createBucket(DEST_BUCKET_NAME, Region.EU_Ireland);

        s3UsWest2.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(
                BUCKET_NAME, new BucketVersioningConfiguration(
                "Enabled")));
        euS3.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(
                DEST_BUCKET_NAME, new BucketVersioningConfiguration(
                "Enabled")));
    }

    @AfterClass
    public static void cleanUp() {
        CryptoTestUtils.deleteBucketAndAllContents(s3UsWest2, BUCKET_NAME);
        CryptoTestUtils.deleteBucketAndAllContents(euS3, DEST_BUCKET_NAME);
        if (euS3 != null) {
            euS3.shutdown();
        }
        if (s3UsWest2 != null) {
            s3UsWest2.shutdown();
        }
    }

    private static void assertTransition(Transition expected, Transition actual) {
        assertEquals(expected.getDate(), actual.getDate());
        assertEquals(expected.getDays(), actual.getDays());
        assertEquals(expected.getStorageClass(), actual.getStorageClass());
    }

    private static void assertNoncurrentTransition(
            NoncurrentVersionTransition expected,
            NoncurrentVersionTransition actual) {
        assertEquals(expected.getDays(), actual.getDays());
        assertEquals(expected.getStorageClass(), actual.getStorageClass());
    }

    @Test
    public void testStandardInFrequentlyStorageClass() {

        final byte[] contents = "lt test".getBytes(StringUtils.UTF8);
        final ObjectMetadata metadata = new ObjectMetadata();

        metadata.setContentLength(contents.length);

        s3UsWest2.putObject(new PutObjectRequest(
                BUCKET_NAME,
                key,
                new ByteArrayInputStream(contents),
                metadata)
                                    .withStorageClass(StorageClass.StandardInfrequentAccess));

        ObjectListing bucketList = s3UsWest2.listObjects(BUCKET_NAME, key);
        boolean keyFound = false;
        for (S3ObjectSummary summary : bucketList.getObjectSummaries()) {
            if (key.equals(summary.getKey())) {
                keyFound = true;
                assertEquals(StorageClass.StandardInfrequentAccess, StorageClass.fromValue(summary.getStorageClass()));
            }
        }

        if (!keyFound) {
            fail("List Object didn't retrieve the key from the bucket");
        }
    }

    @Test
    public void testReplication() {

        final String RULE = "replication-rule-1-"
                            + System.currentTimeMillis();

        final BucketReplicationConfiguration configuration = new BucketReplicationConfiguration()
                .withRoleARN(ROLE);

        configuration.addRule(
                RULE,
                new ReplicationRule()
                        .withPrefix("testPrefix1")
                        .withStatus(ReplicationRuleStatus.Enabled)
                        .withDestinationConfig(
                                new ReplicationDestinationConfig()
                                        .withBucketARN(DEST_BUCKET_ARN)
                                        .withStorageClass(StorageClass.StandardInfrequentAccess)));

        s3UsWest2.setBucketReplicationConfiguration(BUCKET_NAME, configuration);

        BucketReplicationConfiguration retrievedReplicationConfig = s3UsWest2
                .getBucketReplicationConfiguration(BUCKET_NAME);

        ReplicationRule replRule1 = retrievedReplicationConfig.getRule(RULE);
        assertEquals(StorageClass.StandardInfrequentAccess.toString(), replRule1.getDestinationConfig()
                                                                                .getStorageClass());
    }

    @Test
    public void testLifecycleTransition() {
        BucketLifecycleConfiguration bucketLifecycleConfiguration = s3UsWest2
                .getBucketLifecycleConfiguration(BUCKET_NAME);
        assertNull(bucketLifecycleConfiguration);

        bucketLifecycleConfiguration = new BucketLifecycleConfiguration()
                .withRules(new Rule()
                                   .withId(RULE_ID)
                                   .withExpirationInDays(EXPIRATION_IN_DAYS)
                                   .withPrefix(KEY_PREFIX)
                                   .withStatus(BucketLifecycleConfiguration.ENABLED)
                                   .addTransition(standardIATransition)
                                   .addTransition(glacierTransition)
                                   .addNoncurrentVersionTransition(standardIANoncurrentTransition)
                                   .addNoncurrentVersionTransition(glacierNonCurrentTransition));

        s3UsWest2.setBucketLifecycleConfiguration(BUCKET_NAME, bucketLifecycleConfiguration);

        bucketLifecycleConfiguration = s3UsWest2
                .getBucketLifecycleConfiguration(BUCKET_NAME);
        assertNotNull(bucketLifecycleConfiguration);

        List<Rule> rules = bucketLifecycleConfiguration.getRules();
        assertEquals(1, rules.size());

        Rule rule = rules.get(0);
        assertEquals(2, rule.getTransitions().size());
        assertEquals(2, rule.getNoncurrentVersionTransitions().size());
        assertTransition(standardIATransition, rule.getTransitions().get(0));
        assertTransition(glacierTransition, rule.getTransitions().get(1));
        assertNoncurrentTransition(standardIANoncurrentTransition, rule.getNoncurrentVersionTransitions().get(0));
        assertNoncurrentTransition(glacierNonCurrentTransition, rule.getNoncurrentVersionTransitions().get(1));
        assertEquals(EXPIRATION_IN_DAYS, rule.getExpirationInDays());
        assertEquals(KEY_PREFIX, rule.getPrefix());
        assertEquals(RULE_ID, rule.getId());
        assertEquals(BucketLifecycleConfiguration.ENABLED, rule.getStatus());

        // The deprecated method should return the first transition.
        assertTransition(glacierTransition, rule.getTransition());
        assertNoncurrentTransition(glacierNonCurrentTransition, rule.getNoncurrentVersionTransition());
    }
}
