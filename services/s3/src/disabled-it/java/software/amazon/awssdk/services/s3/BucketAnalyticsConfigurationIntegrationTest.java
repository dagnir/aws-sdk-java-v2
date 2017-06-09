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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.AmazonS3Exception;
import software.amazon.awssdk.services.s3.model.DeleteBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.ListBucketAnalyticsConfigurationsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketAnalyticsConfigurationsResult;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.SetBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsAndOperator;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsConfiguration;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsExportDestination;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsFilter;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsFilterPredicate;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsPrefixPredicate;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsS3BucketDestination;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsS3ExportFileFormat;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsTagPredicate;
import software.amazon.awssdk.services.s3.model.analytics.StorageClassAnalysis;
import software.amazon.awssdk.services.s3.model.analytics.StorageClassAnalysisDataExport;
import software.amazon.awssdk.services.s3.model.analytics.StorageClassAnalysisSchemaVersion;
import software.amazon.awssdk.test.util.RandomTempFile;

public class BucketAnalyticsConfigurationIntegrationTest extends S3IntegrationTestBase {

    /** The bucket created and used by these tests. */
    private static final String BUCKET_NAME = "java-bucket-analytics-integ-test-" + new Date().getTime();

    private static final String BUCKET_ARN = "arn:aws:s3:::" + BUCKET_NAME;

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
    public void testAnalyticsConfiguration_works_properly_with_setting_only_required_fields() throws Exception {
        String configId = "id";
        AnalyticsConfiguration config = new AnalyticsConfiguration()
                .withId(configId)
                .withStorageClassAnalysis(getStorageClassAnalysis());

        s3.setBucketAnalyticsConfiguration(new SetBucketAnalyticsConfigurationRequest(BUCKET_NAME, config));

        config = s3.getBucketAnalyticsConfiguration(new GetBucketAnalyticsConfigurationRequest(BUCKET_NAME, configId))
                   .getAnalyticsConfiguration();


        assertEquals(configId, config.getId());
        assertNull(config.getFilter());
        assertEquals(StorageClassAnalysisSchemaVersion.V_1.toString(),
                     config.getStorageClassAnalysis().getDataExport().getOutputSchemaVersion());

        AnalyticsS3BucketDestination s3BucketDestination =
                config.getStorageClassAnalysis().getDataExport().getDestination().getS3BucketDestination();
        assertEquals(BUCKET_ARN, s3BucketDestination.getBucketArn());
        assertEquals(AnalyticsS3ExportFileFormat.CSV.toString(), s3BucketDestination.getFormat());
        assertNull(s3BucketDestination.getBucketAccountId());
        assertNull(s3BucketDestination.getPrefix());
    }

    @Test
    public void testDeleteBucketAnalyticsConfiguration() throws Exception {
        String configId = "id";
        AnalyticsConfiguration config = new AnalyticsConfiguration()
                .withId(configId)
                .withStorageClassAnalysis(getStorageClassAnalysis());

        s3.setBucketAnalyticsConfiguration(new SetBucketAnalyticsConfigurationRequest(BUCKET_NAME, config));

        assertNotNull(s3.getBucketAnalyticsConfiguration(new GetBucketAnalyticsConfigurationRequest(BUCKET_NAME, configId))
                        .getAnalyticsConfiguration());

        s3.deleteBucketAnalyticsConfiguration(new DeleteBucketAnalyticsConfigurationRequest(BUCKET_NAME, configId));

        ListBucketAnalyticsConfigurationsResult result = s3.listBucketAnalyticsConfigurations(
                new ListBucketAnalyticsConfigurationsRequest().withBucketName(BUCKET_NAME));
        assertNull(result.getAnalyticsConfigurationList());
    }

