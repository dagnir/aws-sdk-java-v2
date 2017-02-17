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

package software.amazon.awssdk.services.cloudwatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.SDKGlobalConfiguration;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.DeleteAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmHistoryRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmHistoryResult;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsForMetricRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsForMetricResult;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsResult;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.DisableAlarmActionsRequest;
import software.amazon.awssdk.services.cloudwatch.model.EnableAlarmActionsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResult;
import software.amazon.awssdk.services.cloudwatch.model.HistoryItemType;
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsResult;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricAlarmRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.SetAlarmStateRequest;
import software.amazon.awssdk.test.AwsIntegrationTestBase;
import software.amazon.awssdk.test.util.SdkAsserts;

/**
 * Integration tests for the AWS CloudWatch service.
 */
public class CloudWatchIntegrationTest extends AwsIntegrationTestBase {

    private static final int ONE_WEEK_IN_MILLISECONDS = 1000 * 60 * 60 * 24 * 7;
    private static final int ONE_HOUR_IN_MILLISECONDS = 1000 * 60 * 60;
    /** The CloudWatch client for all tests to use. */
    private static AmazonCloudWatch cloudwatch;

    /**
     * Loads the AWS account info for the integration tests and creates a
     * CloudWatch client for tests to use.
     */
    @BeforeClass
    public static void setUp() throws IOException {
        cloudwatch = new AmazonCloudWatchClient(getCredentials());
    }

    /**
     * Cleans up any existing alarms before and after running the test suite
     */
    @AfterClass
    public static void cleanupAlarms() {
        if (cloudwatch != null) {
            DescribeAlarmsResult describeResult = cloudwatch.describeAlarms(new DescribeAlarmsRequest());
            Collection<String> toDelete = new LinkedList<String>();
            for (MetricAlarm alarm : describeResult.getMetricAlarms()) {
                if (alarm.getMetricName().startsWith(CloudWatchIntegrationTest.class.getName())) {
                    toDelete.add(alarm.getAlarmName());
                }
            }
            if (!toDelete.isEmpty()) {
                DeleteAlarmsRequest delete = new DeleteAlarmsRequest().withAlarmNames(toDelete);
                cloudwatch.deleteAlarms(delete);
            }
        }
    }


    /**
     * Tests putting metrics and then getting them back.
     */

    @Test
    public void put_get_metricdata_list_metric_returns_success() throws
                                                                 InterruptedException {
        String measureName = this.getClass().getName() + System.currentTimeMillis();

        MetricDatum datum = new MetricDatum().withDimensions(
                new Dimension().withName("InstanceType").withValue("m1.small"))
                                             .withMetricName(measureName).withTimestamp(new Date())
                                             .withUnit("Count").withValue(42.0);

        cloudwatch.putMetricData(new PutMetricDataRequest()
                                         .withNamespace("AWS.EC2").withMetricData(datum));

        // TODO: get an ETA on the arrival of this data
        Thread.sleep(60 * 1000);

        GetMetricStatisticsRequest getRequest = new GetMetricStatisticsRequest()
                .withStartTime(
                        new Date(new Date().getTime()
                                 - ONE_WEEK_IN_MILLISECONDS))
                .withNamespace("AWS.EC2")
                .withPeriod(60 * 60)
                .withDimensions(new Dimension().withName("InstanceType").withValue("m1.small"))
                .withMetricName(measureName)
                .withStatistics("Average", "Maximum", "Minimum", "Sum")
                .withEndTime(new Date());
        GetMetricStatisticsResult result = cloudwatch
                .getMetricStatistics(getRequest);

        assertNotNull(result.getLabel());
        assertEquals(measureName, result.getLabel());

        assertEquals(1, result.getDatapoints().size());
        for (Datapoint datapoint : result.getDatapoints()) {
            assertEquals(datum.getValue(), datapoint.getAverage());
            assertEquals(datum.getValue(), datapoint.getMaximum());
            assertEquals(datum.getValue(), datapoint.getMinimum());
            assertEquals(datum.getValue(), datapoint.getSum());
            assertNotNull(datapoint.getTimestamp());
            assertEquals(datum.getUnit(), datapoint.getUnit());
        }

        ListMetricsResult listResult = cloudwatch.listMetrics();

        boolean seenDimensions = false;
        assertTrue(listResult.getMetrics().size() > 0);
        for (Metric metric : listResult.getMetrics()) {
            assertNotNull(metric.getMetricName());
            assertNotNull(metric.getNamespace());

            for (Dimension dimension : metric.getDimensions()) {
                seenDimensions = true;
                assertNotNull(dimension.getName());
                assertNotNull(dimension.getValue());
            }
        }
        assertTrue(seenDimensions);
    }


