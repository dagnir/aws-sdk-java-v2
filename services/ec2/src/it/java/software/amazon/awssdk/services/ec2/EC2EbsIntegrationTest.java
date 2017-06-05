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

package software.amazon.awssdk.services.ec2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.services.ec2.model.CreateVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumeAttributeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumeAttributeResult;
import software.amazon.awssdk.services.ec2.model.DescribeVolumeStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumeStatusResult;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.EnableVolumeIORequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.ModifyVolumeAttributeRequest;
import software.amazon.awssdk.services.ec2.model.TagDescription;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeStatusItem;
import software.amazon.awssdk.services.ec2.model.VolumeType;

/**
 * Integration tests for the EBS operations in the Amazon EC2 Java client
 * library.
 */
public class EC2EbsIntegrationTest extends EC2IntegrationTestBase {

    /** We need to use this specific zone because it supports consistent iops. */
    private static final String US_EAST_1_B = "us-east-1b";
    private static final Integer VOLUME_SIZE = 4; // GB
    private static final Integer PROVISIONED_IOPS = 100; // per second
    private String volumeId;

    /** Release resources used in testing. */
    @After
    public void tearDown() {
        if (volumeId != null) {
            ec2.deleteVolume(DeleteVolumeRequest.builder().volumeId(volumeId).build());
        }
    }

    @Test
    public void testNoVolumeType() throws Exception {

        Volume volume = ec2.createVolume(
                CreateVolumeRequest.builder().size(VOLUME_SIZE).availabilityZone(US_EAST_1_B).build()).volume();

        volumeId = volume.volumeId();

        assertEquals(US_EAST_1_B, volume.availabilityZone());
        assertEquals(VolumeType.Standard.toString(), volume.volumeType());
        assertNull(volume.iops());
    }

