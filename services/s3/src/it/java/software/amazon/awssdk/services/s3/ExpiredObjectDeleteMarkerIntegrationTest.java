package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration.Rule;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.SetBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.lifecycle.LifecycleFilter;
import software.amazon.awssdk.services.s3.model.lifecycle.LifecyclePrefixPredicate;

public class ExpiredObjectDeleteMarkerIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET_NAME = "java-sdk-event-notif-test-" + System.currentTimeMillis();

    @BeforeClass
    public static void setupFixture() {
        s3.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));

        s3.createBucket(new CreateBucketRequest(BUCKET_NAME));
    }

    @AfterClass
    public static void tearDownFixture() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET_NAME);
    }

    @Test
    public void setBucketLifecycle_WithExpiredObjectDeleteMarkersTrue_ExpiredObjectDeleteMarkerIsTrueOnGet() {
        setBucketLifecycleConfiguration(new BucketLifecycleConfiguration()
                .withRules(newRuleWithExpiredObjectDeleteMarkerStatus(true)));
        BucketLifecycleConfiguration config = getBucketLifecycleConfiguration();
        assertTrue(getFirstRule(config).isExpiredObjectDeleteMarker());
    }

    @Test
    public void setBucketLifecycle_WithExpiredObjectDeleteMarkersFalse_ExpiredObjectDeleteMarkerIsFalseOnGet() {
        setBucketLifecycleConfiguration(new BucketLifecycleConfiguration()
                .withRules(newRuleWithExpiredObjectDeleteMarkerStatus(false)
                        .withNoncurrentVersionExpirationInDays(3)));
        BucketLifecycleConfiguration config = getBucketLifecycleConfiguration();
        assertFalse(getFirstRule(config).isExpiredObjectDeleteMarker());
        assertEquals(3, getFirstRule(config).getNoncurrentVersionExpirationInDays());
    }

    // CurrentVersionExpiration and ExpiredObjectDeleteMarker are mutually exclusive
    @Test(expected = AmazonS3Exception.class)
    public void setBucketLifecycle_WithExpiredObjectDeleteMarkersTrue_CurrentVersionExpirationSet() {
        setBucketLifecycleConfiguration(new BucketLifecycleConfiguration()
                .withRules(newRuleWithExpiredObjectDeleteMarkerStatus(true)
                        .withExpirationInDays(3)));
    }

    @Test
    public void setBucketLifecycle_WithExpiredObjectDeleteMarkersNotSet_ExpiredObjectDeleteMarkerIsFalseOnGet() {
        setBucketLifecycleConfiguration(new BucketLifecycleConfiguration().withRules(new Rule().withId("id")
                .withFilter(new LifecycleFilter(new LifecyclePrefixPredicate(UUID.randomUUID().toString())))
                .withStatus(BucketLifecycleConfiguration.ENABLED).withNoncurrentVersionExpirationInDays(3)));
        BucketLifecycleConfiguration config = getBucketLifecycleConfiguration();
        assertFalse(getFirstRule(config).isExpiredObjectDeleteMarker());
    }

    /**
     * Tests one rule with ExpiredObject Delete Marker true and another with it false
     */
    @Test
    public void setBucketLifecycle_WithMultipleRulesBothWithExpiredObjectDeleteMarkerSet() {
        String ruleOneId = "rule1";
        String ruleTwoId = "rule2";
        setBucketLifecycleConfiguration(new BucketLifecycleConfiguration().withRules(
                newRuleWithExpiredObjectDeleteMarkerStatus(ruleOneId, true), newRuleWithExpiredObjectDeleteMarkerStatus(ruleTwoId, false)
                        .withNoncurrentVersionExpirationInDays(1)));
        BucketLifecycleConfiguration config = getBucketLifecycleConfiguration();
        assertTrue(getRuleById(config, ruleOneId).isExpiredObjectDeleteMarker());
        assertFalse(getRuleById(config, ruleTwoId).isExpiredObjectDeleteMarker());
    }

    @Test
    public void setBucketLifecycle_WithMultipleRulesBothWithExpiredObjectDeleteMarkerSetToTrue() {
        String ruleOneId = "rule1";
        String ruleTwoId = "rule2";
        setBucketLifecycleConfiguration(new BucketLifecycleConfiguration().withRules(
                newRuleWithExpiredObjectDeleteMarkerStatus(ruleOneId, true), newRuleWithExpiredObjectDeleteMarkerStatus(ruleTwoId, true)));
        BucketLifecycleConfiguration config = getBucketLifecycleConfiguration();
        assertTrue(getRuleById(config, ruleOneId).isExpiredObjectDeleteMarker());
        assertTrue(getRuleById(config, ruleTwoId).isExpiredObjectDeleteMarker());
    }

    /**
     * 400 Invalid Request will return from the response. Error message is expected to be
     * "Found overlapping prefixes 'foo/bar' and 'foo/bar/buu' for same action type 'Expiration'".
     */
    @Test(expected = AmazonS3Exception.class)
    public void setBucketLifecycle_WithMultipleRulesWithOverlappingPrefixes() {
        String prefix1 = "foo/bar";
        String prefix2 = "foo/bar/buu";
        setBucketLifecycleConfiguration(new BucketLifecycleConfiguration().withRules(
                newRuleWithPrefixAndExpiredObjectDeleteMarker(prefix1, true),
                newRuleWithPrefixAndExpiredObjectDeleteMarker(prefix2, false)));
    }

    /**
     * 400 Invalid Request will return from the response. Error message is expected to be
     * "Found overlapping prefixes 'foo/bar' and 'foo/bar/buu' for same action type 'Expiration'".
     */
    @Test(expected = AmazonS3Exception.class)
    public void setBucketLifecycle_WithMultipleRulesWithOverlappingPrefixes_DifferentExpiration() {
        String prefix1 = "foo/bar";
        String prefix2 = "foo/bar/buu";
        setBucketLifecycleConfiguration(new BucketLifecycleConfiguration().withRules(
                newRuleWithPrefix(prefix1).withExpiredObjectDeleteMarker(true),
                newRuleWithPrefix(prefix2).withExpirationInDays(3)));
    }

    private void setBucketLifecycleConfiguration(BucketLifecycleConfiguration lifecycleConfig) {
        s3.setBucketLifecycleConfiguration(new SetBucketLifecycleConfigurationRequest(BUCKET_NAME, lifecycleConfig));
    }

    private BucketLifecycleConfiguration getBucketLifecycleConfiguration() {
        return s3.getBucketLifecycleConfiguration(BUCKET_NAME);
    }

    /**
     * Return the first rule in a {@link BucketLifecycleConfiguration}'s list of rules
     */
    private Rule getFirstRule(BucketLifecycleConfiguration config) {
        return config.getRules().get(0);
    }

    /**
     * Convenience method to create a rule with a unique id and prefix
     *
     * @param status
     *            Value of expired object delete marker attribute
     */
    private Rule newRuleWithExpiredObjectDeleteMarkerStatus(boolean status) {
        return new Rule().withId(UUID.randomUUID().toString()).withPrefix(UUID.randomUUID().toString())
                .withExpiredObjectDeleteMarker(status).withStatus(BucketLifecycleConfiguration.ENABLED);
    }

    /**
     * Convenience method to create a rule with a given id and unique prefix
     *
     * @param ruleId
     *            Id of the rule that will be created
     * @param status
     *            Value of expired object delete marker attribute
     */
    private Rule newRuleWithExpiredObjectDeleteMarkerStatus(String ruleId, boolean status) {
        return new Rule().withId(ruleId).withPrefix(UUID.randomUUID().toString()).withExpiredObjectDeleteMarker(status)
                .withStatus(BucketLifecycleConfiguration.ENABLED);
    }

    /**
     * Convenience method to create a rule with a given prefix and expired object delete marker.
     *
     * @param prefix
     *            The key prefix the rule is applying to
     *
     * @param expiredObjectDeleteMarker
     *            Value of expired object delete marker.
     */
    private Rule newRuleWithPrefixAndExpiredObjectDeleteMarker(String prefix, boolean expiredObjectDeleteMarker) {
        return new Rule().withId(UUID.randomUUID().toString()).withPrefix(prefix)
                .withExpiredObjectDeleteMarker(expiredObjectDeleteMarker)
                .withStatus(BucketLifecycleConfiguration.ENABLED);
    }

    /**
     * Convenience method to create a rule with a given prefix.
     *
     * @param prefix
     *            The key prefix the rule is applying to
     */
    private Rule newRuleWithPrefix(String prefix) {
        return new Rule().withId(UUID.randomUUID().toString()).withPrefix(prefix)
                .withStatus(BucketLifecycleConfiguration.ENABLED);
    }

    /**
     * Convenience method to find a rule by it's unique id in a {@link BucketLifecycleConfiguration}
     * 's list of {@link Rule}s
     */
    private Rule getRuleById(BucketLifecycleConfiguration config, String ruleId) {
        for (Rule rule : config.getRules()) {
            if (rule.getId().equals(ruleId)) {
                return rule;
            }
        }
        return null;
    }
}