    @Test
    public void testListBucketAnalyticsConfiguration() throws Exception {
        String configId = "id";
        String configId2 = "id2";
        AnalyticsConfiguration config = new AnalyticsConfiguration()
                .withId(configId)
                .withStorageClassAnalysis(getStorageClassAnalysis());

        s3.setBucketAnalyticsConfiguration(new SetBucketAnalyticsConfigurationRequest(BUCKET_NAME, config));
        s3.setBucketAnalyticsConfiguration(new SetBucketAnalyticsConfigurationRequest(BUCKET_NAME, config.withId(configId2)));

        ListBucketAnalyticsConfigurationsResult result = s3.listBucketAnalyticsConfigurations(
                new ListBucketAnalyticsConfigurationsRequest().withBucketName(BUCKET_NAME));
        List<AnalyticsConfiguration> analyticsConfigurationList = result.getAnalyticsConfigurationList();
        assertNull(result.getContinuationToken());
        assertNull(result.getNextContinuationToken());
        assertFalse(result.isTruncated());
        assertEquals(2, analyticsConfigurationList.size());
        assertEquals(configId, analyticsConfigurationList.get(0).getId());
        assertEquals(configId2, analyticsConfigurationList.get(1).getId());

        s3.deleteBucketAnalyticsConfiguration(new DeleteBucketAnalyticsConfigurationRequest(BUCKET_NAME, configId));
        s3.deleteBucketAnalyticsConfiguration(new DeleteBucketAnalyticsConfigurationRequest(BUCKET_NAME, configId2));
    }

    @Test(expected = AmazonS3Exception.class)
    public void testAnalyticsConfiguration_works_properly_with_emptyFilter() throws Exception {
        String configId = "id";
        AnalyticsConfiguration config = new AnalyticsConfiguration()
                .withId(configId)
                .withFilter(new AnalyticsFilter())
                .withStorageClassAnalysis(getStorageClassAnalysis());

        s3.setBucketAnalyticsConfiguration(new SetBucketAnalyticsConfigurationRequest(BUCKET_NAME, config));
    }

    @Test(expected = AmazonS3Exception.class)
    public void testAnalyticsConfiguration_works_properly_with_emptyPrefix() throws Exception {
        String configId = "id";
        AnalyticsConfiguration config = new AnalyticsConfiguration()
                .withId(configId)
                .withFilter(new AnalyticsFilter().withPredicate(new AnalyticsPrefixPredicate("")))
                .withStorageClassAnalysis(getStorageClassAnalysis());

        s3.setBucketAnalyticsConfiguration(new SetBucketAnalyticsConfigurationRequest(BUCKET_NAME, config));
    }

    @Test
    public void testAnalyticsConfiguration_works_properly_with_onlyTag() throws Exception {
        String configId = "id";
        AnalyticsConfiguration config = new AnalyticsConfiguration()
                .withId(configId)
                .withFilter(new AnalyticsFilter().withPredicate(new AnalyticsTagPredicate(new Tag("key", "value"))))
                .withStorageClassAnalysis(getStorageClassAnalysis());

        s3.setBucketAnalyticsConfiguration(new SetBucketAnalyticsConfigurationRequest(BUCKET_NAME, config));

        config = s3.getBucketAnalyticsConfiguration(new GetBucketAnalyticsConfigurationRequest(BUCKET_NAME, configId))
                   .getAnalyticsConfiguration();

        assertEquals(configId, config.getId());
        assertEquals("key", ((AnalyticsTagPredicate) config.getFilter().getPredicate()).getTag().getKey());
        assertEquals("value", ((AnalyticsTagPredicate) config.getFilter().getPredicate()).getTag().getValue());
        assertEquals(StorageClassAnalysisSchemaVersion.V_1.toString(),
                     config.getStorageClassAnalysis().getDataExport().getOutputSchemaVersion());

        AnalyticsS3BucketDestination s3BucketDestination =
                config.getStorageClassAnalysis().getDataExport().getDestination().getS3BucketDestination();
        assertEquals(BUCKET_ARN, s3BucketDestination.getBucketArn());
        assertEquals(AnalyticsS3ExportFileFormat.CSV.toString(), s3BucketDestination.getFormat());
        assertNull(s3BucketDestination.getBucketAccountId());
        assertNull(s3BucketDestination.getPrefix());
    }

