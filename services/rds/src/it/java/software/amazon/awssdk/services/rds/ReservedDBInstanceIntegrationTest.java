/*
 * Copyright 2011-2017 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package software.amazon.awssdk.services.rds;

import static org.junit.Assert.*;

import org.junit.Test;
import software.amazon.awssdk.services.rds.model.DescribeReservedDBInstancesOfferingsResult;
import software.amazon.awssdk.services.rds.model.DescribeReservedDBInstancesResult;
import software.amazon.awssdk.services.rds.model.ReservedDBInstance;
import software.amazon.awssdk.services.rds.model.ReservedDBInstancesOffering;


/**
 * Tests of the reserved instance apis
 */
public class ReservedDBInstanceIntegrationTest extends IntegrationTestBase {

    @Test
    public void testDescribeReservedInstanceOfferings() throws Exception {
        DescribeReservedDBInstancesOfferingsResult result = rds.describeReservedDBInstancesOfferings();
        assertNotNull(result);
        assertNotNull(result.getReservedDBInstancesOfferings());
        assertTrue(result.getReservedDBInstancesOfferings().size() > 0);

        for (ReservedDBInstancesOffering offering : result.getReservedDBInstancesOfferings()) {
            assertNotNull(offering);
            assertNotEmpty(offering.getCurrencyCode());
            assertNotEmpty(offering.getDBInstanceClass());
            assertNotEmpty(offering.getProductDescription());
            assertNotEmpty(offering.getReservedDBInstancesOfferingId());
            assertNotNull(offering.getDuration());
            assertNotNull(offering.getFixedPrice());
            assertNotNull(offering.getMultiAZ());
            assertNotNull(offering.getUsagePrice());
        }
    }

    @Test
    public void testDescribeReservedInstances() throws Exception {
        DescribeReservedDBInstancesResult result = rds.describeReservedDBInstances();
        assertNotNull(result);
        assertNotNull(result.getReservedDBInstances());

        // TODO: this code never runs because we don't have any reserved
        // instances. We need to be able to purchase a reserved instance without
        // actually buying it, like we do with EC2.
        for (ReservedDBInstance instance : result.getReservedDBInstances()) {
            assertNotNull(instance);
            assertNotEmpty(instance.getCurrencyCode());
            assertNotEmpty(instance.getDBInstanceClass());
            assertNotEmpty(instance.getProductDescription());
            assertNotEmpty(instance.getReservedDBInstanceId());
            assertNotEmpty(instance.getReservedDBInstancesOfferingId());
            assertNotEmpty(instance.getState());
            assertNotNull(instance.getDBInstanceCount());
            assertNotNull(instance.getDuration());
            assertNotNull(instance.getFixedPrice());
            assertNotNull(instance.getMultiAZ());
            assertNotNull(instance.getStartTime());
            assertNotNull(instance.getUsagePrice());
        }
    }
}
