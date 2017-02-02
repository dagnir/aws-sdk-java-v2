/*
* Copyright 2013-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License").
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
* http://aws.amazon.com/apache2.0
*
* or in the "license" file accompanying this file. This file is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package software.amazon.awssdk.services.logs;

import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.logs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.logs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.logs.model.DeleteLogGroupRequest;
import software.amazon.awssdk.services.logs.model.DeleteLogStreamRequest;
import software.amazon.awssdk.services.logs.model.DeleteMetricFilterRequest;
import software.amazon.awssdk.services.logs.model.DeleteRetentionPolicyRequest;
import software.amazon.awssdk.services.logs.model.GetLogEventsRequest;
import software.amazon.awssdk.services.logs.model.GetLogEventsResult;
import software.amazon.awssdk.services.logs.model.InputLogEvent;
import software.amazon.awssdk.services.logs.model.InvalidSequenceTokenException;
import software.amazon.awssdk.services.logs.model.LogGroup;
import software.amazon.awssdk.services.logs.model.LogStream;
import software.amazon.awssdk.services.logs.model.MetricFilter;
import software.amazon.awssdk.services.logs.model.MetricFilterMatchRecord;
import software.amazon.awssdk.services.logs.model.MetricTransformation;
import software.amazon.awssdk.services.logs.model.OutputLogEvent;
import software.amazon.awssdk.services.logs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.logs.model.PutLogEventsResult;
import software.amazon.awssdk.services.logs.model.PutMetricFilterRequest;
import software.amazon.awssdk.services.logs.model.PutRetentionPolicyRequest;
import software.amazon.awssdk.services.logs.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.logs.model.TestMetricFilterRequest;
import software.amazon.awssdk.services.logs.model.TestMetricFilterResult;

/**
 * Integration tests for the CloudWatch Logs service.
 */
public class ServiceIntegrationTest extends IntegrationTestBase {

    /* Components of the message body */
    private static final long LOG_MESSAGE_TIMESTAMP = System.currentTimeMillis();
    private static final String LOG_MESSAGE_PREFIX = "java-integ-test";
    private static final String LOG_MESSAGE_CONTENT = "boom";

    /* The log message body and the pattern that we use to filter out such message */
    private static final String LOG_MESSAGE = String.format("%s [%d] %s", LOG_MESSAGE_PREFIX, LOG_MESSAGE_TIMESTAMP,
            LOG_MESSAGE_CONTENT);
    private static final String LOG_METRIC_FILTER_PATTERN = "[prefix=java-integ-test, timestamp, content]";

    private static final String CLOUDWATCH_METRIC_NAME = "java-integ-test-transformed-metric-name";
    private static final String CLOUDWATCH_METRIC_NAMESPACE = "java-integ-test-transformed-metric-namespace";

    private static final int LOG_RETENTION_DAYS = 5;

    private final String nameSuffix = String.valueOf(System.currentTimeMillis());
    private final String logGroupName = "java-integ-test-log-group-name-" + nameSuffix;
    private final String logStreamName = "java-integ-test-log-stream-name-" + nameSuffix;
    private final String logMetricFilterName = "java-integ-test-log-metric-filter-" + nameSuffix;

    @Before
    public void setup() throws FileNotFoundException, IOException {
        testCreateLogGroup(logGroupName);
        testCreateLogStream(logGroupName, logStreamName);
        testCreateMetricFilter(logGroupName, logMetricFilterName);
    }

    @After
    public void tearDown() {
        try {
            awsLogs.deleteLogStream(new DeleteLogStreamRequest(logGroupName, logStreamName));
        } catch (AmazonServiceException ase) {
            System.err.println("Unable to delete log stream " + logStreamName);
        }
        try {
            awsLogs.deleteMetricFilter(new DeleteMetricFilterRequest(logGroupName, logMetricFilterName));
        } catch (AmazonServiceException ase) {
            System.err.println("Unable to delete metric filter " + logMetricFilterName);
        }
        try {
            awsLogs.deleteLogGroup(new DeleteLogGroupRequest(logGroupName));
        } catch (AmazonServiceException ase) {
            System.err.println("Unable to delete log group " + logGroupName);
        }
    }

