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
            ec2.deleteVolume(new DeleteVolumeRequest(volumeId));
        }
    }

    @Test
    public void testNoVolumeType() throws Exception {

        Volume volume = ec2.createVolume(
                new CreateVolumeRequest(VOLUME_SIZE, US_EAST_1_B)
                                        ).getVolume();

        volumeId = volume.getVolumeId();

        assertEquals(US_EAST_1_B, volume.getAvailabilityZone());
        assertEquals(VolumeType.Standard.toString(), volume.getVolumeType());
        assertNull(volume.getIops());
    }

    /**
     * Test volume operations
     */
    @Test
    public void testVolumeOperations() throws Exception {

        // Create a test volume
        Volume volume = ec2.createVolume(
                new CreateVolumeRequest(VOLUME_SIZE, US_EAST_1_B)
                        .withIops(PROVISIONED_IOPS)
                        .withVolumeType(VolumeType.Io1)
                                        ).getVolume();

        volumeId = volume.getVolumeId();

        assertEquals(US_EAST_1_B, volume.getAvailabilityZone());
        assertEquals(VolumeType.Io1.toString(), volume.getVolumeType());
        assertEquals(PROVISIONED_IOPS, volume.getIops());

        // Add tags to this volume
        tagResource(volumeId, TAGS);

        // Try describing it with a filter
        List<Volume> volumes = ec2.describeVolumes(new DescribeVolumesRequest()
                                                           .withFilters(new Filter()
                                                                                .withName("volume-id")
                                                                                .withValues(volumeId))
                                                  ).getVolumes();
        assertEquals(1, volumes.size());

        volume = volumes.get(0);
        assertEqualUnorderedTagLists(TAGS, volume.getTags());
        assertEquals(VolumeType.Io1.toString(), volume.getVolumeType());
        assertEquals(PROVISIONED_IOPS, volume.getIops());

        // Get all tags
        List<TagDescription> tagDescriptions = ec2.describeTags().getTags();
        assertTrue(tagDescriptions.size() > 1);
        assertTagDescriptionsValid(tagDescriptions);

        // Filter tags by resource id
        tagDescriptions = ec2.describeTags(new DescribeTagsRequest()
                                                   .withFilters(new Filter()
                                                                        .withName("resource-id")
                                                                        .withValues(volumeId))
                                          ).getTags();
        assertEquals(TAGS.size(), tagDescriptions.size());
        assertTagDescriptionsValid(tagDescriptions);

        // Delete Tags
        ec2.deleteTags(new DeleteTagsRequest()
                               .withResources(volumeId)
                               .withTags(TAGS));
        Thread.sleep(1000 * 30);
        tagDescriptions = ec2.describeTags(new DescribeTagsRequest()
                                                   .withFilters(new Filter()
                                                                        .withName("resource-id")
                                                                        .withValues(volumeId))
                                          ).getTags();
        assertEquals(0, tagDescriptions.size());

        // Describe volume attribute
        DescribeVolumeAttributeResult describeVolumeAttribute =
                ec2.describeVolumeAttribute(new DescribeVolumeAttributeRequest()
                                                    .withAttribute("autoEnableIO")
                                                    .withVolumeId(volumeId));
        assertEquals(volumeId, describeVolumeAttribute.getVolumeId());
        assertFalse(describeVolumeAttribute.getAutoEnableIO());

        // Describe volume status by filter
        DescribeVolumeStatusResult describeVolumeStatus =
                ec2.describeVolumeStatus(new DescribeVolumeStatusRequest()
                                                 .withFilters(new Filter("volume-id").withValues(volumeId)));
        assertEquals(1, describeVolumeStatus.getVolumeStatuses().size());
        VolumeStatusItem volumeStatusItem = describeVolumeStatus.getVolumeStatuses().get(0);
        assertNotNull(volumeStatusItem);
        assertNotNull(volumeStatusItem.getActions());
        assertNotNull(volumeStatusItem.getAvailabilityZone());
        assertNotNull(volumeStatusItem.getEvents());
        assertNotNull(volumeStatusItem.getVolumeStatus());
        assertEquals(volumeId, volumeStatusItem.getVolumeId());

        // Describe volume status by id
        describeVolumeStatus = ec2.describeVolumeStatus(new DescribeVolumeStatusRequest()
                                                                .withVolumeIds(volumeId));
        assertEquals(1, describeVolumeStatus.getVolumeStatuses().size());
        volumeStatusItem = describeVolumeStatus.getVolumeStatuses().get(0);
        assertNotNull(volumeStatusItem);
        assertNotNull(volumeStatusItem.getActions());
        assertNotNull(volumeStatusItem.getAvailabilityZone());
        assertNotNull(volumeStatusItem.getEvents());
        assertNotNull(volumeStatusItem.getVolumeStatus());
        assertEquals(volumeId, volumeStatusItem.getVolumeId());

        // Modify volume attribute
        ec2.modifyVolumeAttribute(new ModifyVolumeAttributeRequest()
                                          .withVolumeId(volumeId)
                                          .withAutoEnableIO(true));
        describeVolumeAttribute = ec2.describeVolumeAttribute(
                new DescribeVolumeAttributeRequest()
                        .withAttribute("autoEnableIO")
                        .withVolumeId(volumeId));
        assertEquals(volumeId, describeVolumeAttribute.getVolumeId());
        assertEquals(true, describeVolumeAttribute.getAutoEnableIO());

        ec2.modifyVolumeAttribute(new ModifyVolumeAttributeRequest()
                                          .withVolumeId(volumeId)
                                          .withAutoEnableIO(false));
        describeVolumeAttribute = ec2.describeVolumeAttribute(new DescribeVolumeAttributeRequest()
                                                                      .withAttribute("autoEnableIO")
                                                                      .withVolumeId(volumeId));
        assertEquals(volumeId, describeVolumeAttribute.getVolumeId());
        assertFalse(describeVolumeAttribute.getAutoEnableIO());

        // Enable volume io -- this should throw an exception because io is already enabled
        try {
            ec2.enableVolumeIO(new EnableVolumeIORequest()
                                       .withVolumeId(volumeId));
            fail("Expected an exception");
        } catch (AmazonClientException expected) {
            assertTrue(expected.getMessage().contains("already has IO enabled"));
        }
    }

    private void assertTagDescriptionsValid(List<TagDescription> tags) {
        for (TagDescription tag : tags) {
            assertNotNull(tag.getKey());
            assertNotNull(tag.getValue());
            assertNotNull(tag.getResourceId());
            assertNotNull(tag.getResourceType());
        }
    }

}