    /**
     * Test volume operations
     */
    @Test
    public void testVolumeOperations() throws Exception {

        // Create a test volume
        Volume volume = ec2.createVolume(
                CreateVolumeRequest.builder().size(VOLUME_SIZE).availabilityZone(US_EAST_1_B).iops(PROVISIONED_IOPS)
                                   .volumeType(VolumeType.Io1).build()).volume();

        volumeId = volume.volumeId();

        assertEquals(US_EAST_1_B, volume.availabilityZone());
        assertEquals(VolumeType.Io1.toString(), volume.volumeType());
        assertEquals(PROVISIONED_IOPS, volume.iops());

        // Add tags to this volume
        tagResource(volumeId, TAGS);

        // Try describing it with a filter
        List<Volume> volumes = ec2.describeVolumes(DescribeVolumesRequest.builder()
                                                                         .filters(Filter.builder()
                                                                                        .name("volume-id")
                                                                                        .values(volumeId).build()).build()
                                                  ).volumes();
        assertEquals(1, volumes.size());

        volume = volumes.get(0);
        assertEqualUnorderedTagLists(TAGS, volume.tags());
        assertEquals(VolumeType.Io1.toString(), volume.volumeType());
        assertEquals(PROVISIONED_IOPS, volume.iops());

        // Get all tags
        List<TagDescription> tagDescriptions = ec2.describeTags(DescribeTagsRequest.builder().build()).tags();
        assertTrue(tagDescriptions.size() > 1);
        assertTagDescriptionsValid(tagDescriptions);

        // Filter tags by resource id
        tagDescriptions = ec2.describeTags(DescribeTagsRequest.builder()
                                                              .filters(Filter.builder()
                                                                             .name("resource-id")
                                                                             .values(volumeId).build()).build()
                                          ).tags();
        assertEquals(TAGS.size(), tagDescriptions.size());
        assertTagDescriptionsValid(tagDescriptions);

        // Delete Tags
        ec2.deleteTags(DeleteTagsRequest.builder()
                                        .resources(volumeId)
                                        .tags(TAGS).build());
        Thread.sleep(1000 * 30);
        tagDescriptions = ec2.describeTags(DescribeTagsRequest.builder()
                                                              .filters(Filter.builder()
                                                                             .name("resource-id")
                                                                             .values(volumeId).build()).build()
                                          ).tags();
        assertEquals(0, tagDescriptions.size());

        // Describe volume attribute
        DescribeVolumeAttributeResult describeVolumeAttribute =
                ec2.describeVolumeAttribute(DescribeVolumeAttributeRequest.builder()
                                                                          .attribute("autoEnableIO")
                                                                          .volumeId(volumeId).build());
        assertEquals(volumeId, describeVolumeAttribute.volumeId());
        assertFalse(describeVolumeAttribute.autoEnableIO());

        // Describe volume status by filter
        DescribeVolumeStatusResult describeVolumeStatus =
                ec2.describeVolumeStatus(DescribeVolumeStatusRequest.builder()
                                                                    .filters(Filter.builder().name("volume-id").values(volumeId)
                                                                                   .build()).build());
        assertEquals(1, describeVolumeStatus.volumeStatuses().size());
        VolumeStatusItem volumeStatusItem = describeVolumeStatus.volumeStatuses().get(0);
        assertNotNull(volumeStatusItem);
        assertNotNull(volumeStatusItem.actions());
        assertNotNull(volumeStatusItem.availabilityZone());
        assertNotNull(volumeStatusItem.events());
        assertNotNull(volumeStatusItem.volumeStatus());
        assertEquals(volumeId, volumeStatusItem.volumeId());

        // Describe volume status by id
        describeVolumeStatus = ec2.describeVolumeStatus(DescribeVolumeStatusRequest.builder()
                                                                                   .volumeIds(volumeId).build());
        assertEquals(1, describeVolumeStatus.volumeStatuses().size());
        volumeStatusItem = describeVolumeStatus.volumeStatuses().get(0);
        assertNotNull(volumeStatusItem);
        assertNotNull(volumeStatusItem.actions());
        assertNotNull(volumeStatusItem.availabilityZone());
        assertNotNull(volumeStatusItem.events());
        assertNotNull(volumeStatusItem.volumeStatus());
        assertEquals(volumeId, volumeStatusItem.volumeId());

        // Modify volume attribute
        ec2.modifyVolumeAttribute(ModifyVolumeAttributeRequest.builder()
                                                              .volumeId(volumeId)
                                                              .autoEnableIO(true).build());
        describeVolumeAttribute = ec2.describeVolumeAttribute(
                DescribeVolumeAttributeRequest.builder()
                                              .attribute("autoEnableIO")
                                              .volumeId(volumeId).build());
        assertEquals(volumeId, describeVolumeAttribute.volumeId());
        assertEquals(true, describeVolumeAttribute.autoEnableIO());

        ec2.modifyVolumeAttribute(ModifyVolumeAttributeRequest.builder()
                                                              .volumeId(volumeId)
                                                              .autoEnableIO(false).build());
        describeVolumeAttribute = ec2.describeVolumeAttribute(DescribeVolumeAttributeRequest.builder()
                                                                                            .attribute("autoEnableIO")
                                                                                            .volumeId(volumeId).build());
        assertEquals(volumeId, describeVolumeAttribute.volumeId());
        assertFalse(describeVolumeAttribute.autoEnableIO());

        // Enable volume io -- this should throw an exception because io is already enabled
        try {
            ec2.enableVolumeIO(EnableVolumeIORequest.builder()
                                                    .volumeId(volumeId).build());
            fail("Expected an exception");
        } catch (AmazonClientException expected) {
            assertTrue(expected.getMessage().contains("already has IO enabled"));
        }
    }

    private void assertTagDescriptionsValid(List<TagDescription> tags) {
        for (TagDescription tag : tags) {
            assertNotNull(tag.key());
            assertNotNull(tag.value());
            assertNotNull(tag.resourceId());
            assertNotNull(tag.resourceType());
        }
    }

}