    /**
     * Test uploading and retrieving log events.
     */
    @Test
    public void testEventsLogging() {
        // No log event is expected in the newly created log stream
        GetLogEventsResult getResult = awsLogs.getLogEvents(new GetLogEventsRequest(logGroupName, logStreamName));
        Assert.assertTrue(getResult.getEvents().isEmpty());

        // Insert a new log event
        PutLogEventsResult putResult = awsLogs.putLogEvents(new PutLogEventsRequest(logGroupName, logStreamName,
                Arrays.asList(new InputLogEvent().withMessage(LOG_MESSAGE).withTimestamp(LOG_MESSAGE_TIMESTAMP))));

        Assert.assertNotNull(putResult.getNextSequenceToken());

        // The new log event is not instantly available in GetLogEvents operation.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        }

        // Pull the event from the log stream
        getResult = awsLogs.getLogEvents(new GetLogEventsRequest(logGroupName, logStreamName));
        Assert.assertEquals(1, getResult.getEvents().size());
        Assert.assertNotNull(getResult.getNextBackwardToken());
        Assert.assertNotNull(getResult.getNextForwardToken());

        OutputLogEvent event = getResult.getEvents().get(0);
        Assert.assertEquals(LOG_MESSAGE, event.getMessage());
        Assert.assertEquals(LOG_MESSAGE_TIMESTAMP, event.getTimestamp().longValue());
        Assert.assertTrue(event.getIngestionTime() > event.getTimestamp());

        // Use DescribeLogStreams API to verify that the new log event has
        // updated the following parameters of the log stream.
        final LogStream stream = findLogStreamByName(awsLogs, logGroupName, logStreamName);
        Assert.assertEquals(LOG_MESSAGE_TIMESTAMP, stream.getFirstEventTimestamp().longValue());
        Assert.assertEquals(LOG_MESSAGE_TIMESTAMP, stream.getLastEventTimestamp().longValue());
        Assert.assertNotNull(stream.getLastIngestionTime());
    }

    /**
     * Use the TestMetricFilter API to verify the correctness of the metric filter pattern we have
     * been using in this integration test.
     */
    @Test
    public void testMetricFilter() {
        TestMetricFilterResult testResult = awsLogs.testMetricFilter(
                new TestMetricFilterRequest().withFilterPattern(LOG_METRIC_FILTER_PATTERN).withLogEventMessages(
                        LOG_MESSAGE, "Another message with some content that does not match the filter pattern..."));

        Assert.assertEquals(1, testResult.getMatches().size());

        MetricFilterMatchRecord match = testResult.getMatches().get(0);
        // Event numbers starts from 1
        Assert.assertEquals(1, match.getEventNumber().longValue());
        Assert.assertEquals(LOG_MESSAGE, match.getEventMessage());

        // Verify the extracted values
        Map<String, String> extractedValues = match.getExtractedValues();
        Assert.assertEquals(3, extractedValues.size());
        Assert.assertEquals(LOG_MESSAGE_PREFIX, extractedValues.get("$prefix"));
        Assert.assertEquals(LOG_MESSAGE_TIMESTAMP, Long.parseLong(extractedValues.get("$timestamp")));
        Assert.assertEquals(LOG_MESSAGE_CONTENT, extractedValues.get("$content"));
    }

    /**
     * Tests that we have deserialized the exception response correctly. See TT0064111680
     */
    @Test
    public void putLogEvents_InvalidSequenceNumber_HasExpectedSequenceNumberInException() {
        // First call to PutLogEvents does not need a sequence number, subsequent calls do
        awsLogs.putLogEvents(new PutLogEventsRequest().withLogGroupName(logGroupName).withLogStreamName(logStreamName)
                .withLogEvents(new InputLogEvent().withMessage(LOG_MESSAGE).withTimestamp(LOG_MESSAGE_TIMESTAMP)));
        try {
            // This call requires a sequence number, if we provide an invalid one the service should
            // throw an exception with the expected sequence number
            awsLogs.putLogEvents(
                    new PutLogEventsRequest().withLogGroupName(logGroupName).withLogStreamName(logStreamName)
                            .withLogEvents(
                                    new InputLogEvent().withMessage(LOG_MESSAGE).withTimestamp(LOG_MESSAGE_TIMESTAMP))
                    .withSequenceToken("invalid"));
        } catch (InvalidSequenceTokenException e) {
            assertNotNull(e.getExpectedSequenceToken());
        }
    }

