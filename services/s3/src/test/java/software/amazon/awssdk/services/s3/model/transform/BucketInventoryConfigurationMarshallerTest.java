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

package software.amazon.awssdk.services.s3.model.transform;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToXml;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.s3.AmazonS3;
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
import utils.http.S3WireMockTestBase;

public class BucketInventoryConfigurationMarshallerTest extends S3WireMockTestBase {

    private static final String BUCKET_NAME = "destination-bucket";

    private AmazonS3 s3;

    @Before
    public void setUp() throws Exception {
        s3 = buildClient();
    }

    @Test
    public void testSetBucketInventoryConfiguration() {
        String id = "inventory-id";
        List<String> optionalFields = new ArrayList<String>() {
            {
                add(InventoryOptionalField.LastModifiedDate.toString());
                add(InventoryOptionalField.StorageClass.toString());
                add(InventoryOptionalField.ReplicationStatus.toString());
            }
        };
        InventoryS3BucketDestination s3BucketDestination = new InventoryS3BucketDestination()
                .withAccountId("accountId")
                .withBucketArn("arn:aws:s3:::bucket")
                .withFormat(InventoryFormat.CSV)
                .withPrefix("prefix");
        InventoryDestination destination = new InventoryDestination().withS3BucketDestination(s3BucketDestination);

        InventoryConfiguration config = new InventoryConfiguration()
                .withId(id)
                .withDestination(destination)
                .withEnabled(true)
                .withFilter(new InventoryFilter(new InventoryPrefixPredicate("prefix")))
                .withIncludedObjectVersions(InventoryIncludedObjectVersions.All)
                .withOptionalFields(optionalFields)
                .withSchedule(new InventorySchedule().withFrequency(InventoryFrequency.Daily));

        try {
            s3.setBucketInventoryConfiguration(new SetBucketInventoryConfigurationRequest(BUCKET_NAME, config));
        } catch (Exception expected) {
            // Expected.
        }

        verify(putRequestedFor(urlEqualTo(String.format("/%s/?inventory&id=%s", BUCKET_NAME, id)))
                       .withRequestBody(equalToXml(getExpectedMarshalledXml("InventoryConfiguration.xml"))));
    }
}