    /**
     * Tests handling a "request too large" error. This breaks our parser right
     * now and is therefore disabled.
     */

    @Test
    public void put_metric_large_data_throws_request_entity_large_exception()
            throws Exception {
        String measureName = this.getClass().getName() + System.currentTimeMillis();
        long now = System.currentTimeMillis();
        double value = 42.0;

        Collection<MetricDatum> data = new LinkedList<MetricDatum>();
        for (int i = ONE_WEEK_IN_MILLISECONDS; i >= 0; i -= ONE_HOUR_IN_MILLISECONDS) {
            long time = now - i;
            MetricDatum datum = new MetricDatum().withDimensions(
                    new Dimension().withName("InstanceType").withValue("m1.small"))
                                                 .withMetricName(measureName).withTimestamp(new Date(time))
                                                 .withUnit("Count").withValue(value);
            data.add(datum);
        }

        try {
            cloudwatch.putMetricData(new PutMetricDataRequest().withNamespace(
                    "AWS/EC2").withMetricData(data));
            fail("Expected an error");
        } catch (AmazonServiceException e) {
            assertTrue(413 == e.getStatusCode());
        }
    }

    /**
     * Tests setting the state for an alarm and reading its history.
     */
    @Test
    public void describe_alarms_returns_values_set() {
        String metricName = this.getClass().getName()
                            + System.currentTimeMillis();

        PutMetricAlarmRequest[] rqs = createTwoNewAlarms(metricName);

        PutMetricAlarmRequest rq1 = rqs[0];
        PutMetricAlarmRequest rq2 = rqs[1];

        /*
         * Set the state
         */
        SetAlarmStateRequest setAlarmStateRequest = new SetAlarmStateRequest()
                .withAlarmName(rq1.getAlarmName()).withStateValue("ALARM")
                .withStateReason("manual");
        cloudwatch.setAlarmState(setAlarmStateRequest);
        setAlarmStateRequest = new SetAlarmStateRequest().withAlarmName(
                rq2.getAlarmName()).withStateValue("ALARM").withStateReason(
                "manual");
        cloudwatch.setAlarmState(setAlarmStateRequest);

        DescribeAlarmsForMetricResult describeResult = cloudwatch
                .describeAlarmsForMetric(new DescribeAlarmsForMetricRequest()
                                                 .withDimensions(rq1.getDimensions()).withMetricName(
                                metricName).withNamespace(rq1.getNamespace()));
        assertEquals(2, describeResult.getMetricAlarms().size());
        for (MetricAlarm alarm : describeResult.getMetricAlarms()) {
            assertTrue(rq1.getAlarmName().equals(alarm.getAlarmName())
                       || rq2.getAlarmName().equals(alarm.getAlarmName()));
            assertEquals(setAlarmStateRequest.getStateValue(), alarm
                    .getStateValue());
            assertEquals(setAlarmStateRequest.getStateReason(), alarm
                    .getStateReason());
        }

        /*
         * Get the history
         */
        DescribeAlarmHistoryRequest alarmHistoryRequest = new DescribeAlarmHistoryRequest()
                .withAlarmName(rq1.getAlarmName()).withHistoryItemType(HistoryItemType.StateUpdate);
        DescribeAlarmHistoryResult historyResult = cloudwatch
                .describeAlarmHistory(alarmHistoryRequest);
        assertEquals(1, historyResult.getAlarmHistoryItems().size());
    }

