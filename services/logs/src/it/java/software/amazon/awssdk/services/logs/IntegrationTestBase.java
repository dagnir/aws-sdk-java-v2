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

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import software.amazon.awssdk.services.logs.model.DescribeLogGroupsRequest;
import software.amazon.awssdk.services.logs.model.DescribeLogGroupsResult;
import software.amazon.awssdk.services.logs.model.DescribeLogStreamsRequest;
import software.amazon.awssdk.services.logs.model.DescribeLogStreamsResult;
import software.amazon.awssdk.services.logs.model.DescribeMetricFiltersRequest;
import software.amazon.awssdk.services.logs.model.DescribeMetricFiltersResult;
import software.amazon.awssdk.services.logs.model.LogGroup;
import software.amazon.awssdk.services.logs.model.LogStream;
import software.amazon.awssdk.services.logs.model.MetricFilter;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

/**
 * Base class for CloudWatch Logs integration tests.
 */
public abstract class IntegrationTestBase extends AWSIntegrationTestBase {

    /** Shared CloudWatch Logs client for all tests to use */
    protected static AWSLogsClient awsLogs;

    /**
     * Loads the AWS account info for the integration tests and creates an CloudWatch Logs client
     * for tests to use.
     */
    @BeforeClass
    public static void setupFixture() throws FileNotFoundException, IOException {
        awsLogs = new AWSLogsClient(getCredentials());
    }

    /*
     * Test helper functions
     */

    /**
     * @return The LogGroup object included in the DescribeLogGroups response, or null if such group
     *         is not found.
     */
    protected static LogGroup findLogGroupByName(final AWSLogs awsLogs, final String groupName) {
        String nextToken = null;

        do {
            DescribeLogGroupsResult result = awsLogs
                    .describeLogGroups(new DescribeLogGroupsRequest().withNextToken(nextToken));

            for (LogGroup group : result.getLogGroups()) {
                if (group.getLogGroupName().equals(groupName)) {
                    return group;
                }
            }
            nextToken = result.getNextToken();
        } while (nextToken != null);

        return null;
    }

    /**
     * @return The LogStream object included in the DescribeLogStreams response, or null if such
     *         stream is not found in the specified group.
     */
    protected static LogStream findLogStreamByName(final AWSLogs awsLogs,
                                                   final String logGroupName,
                                                   final String logStreamName) {
        String nextToken = null;

        do {
            DescribeLogStreamsResult result = awsLogs
                    .describeLogStreams(new DescribeLogStreamsRequest(logGroupName).withNextToken(nextToken));

            for (LogStream stream : result.getLogStreams()) {
                if (stream.getLogStreamName().equals(logStreamName)) {
                    return stream;
                }
            }
            nextToken = result.getNextToken();
        } while (nextToken != null);

        return null;
    }

    /**
     * @return The MetricFilter object included in the DescribeMetricFilters response, or null if
     *         such filter is not found in the specified group.
     */
    protected static MetricFilter findMetricFilterByName(final AWSLogs awsLogs,
                                                         final String logGroupName,
                                                         final String filterName) {
        String nextToken = null;

        do {
            DescribeMetricFiltersResult result = awsLogs
                    .describeMetricFilters(new DescribeMetricFiltersRequest(logGroupName).withNextToken(nextToken));

            for (MetricFilter mf : result.getMetricFilters()) {
                if (mf.getFilterName().equals(filterName)) {
                    return mf;
                }
            }
            nextToken = result.getNextToken();
        } while (nextToken != null);

        return null;
    }
}
