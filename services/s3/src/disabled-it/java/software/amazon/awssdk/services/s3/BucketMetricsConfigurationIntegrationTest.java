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
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.DeleteBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.ListBucketMetricsConfigurationsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketMetricsConfigurationsResult;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.SetBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.metrics.MetricsAndOperator;
import software.amazon.awssdk.services.s3.model.metrics.MetricsConfiguration;
import software.amazon.awssdk.services.s3.model.metrics.MetricsFilter;
import software.amazon.awssdk.services.s3.model.metrics.MetricsFilterPredicate;
import software.amazon.awssdk.services.s3.model.metrics.MetricsPrefixPredicate;
import software.amazon.awssdk.services.s3.model.metrics.MetricsTagPredicate;
import software.amazon.awssdk.test.util.RandomTempFile;

public class BucketMetricsConfigurationIntegrationTest extends S3IntegrationTestBase {

    /** The bucket created and used by these tests. */
    private static final String BUCKET_NAME = "java-bucket-metrics-integ-test-" + new Date().getTime();

    /** The key used in these tests. */
    private static final String KEY = "key";

    @BeforeClass
    public static void setUpFixture() throws Exception {
        S3IntegrationTestBase.setUp();
        s3.createBucket(BUCKET_NAME);
        s3.putObject(new PutObjectRequest(BUCKET_NAME, KEY, new RandomTempFile("foo", 1024)));
    }

    @AfterClass
    public static void tearDownFixture() {
        CryptoTestUtils.deleteBucketAndAllContents(s3, BUCKET_NAME);
    }

    @Test
    public void test_bucketMetricsConfiguration_operations_works_properly() throws Exception {
        String configId = "id";
        MetricsConfiguration config = new MetricsConfiguration().withId(configId);

        s3.setBucketMetricsConfiguration(new SetBucketMetricsConfigurationRequest(BUCKET_NAME, config));

        config = s3.getBucketMetricsConfiguration(new GetBucketMetricsConfigurationRequest(BUCKET_NAME, configId))
                   .getMetricsConfiguration();

        assertEquals(configId, config.getId());

        s3.deleteBucketMetricsConfiguration(new DeleteBucketMetricsConfigurationRequest(BUCKET_NAME, configId));

        ListBucketMetricsConfigurationsResult result = s3.listBucketMetricsConfigurations(
                new ListBucketMetricsConfigurationsRequest().withBucketName(BUCKET_NAME));
        assertNull(result.getMetricsConfigurationList());
    }

    @Test
    public void testListBucketMetricsConfigurations_works_properly() throws Exception {
        String configId1 = "id1";
        String configId2 = "id2";
        String prefix = "prefix";
        MetricsPrefixPredicate predicate = new MetricsPrefixPredicate(prefix);

        MetricsConfiguration config1 = new MetricsConfiguration().withId(configId1);
        MetricsConfiguration config2 = new MetricsConfiguration().withId(configId2)
                                                                 .withFilter(new MetricsFilter().withPredicate(predicate));

        s3.setBucketMetricsConfiguration(new SetBucketMetricsConfigurationRequest(BUCKET_NAME, config1));
        s3.setBucketMetricsConfiguration(new SetBucketMetricsConfigurationRequest(BUCKET_NAME, config2));

        ListBucketMetricsConfigurationsResult result = s3.listBucketMetricsConfigurations(
                new ListBucketMetricsConfigurationsRequest().withBucketName(BUCKET_NAME));
        List<MetricsConfiguration> metricsConfigurations = result.getMetricsConfigurationList();

        // First metrics configuration
        assertEquals(2, metricsConfigurations.size());
        assertEquals(configId1, metricsConfigurations.get(0).getId());
        assertNull(metricsConfigurations.get(0).getFilter());

        // First metrics configuration
        assertEquals(configId2, metricsConfigurations.get(1).getId());
        assertEquals(prefix, ((MetricsPrefixPredicate) metricsConfigurations.get(1).getFilter().getPredicate()).getPrefix());

        s3.deleteBucketMetricsConfiguration(new DeleteBucketMetricsConfigurationRequest(BUCKET_NAME, configId1));
        s3.deleteBucketMetricsConfiguration(new DeleteBucketMetricsConfigurationRequest(BUCKET_NAME, configId2));
    }

