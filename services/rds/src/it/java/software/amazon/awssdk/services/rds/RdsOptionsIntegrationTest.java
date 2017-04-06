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

        List<OptionGroup> groups = rds.describeOptionGroups(new DescribeOptionGroupsRequest()).getOptionGroupsList();

        for (OptionGroup optionGroup : groups) {
            if (optionGroup.getOptionGroupName().contains("TestOptionGroupName")) {
                rds.deleteOptionGroup(new DeleteOptionGroupRequest()
                                              .withOptionGroupName(optionGroup.getOptionGroupName()));
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

        List<OptionGroup> optionGroups = rds.describeOptionGroups(new DescribeOptionGroupsRequest()).getOptionGroupsList();
        for (OptionGroup optionGroup : optionGroups) {
            log(optionGroup);
        }
        int optionGroupsCount = optionGroups.size();

        // defaults
        String optionGroupName = "TestOptionGroupName" + System.currentTimeMillis();
        String optionGroupDescription = "Test Option Group";

        // create group
        OptionGroup optionGroup = rds.createOptionGroup(
                new CreateOptionGroupRequest()
                        .withEngineName(ENGINE_NAME)
                        .withMajorEngineVersion(MAJOR_ENGINE_VERSION)
                        .withOptionGroupName(optionGroupName)
                        .withOptionGroupDescription(optionGroupDescription));

        // verify option group
        assertNotNull(optionGroup);
        assertEquals(optionGroupName.toLowerCase(), optionGroup.getOptionGroupName());
        assertEquals(ENGINE_NAME, optionGroup.getEngineName());
        assertEquals(MAJOR_ENGINE_VERSION, optionGroup.getMajorEngineVersion());
        assertNotNull(optionGroup.getOptions());
        assertEquals(0, optionGroup.getOptions().size());

        // verify there's a new group
        optionGroups = rds.describeOptionGroups(new DescribeOptionGroupsRequest()).getOptionGroupsList();
        assertEquals(optionGroupsCount + 1, optionGroups.size());

        // modify group by adding options
        List<OptionGroupOption> optionGroupOptions = rds.describeOptionGroupOptions(
                new DescribeOptionGroupOptionsRequest()
                        .withEngineName(ENGINE_NAME)
                        .withMajorEngineVersion(MAJOR_ENGINE_VERSION)).getOptionGroupOptions();

        List<String> securityGroupName = new ArrayList<String>();
        for (DBSecurityGroup securityGroup : rds.describeDBSecurityGroups(new DescribeDBSecurityGroupsRequest()).getDBSecurityGroups()) {
            securityGroupName.add(securityGroup.getDBSecurityGroupName());
        }

        rds.modifyOptionGroup(
                new ModifyOptionGroupRequest()
                        .withOptionGroupName(optionGroupName)
                        .withApplyImmediately(true)
                        .withOptionsToInclude(
                                new OptionConfiguration()
                                        .withOptionName("OEM")
                                        .withDBSecurityGroupMemberships(securityGroupName)
                                        .withPort(1984)));

        // verify update
        optionGroup = rds.describeOptionGroups(
                new DescribeOptionGroupsRequest()
                        .withOptionGroupName(optionGroupName)).getOptionGroupsList().get(0);
        log(optionGroup);
        assertEquals(1, optionGroup.getOptions().size());

        // Add a permanent option ( Oracle TDE )
        optionGroup = rds.modifyOptionGroup(
                new ModifyOptionGroupRequest()
                        .withOptionGroupName(optionGroupName)
                        .withApplyImmediately(true)
                        .withOptionsToInclude(
                                new OptionConfiguration()
                                        .withOptionName("TDE")));

        // verify update
        optionGroup = rds.describeOptionGroups(
                new DescribeOptionGroupsRequest()
                        .withOptionGroupName(optionGroupName)).getOptionGroupsList().get(0);
        log(optionGroup);
        assertEquals(2, optionGroup.getOptions().size());

        Option tdeOption = null;
        for (Option option : optionGroup.getOptions()) {
            if (option.getOptionName().equalsIgnoreCase("TDE")) {
                tdeOption = option;
                break;
            }
        }
        assertNotNull(tdeOption);
        assertTrue(tdeOption.getPermanent());
        assertTrue(tdeOption.getPersistent());

        List<String> optionsToRemove = new ArrayList<String>();
        optionsToRemove.add("OEM");
        optionsToRemove.add("TDE");

        // modify group by removing options
        rds.modifyOptionGroup(
                new ModifyOptionGroupRequest()
                        .withOptionGroupName(optionGroupName)
                        .withApplyImmediately(true)
                        .withOptionsToRemove(optionsToRemove));

        // verify update
        optionGroup = rds.describeOptionGroups(
                new DescribeOptionGroupsRequest()
                        .withOptionGroupName(optionGroupName)).getOptionGroupsList().get(0);
        log(optionGroup);
        assertEquals(0, optionGroup.getOptions().size());

        // delete the group
        rds.deleteOptionGroup(
                new DeleteOptionGroupRequest()
                        .withOptionGroupName(optionGroupName));

        // verify delete
        optionGroups = rds.describeOptionGroups(new DescribeOptionGroupsRequest()).getOptionGroupsList();

        assertEquals(optionGroupsCount, optionGroups.size());

    }

    private void log(OptionGroup optionGroup) {
        System.out.println("Option Group");
        System.out.println(String.format("Engine Name: %s", optionGroup.getEngineName()));
        System.out.println(String.format("Major Engine Version: %s", optionGroup.getMajorEngineVersion()));
        System.out.println(String.format("Option Group Description: %s", optionGroup.getOptionGroupDescription()));
        System.out.println(String.format("Option Group Name: %s", optionGroup.getOptionGroupName()));
        System.out.println(String.format("Options: %s", optionGroup.getOptions().size()));

        for (int i = 0; i < optionGroup.getOptions().size(); i++) {
            Option option = optionGroup.getOptions().get(i);
            System.out.println(i);
            Log(option);
            System.out.println();
        }
    }

    private void Log(Option option) {
        System.out.println("Option");
        System.out.println(String.format("Option Name: %s", option.getOptionName()));
        System.out.println(String.format("Option Description: %s", option.getOptionDescription()));
        System.out.println(String.format("Permanent: %s", option.getPermanent()));
        System.out.println(String.format("Persistent: %s", option.getPersistent()));
        System.out.println(String.format("Port: %s", option.getPort()));
        System.out.println(String.format("DBSecurityGroupMemberships: %s", option.getDBSecurityGroupMemberships().size()));

    }
}
