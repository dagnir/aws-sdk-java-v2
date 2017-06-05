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

import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.services.rds.model.CreateOptionGroupRequest;
import software.amazon.awssdk.services.rds.model.DBSecurityGroup;
import software.amazon.awssdk.services.rds.model.DeleteOptionGroupRequest;
import software.amazon.awssdk.services.rds.model.DescribeDBSecurityGroupsRequest;
import software.amazon.awssdk.services.rds.model.DescribeOptionGroupOptionsRequest;
import software.amazon.awssdk.services.rds.model.DescribeOptionGroupsRequest;
import software.amazon.awssdk.services.rds.model.ModifyOptionGroupRequest;
import software.amazon.awssdk.services.rds.model.Option;
import software.amazon.awssdk.services.rds.model.OptionConfiguration;
import software.amazon.awssdk.services.rds.model.OptionGroup;
import software.amazon.awssdk.services.rds.model.OptionGroupOption;

/**
 * Integration tests for RDS Options
 *
 */
public class RdsOptionsIntegrationTest extends IntegrationTestBase {

    private static final String DB_INSTANCE_CLASS = "db.m1.small";
    private static final int PORT = 1234;

    private static final String DB_INSTANCE_NAME = "java-integ-test-db-" + System.currentTimeMillis();
    private static final String ENGINE_NAME = "oracle-ee";
    private static final String MAJOR_ENGINE_VERSION = "11.2";

    @AfterClass
    public static void tearDownOptionsTest() throws Exception {

        List<OptionGroup> groups = rds.describeOptionGroups(DescribeOptionGroupsRequest.builder().build()).optionGroupsList();

        for (OptionGroup optionGroup : groups) {
            if (optionGroup.optionGroupName().contains("TestOptionGroupName")) {
                rds.deleteOptionGroup(DeleteOptionGroupRequest.builder()
                                                              .optionGroupName(optionGroup.optionGroupName()).build());
            }

        }
    }