    /**
     * Tests disabling and enabling alarm actions
     */
    @Test
    public void disable_enable_alarms_returns_success() {
        String metricName = this.getClass().getName()
                            + System.currentTimeMillis();

        PutMetricAlarmRequest[] rqs = createTwoNewAlarms(metricName);

        PutMetricAlarmRequest rq1 = rqs[0];
        PutMetricAlarmRequest rq2 = rqs[1];

        /*
         * Disable
         */
        DisableAlarmActionsRequest disable = new DisableAlarmActionsRequest()
                .withAlarmNames(rq1.getAlarmName(), rq2.getAlarmName());
        cloudwatch.disableAlarmActions(disable);

        DescribeAlarmsForMetricResult describeResult = cloudwatch
                .describeAlarmsForMetric(new DescribeAlarmsForMetricRequest()
                                                 .withDimensions(rq1.getDimensions()).withMetricName(
                                metricName).withNamespace(rq1.getNamespace()));
        assertEquals(2, describeResult.getMetricAlarms().size());
        for (MetricAlarm alarm : describeResult.getMetricAlarms()) {
            assertTrue(rq1.getAlarmName().equals(alarm.getAlarmName())
                       || rq2.getAlarmName().equals(alarm.getAlarmName()));
            assertFalse(alarm.isActionsEnabled());
        }

        /*
         * Enable
         */
        EnableAlarmActionsRequest enable = new EnableAlarmActionsRequest()
                .withAlarmNames(rq1.getAlarmName(), rq2.getAlarmName());
        cloudwatch.enableAlarmActions(enable);

        describeResult = cloudwatch
                .describeAlarmsForMetric(new DescribeAlarmsForMetricRequest()
                                                 .withDimensions(rq1.getDimensions()).withMetricName(
                                metricName).withNamespace(rq1.getNamespace()));
        assertEquals(2, describeResult.getMetricAlarms().size());
        for (MetricAlarm alarm : describeResult.getMetricAlarms()) {
            assertTrue(rq1.getAlarmName().equals(alarm.getAlarmName())
                       || rq2.getAlarmName().equals(alarm.getAlarmName()));
            assertTrue(alarm.isActionsEnabled());
        }
    }

    /**
     * Creates two alarms on the metric name given and returns the two requests
     * as an array.
     */
    private PutMetricAlarmRequest[] createTwoNewAlarms(String metricName) {
        PutMetricAlarmRequest[] rqs = new PutMetricAlarmRequest[2];

        /*
         * Put two metric alarms
         */
        rqs[0] = new PutMetricAlarmRequest().withActionsEnabled(true)
                                            .withAlarmDescription("Some alarm description").withAlarmName(
                        "An Alarm Name" + metricName).withComparisonOperator(
                        "GreaterThanThreshold").withDimensions(
                        new Dimension().withName("InstanceType").withValue(
                                "m1.small")).withEvaluationPeriods(1)
                                            .withMetricName(metricName).withNamespace("AWS/EC2")
                                            .withPeriod(60).withStatistic("Average").withThreshold(1.0)
                                            .withUnit("Count");

        cloudwatch.putMetricAlarm(rqs[0]);

        rqs[1] = new PutMetricAlarmRequest().withActionsEnabled(true)
                                            .withAlarmDescription("Some alarm description 2")
                                            .withAlarmName("An Alarm Name 2" + metricName)
                                            .withComparisonOperator("GreaterThanThreshold").withDimensions(
                        new Dimension().withName("InstanceType").withValue(
                                "m1.small")).withEvaluationPeriods(1)
                                            .withMetricName(metricName).withNamespace("AWS/EC2")
                                            .withPeriod(60).withStatistic("Average").withThreshold(2.0)
                                            .withUnit("Count");
        cloudwatch.putMetricAlarm(rqs[1]);
        return rqs;
    }

    /**
     * Tests that an error response from CloudWatch is correctly unmarshalled
     * into an AmazonServiceException object.
     */
    @Test
    public void testExceptionHandling() throws Exception {
        try {
            cloudwatch.getMetricStatistics(new GetMetricStatisticsRequest()
                                                   .withNamespace("fake-namespace"));
            fail("Expected an AmazonServiceException, but wasn't thrown");
        } catch (AmazonServiceException e) {
            SdkAsserts.assertValidException(e);
        }
    }

    /**
     * In the following test, we purposely setting the time offset to trigger a clock skew error.
     * The time offset must be fixed and then we validate the global value for time offset has been
     * update.
     */
    @Test
    public void testClockSkew() {
        SDKGlobalConfiguration.setGlobalTimeOffset(3600);

        AmazonCloudWatch cloudwatch = new AmazonCloudWatchClient(getCredentials());
        cloudwatch.listMetrics();
        assertTrue(SDKGlobalConfiguration.getGlobalTimeOffset() < 3600);
        // subsequent changes to the global time offset won't affect existing client
        SDKGlobalConfiguration.setGlobalTimeOffset(3600);
        cloudwatch.listMetrics();
        assertTrue(SDKGlobalConfiguration.getGlobalTimeOffset() == 3600);
    }
}
