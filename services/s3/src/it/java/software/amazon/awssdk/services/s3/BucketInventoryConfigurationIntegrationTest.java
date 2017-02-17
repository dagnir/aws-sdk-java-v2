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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.internal.crypto.CryptoTestUtils;
import software.amazon.awssdk.services.s3.model.DeleteBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.ListBucketInventoryConfigurationsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.SetBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.inventory.InventoryConfiguration;
import software.amazon.awssdk.services.s3.model.inventory.InventoryDestination;
import software.amazon.awssdk.services.s3.model.inventory.InventoryFilter;
import software.amazon.awssdk.services.s3.model.inventory.InventoryFormat;
import software.amazon.awssdk.services.s3.model.inventory.InventoryFrequency;
import software.amazon.awssdk.services.s3.model.inventory.InventoryIncludedObjectVersions;
import software.amazon.awssdk.services.s3.model.inventory.InventoryOptionalField;
import software.amazon.awssdk.services.s3.model.inventory.InventoryPrefixPredicate;
import software.amazon.awssdk.services.s3.model.inventory.InventoryS3BucketDestination;
import software.amazon.awssdk.services.s3.model.inventory.InventorySchedule;
import software.amazon.awssdk.test.util.RandomTempFile;

public class BucketInventoryConfigurationIntegrationTest extends S3IntegrationTestBase {

    /** The bucket created and used by these tests. */
    private static final String BUCKET_NAME = "java-bucket-inventory-integ-test-" + new Date().getTime();

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
    public void testInventoryConfiguration_works_properly_with_setting_only_required_fields() throws Exception {
        String configId = "id";
        InventoryS3BucketDestination s3BucketDestination = new InventoryS3BucketDestination()
                .withBucketArn(BUCKET_ARN)
                .withFormat(InventoryFormat.CSV);
        InventoryDestination destination = new InventoryDestination().withS3BucketDestination(s3BucketDestination);

        InventoryConfiguration config = new InventoryConfiguration()
                .withEnabled(true)
                .withId(configId)
                .withDestination(destination)
                .withIncludedObjectVersions(InventoryIncludedObjectVersions.All)
                .withSchedule(new InventorySchedule().withFrequency(InventoryFrequency.Daily));


        s3.setBucketInventoryConfiguration(new SetBucketInventoryConfigurationRequest(BUCKET_NAME, config));

        config = s3.getBucketInventoryConfiguration(new GetBucketInventoryConfigurationRequest(BUCKET_NAME, configId))
                   .getInventoryConfiguration();

        assertEquals(configId, config.getId());
        assertTrue(config.isEnabled());
        assertEquals(InventoryIncludedObjectVersions.All.toString(), config.getIncludedObjectVersions());
        assertEquals(InventoryFrequency.Daily.toString(), config.getSchedule().getFrequency());
        s3BucketDestination = config.getDestination().getS3BucketDestination();
        assertEquals(BUCKET_ARN, s3BucketDestination.getBucketArn());
        assertEquals(InventoryFormat.CSV.toString(), s3BucketDestination.getFormat());
        assertNull(s3BucketDestination.getAccountId());
        assertNull(s3BucketDestination.getPrefix());


        s3.deleteBucketInventoryConfiguration(new DeleteBucketInventoryConfigurationRequest(BUCKET_NAME, configId));
        List<InventoryConfiguration> configurations = s3.listBucketInventoryConfigurations(
                new ListBucketInventoryConfigurationsRequest().withBucketName(BUCKET_NAME))
                                                        .getInventoryConfigurationList();
        assertNull(configurations);
    }

    @Test
    public void testInventoryConfiguration_with_filter() throws Exception {
        String configId = "id";
        String prefix = "prefix";
        String accountId = "test-account";
        List<String> optionalFields = new ArrayList<String>() {
            {
                add(InventoryOptionalField.ETag.toString());
                add(InventoryOptionalField.Size.toString());
            }
        };

        InventoryS3BucketDestination s3BucketDestination = new InventoryS3BucketDestination()
                .withBucketArn(BUCKET_ARN)
                .withFormat(InventoryFormat.CSV)
                .withAccountId(accountId)
                .withPrefix(prefix);
        InventoryDestination destination = new InventoryDestination().withS3BucketDestination(s3BucketDestination);

        InventoryConfiguration config = new InventoryConfiguration()
                .withEnabled(true)
                .withId(configId)
                .withDestination(destination)
                .withIncludedObjectVersions(InventoryIncludedObjectVersions.All)
                .withSchedule(new InventorySchedule().withFrequency(InventoryFrequency.Daily))
                .withFilter(new InventoryFilter(new InventoryPrefixPredicate(prefix)))
                .withOptionalFields(optionalFields);


        s3.setBucketInventoryConfiguration(new SetBucketInventoryConfigurationRequest(BUCKET_NAME, config));

        config = s3.getBucketInventoryConfiguration(new GetBucketInventoryConfigurationRequest(BUCKET_NAME, configId))
                   .getInventoryConfiguration();

        assertEquals(configId, config.getId());
        assertTrue(config.isEnabled());
        assertEquals(InventoryIncludedObjectVersions.All.toString(), config.getIncludedObjectVersions());
        assertEquals(InventoryFrequency.Daily.toString(), config.getSchedule().getFrequency());
        s3BucketDestination = config.getDestination().getS3BucketDestination();
        assertEquals(BUCKET_ARN, s3BucketDestination.getBucketArn());
        assertEquals(InventoryFormat.CSV.toString(), s3BucketDestination.getFormat());
        assertEquals(accountId, s3BucketDestination.getAccountId());
        assertEquals(prefix, s3BucketDestination.getPrefix());
        assertEquals(prefix, ((InventoryPrefixPredicate) config.getInventoryFilter().getPredicate()).getPrefix());
        assertTrue(config.getOptionalFields().containsAll(optionalFields));

        s3.deleteBucketInventoryConfiguration(new DeleteBucketInventoryConfigurationRequest(BUCKET_NAME, configId));
    }
}
