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

import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.internal.ServiceUtils;
import software.amazon.awssdk.services.s3.model.AbortIncompleteMultipartUpload;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration.Rule;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration.Transition;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.lifecycle.LifecycleAndOperator;
import software.amazon.awssdk.services.s3.model.lifecycle.LifecycleFilter;
import software.amazon.awssdk.services.s3.model.lifecycle.LifecycleFilterPredicate;
import software.amazon.awssdk.services.s3.model.lifecycle.LifecyclePrefixPredicate;
import software.amazon.awssdk.services.s3.model.lifecycle.LifecycleTagPredicate;
import software.amazon.awssdk.test.util.RandomTempFile;

/**
 * Integration tests for multi object delete.
 */
public class BucketLifecycleConfigurationIntegrationTest extends S3IntegrationTestBase {

    private static final boolean ANDROID_TESTING = false;

    /** The bucket created and used by these tests. */
    private static final String BUCKET_NAME = "java-bucket-lifecycle-integ-test-" + new Date().getTime();

    /** The key used in these tests. */
    private static final String KEY = "key";
    /** The expiration date of an object in Java Date format for the tests. */
    private static final String EXPIRATION_DATE = "2012-06-01T00:00:00.000Z";
    /** The transition date of an object (change the storage class) in Java Date format for the tests. */
    private static final String TRANSITION_DATE = "2012-05-31T00:00:00.000Z";
    /** The time in days from  object's creation to its expiration used in the tests. */
    private static final int EXPIRATION_IN_DAYS = 10;
    /** The time in days from an object's creation to the transition time(change the storage class) used in the tests. */
    private static final int TRANSITION_TIME_IN_DAYS = 5;
    /** The file containing the test data uploaded to S3. */
    private static File file = null;
    /** The inputStream containing the test data uploaded to S3. */
    private static byte[] tempData;

    @AfterClass
    public static void tearDownFixture() throws Exception {
        deleteBucketAndAllContents(BUCKET_NAME);

        if (file != null) {
            file.delete();
        }
    }

    /**
     * Creates and initializes all the test resources needed for these tests.
     */
    @BeforeClass
    public static void setUpFixture() throws Exception {

        S3IntegrationTestBase.setUp();

        if (!ANDROID_TESTING) {
            setUpCredentials();
        }

        tempData = tempDataBuffer(1000);

        s3.createBucket(BUCKET_NAME);
        waitForBucketCreation(BUCKET_NAME);

        ObjectMetadata metadata = null;
        if (!ANDROID_TESTING) {
            file = new RandomTempFile("get-object-integ-test", 1000L);
            s3.putObject(BUCKET_NAME, KEY, file);
        } else {
            file = S3IntegrationTestBase.getRandomTempFile("foo", 1000L);
            ByteArrayInputStream bais = new ByteArrayInputStream(tempData);

            metadata = new ObjectMetadata();
            metadata.setContentLength(1000);

            s3.putObject(new PutObjectRequest(BUCKET_NAME, KEY, bais, metadata));
            bais.close();
        }

    }

    @Before
    public void setup() throws Exception {
        // Check the bucket for its existing lifecycle config
        assertNull(s3.getBucketLifecycleConfiguration(BUCKET_NAME));
    }

    @After
    public void teardown() throws Exception {
        // Delete the config
        s3.deleteBucketLifecycleConfiguration(BUCKET_NAME);
        assertNull(waitForBucketLifecycleConfigurationDelete(BUCKET_NAME));
    }