    /**
     * Tests the RDS operations that require a running instance, including
     * snapshot operations, restore operations, db events, and creating,
     * describing, deleting database instances.
     */
    @Test
    public void optionsTest() {

        List<OptionGroup> optionGroups =
                rds.describeOptionGroups(DescribeOptionGroupsRequest.builder().build()).optionGroupsList();
        for (OptionGroup optionGroup : optionGroups) {
            log(optionGroup);
        }
        int optionGroupsCount = optionGroups.size();

        // defaults
        String optionGroupName = "TestOptionGroupName" + System.currentTimeMillis();
        String optionGroupDescription = "Test Option Group";

        // create group
        OptionGroup optionGroup = rds.createOptionGroup(
                CreateOptionGroupRequest.builder()
                                        .engineName(ENGINE_NAME)
                                        .majorEngineVersion(MAJOR_ENGINE_VERSION)
                                        .optionGroupName(optionGroupName)
                                        .optionGroupDescription(optionGroupDescription).build());

        // verify option group
        assertNotNull(optionGroup);
        assertEquals(optionGroupName.toLowerCase(), optionGroup.optionGroupName());
        assertEquals(ENGINE_NAME, optionGroup.engineName());
        assertEquals(MAJOR_ENGINE_VERSION, optionGroup.majorEngineVersion());
        assertNotNull(optionGroup.options());
        assertEquals(0, optionGroup.options().size());

        // verify there's a new group
        optionGroups = rds.describeOptionGroups(DescribeOptionGroupsRequest.builder().build()).optionGroupsList();
        assertEquals(optionGroupsCount + 1, optionGroups.size());

        // modify group by adding options
        List<OptionGroupOption> optionGroupOptions = rds.describeOptionGroupOptions(
                DescribeOptionGroupOptionsRequest.builder()
                                                 .engineName(ENGINE_NAME)
                                                 .majorEngineVersion(MAJOR_ENGINE_VERSION).build()).optionGroupOptions();

        List<String> securityGroupName = new ArrayList<String>();
        for (DBSecurityGroup securityGroup : rds.describeDBSecurityGroups(DescribeDBSecurityGroupsRequest.builder().build())
                                                .dbSecurityGroups()) {
            securityGroupName.add(securityGroup.dbSecurityGroupName());
        }

        rds.modifyOptionGroup(
                ModifyOptionGroupRequest.builder()
                                        .optionGroupName(optionGroupName)
                                        .applyImmediately(true)
                                        .optionsToInclude(
                                                OptionConfiguration.builder()
                                                                   .optionName("OEM")
                                                                   .dbSecurityGroupMemberships(securityGroupName)
                                                                   .port(1984).build()).build());

        // verify update
        optionGroup = rds.describeOptionGroups(
                DescribeOptionGroupsRequest.builder()
                                           .optionGroupName(optionGroupName).build()).optionGroupsList().get(0);
        log(optionGroup);
        assertEquals(1, optionGroup.options().size());

        // Add a permanent option ( Oracle TDE )
        optionGroup = rds.modifyOptionGroup(
                ModifyOptionGroupRequest.builder()
                                        .optionGroupName(optionGroupName)
                                        .applyImmediately(true)
                                        .optionsToInclude(
                                                OptionConfiguration.builder()
                                                                   .optionName("TDE").build()).build());

        // verify update
        optionGroup = rds.describeOptionGroups(
                DescribeOptionGroupsRequest.builder()
                                           .optionGroupName(optionGroupName).build()).optionGroupsList().get(0);
        log(optionGroup);
        assertEquals(2, optionGroup.options().size());

        Option tdeOption = null;
        for (Option option : optionGroup.options()) {
            if (option.optionName().equalsIgnoreCase("TDE")) {
                tdeOption = option;
                break;
            }
        }
        assertNotNull(tdeOption);
        assertTrue(tdeOption.permanent());
        assertTrue(tdeOption.persistent());

        List<String> optionsToRemove = new ArrayList<String>();
        optionsToRemove.add("OEM");
        optionsToRemove.add("TDE");

        // modify group by removing options
        rds.modifyOptionGroup(
                ModifyOptionGroupRequest.builder()
                                        .optionGroupName(optionGroupName)
                                        .applyImmediately(true)
                                        .optionsToRemove(optionsToRemove).build());

        // verify update
        optionGroup = rds.describeOptionGroups(
                DescribeOptionGroupsRequest.builder()
                                           .optionGroupName(optionGroupName).build()).optionGroupsList().get(0);
        log(optionGroup);
        assertEquals(0, optionGroup.options().size());

        // delete the group
        rds.deleteOptionGroup(
                DeleteOptionGroupRequest.builder()
                                        .optionGroupName(optionGroupName).build());

        // verify delete
        optionGroups = rds.describeOptionGroups(DescribeOptionGroupsRequest.builder().build()).optionGroupsList();

        assertEquals(optionGroupsCount, optionGroups.size());

    }

    private void log(OptionGroup optionGroup) {
        System.out.println("Option Group");
        System.out.println(String.format("Engine Name: %s", optionGroup.engineName()));
        System.out.println(String.format("Major Engine Version: %s", optionGroup.majorEngineVersion()));
        System.out.println(String.format("Option Group Description: %s", optionGroup.optionGroupDescription()));
        System.out.println(String.format("Option Group Name: %s", optionGroup.optionGroupName()));
        System.out.println(String.format("Options: %s", optionGroup.options().size()));

        for (int i = 0; i < optionGroup.options().size(); i++) {
            Option option = optionGroup.options().get(i);
            System.out.println(i);
            Log(option);
            System.out.println();
        }
    }

    private void Log(Option option) {
        System.out.println("Option");
        System.out.println(String.format("Option Name: %s", option.optionName()));
        System.out.println(String.format("Option Description: %s", option.optionDescription()));
        System.out.println(String.format("Permanent: %s", option.permanent()));
        System.out.println(String.format("Persistent: %s", option.persistent()));
        System.out.println(String.format("Port: %s", option.port()));
        System.out.println(String.format("DBSecurityGroupMemberships: %s", option.dbSecurityGroupMemberships().size()));

    }
}