    @Test
    public void testRetentionPolicy() {
        awsLogs.putRetentionPolicy(new PutRetentionPolicyRequest(logGroupName, LOG_RETENTION_DAYS));

        // Use DescribeLogGroup to verify the updated retention policy
        LogGroup group = findLogGroupByName(awsLogs, logGroupName);
        Assert.assertNotNull(group);
        Assert.assertEquals(LOG_RETENTION_DAYS, group.getRetentionInDays().intValue());

        awsLogs.deleteRetentionPolicy(new DeleteRetentionPolicyRequest(logGroupName));

        // Again, use DescribeLogGroup to verify that the retention policy has been deleted
        group = findLogGroupByName(awsLogs, logGroupName);
        Assert.assertNotNull(group);
        Assert.assertNull(group.getRetentionInDays());
    }

    /**
     * Test creating a log group using the specified group name.
     */
    public static void testCreateLogGroup(final String groupName) {
        awsLogs.createLogGroup(new CreateLogGroupRequest(groupName));

        try {
            awsLogs.createLogGroup(new CreateLogGroupRequest(groupName));
            Assert.fail("ResourceAlreadyExistsException is expected.");
        } catch (ResourceAlreadyExistsException expected) {
        }

        final LogGroup createdGroup = findLogGroupByName(awsLogs, groupName);

        Assert.assertNotNull(String.format("Log group [%s] is not found in the DescribeLogGroups response.", groupName),
                createdGroup);

        Assert.assertEquals(groupName, createdGroup.getLogGroupName());
        Assert.assertNotNull(createdGroup.getCreationTime());
        Assert.assertNotNull(createdGroup.getArn());

        /* The log group should have no filter and no stored bytes */
        Assert.assertEquals(0, createdGroup.getMetricFilterCount().intValue());
        Assert.assertEquals(0, createdGroup.getStoredBytes().longValue());

        /* Retention policy is still unspecified */
        Assert.assertNull(createdGroup.getRetentionInDays());

    }

    /**
     * Test creating a log stream for the specified group.
     */
    public static void testCreateLogStream(final String groupName, final String logStreamName) {
        awsLogs.createLogStream(new CreateLogStreamRequest(groupName, logStreamName));

        try {
            awsLogs.createLogStream(new CreateLogStreamRequest(groupName, logStreamName));
            Assert.fail("ResourceAlreadyExistsException is expected.");
        } catch (ResourceAlreadyExistsException expected) {
        }

        final LogStream createdStream = findLogStreamByName(awsLogs, groupName, logStreamName);

        Assert.assertNotNull(
                String.format("Log stream [%s] is not found in the [%s] log group.", logStreamName, groupName),
                createdStream);

        Assert.assertEquals(logStreamName, createdStream.getLogStreamName());
        Assert.assertNotNull(createdStream.getCreationTime());
        Assert.assertNotNull(createdStream.getArn());

        /* The log stream should have no stored bytes */
        Assert.assertEquals(0, createdStream.getStoredBytes().longValue());

        /* No log event is pushed yet */
        Assert.assertNull(createdStream.getFirstEventTimestamp());
        Assert.assertNull(createdStream.getLastEventTimestamp());
        Assert.assertNull(createdStream.getLastIngestionTime());
    }

    /**
     * Test creating a log metric filter for the specified group.
     */
    public void testCreateMetricFilter(final String groupName, final String filterName) {
        awsLogs.putMetricFilter(new PutMetricFilterRequest(groupName, filterName, LOG_METRIC_FILTER_PATTERN,
                Arrays.asList(new MetricTransformation().withMetricName(CLOUDWATCH_METRIC_NAME)
                        .withMetricNamespace(CLOUDWATCH_METRIC_NAMESPACE).withMetricValue("$content"))));

        final MetricFilter mf = findMetricFilterByName(awsLogs, groupName, filterName);

        Assert.assertNotNull(
                String.format("Metric filter [%s] is not found in the [%s] log group.", filterName, groupName), mf);

        Assert.assertEquals(filterName, mf.getFilterName());
        Assert.assertEquals(LOG_METRIC_FILTER_PATTERN, mf.getFilterPattern());
        Assert.assertNotNull(mf.getCreationTime());
        Assert.assertNotNull(mf.getMetricTransformations());

        // Use DescribeLogGroups to verify that LogGroup.metricFilterCount is updated
        final LogGroup group = findLogGroupByName(awsLogs, logGroupName);
        Assert.assertEquals(1, group.getMetricFilterCount().intValue());
    }

}