    @Test
    public void testBucketLifecycle() throws Exception {

        // Apply a config
        String ruleId = UUID.randomUUID().toString();
        Transition transition = new Transition();
        transition.setDays(TRANSITION_TIME_IN_DAYS);
        transition.setStorageClass(StorageClass.Glacier);
        final int daysAfterInitiation = 10;
        Rule rule1 = new Rule()
                .withExpirationInDays(EXPIRATION_IN_DAYS)
                .withId(ruleId).withPrefix("prefix")
                .withStatus(BucketLifecycleConfiguration.ENABLED)
                .withTransition(transition)
                .withAbortIncompleteMultipartUpload(new
                                                            AbortIncompleteMultipartUpload()
                                                            .withDaysAfterInitiation(daysAfterInitiation));
        transition = new Transition();
        transition.setDate(ServiceUtils.parseIso8601Date(TRANSITION_DATE));
        transition.setStorageClass(StorageClass.Glacier);
        Rule rule2 = new Rule()
                .withExpirationDate(ServiceUtils.parseIso8601Date(EXPIRATION_DATE))
                .withTransition(transition).withPrefix("another")
                .withStatus(BucketLifecycleConfiguration.DISABLED);


        BucketLifecycleConfiguration config = new BucketLifecycleConfiguration().withRules(rule1, rule2);
        s3.setBucketLifecycleConfiguration(BUCKET_NAME, config);

        // Check reading it back
        BucketLifecycleConfiguration bucketLifecycleConfiguration = waitForBucketLifecycleConfiguration(BUCKET_NAME);
        assertNotNull(bucketLifecycleConfiguration);
        assertEquals(2, bucketLifecycleConfiguration.getRules().size());
        boolean seen1 = false;
        boolean seen2 = false;
        for (Rule rule : bucketLifecycleConfiguration.getRules()) {
            if (rule.getId().equals(ruleId)) {
                seen1 = true;
                assertEquals(BucketLifecycleConfiguration.ENABLED, rule.getStatus());
                assertEquals("prefix", rule.getPrefix());
                assertEquals(EXPIRATION_IN_DAYS, rule.getExpirationInDays());
                assertEquals(TRANSITION_TIME_IN_DAYS, rule.getTransition().getDays());
                assertEquals(StorageClass.Glacier, rule.getTransition().getStorageClass());
                assertEquals(daysAfterInitiation, rule
                        .getAbortIncompleteMultipartUpload().getDaysAfterInitiation());
            } else {
                seen2 = true;
                assertNotNull(rule.getId());
                assertEquals(BucketLifecycleConfiguration.DISABLED, rule.getStatus());
                assertEquals(ServiceUtils.formatIso8601Date(rule.getExpirationDate()), EXPIRATION_DATE);
                assertEquals(ServiceUtils.formatIso8601Date(rule.getTransition().getDate()), TRANSITION_DATE);
                assertEquals(StorageClass.Glacier, rule.getTransition().getStorageClass());
                assertEquals("another", rule.getPrefix());
            }
        }
        assertTrue(seen1);
        assertTrue(seen2);

        // Now put some objects and see if they have the right headers returned
        String expiringKey = "prefixKey";
        s3.putObject(BUCKET_NAME, expiringKey, file);
        ObjectMetadata metadataExpriringKey = waitForObjectWithExpirationKeyExist(BUCKET_NAME, expiringKey);

        String nonExpiringKey = "anotherKey";
        s3.putObject(BUCKET_NAME, nonExpiringKey, file);
        ObjectMetadata metadataNonExpriringKey = waitForObjectWithNonExpirationKeyExist(BUCKET_NAME, nonExpiringKey);

        assertNotNull(metadataExpriringKey.getExpirationTime());
        assertEquals(ruleId, metadataExpriringKey.getExpirationTimeRuleId());

        assertNull(metadataNonExpriringKey.getExpirationTime());
        assertNull(metadataNonExpriringKey.getExpirationTimeRuleId());


        // There are several APIs that are affected by this header; test them
        ObjectMetadata copyObjectMetadata = null;
        s3.copyObject(new CopyObjectRequest(BUCKET_NAME, expiringKey, BUCKET_NAME, expiringKey + "2"));
        copyObjectMetadata = waitForObjectWithExpirationKeyExist(BUCKET_NAME, expiringKey + "2");

        assertNotNull(copyObjectMetadata.getExpirationTime());
        assertEquals(ruleId, copyObjectMetadata.getExpirationTimeRuleId());

        s3.copyObject(new CopyObjectRequest(BUCKET_NAME, nonExpiringKey, BUCKET_NAME, nonExpiringKey + "2"));
        copyObjectMetadata = waitForObjectWithNonExpirationKeyExist(BUCKET_NAME, nonExpiringKey + "2");

        assertNull(copyObjectMetadata.getExpirationTime());
        assertNull(copyObjectMetadata.getExpirationTimeRuleId());


        metadataExpriringKey = waitForObjectWithExpirationKeyExist(BUCKET_NAME, expiringKey);
        assertNotNull(metadataExpriringKey.getExpirationTime());
        assertEquals(ruleId, metadataExpriringKey.getExpirationTimeRuleId());

        metadataNonExpriringKey = waitForObjectWithNonExpirationKeyExist(BUCKET_NAME, nonExpiringKey);
        assertNull(metadataNonExpriringKey.getExpirationTime());
        assertNull(metadataNonExpriringKey.getExpirationTimeRuleId());

        // TODO: test multipart upload


    }

