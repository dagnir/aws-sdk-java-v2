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

package software.amazon.awssdk.services.s3.model.transform;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.BucketNotificationConfiguration;
import software.amazon.awssdk.services.s3.model.CloudFunctionConfiguration;
import software.amazon.awssdk.services.s3.model.FilterRule;
import software.amazon.awssdk.services.s3.model.LambdaConfiguration;
import software.amazon.awssdk.services.s3.model.QueueConfiguration;
import software.amazon.awssdk.services.s3.model.TopicConfiguration;

public class BucketNotificationConfigurationStaxUnmarshallerTest {

    private static final String TOPIC_INPUT = "TopicConfiguration.xml";
    private static final String QUEUE_INPUT = "QueueConfiguration.xml";
    private static final String LAMBDA_INPUT = "LambdaConfiguration.xml";
    private static final String CLOUD_FUNCTION_INPUT = "CloudFunctionConfiguration.xml";

    private final BucketNotificationConfigurationStaxUnmarshaller unmarshaller = BucketNotificationConfigurationStaxUnmarshaller
            .getInstance();

    @Test
    public void unmarshall_ValidTopicConfiguration() throws Exception {
        BucketNotificationConfiguration config = unmarshaller.unmarshall(getResource(TOPIC_INPUT));

        TopicConfiguration topicConfig = (TopicConfiguration) config.getConfigurationByName("TopicConfigId");
        assertEquals("some-topic-arn", topicConfig.getTopicARN());
        assertEventsUnmarshalledCorrectly(topicConfig.getEvents());
        assertFilterRulesUnmarshalledCorrectly(topicConfig.getFilter().getS3KeyFilter().getFilterRules());
    }

    @Test
    public void unmarshall_ValidQueueConfiguration() throws Exception {
        BucketNotificationConfiguration config = unmarshaller.unmarshall(getResource(QUEUE_INPUT));

        QueueConfiguration queueConfig = (QueueConfiguration) config.getConfigurationByName("QueueConfigId");
        assertEquals("some-queue-arn", queueConfig.getQueueARN());
        assertEventsUnmarshalledCorrectly(queueConfig.getEvents());
        assertFilterRulesUnmarshalledCorrectly(queueConfig.getFilter().getS3KeyFilter().getFilterRules());
    }

    @Test
    public void unmarshall_ValidLambdaConfiguration() throws Exception {
        BucketNotificationConfiguration config = unmarshaller.unmarshall(getResource(LAMBDA_INPUT));

        LambdaConfiguration lambdaConfig = (LambdaConfiguration) config.getConfigurationByName("LambdaConfigId");
        assertEquals("some-lambda-function-arn", lambdaConfig.getFunctionARN());
        assertEventsUnmarshalledCorrectly(lambdaConfig.getEvents());
        assertFilterRulesUnmarshalledCorrectly(lambdaConfig.getFilter().getS3KeyFilter().getFilterRules());
    }

    /**
     * If CloudFunctionConfiguration has the InvocationRole attribute it should be unmarshalled into
     * a {@link CloudFunctionConfiguration}
     */
    @Test
    public void unmarshall_ValidCloudFunctionConfiguration() throws Exception {
        BucketNotificationConfiguration config = unmarshaller.unmarshall(getResource(CLOUD_FUNCTION_INPUT));

        CloudFunctionConfiguration cloudFunctionConfig = (CloudFunctionConfiguration) config
                .getConfigurationByName("CloudFunctionConfigId");
        assertEquals("some-cloud-function-arn", cloudFunctionConfig.getCloudFunctionARN());
        assertEquals("some-cloud-invoc-role", cloudFunctionConfig.getInvocationRoleARN());
        assertEventsUnmarshalledCorrectly(cloudFunctionConfig.getEvents());
        assertFilterRulesUnmarshalledCorrectly(cloudFunctionConfig.getFilter().getS3KeyFilter().getFilterRules());
    }

    private InputStream getResource(String inputFile) {
        return getClass().getResourceAsStream(inputFile);
    }

    private void assertEventsUnmarshalledCorrectly(Set<String> events) {
        assertThat(events, containsInAnyOrder("SomeEvent1", "SomeEvent2"));
    }

    private void assertFilterRulesUnmarshalledCorrectly(List<FilterRule> rules) {
        final FilterRule ruleOne = rules.get(0);
        assertThat(ruleOne.getName(), equalTo("Prefix"));
        assertThat(ruleOne.getValue(), equalTo("some-prefix"));

        final FilterRule ruleTwo = rules.get(1);
        assertThat(ruleTwo.getName(), equalTo("Suffix"));
        assertThat(ruleTwo.getValue(), equalTo("some-suffix"));
    }
}
