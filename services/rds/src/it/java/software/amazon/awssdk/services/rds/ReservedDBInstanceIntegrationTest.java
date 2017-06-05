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

package software.amazon.awssdk.services.rds;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import software.amazon.awssdk.services.rds.model.DescribeReservedDBInstancesOfferingsRequest;
import software.amazon.awssdk.services.rds.model.DescribeReservedDBInstancesOfferingsResult;
import software.amazon.awssdk.services.rds.model.DescribeReservedDBInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeReservedDBInstancesResult;
import software.amazon.awssdk.services.rds.model.ReservedDBInstance;
import software.amazon.awssdk.services.rds.model.ReservedDBInstancesOffering;


/**
 * Tests of the reserved instance apis
 */
public class ReservedDBInstanceIntegrationTest extends IntegrationTestBase {

    @Test
    public void testDescribeReservedInstanceOfferings() throws Exception {
        DescribeReservedDBInstancesOfferingsResult result =
                rds.describeReservedDBInstancesOfferings(DescribeReservedDBInstancesOfferingsRequest.builder().build());
        assertNotNull(result);
        assertNotNull(result.reservedDBInstancesOfferings());
        assertTrue(result.reservedDBInstancesOfferings().size() > 0);

        for (ReservedDBInstancesOffering offering : result.reservedDBInstancesOfferings()) {
            assertNotNull(offering);
            assertNotEmpty(offering.currencyCode());
            assertNotEmpty(offering.dbInstanceClass());
            assertNotEmpty(offering.productDescription());
            assertNotEmpty(offering.reservedDBInstancesOfferingId());
            assertNotNull(offering.duration());
            assertNotNull(offering.fixedPrice());
            assertNotNull(offering.multiAZ());
            assertNotNull(offering.usagePrice());
        }
    }

    @Test
    public void testDescribeReservedInstances() throws Exception {
        DescribeReservedDBInstancesResult result =
                rds.describeReservedDBInstances(DescribeReservedDBInstancesRequest.builder().build());
        assertNotNull(result);
        assertNotNull(result.reservedDBInstances());

        // TODO: this code never runs because we don't have any reserved
        // instances. We need to be able to purchase a reserved instance without
        // actually buying it, like we do with EC2.
        for (ReservedDBInstance instance : result.reservedDBInstances()) {
            assertNotNull(instance);
            assertNotEmpty(instance.currencyCode());
            assertNotEmpty(instance.dbInstanceClass());
            assertNotEmpty(instance.productDescription());
            assertNotEmpty(instance.reservedDBInstanceId());
            assertNotEmpty(instance.reservedDBInstancesOfferingId());
            assertNotEmpty(instance.state());
            assertNotNull(instance.dbInstanceCount());
            assertNotNull(instance.duration());
            assertNotNull(instance.fixedPrice());
            assertNotNull(instance.multiAZ());
            assertNotNull(instance.startTime());
            assertNotNull(instance.usagePrice());
        }
    }
}