    @Test
    public void testBucketLifecycle_With_OnlyPrefix_InFilter() throws Exception {
        Rule rule = getRuleWithoutPrefixAndFilter();
        rule.setFilter(new LifecycleFilter().withPredicate(new LifecyclePrefixPredicate("prefix")));

        BucketLifecycleConfiguration config = new BucketLifecycleConfiguration().withRules(rule);
        s3.setBucketLifecycleConfiguration(BUCKET_NAME, config);

        // Check reading it back
        BucketLifecycleConfiguration bucketLifecycleConfiguration = waitForBucketLifecycleConfiguration(BUCKET_NAME);
        assertNotNull(bucketLifecycleConfiguration);
        assertEquals(1, bucketLifecycleConfiguration.getRules().size());

        Rule actualRule = bucketLifecycleConfiguration.getRules().get(0);
        assertNull(actualRule.getPrefix());

        assertTrue(actualRule.getFilter().getPredicate() instanceof LifecyclePrefixPredicate);
        assertEquals("prefix", ((LifecyclePrefixPredicate) actualRule.getFilter().getPredicate()).getPrefix());
    }

    @Test
    public void testBucketLifecycle_With_OnlyTag_InFilter() throws Exception {
        Rule rule = getRuleWithoutPrefixAndFilter();
        rule.setFilter(new LifecycleFilter().withPredicate(new LifecycleTagPredicate(new Tag("key", "value"))));

        BucketLifecycleConfiguration config = new BucketLifecycleConfiguration().withRules(rule);
        s3.setBucketLifecycleConfiguration(BUCKET_NAME, config);

        // Check reading it back
        BucketLifecycleConfiguration bucketLifecycleConfiguration = waitForBucketLifecycleConfiguration(BUCKET_NAME);
        assertNotNull(bucketLifecycleConfiguration);
        assertEquals(1, bucketLifecycleConfiguration.getRules().size());

        LifecycleTagPredicate filterCriteria =
                (LifecycleTagPredicate) bucketLifecycleConfiguration.getRules().get(0).getFilter().getPredicate();
        assertEquals("key", filterCriteria.getTag().getKey());
        assertEquals("value", filterCriteria.getTag().getValue());
    }

    @Test
    public void testBucketLifecycle_With_OnlyAndOperator_InFilter() throws Exception {
        List<LifecycleFilterPredicate> andOperands = new ArrayList<LifecycleFilterPredicate>();
        andOperands.add(new LifecycleTagPredicate(new Tag("key1", "value1")));
        andOperands.add(new LifecycleTagPredicate(new Tag("key2", "value2")));

        Rule rule = getRuleWithoutPrefixAndFilter();
        rule.setFilter(new LifecycleFilter().withPredicate(new LifecycleAndOperator(andOperands)));

        BucketLifecycleConfiguration config = new BucketLifecycleConfiguration().withRules(rule);
        s3.setBucketLifecycleConfiguration(BUCKET_NAME, config);

        // Check reading it back
        BucketLifecycleConfiguration bucketLifecycleConfiguration = waitForBucketLifecycleConfiguration(BUCKET_NAME);
        assertNotNull(bucketLifecycleConfiguration);
        assertEquals(1, bucketLifecycleConfiguration.getRules().size());

        Rule actualRule = bucketLifecycleConfiguration.getRules().get(0);
        LifecycleFilterPredicate actualAndPredicate = actualRule.getFilter().getPredicate();
        assertTrue(actualAndPredicate instanceof LifecycleAndOperator);
        List<LifecycleFilterPredicate> actualAndOperands = ((LifecycleAndOperator) actualAndPredicate).getOperands();
        assertEquals(2, actualAndOperands.size());
    }