    @Test
    public void testOnlyPrefix() throws Exception {
        String configId = "id";
        String prefix = "prefix";
        MetricsFilter filter = new MetricsFilter(new MetricsPrefixPredicate(prefix));
        MetricsConfiguration config = new MetricsConfiguration().withId(configId).withFilter(filter);

        s3.setBucketMetricsConfiguration(new SetBucketMetricsConfigurationRequest(BUCKET_NAME, config));

        config = s3.getBucketMetricsConfiguration(new GetBucketMetricsConfigurationRequest(BUCKET_NAME, configId))
                   .getMetricsConfiguration();

        assertEquals(configId, config.getId());
        assertEquals(prefix, ((MetricsPrefixPredicate) config.getFilter().getPredicate()).getPrefix());

        s3.deleteBucketMetricsConfiguration(new DeleteBucketMetricsConfigurationRequest(BUCKET_NAME, configId));
    }

    @Test
    public void testOnlyTag() throws Exception {
        String configId = "id";
        String key = "key";
        String value = "value";

        MetricsTagPredicate predicate = new MetricsTagPredicate(new Tag(key, value));
        MetricsConfiguration config = new MetricsConfiguration().withId(configId)
                                                                .withFilter(new MetricsFilter(predicate));

        s3.setBucketMetricsConfiguration(new SetBucketMetricsConfigurationRequest(BUCKET_NAME, config));

        config = s3.getBucketMetricsConfiguration(new GetBucketMetricsConfigurationRequest(BUCKET_NAME, configId))
                   .getMetricsConfiguration();

        assertEquals(configId, config.getId());
        assertEquals(key, ((MetricsTagPredicate) config.getFilter().getPredicate()).getTag().getKey());
        assertEquals(value, ((MetricsTagPredicate) config.getFilter().getPredicate()).getTag().getValue());

        s3.deleteBucketMetricsConfiguration(new DeleteBucketMetricsConfigurationRequest(BUCKET_NAME, configId));
    }

    @Test
    public void testOnlyAndOperator() throws Exception {
        String configId = "id";
        String prefix = "prefix";
        String key = "key";
        String value = "value";
        List<MetricsFilterPredicate> operands = new ArrayList<MetricsFilterPredicate>();
        operands.add(new MetricsPrefixPredicate(prefix));
        operands.add(new MetricsTagPredicate(new Tag(key, value)));

        MetricsConfiguration config = new MetricsConfiguration().withId(configId)
                                                                .withFilter(new MetricsFilter(new MetricsAndOperator(operands)));

        s3.setBucketMetricsConfiguration(new SetBucketMetricsConfigurationRequest(BUCKET_NAME, config));

        config = s3.getBucketMetricsConfiguration(new GetBucketMetricsConfigurationRequest(BUCKET_NAME, configId))
                   .getMetricsConfiguration();

        assertEquals(configId, config.getId());
        operands = ((MetricsAndOperator) config.getFilter().getPredicate()).getOperands();
        assertEquals(prefix, ((MetricsPrefixPredicate) operands.get(0)).getPrefix());
        assertEquals(key, ((MetricsTagPredicate) operands.get(1)).getTag().getKey());
        assertEquals(value, ((MetricsTagPredicate) operands.get(1)).getTag().getValue());

        s3.deleteBucketMetricsConfiguration(new DeleteBucketMetricsConfigurationRequest(BUCKET_NAME, configId));
    }

    @Test(expected = AmazonS3Exception.class)
    public void testEmptyFilter() throws Exception {
        String configId = "id";
        MetricsConfiguration config = new MetricsConfiguration().withId(configId)
                                                                .withFilter(new MetricsFilter());

        s3.setBucketMetricsConfiguration(new SetBucketMetricsConfigurationRequest(BUCKET_NAME, config));
    }

    @Test(expected = AmazonS3Exception.class)
    public void testEmptyPrefix() throws Exception {
        String configId = "id";
        String prefix = "";
        MetricsPrefixPredicate predicate = new MetricsPrefixPredicate(prefix);
        MetricsConfiguration config = new MetricsConfiguration().withId(configId)
                                                                .withFilter(new MetricsFilter(predicate));

        s3.setBucketMetricsConfiguration(new SetBucketMetricsConfigurationRequest(BUCKET_NAME, config));
    }
}
