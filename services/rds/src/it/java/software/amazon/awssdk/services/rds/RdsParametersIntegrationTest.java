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
                CreateDBParameterGroupRequest.builder()
                        .dbParameterGroupName(parameterGroupName)
                        .description("description")
                        .dbParameterGroupFamily(ENGINE).build());
        assertEquals(parameterGroupName, parameterGroup.dbParameterGroupName());
        assertEquals("description", parameterGroup.description());
        assertTrue(parameterGroup.dbParameterGroupFamily().startsWith("mysql"));


        // Describe it
        List<DBParameterGroup> dbParameterGroups = rds.describeDBParameterGroups(
                DescribeDBParameterGroupsRequest.builder()
                        .dbParameterGroupName(parameterGroupName)
                        .maxRecords(20).build()).dbParameterGroups();
        assertEquals(1, dbParameterGroups.size());
        assertEquals(parameterGroupName, dbParameterGroups.get(0).dbParameterGroupName());
        assertEquals("description", dbParameterGroups.get(0).description());
        assertTrue(dbParameterGroups.get(0).dbParameterGroupFamily().startsWith("mysql"));


        // Describe the params in a group
        List<Parameter> parameters = rds.describeDBParameters(
                DescribeDBParametersRequest.builder()
                        .dbParameterGroupName(parameterGroupName)
                        .maxRecords(20).build()).parameters();
        System.out.println("Total parameters returned: " + parameters.size());
        // We can't request a specific parameter, so we rely on the fact that most
        // parameters will have the following fields populated.
        assertValidParameter(parameters.get(0));


        // Describe the defaults for an engine
        EngineDefaults engineDefaultParameters = rds.describeEngineDefaultParameters(
                DescribeEngineDefaultParametersRequest.builder()
                        .dbParameterGroupFamily(ENGINE)
                        .maxRecords(20).build());
        assertEquals(ENGINE, engineDefaultParameters.dbParameterGroupFamily());
        assertFalse(engineDefaultParameters.parameters().isEmpty());
        assertValidParameter(engineDefaultParameters.parameters().get(0));


        // Reset the parameter group
        String resetParameterGroupName = rds.resetDBParameterGroup(
                ResetDBParameterGroupRequest.builder()
                        .dbParameterGroupName(parameterGroupName)
                        .resetAllParameters(true).build()).dbParameterGroupName();
        assertEquals(parameterGroupName, resetParameterGroupName);


        // Modify the parameter group
        Parameter newParameter = Parameter.builder()
                .parameterName("character_set_client")
                .parameterValue("ascii")
                .applyMethod("immediate").build();
        String modifiedParameterGroupName = rds.modifyDBParameterGroup(
                ModifyDBParameterGroupRequest.builder()
                        .dbParameterGroupName(parameterGroupName)
                        .parameters(newParameter).build()).dbParameterGroupName();
        assertEquals(parameterGroupName, modifiedParameterGroupName);


        // Delete it
        rds.deleteDBParameterGroup(
                DeleteDBParameterGroupRequest.builder()
                        .dbParameterGroupName(parameterGroupName).build());
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
        assertNotEmpty(parameter.allowedValues());
        assertNotEmpty(parameter.applyType());
        assertNotEmpty(parameter.dataType());
        assertNotEmpty(parameter.description());
        assertNotEmpty(parameter.parameterName());
        assertNotEmpty(parameter.source());
    }

}