    @Test
    public void testBucketLifecycle_With_EmptyFilter() throws Exception {
        Rule rule = getRuleWithoutPrefixAndFilter();
        rule.setFilter(new LifecycleFilter());

        BucketLifecycleConfiguration config = new BucketLifecycleConfiguration().withRules(rule);
        s3.setBucketLifecycleConfiguration(BUCKET_NAME, config);

        // Check reading it back
        BucketLifecycleConfiguration bucketLifecycleConfiguration = waitForBucketLifecycleConfiguration(BUCKET_NAME);
        assertNotNull(bucketLifecycleConfiguration);
        assertEquals(1, bucketLifecycleConfiguration.getRules().size());

        Rule actualRule = bucketLifecycleConfiguration.getRules().get(0);
        LifecycleFilterPredicate predicate = actualRule.getFilter().getPredicate();
        assertNull(predicate);
    }

    @Test
    public void testBucketLifecycle_With_EmptyPrefix_InFilter() throws Exception {
        Rule rule = getRuleWithoutPrefixAndFilter();
        rule.setFilter(new LifecycleFilter(new LifecyclePrefixPredicate("")));

        BucketLifecycleConfiguration config = new BucketLifecycleConfiguration().withRules(rule);
        s3.setBucketLifecycleConfiguration(BUCKET_NAME, config);

        // Check reading it back
        BucketLifecycleConfiguration bucketLifecycleConfiguration = waitForBucketLifecycleConfiguration(BUCKET_NAME);
        assertNotNull(bucketLifecycleConfiguration);
        assertEquals(1, bucketLifecycleConfiguration.getRules().size());

        Rule actualRule = bucketLifecycleConfiguration.getRules().get(0);
        LifecyclePrefixPredicate predicate = (LifecyclePrefixPredicate) actualRule.getFilter().getPredicate();
        assertEquals("", predicate.getPrefix());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBucketLifecycle_With_DeprecatedPrefix_And_PrefixInFilter() throws Exception {
        Rule rule = getRuleWithoutPrefixAndFilter();
        rule.setPrefix("prefix1");
        rule.setFilter(new LifecycleFilter().withPredicate(new LifecyclePrefixPredicate("prefix")));

        BucketLifecycleConfiguration config = new BucketLifecycleConfiguration().withRules(rule);
        s3.setBucketLifecycleConfiguration(BUCKET_NAME, config);
    }

    /**
     * The And operator should have atleast 2 values in it.
     */
    @Test(expected = AmazonS3Exception.class)
    public void testBucketLifecycle_With_OnlyOneValueInAndOperator_InFilter() throws Exception {
        List<LifecycleFilterPredicate> andOperands = new ArrayList<LifecycleFilterPredicate>();
        andOperands.add(new LifecycleTagPredicate(new Tag("key1", "value1")));

        Rule rule = getRuleWithoutPrefixAndFilter();
        rule.setFilter(new LifecycleFilter().withPredicate(new LifecycleAndOperator(andOperands)));

        BucketLifecycleConfiguration config = new BucketLifecycleConfiguration().withRules(rule);
        s3.setBucketLifecycleConfiguration(BUCKET_NAME, config);
    }

    /**
     * A filter or the deprecated prefix should be specified
     * in a rule.
     */
    @Test
    public void testBucketLifecycle_With_NoPrefix_And_NoFilter() throws Exception {
        Rule rule = getRuleWithoutPrefixAndFilter();
        BucketLifecycleConfiguration config = new BucketLifecycleConfiguration().withRules(rule);
        s3.setBucketLifecycleConfiguration(BUCKET_NAME, config);

        // Check reading it back
        BucketLifecycleConfiguration bucketLifecycleConfiguration = waitForBucketLifecycleConfiguration(BUCKET_NAME);
        assertNotNull(bucketLifecycleConfiguration);
        assertEquals(1, bucketLifecycleConfiguration.getRules().size());

        Rule actualRule = bucketLifecycleConfiguration.getRules().get(0);
        assertThat(actualRule.getPrefix(), isEmptyString());
        assertNull(actualRule.getFilter());
    }

    /**
     * Can't combine v1 Rule and v2 Rule
     */
    @Test(expected = AmazonS3Exception.class)
    public void testBucketLifecycle_With_MultipleRules_Using_BothOldAndNewLifeCycleConfigFormat() throws Exception {
        Rule rule1 = getRuleWithoutPrefixAndFilter().withPrefix("prefix1");
        Rule rule2 = getRuleWithoutPrefixAndFilter()
                .withFilter(new LifecycleFilter().withPredicate(new LifecyclePrefixPredicate("prefix")));

        BucketLifecycleConfiguration config = new BucketLifecycleConfiguration().withRules(rule1, rule2);
        s3.setBucketLifecycleConfiguration(BUCKET_NAME, config);
    }

    @Test
    public void testBucketLifecycle_With_TwoRules_HavingSamePrefix() throws Exception {
        Rule rule1 = getRuleWithoutPrefixAndFilter()
                .withFilter(new LifecycleFilter().withPredicate(new LifecyclePrefixPredicate("prefix")));
        Rule rule2 = getRuleWithoutPrefixAndFilter()
                .withFilter(new LifecycleFilter().withPredicate(new LifecyclePrefixPredicate("prefix")));

        BucketLifecycleConfiguration config = new BucketLifecycleConfiguration().withRules(rule1, rule2);
        s3.setBucketLifecycleConfiguration(BUCKET_NAME, config);
    }

    private Rule getRuleWithoutPrefixAndFilter() {
        // Apply a config
        String ruleId = UUID.randomUUID().toString();
        Transition transition = new Transition();
        transition.setDays(TRANSITION_TIME_IN_DAYS);
        transition.setStorageClass(StorageClass.Glacier);
        final int daysAfterInitiation = 10;
        return new Rule()
                .withExpirationInDays(EXPIRATION_IN_DAYS)
                .withId(ruleId)
                .withStatus(BucketLifecycleConfiguration.ENABLED)
                .withTransition(transition);
    }

    /**
     * waiting a lifecycle configuration become valid
     * When exceed the poll time, will throw Max poll time exceeded exception
     */
    private BucketLifecycleConfiguration waitForBucketLifecycleConfiguration(
            String bucketName) throws Exception {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000);
        int hits = 0;
        while (System.currentTimeMillis() < endTime) {
            BucketLifecycleConfiguration bucketLifecycleConfiguration = s3.getBucketLifecycleConfiguration(bucketName);
            if (bucketLifecycleConfiguration == null) {
                Thread.sleep(1000);
                hits = 0;
            }
            if (hits++ == 10) {
                return bucketLifecycleConfiguration;
            }
        }
        maxPollTimeExceeded();
        return null;
    }

