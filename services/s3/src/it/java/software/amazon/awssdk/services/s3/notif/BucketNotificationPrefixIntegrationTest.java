package software.amazon.awssdk.services.s3.notif;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.BucketNotificationConfiguration;
import software.amazon.awssdk.services.s3.model.Filter;
import software.amazon.awssdk.services.s3.model.FilterRule;
import software.amazon.awssdk.services.s3.model.NotificationConfiguration;
import software.amazon.awssdk.services.s3.model.S3Event;
import software.amazon.awssdk.services.s3.model.S3KeyFilter;
import software.amazon.awssdk.services.s3.model.S3KeyFilter.FilterRuleName;
import software.amazon.awssdk.services.s3.model.TopicConfiguration;
import software.amazon.awssdk.services.sns.AmazonSNSClient;

public class BucketNotificationPrefixIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET_NAME = "bucket-notification-prefix-integ-" + System.currentTimeMillis();
    private static final String TOPIC_NAME = "bucket-notification-prefix-integ-topic";
    private static final S3Event S3_EVENT = S3Event.ObjectCreated;
    private static final String NOTIFICATION_CONFIG_NAME = "notification-config-one";
    private static final String OTHER_NOTIFICATION_CONFIG_NAME = "notification-config-two";
    private static final String PREFIX_VALUE = "some-prefix";
    private static final String SUFFIX_VALUE = "/some-suffix";
    private static final String OTHER_SUFFIX_VALUE = "/some-other-suffix";

    private static String topicArn;
    private static AmazonSNSClient sns;

    @BeforeClass
    public static void setupFixture() {
        sns = new AmazonSNSClient(credentials);
        s3.createBucket(BUCKET_NAME);
        topicArn = sns.createTopic(TOPIC_NAME).getTopicArn();
        BucketNotificationTestUtils.authorizeS3ToSendToSNS(sns, topicArn, BUCKET_NAME);
    }

    @AfterClass
    public static void tearDownFixture() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET_NAME);
        sns.deleteTopic(topicArn);
    }

    @Test(expected = AmazonClientException.class)
    public void putBucketConfiguration_WithFilterButNoFilterCriteria_ThrowsAmazonClientException() {
        NotificationConfiguration notifConfig = createNotificationConfiguration(null);
        s3.setBucketNotificationConfiguration(BUCKET_NAME,
                new BucketNotificationConfiguration().addConfiguration("invalid-config", notifConfig));
    }

    @Test(expected = AmazonClientException.class)
    public void putBucketConfiguration_WithFilterCriteriaButNullFilterRules_ThrowsAmazonClientException() {
        NotificationConfiguration notifConfig = createNotificationConfiguration(new S3KeyFilter());
        s3.setBucketNotificationConfiguration(BUCKET_NAME,
                new BucketNotificationConfiguration().addConfiguration("invalid-config", notifConfig));
    }

    @Test(expected = AmazonClientException.class)
    public void putBucketConfiguration_WithFilterCriteriaButEmptyFilterRules_ThrowsAmazonClientException() {
        NotificationConfiguration notifConfig = createNotificationConfiguration(new S3KeyFilter()
                .withFilterRules(new ArrayList<FilterRule>()));
        s3.setBucketNotificationConfiguration(BUCKET_NAME,
                new BucketNotificationConfiguration().addConfiguration("invalid-config", notifConfig));
    }

    @Test
    public void putBucketConfiguration_WithPrefix_ReturnsSameConfigOnGet() throws Exception {
        setBucketConfigurationWithRules(newPrefixRule(PREFIX_VALUE));

        BucketNotificationConfiguration config = s3.getBucketNotificationConfiguration(BUCKET_NAME);
        FilterRule filterRule = getFirstS3KeyFilterRule(config.getConfigurationByName(NOTIFICATION_CONFIG_NAME));
        assertIsPrefixRuleWithValue(filterRule, PREFIX_VALUE);
    }

    @Test
    public void putBucketConfiguration_WithSuffix_ReturnsSameConfigOnGet() throws Exception {
        setBucketConfigurationWithRules(newSuffixRule(SUFFIX_VALUE));

        BucketNotificationConfiguration config = s3.getBucketNotificationConfiguration(BUCKET_NAME);
        FilterRule filterRule = getFirstS3KeyFilterRule(config.getConfigurationByName(NOTIFICATION_CONFIG_NAME));
        assertIsSuffixRuleWithValue(filterRule, SUFFIX_VALUE);
    }

    @Test
    public void putBucketConfiguration_WithPrefixAndSuffix_ReturnsSameConfigOnGet() throws Exception {
        setBucketConfigurationWithRules(newPrefixRule(PREFIX_VALUE), newSuffixRule(SUFFIX_VALUE));

        BucketNotificationConfiguration config = s3.getBucketNotificationConfiguration(BUCKET_NAME);
        NotificationConfiguration notificationConfig = config.getConfigurationByName(NOTIFICATION_CONFIG_NAME);

        FilterRule filterRulePrefix = getFirstS3KeyFilterRule(notificationConfig);
        assertIsPrefixRuleWithValue(filterRulePrefix, PREFIX_VALUE);

        FilterRule filterRuleSuffix = getS3KeyFilterRuleByIndex(notificationConfig, 1);
        assertIsSuffixRuleWithValue(filterRuleSuffix, SUFFIX_VALUE);
    }

    @Test
    public void putBucketConfiguration_MultipleConfigurationsWithFilterCriteria_ReturnsSameCriteriaOnGet()
            throws Exception {
        s3.setBucketNotificationConfiguration(BUCKET_NAME, new BucketNotificationConfiguration()
                .withNotificationConfiguration(getMultipleNotificationConfiguration()));

        BucketNotificationConfiguration config = s3.getBucketNotificationConfiguration(BUCKET_NAME);

        final NotificationConfiguration firstConfig = config.getConfigurationByName(NOTIFICATION_CONFIG_NAME);
        // Assert first rule of first configuration is the correct prefix rule
        assertIsPrefixRuleWithValue(getFirstS3KeyFilterRule(firstConfig), PREFIX_VALUE);

        // Assert second rule of first configuration is the correct suffix rule
        assertIsSuffixRuleWithValue(getS3KeyFilterRuleByIndex(firstConfig, 1), OTHER_SUFFIX_VALUE);

        // Assert first rule of second configuration is the correct suffix rule
        final NotificationConfiguration secondConfig = config.getConfigurationByName(OTHER_NOTIFICATION_CONFIG_NAME);
        assertIsSuffixRuleWithValue(getFirstS3KeyFilterRule(secondConfig), SUFFIX_VALUE);
    }

    private void assertIsPrefixRuleWithValue(FilterRule filterRule, String value) {
        assertEquals(FilterRuleName.Prefix.toString(), filterRule.getName());
        assertEquals(value, filterRule.getValue());
    }

    private void assertIsSuffixRuleWithValue(FilterRule filterRule, String value) {
        assertEquals(FilterRuleName.Suffix.toString(), filterRule.getName());
        assertEquals(value, filterRule.getValue());
    }

    private FilterRule newPrefixRule(String prefixValue) {
        return FilterRuleName.Prefix.newRule(prefixValue);
    }

    private FilterRule newSuffixRule(String suffixValue) {
        return FilterRuleName.Suffix.newRule(suffixValue);
    }

    private NotificationConfiguration createNotificationConfiguration(S3KeyFilter filterCriteria) {
        TopicConfiguration topicConfiguration = new TopicConfiguration(topicArn, EnumSet.of(S3_EVENT));
        return topicConfiguration.withFilter(new Filter().withS3KeyFilter(filterCriteria));
    }

    private void setBucketConfigurationWithRules(FilterRule... filterRules) {
        S3KeyFilter s3KeyFilter = null;
        if (filterRules != null) {
            s3KeyFilter = new S3KeyFilter().withFilterRules(filterRules);
        }
        NotificationConfiguration notificationConfiguration = createNotificationConfiguration(s3KeyFilter);
        s3.setBucketNotificationConfiguration(BUCKET_NAME, new BucketNotificationConfiguration(
                NOTIFICATION_CONFIG_NAME, notificationConfiguration));
    }

    /**
     * Create multiple {@link NotificationConfiguration}'s, each with a set of {@link Filter}
     * criteria
     * 
     * @param suffix
     *            Value to use for {@link FilterRuleName#Suffix} rules
     * @param prefix
     *            Value to use for {@link FilterRuleName#Prefix} rules
     * @return Map of {@link NotificationConfiguration}'s created
     */
    private Map<String, NotificationConfiguration> getMultipleNotificationConfiguration() {
        NotificationConfiguration notif1 = createNotificationConfiguration(new S3KeyFilter().withFilterRules(
                newPrefixRule(PREFIX_VALUE), newSuffixRule(OTHER_SUFFIX_VALUE)));
        NotificationConfiguration notif2 = createNotificationConfiguration(new S3KeyFilter()
                .withFilterRules(newSuffixRule(SUFFIX_VALUE)));

        Map<String, NotificationConfiguration> configs = new HashMap<String, NotificationConfiguration>();
        configs.put(NOTIFICATION_CONFIG_NAME, notif1);
        configs.put(OTHER_NOTIFICATION_CONFIG_NAME, notif2);
        return configs;
    }

    /**
     * Convenience method to return the first {@link FilterRule} for a
     * {@link NotificationConfiguration}'s {@link S3KeyFilter}
     */
    private FilterRule getFirstS3KeyFilterRule(NotificationConfiguration notificationConfig) {
        return getS3KeyFilterRuleByIndex(notificationConfig, 0);
    }

    private FilterRule getS3KeyFilterRuleByIndex(NotificationConfiguration notificationConfig, int index) {
        return notificationConfig.getFilter().getS3KeyFilter().getFilterRules().get(index);
    }

}
