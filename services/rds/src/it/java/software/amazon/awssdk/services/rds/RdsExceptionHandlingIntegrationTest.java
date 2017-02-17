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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException.ErrorType;
import software.amazon.awssdk.services.rds.model.DBInstanceNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDBInstancesRequest;

/**
 * Integration test for the typed exception handling in RDS.
 *
 * @author Jason Fulghum fulghum@amazon.com
 */
public class RdsExceptionHandlingIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that a custom RDS exception is thrown as expected, and correctly
     * populated.
     */
    @Test
    public void testExceptionHandling() throws Exception {
        try {
            rds.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("non-existant-db-identifier"));
            fail("Expected exception not thrown");
        } catch (DBInstanceNotFoundException nfe) {
            assertNotEmpty(nfe.getErrorCode());
            assertEquals(ErrorType.Client, nfe.getErrorType());
            assertNotEmpty(nfe.getMessage());
            assertNotEmpty(nfe.getRequestId());
            assertNotEmpty(nfe.getServiceName());
            assertTrue(nfe.getStatusCode() >= 400);
        }
    }
}
