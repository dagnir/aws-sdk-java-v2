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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.rds.model.DescribeOrderableDBInstanceOptionsRequest;
import software.amazon.awssdk.services.rds.model.DescribeOrderableDBInstanceOptionsResult;
import software.amazon.awssdk.services.rds.model.OrderableDBInstanceOption;

/**
 * Tests of the DescribeOrderableDBInstanceOptions API.
 */
public class DescribeOrderableDBInstanceOptionsIntegrationTest extends IntegrationTestBase {

    private static final String DB_INSTANCE_CLASS = "db.m1.small";

    /**
     * Just kicks the tires a little.
     */
    @Test
    public void smokeTest() throws Exception {

        String engine = "mysql";
        DescribeOrderableDBInstanceOptionsResult result = rds
                .describeOrderableDBInstanceOptions(DescribeOrderableDBInstanceOptionsRequest.builder().engine(engine).build());

        assertNotNull(result);
        assertNotNull(result.orderableDBInstanceOptions());
        for (OrderableDBInstanceOption opt : result.orderableDBInstanceOptions()) {
            assertNotNull(opt.availabilityZones());
            assertTrue(opt.availabilityZones().size() > 0);
            assertNotEmpty(opt.dbInstanceClass());
            assertEquals(engine, opt.engine());
            assertNotEmpty(opt.engineVersion());
            assertNotEmpty(opt.licenseModel());
            assertNotNull(opt.multiAZCapable());
            assertNotNull(opt.readReplicaCapable());
        }
    }

    /**
     * Tests filtering abilities.
     */
    @Test
    public void testFiltering() throws Exception {
        /*
         * Filter based on an engine version
         */
        String engine = "mysql";
        String engineVersion = "5.7.11";

        DescribeOrderableDBInstanceOptionsResult result = rds
                .describeOrderableDBInstanceOptions(DescribeOrderableDBInstanceOptionsRequest.builder().engine(engine)
                                                                                             .engineVersion(engineVersion)
                                                                                             .build());

        assertNotNull(result);
        assertNotNull(result.orderableDBInstanceOptions());
        for (OrderableDBInstanceOption opt : result.orderableDBInstanceOptions()) {
            assertEquals(engine, opt.engine());
            assertEquals(engineVersion, opt.engineVersion());
        }

        // rds.describeDBEngineVersions();

        /*
         * Filter on a license model
         */
        engine = "mysql";
        String licenseModel = "general-public-license";
        result = rds.describeOrderableDBInstanceOptions(
                DescribeOrderableDBInstanceOptionsRequest.builder().engine(engine).licenseModel(licenseModel).build());

        assertNotNull(result);
        assertNotNull(result.orderableDBInstanceOptions());
        for (OrderableDBInstanceOption opt : result.orderableDBInstanceOptions()) {
            assertEquals(engine, opt.engine());
            assertEquals(licenseModel, opt.licenseModel());
        }

        /*
         * Filter on a license modelf for which there are no results
         */
        licenseModel = "bring-your-own-license";
        try {
            result = rds.describeOrderableDBInstanceOptions(
                    DescribeOrderableDBInstanceOptionsRequest.builder().engine(engine).licenseModel(licenseModel).build());
            fail();
        } catch (Exception e) {
            // do nothing, expect an exception
        }

    }

    /**
     * Tests pagination of results
     */
    @Test
    public void testPagination() throws Exception {
        String engine = "mysql";
        DescribeOrderableDBInstanceOptionsResult result = rds.describeOrderableDBInstanceOptions(
                DescribeOrderableDBInstanceOptionsRequest.builder().engine(engine).build());
        assertNotNull(result);
        assertNotNull(result.orderableDBInstanceOptions());

        int totalResults = result.orderableDBInstanceOptions().size();
        while (result.marker() != null && result.marker().length() > 0) {
            result = rds.describeOrderableDBInstanceOptions(
                    DescribeOrderableDBInstanceOptionsRequest.builder().engine(engine).marker(result.marker()).build());
            assertNotNull(result);
            assertNotNull(result.orderableDBInstanceOptions());
            totalResults += result.orderableDBInstanceOptions().size();
        }

        int numResults = 0;
        int pageSize = 20;  // min page size
        do {
            result = rds.describeOrderableDBInstanceOptions(DescribeOrderableDBInstanceOptionsRequest.builder()
                                                                                                     .engine(engine)
                                                                                                     .maxRecords(pageSize)
                                                                                                     .marker(result.marker())
                                                                                                     .build());
            assertNotNull(result);
            assertNotNull(result.orderableDBInstanceOptions());
            numResults += result.orderableDBInstanceOptions().size();
            assertTrue(result.orderableDBInstanceOptions().size() <= pageSize);
        } while (result.marker() != null && result.marker().length() > 0);

        assertEquals(totalResults, numResults);
    }

    @Test(expected = AmazonClientException.class)
    public void testInvalidParam() {
        rds.describeOrderableDBInstanceOptions(DescribeOrderableDBInstanceOptionsRequest.builder().engine("mysql")
                                                                                        .licenseModel("not-a-valid-license")
                                                                                        .build());
    }

    @Test(expected = AmazonClientException.class)
    public void testEngineMissing() {
        rds.describeOrderableDBInstanceOptions(DescribeOrderableDBInstanceOptionsRequest.builder().build());
    }
}
