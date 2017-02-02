package software.amazon.awssdk.services.ec2;

import software.amazon.awssdk.test.AWSIntegrationTestBase;
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

/**
 * Base class for EC2 integration tests; responsible for loading AWS account
 * info for running the tests, and instantiating EC2 clients for tests to use.
 *
 * @author fulghum@amazon.com
 */
public abstract class EC2IntegrationTestBase extends AWSIntegrationTestBase {
    protected static final Log log = LogFactory.getLog(EC2IntegrationTestBase.class);

    /** Shared EC2 client for all tests to use */
    protected static AmazonEC2Client ec2;

    /** Shared EC2 async client for tests to use */
    protected static AmazonEC2AsyncClient ec2Async;

    /** The default tags to test with */
    protected static final List<Tag> TAGS = Arrays.asList(new Tag("foo", "bar"), new Tag("baz", ""));

    protected static final String AMI_ID     = "ami-7f418316";
    protected static final String KERNEL_ID  = "aki-a71cf9ce";
    protected static final String RAMDISK_ID = "ari-a51cf9cc";


    /**
     * Loads the AWS account info for the integration tests and creates an EC2
     * client for tests to use.
     *
     * @throws IOException
     */
    @BeforeClass
    public static void setupClients() throws IOException {
        ec2 = new AmazonEC2Client(getCredentials());
        ec2Async = new AmazonEC2AsyncClient(getCredentials());
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
                instance = ec2 .describeInstances(
                        new DescribeInstancesRequest()
                                .withInstanceIds(instanceId)
                        ).getReservations().get(0).getInstances().get(0);

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

            if (state.toString().equalsIgnoreCase(
                    instance.getState().getName())) {
                return instance;
            } else if (InstanceStateName.Terminated.toString().equals
                    (instance.getState().getName())) {
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
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        DescribeInstancesResult result = ec2.describeInstances(request.withInstanceIds(instanceId));

        List<Reservation> reservations = result.getReservations();
        if ( reservations.isEmpty() )
            return null;
        assertEquals(1, reservations.size());

        List<Instance> instances = reservations.get(0).getInstances();
        if ( instances.isEmpty() )
            return null;
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
        ec2.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceId));
    }

    protected static void tagResource(String resourceId, List<Tag> tags) {
        ec2.createTags(new CreateTagsRequest().withResources(resourceId).withTags(tags));

        int attempts = 0;
        DescribeTagsResult tagsResult = null;
        do {
            try {
                Thread.sleep(1000);
            } catch ( Exception e ) {
            }
            tagsResult = ec2.describeTags(new DescribeTagsRequest().withFilters(new Filter().withName("resource-id")
                    .withValues(resourceId)));
        } while ( !allTagsPresent(tagsResult, tags) && attempts++ < 45 );
    }

    private static boolean allTagsPresent(DescribeTagsResult result, List<Tag> expected) {
        Map<String, String> expectedTags = convertTagListToMap(expected);

        for ( TagDescription tag : result.getTags() ) {
            if ( expectedTags.containsKey(tag.getKey()) ) {
                if ( expectedTags.get(tag.getKey()).equals(tag.getValue()) ) {
                    expectedTags.remove(tag.getKey());
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
        for ( Tag tag : tags ) {
            map.put(tag.getKey(), tag.getValue());
        }
        return map;
    }

    protected String loadResource(String resource) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copy(inputStream, outputStream);
        return outputStream.toString();
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
}
