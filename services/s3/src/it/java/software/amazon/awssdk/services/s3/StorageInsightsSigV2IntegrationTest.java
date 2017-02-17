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
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.DeleteBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.ListBucketInventoryConfigurationsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketMetricsConfigurationsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketMetricsConfigurationsResult;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.SetBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.SetBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.SetBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsConfiguration;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsExportDestination;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsS3BucketDestination;
import software.amazon.awssdk.services.s3.model.analytics.AnalyticsS3ExportFileFormat;
import software.amazon.awssdk.services.s3.model.analytics.StorageClassAnalysis;
import software.amazon.awssdk.services.s3.model.analytics.StorageClassAnalysisDataExport;
import software.amazon.awssdk.services.s3.model.analytics.StorageClassAnalysisSchemaVersion;
import software.amazon.awssdk.services.s3.model.inventory.InventoryConfiguration;
import software.amazon.awssdk.services.s3.model.inventory.InventoryDestination;
import software.amazon.awssdk.services.s3.model.inventory.InventoryFormat;
import software.amazon.awssdk.services.s3.model.inventory.InventoryFrequency;
import software.amazon.awssdk.services.s3.model.inventory.InventoryIncludedObjectVersions;
import software.amazon.awssdk.services.s3.model.inventory.InventoryS3BucketDestination;
import software.amazon.awssdk.services.s3.model.inventory.InventorySchedule;
import software.amazon.awssdk.services.s3.model.metrics.MetricsConfiguration;
import software.amazon.awssdk.test.util.RandomTempFile;

public class StorageInsightsSigV2IntegrationTest {

    /** The bucket created and used by these tests. */
    private static final String BUCKET_NAME = "java-bucket-inventory-integ-test-" + new Date().getTime();

    private static final String BUCKET_ARN = "arn:aws:s3:::" + BUCKET_NAME;

    /** The key used in these tests. */
    private static final String KEY = "key";

    private static AmazonS3 s3SigV2;

    @BeforeClass
    public static void setUpFixture() throws Exception {
        ClientConfiguration clientConfiguration = new ClientConfiguration().withSignerOverride("S3SignerType");
        s3SigV2 = new AmazonS3Client(clientConfiguration);
        s3SigV2.createBucket(BUCKET_NAME);
        s3SigV2.putObject(new PutObjectRequest(BUCKET_NAME, KEY, new RandomTempFile("foo", 1024)));
    }

    @AfterClass
    public static void tearDownFixture() {
        CryptoTestUtils.deleteBucketAndAllContents(s3SigV2, BUCKET_NAME);
    }

    @Test
    public void testInventoryOperationsWorkWithSigV2Signing() throws Exception {
        String id = "id";
        InventoryS3BucketDestination s3BucketDestination = new InventoryS3BucketDestination()
                .withBucketArn(BUCKET_ARN)
                .withFormat(InventoryFormat.CSV);
        InventoryDestination destination = new InventoryDestination().withS3BucketDestination(s3BucketDestination);

        InventoryConfiguration config = new InventoryConfiguration()
                .withEnabled(true)
                .withId(id)
                .withDestination(destination)
                .withIncludedObjectVersions(InventoryIncludedObjectVersions.All)
                .withSchedule(new InventorySchedule().withFrequency(InventoryFrequency.Daily));


        s3SigV2.setBucketInventoryConfiguration(new SetBucketInventoryConfigurationRequest(BUCKET_NAME, config));

        config = s3SigV2.getBucketInventoryConfiguration(new GetBucketInventoryConfigurationRequest(BUCKET_NAME, id))
                        .getInventoryConfiguration();

        assertEquals(id, config.getId());
        assertTrue(config.isEnabled());
        assertEquals(InventoryIncludedObjectVersions.All.toString(), config.getIncludedObjectVersions());
        assertEquals(InventoryFrequency.Daily.toString(), config.getSchedule().getFrequency());
        s3BucketDestination = config.getDestination().getS3BucketDestination();
        assertEquals(BUCKET_ARN, s3BucketDestination.getBucketArn());
        assertEquals(InventoryFormat.CSV.toString(), s3BucketDestination.getFormat());
        assertNull(s3BucketDestination.getAccountId());
        assertNull(s3BucketDestination.getPrefix());


        s3SigV2.deleteBucketInventoryConfiguration(new DeleteBucketInventoryConfigurationRequest(BUCKET_NAME, id));
        List<InventoryConfiguration> configurations = s3SigV2.listBucketInventoryConfigurations(
                new ListBucketInventoryConfigurationsRequest().withBucketName(BUCKET_NAME))
                                                             .getInventoryConfigurationList();
        assertNull(configurations);
    }

    @Test
    public void testAnalyticsOperationsWorkWithSigV2Signing() throws Exception {
        String id = "id";
        AnalyticsConfiguration config = new AnalyticsConfiguration()
                .withId(id)
                .withStorageClassAnalysis(getStorageClassAnalysis());

        s3SigV2.setBucketAnalyticsConfiguration(new SetBucketAnalyticsConfigurationRequest(BUCKET_NAME, config));

        config = s3SigV2.getBucketAnalyticsConfiguration(new GetBucketAnalyticsConfigurationRequest(BUCKET_NAME, id))
                        .getAnalyticsConfiguration();


        assertEquals(id, config.getId());
        assertNull(config.getFilter());
        assertEquals(StorageClassAnalysisSchemaVersion.V_1.toString(), config.getStorageClassAnalysis().getDataExport().getOutputSchemaVersion());

        AnalyticsS3BucketDestination s3BucketDestination = config.getStorageClassAnalysis().getDataExport().getDestination().getS3BucketDestination();
        assertEquals(BUCKET_ARN, s3BucketDestination.getBucketArn());
        assertEquals(AnalyticsS3ExportFileFormat.CSV.toString(), s3BucketDestination.getFormat());
        assertNull(s3BucketDestination.getBucketAccountId());
        assertNull(s3BucketDestination.getPrefix());
    }

    @Test
    public void testMetricsOperationsWorkWithSigV2Signing() throws Exception {
        String id = "id";
        MetricsConfiguration config = new MetricsConfiguration().withId(id);

        s3SigV2.setBucketMetricsConfiguration(new SetBucketMetricsConfigurationRequest(BUCKET_NAME, config));

        config = s3SigV2.getBucketMetricsConfiguration(new GetBucketMetricsConfigurationRequest(BUCKET_NAME, id))
                        .getMetricsConfiguration();

        assertEquals(id, config.getId());

        s3SigV2.deleteBucketMetricsConfiguration(new DeleteBucketMetricsConfigurationRequest(BUCKET_NAME, id));

        ListBucketMetricsConfigurationsResult result = s3SigV2.listBucketMetricsConfigurations(
                new ListBucketMetricsConfigurationsRequest().withBucketName(BUCKET_NAME));
        assertNull(result.getMetricsConfigurationList());
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
