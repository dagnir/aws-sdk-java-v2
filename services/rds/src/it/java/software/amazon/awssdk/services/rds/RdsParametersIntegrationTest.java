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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.services.rds.model.CreateDBParameterGroupRequest;
import software.amazon.awssdk.services.rds.model.DBParameterGroup;
import software.amazon.awssdk.services.rds.model.DeleteDBParameterGroupRequest;
import software.amazon.awssdk.services.rds.model.DescribeDBParameterGroupsRequest;
import software.amazon.awssdk.services.rds.model.DescribeDBParametersRequest;
import software.amazon.awssdk.services.rds.model.DescribeEngineDefaultParametersRequest;
import software.amazon.awssdk.services.rds.model.EngineDefaults;
import software.amazon.awssdk.services.rds.model.ModifyDBParameterGroupRequest;
import software.amazon.awssdk.services.rds.model.Parameter;
import software.amazon.awssdk.services.rds.model.ResetDBParameterGroupRequest;

/**
 * Integration tests for RDS parameter and parameter group operations.
 *
 * @author Jason Fulghum fulghum@amazon.com
 */
public class RdsParametersIntegrationTest extends IntegrationTestBase {

    /**
     * Tests each of the RDS parameter operations to verify that we can call
     * them and correctly unmarshall the results.
     */
    @Test
    public void testDBParameterOperations() throws Exception {
        parameterGroupName = "java-integ-test-param-group-" + new Date().getTime();

        // Create a parameter group
        DBParameterGroup parameterGroup = rds.createDBParameterGroup(
                new CreateDBParameterGroupRequest()
                        .withDBParameterGroupName(parameterGroupName)
                        .withDescription("description")
                        .withDBParameterGroupFamily(ENGINE));
        assertEquals(parameterGroupName, parameterGroup.getDBParameterGroupName());
        assertEquals("description", parameterGroup.getDescription());
        assertTrue(parameterGroup.getDBParameterGroupFamily().startsWith("mysql"));


        // Describe it
        List<DBParameterGroup> dbParameterGroups = rds.describeDBParameterGroups(
                new DescribeDBParameterGroupsRequest()
                        .withDBParameterGroupName(parameterGroupName)
                        .withMaxRecords(20)
                                                                                ).getDBParameterGroups();
        assertEquals(1, dbParameterGroups.size());
        assertEquals(parameterGroupName, dbParameterGroups.get(0).getDBParameterGroupName());
        assertEquals("description", dbParameterGroups.get(0).getDescription());
        assertTrue(dbParameterGroups.get(0).getDBParameterGroupFamily().startsWith("mysql"));


        // Describe the params in a group
        List<Parameter> parameters = rds.describeDBParameters(
                new DescribeDBParametersRequest()
                        .withDBParameterGroupName(parameterGroupName)
                        .withMaxRecords(20)
                                                             ).getParameters();
        System.out.println("Total parameters returned: " + parameters.size());
        // We can't request a specific parameter, so we rely on the fact that most
        // parameters will have the following fields populated.
        assertValidParameter(parameters.get(0));


        // Describe the defaults for an engine
        EngineDefaults engineDefaultParameters = rds.describeEngineDefaultParameters(
                new DescribeEngineDefaultParametersRequest()
                        .withDBParameterGroupFamily(ENGINE)
                        .withMaxRecords(20));
        assertEquals(ENGINE, engineDefaultParameters.getDBParameterGroupFamily());
        assertFalse(engineDefaultParameters.getParameters().isEmpty());
        assertValidParameter(engineDefaultParameters.getParameters().get(0));


        // Reset the parameter group
        String resetParameterGroupName = rds.resetDBParameterGroup(
                new ResetDBParameterGroupRequest()
                        .withDBParameterGroupName(parameterGroupName)
                        .withResetAllParameters(true)
                                                                  ).getDBParameterGroupName();
        assertEquals(parameterGroupName, resetParameterGroupName);


        // Modify the parameter group
        Parameter newParameter = new Parameter()
                .withParameterName("character_set_client")
                .withParameterValue("ascii")
                .withApplyMethod("immediate");
        String modifiedParameterGroupName = rds.modifyDBParameterGroup(
                new ModifyDBParameterGroupRequest()
                        .withDBParameterGroupName(parameterGroupName)
                        .withParameters(newParameter)
                                                                      ).getDBParameterGroupName();
        assertEquals(parameterGroupName, modifiedParameterGroupName);


        // Delete it
        rds.deleteDBParameterGroup(
                new DeleteDBParameterGroupRequest()
                        .withDBParameterGroupName(parameterGroupName));
    }


    /*
     * Private Test Helpers
     */

    /**
     * Sanity checks a parameter to assert that the basic fields are populated.
     * If any missing data is found, this method will fail the current test.
     *
     * @param parameter
     *            The parameter to check.
     */
    private void assertValidParameter(Parameter parameter) {
        assertNotEmpty(parameter.getAllowedValues());
        assertNotEmpty(parameter.getApplyType());
        assertNotEmpty(parameter.getDataType());
        assertNotEmpty(parameter.getDescription());
        assertNotEmpty(parameter.getParameterName());
        assertNotEmpty(parameter.getSource());
    }

}