    @Test
    public void testAnalyticsConfiguration_works_properly_with_validAndOperator() throws Exception {
        String configId = "id";
        String prefix = "prefix";
        String key = "k";
        String value = "v";
        AnalyticsPrefixPredicate analyticsPrefixPredicate = new AnalyticsPrefixPredicate(prefix);
        AnalyticsTagPredicate analyticsTagPredicate = new AnalyticsTagPredicate(new Tag(key, value));
        List<AnalyticsFilterPredicate> operators = new ArrayList<AnalyticsFilterPredicate>();
        operators.add(analyticsPrefixPredicate);
        operators.add(analyticsTagPredicate);

        AnalyticsConfiguration config = new AnalyticsConfiguration()
                .withId(configId)
                .withFilter(new AnalyticsFilter().withPredicate(new AnalyticsAndOperator(operators)))
                .withStorageClassAnalysis(getStorageClassAnalysis());

        s3.setBucketAnalyticsConfiguration(new SetBucketAnalyticsConfigurationRequest(BUCKET_NAME, config));

        config = s3.getBucketAnalyticsConfiguration(new GetBucketAnalyticsConfigurationRequest(BUCKET_NAME, configId))
                   .getAnalyticsConfiguration();

        assertEquals(configId, config.getId());
        operators = ((AnalyticsAndOperator) config.getFilter().getPredicate()).getOperands();
        assertEquals(prefix, ((AnalyticsPrefixPredicate) operators.get(0)).getPrefix());
        assertEquals(key, ((AnalyticsTagPredicate) operators.get(1)).getTag().getKey());
        assertEquals(value, ((AnalyticsTagPredicate) operators.get(1)).getTag().getValue());
        assertEquals(StorageClassAnalysisSchemaVersion.V_1.toString(),
                     config.getStorageClassAnalysis().getDataExport().getOutputSchemaVersion());

        AnalyticsS3BucketDestination s3BucketDestination =
                config.getStorageClassAnalysis().getDataExport().getDestination().getS3BucketDestination();
        assertEquals(BUCKET_ARN, s3BucketDestination.getBucketArn());
        assertEquals(AnalyticsS3ExportFileFormat.CSV.toString(), s3BucketDestination.getFormat());
        assertNull(s3BucketDestination.getBucketAccountId());
        assertNull(s3BucketDestination.getPrefix());
    }

    @Test(expected = AmazonS3Exception.class)
    public void setBucketAnalyticsConfiguration_fails_when_requiredfield_is_missing() throws Exception {
        String configId = "id";
        StorageClassAnalysisDataExport dataExport = new StorageClassAnalysisDataExport()
                .withOutputSchemaVersion(StorageClassAnalysisSchemaVersion.V_1)
                .withDestination(new AnalyticsExportDestination());

        AnalyticsConfiguration config = new AnalyticsConfiguration()
                .withId(configId)
                .withStorageClassAnalysis(new StorageClassAnalysis().withDataExport(dataExport));

        s3.setBucketAnalyticsConfiguration(new SetBucketAnalyticsConfigurationRequest(BUCKET_NAME, config));
    }

    private StorageClassAnalysis getStorageClassAnalysis() {
        AnalyticsS3BucketDestination s3BucketDestination = new AnalyticsS3BucketDestination()
                .withBucketArn(BUCKET_ARN)
                .withFormat(AnalyticsS3ExportFileFormat.CSV);

        return new StorageClassAnalysis().withDataExport(
                new StorageClassAnalysisDataExport()
                        .withOutputSchemaVersion(StorageClassAnalysisSchemaVersion.V_1)
                        .withDestination(new AnalyticsExportDestination().withS3BucketDestination(s3BucketDestination)));
    }
}
