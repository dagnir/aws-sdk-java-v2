/*
 * Copyright 2015-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.services.ec2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.services.ec2.model.CreatePlacementGroupRequest;
import software.amazon.awssdk.services.ec2.model.DeletePlacementGroupRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribePlacementGroupsRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Image;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Placement;
import software.amazon.awssdk.services.ec2.model.PlacementGroup;
import software.amazon.awssdk.services.ec2.model.PlacementStrategy;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.VirtualizationType;

/**
 * Integration tests for the EC2 compute cluster APIs.
 */
public class EC2ClusterComputingIntegrationTest extends EC2IntegrationTestBase {

    private static final String PLACEMENT_GROUP_NAME =
        "integ-test-placement-group-" + System.currentTimeMillis();

    private static String HVM_AMI_ID;

    private static String createdInstanceId;

    @BeforeClass
    public static void setup() {
        HVM_AMI_ID = findPublicHvmAmiId();
    }

    /** Release all resources acquired during tests */
    @AfterClass
    public static void tearDown() {
        try {
            ec2.terminateInstances(new TerminateInstancesRequest()
                    .withInstanceIds(createdInstanceId)
                    );
            waitForInstanceToTransitionToState(
                    createdInstanceId, InstanceStateName.Terminated);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        try {
            ec2.deletePlacementGroup(new DeletePlacementGroupRequest()
                    .withGroupName(PLACEMENT_GROUP_NAME)
                    );
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Describe an HVM image and look for the virtualizationType property
     */
    @Test
    public void testDescribeHvmImage() {
        List<Image> images = ec2.describeImages(new DescribeImagesRequest()
                .withImageIds(HVM_AMI_ID)
                ).getImages();
        assertEquals(1, images.size());
        assertEquals(VirtualizationType.Hvm.toString(),
                     images.get(0).getVirtualizationType());
    }

    /**
     * Runs through the EC2 cluster computing APIs to test that we can create
     * placement groups, launch instances into them, etc.
     */
    @Test
    public void testLaunchClusterInstanceFromHvmImage() throws Exception {

        // Create a placement group to test with
        ec2.createPlacementGroup(new CreatePlacementGroupRequest()
            .withGroupName(PLACEMENT_GROUP_NAME)
            .withStrategy(PlacementStrategy.Cluster));

        Thread.sleep(1000 * 5);

        // Describe placement groups
        List<PlacementGroup> placementGroups =
                ec2.describePlacementGroups(new DescribePlacementGroupsRequest()
                        .withGroupNames(PLACEMENT_GROUP_NAME)
                        ).getPlacementGroups();
        assertEquals(1, placementGroups.size());

        PlacementGroup pg = placementGroups.get(0);
        assertEquals(PLACEMENT_GROUP_NAME, pg.getGroupName());
        assertNotNull(pg.getState());
        assertEquals(PlacementStrategy.Cluster.toString(),
                     pg.getStrategy());


        // Describe placement groups with a filter
        placementGroups = ec2.describePlacementGroups(new DescribePlacementGroupsRequest()
                        .withFilters(new Filter()
                            .withName("group-name")
                            .withValues(PLACEMENT_GROUP_NAME)
                         )
                ).getPlacementGroups();
        assertEquals(1, placementGroups.size());

        pg = placementGroups.get(0);
        assertEquals(PLACEMENT_GROUP_NAME, pg.getGroupName());
        assertNotNull(pg.getState());
        assertEquals(PlacementStrategy.Cluster.toString(),
                     pg.getStrategy());


        // Launch an instance into our new placement group
        createdInstanceId = ec2.runInstances(new RunInstancesRequest()
            .withImageId(HVM_AMI_ID)
            .withMinCount(1)
            .withMaxCount(1)
            .withInstanceType(InstanceType.C42xlarge.toString())
            .withPlacement(new Placement().withGroupName(PLACEMENT_GROUP_NAME))
        ).getReservation().getInstances().get(0).getInstanceId();

        waitForInstanceToTransitionToState(
                createdInstanceId, InstanceStateName.Running);

        // Check its virtualizationType
        Instance instance = describeInstance(createdInstanceId);
        assertEquals(VirtualizationType.Hvm.toString(),
                     instance.getVirtualizationType());
        assertEquals(PLACEMENT_GROUP_NAME,
                     instance.getPlacement().getGroupName());
    }

    private static String findPublicHvmAmiId() {
        List<Image> hvmImages = ec2.describeImages(new DescribeImagesRequest()
                .withFilters(
                        new Filter()
                            .withName("virtualization-type")
                            .withValues("hvm"),
                        new Filter()
                            .withName("is-public")
                            .withValues("true"))
                ).getImages();

        Assert.assertTrue("Cannot find a public HVM AMI.", hvmImages.size() > 0);

        return hvmImages.get(0).getImageId();
    }
}
