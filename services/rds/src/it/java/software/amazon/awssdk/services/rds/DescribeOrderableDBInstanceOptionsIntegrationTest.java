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
                .describeOrderableDBInstanceOptions(new DescribeOrderableDBInstanceOptionsRequest().withEngine(engine));

        assertNotNull(result);
        assertNotNull(result.getOrderableDBInstanceOptions());
        for (OrderableDBInstanceOption opt : result.getOrderableDBInstanceOptions()) {
            assertNotNull(opt.getAvailabilityZones());
            assertTrue(opt.getAvailabilityZones().size() > 0);
            assertNotEmpty(opt.getDBInstanceClass());
            assertEquals(engine, opt.getEngine());
            assertNotEmpty(opt.getEngineVersion());
            assertNotEmpty(opt.getLicenseModel());
            assertNotNull(opt.getMultiAZCapable());
            assertNotNull(opt.getReadReplicaCapable());
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
                .describeOrderableDBInstanceOptions(new DescribeOrderableDBInstanceOptionsRequest().withEngine(engine)
                                                                                                   .withEngineVersion(engineVersion));

        assertNotNull(result);
        assertNotNull(result.getOrderableDBInstanceOptions());
        for (OrderableDBInstanceOption opt : result.getOrderableDBInstanceOptions()) {
            assertEquals(engine, opt.getEngine());
            assertEquals(engineVersion, opt.getEngineVersion());
        }

        // rds.describeDBEngineVersions();

        /*
         * Filter on a license model
         */
        engine = "mysql";
        String licenseModel = "general-public-license";
        result = rds.describeOrderableDBInstanceOptions(new DescribeOrderableDBInstanceOptionsRequest().withEngine(engine).withLicenseModel(licenseModel));

        assertNotNull(result);
        assertNotNull(result.getOrderableDBInstanceOptions());
        for (OrderableDBInstanceOption opt : result.getOrderableDBInstanceOptions()) {
            assertEquals(engine, opt.getEngine());
            assertEquals(licenseModel, opt.getLicenseModel());
        }

        /*
         * Filter on a license modelf for which there are no results
         */
        licenseModel = "bring-your-own-license";
        try {
            result = rds.describeOrderableDBInstanceOptions(new DescribeOrderableDBInstanceOptionsRequest().withEngine(engine).withLicenseModel(licenseModel));
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
        DescribeOrderableDBInstanceOptionsResult result = rds.describeOrderableDBInstanceOptions(new DescribeOrderableDBInstanceOptionsRequest().withEngine(engine));
        assertNotNull(result);
        assertNotNull(result.getOrderableDBInstanceOptions());

        int totalResults = result.getOrderableDBInstanceOptions().size();
        while (result.getMarker() != null && result.getMarker().length() > 0) {
            result = rds.describeOrderableDBInstanceOptions(new DescribeOrderableDBInstanceOptionsRequest().withEngine(engine).withMarker(result.getMarker()));
            assertNotNull(result);
            assertNotNull(result.getOrderableDBInstanceOptions());
            totalResults += result.getOrderableDBInstanceOptions().size();
        }

        int numResults = 0;
        int pageSize = 20;  // min page size
        do {
            result = rds.describeOrderableDBInstanceOptions(new DescribeOrderableDBInstanceOptionsRequest()
                                                                    .withEngine(engine).withMaxRecords(pageSize).withMarker(result.getMarker()));
            assertNotNull(result);
            assertNotNull(result.getOrderableDBInstanceOptions());
            numResults += result.getOrderableDBInstanceOptions().size();
            assertTrue(result.getOrderableDBInstanceOptions().size() <= pageSize);
        } while (result.getMarker() != null && result.getMarker().length() > 0);

        assertEquals(totalResults, numResults);
    }

    @Test(expected = AmazonClientException.class)
    public void testInvalidParam() {
        rds.describeOrderableDBInstanceOptions(new DescribeOrderableDBInstanceOptionsRequest().withEngine("mysql")
                                                                                              .withLicenseModel("not-a-valid-license"));
    }

    @Test(expected = AmazonClientException.class)
    public void testEngineMissing() {
        rds.describeOrderableDBInstanceOptions(new DescribeOrderableDBInstanceOptionsRequest());
    }
}