    /**
     * waiting a lifecycle configuration become deleted
     * When exceed the poll time, will throw Max poll time exceeded exception
     */
    private BucketLifecycleConfiguration waitForBucketLifecycleConfigurationDelete(String bucketName) throws Exception {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000);
        int hits = 0;
        while (System.currentTimeMillis() < endTime) {
            BucketLifecycleConfiguration bucketLifecycleConfiguration = null;

            if ((bucketLifecycleConfiguration = s3.getBucketLifecycleConfiguration(bucketName)) != null) {
                Thread.sleep(1000);
                hits = 0;
            }
            if (hits++ == 10) {
                return bucketLifecycleConfiguration;
            }
        }
        maxPollTimeExceeded();
        return null;

    }

    /**
     * waiting a object with expiring key exist
     * When exceed the poll time, will throw Max poll time exceeded exception
     */
    private ObjectMetadata waitForObjectWithExpirationKeyExist(String bucketName, String key) throws Exception {
        long startTime = System.currentTimeMillis();
        ObjectMetadata metadata = null;
        long endTime = startTime + (10 * 60 * 1000);
        int hits = 0;
        while (System.currentTimeMillis() < endTime) {
            if (!doesObjectExist(bucketName, key)
                || (metadata = s3.getObjectMetadata(bucketName, key)).getExpirationTime() == null) {
                Thread.sleep(1000);
                hits = 0;
            }
            if (hits++ == 10) {
                return metadata;
            }
        }
        maxPollTimeExceeded();
        return null;
    }

    /**
     * waiting a object without expiring key exist
     * When exceed the poll time, will throw Max poll time exceeded exception
     */
    private ObjectMetadata waitForObjectWithNonExpirationKeyExist(String bucketName, String key) throws Exception {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000);
        ObjectMetadata metadata = null;
        int hits = 0;
        while (System.currentTimeMillis() < endTime) {
            if (!doesObjectExist(bucketName, key)
                || (metadata = s3.getObjectMetadata(bucketName, key)).getExpirationTime() != null) {
                Thread.sleep(1000);
                hits = 0;
            }
            if (hits++ == 10) {
                return metadata;
            }
        }
        maxPollTimeExceeded();
        return null;
    }

}
