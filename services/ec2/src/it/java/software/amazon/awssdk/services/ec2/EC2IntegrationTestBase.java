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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResult;
import software.amazon.awssdk.services.ec2.model.DescribeTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeTagsResult;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagDescription;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

/**
 * Base class for EC2 integration tests; responsible for loading AWS account
 * info for running the tests, and instantiating EC2 clients for tests to use.
 *
 * @author fulghum@amazon.com
 */
public abstract class EC2IntegrationTestBase extends AwsIntegrationTestBase {
    protected static final Log log = LogFactory.getLog(EC2IntegrationTestBase.class);
    /** The default tags to test with. */
    protected static final List<Tag> TAGS = Arrays.asList(Tag.builder().key("foo").value("bar").build(),
                                                          Tag.builder().key("baz").value("").build());
    protected static final String AMI_ID = "ami-7f418316";
    protected static final String KERNEL_ID = "aki-a71cf9ce";
    protected static final String RAMDISK_ID = "ari-a51cf9cc";
    /** Shared EC2 client for all tests to use. */
    protected static EC2Client ec2;

    /**
     * Loads the AWS account info for the integration tests and creates an EC2
     * client for tests to use.
     *
     */
    @BeforeClass
    public static void setupClients() throws IOException {
        ec2 = EC2Client.builder().credentialsProvider(new StaticCredentialsProvider(getCredentials())).build();
    }

    /**
     * Asserts that the specified date is not null, and is recent (within 24
     * hours of the current date).
     *
     * @param date
     *            The date to test.
     */
    protected static void assertRecent(Date date) {
        assertNotNull(date);

        long dateOffset = new Date().getTime() - date.getTime();
        assertTrue(dateOffset < 1000 * 60 * 60 * 24);
    }

    /**
     * Waits for the specified instance to transition to the specified state,
     * otherwise throws a RuntimeException if this method gives up on the
     * instance ever transitioning to that state.
     *
     * @param instanceId
     *            The ID of the instance to wait for.
     * @param state
     *            The expected state for the instance to transition to.
     * @throws Exception
     *             If any problems were encountered while polling the instance's
     *             state, or if this method gives up on the instance ever
     *             transitioning to the expected state.
     */
    protected static Instance waitForInstanceToTransitionToState(
            String instanceId, InstanceStateName state)
            throws InterruptedException {

        System.out.println(
                "Waiting for instance " + instanceId +
                " to transition to " + state + "...");

        int count = 0;
        while (count++ < 100) {
            Thread.sleep(1000 * 60);

            Instance instance = null;
            try {
                instance = ec2.describeInstances(DescribeInstancesRequest.builder().instanceIds(instanceId).build())
                              .reservations().get(0).instances().get(0);

            } catch (AmazonServiceException ase) {
                if ("InvalidInstanceID.NotFound".equalsIgnoreCase(ase.getErrorCode())) {
                    if (state == InstanceStateName.Running) {
                        // wait till the instance id is available
                        continue;
                    }
                    if (state == InstanceStateName.Terminated) {
                        return null;
                    }
                }
            }

            if (state.toString().equalsIgnoreCase(instance.state().name())) {
                return instance;
            } else if (InstanceStateName.Terminated.toString().equals(instance.state().name())) {
                // There are cases where the instance directly goes from
                // pending to terminated state. In such cases, this while
                // loop waits for a longer time before it could throw the
                // exception. This change would cause the while loop to break
                // soon.
                break;
            }
        }

        throw new RuntimeException("Instance " + instanceId + " never transitioned to " + state);
    }


    /**
     * Queries EC2 to describe the specified instance and returns the resulting
     * instance object.
     *
     * @param instanceId
     *            The ID of the instance to query.
     * @return An instance object for the specified instance ID.
     */
    protected static Instance describeInstance(String instanceId) {
        DescribeInstancesResult result =
                ec2.describeInstances(DescribeInstancesRequest.builder().instanceIds(instanceId).build());

        List<Reservation> reservations = result.reservations();
        if (reservations.isEmpty()) {
            return null;
        }
        assertEquals(1, reservations.size());

        List<Instance> instances = reservations.get(0).instances();
        if (instances.isEmpty()) {
            return null;
        }
        assertEquals(1, instances.size());

        return instances.get(0);
    }

    /**
     * Terminates the specified EC2 instance.
     *
     * @param instanceId
     *            The ID of the EC2 instance to terminate.
     */
    protected static void terminateInstance(String instanceId) {
        ec2.terminateInstances(TerminateInstancesRequest.builder().instanceIds(instanceId).build());
    }

    protected static void tagResource(String resourceId, List<Tag> tags) {
        ec2.createTags(CreateTagsRequest.builder().resources(resourceId).tags(tags).build());

        int attempts = 0;
        DescribeTagsResult tagsResult = null;
        do {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                // Ignored or expected.
            }
            tagsResult = ec2.describeTags(DescribeTagsRequest.builder().filters(Filter.builder()
                                                                                      .name("resource-id")
                                                                                      .values(resourceId)
                                                                                      .build())
                                                             .build());
        } while (!allTagsPresent(tagsResult, tags) && attempts++ < 45);
    }

    private static boolean allTagsPresent(DescribeTagsResult result, List<Tag> expected) {
        Map<String, String> expectedTags = convertTagListToMap(expected);

        for (TagDescription tag : result.tags()) {
            if (expectedTags.containsKey(tag.key())) {
                if (expectedTags.get(tag.key()).equals(tag.value())) {
                    expectedTags.remove(tag.key());
                } else {
                    return false;
                }
            }
        }

        return expectedTags.isEmpty();
    }

    protected static void assertEqualUnorderedTagLists(List<Tag> expected, List<Tag> actual) {
        assertEquals(convertTagListToMap(expected), convertTagListToMap(actual));
    }

    private static Map<String, String> convertTagListToMap(List<Tag> tags) {
        HashMap<String, String> map = new HashMap<String, String>();
        for (Tag tag : tags) {
            map.put(tag.key(), tag.value());
        }
        return map;
    }

    /**
     * Asserts that the specified string is non-null and non-empty.
     *
     * @param s
     *            The string to test.
     */
    protected static void assertStringNotEmpty(String s) {
        assertNotNull(s);
        assertTrue(s.length() > 1);
    }

    protected String loadResource(String resource) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copy(inputStream, outputStream);
        return outputStream.toString();
    }
}
