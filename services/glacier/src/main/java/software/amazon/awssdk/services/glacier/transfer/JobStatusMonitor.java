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

package software.amazon.awssdk.services.glacier.transfer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.policy.Policy;
import software.amazon.awssdk.auth.policy.Principal;
import software.amazon.awssdk.auth.policy.Resource;
import software.amazon.awssdk.auth.policy.Statement;
import software.amazon.awssdk.auth.policy.Statement.Effect;
import software.amazon.awssdk.auth.policy.actions.SQSActions;
import software.amazon.awssdk.auth.policy.conditions.ConditionFactory;
import software.amazon.awssdk.services.glacier.model.StatusCode;
import software.amazon.awssdk.services.sns.AmazonSNS;
import software.amazon.awssdk.services.sns.AmazonSNSClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.AmazonSQS;
import software.amazon.awssdk.services.sqs.AmazonSQSClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import software.amazon.awssdk.util.BinaryUtils;

/**
 * Utility for monitoring the status of an Amazon Glacier job, through Amazon
 * SNS/SQS.
 */
public class JobStatusMonitor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Log log = LogFactory.getLog(JobStatusMonitor.class);

    private AmazonSQS sqs;
    private AmazonSNS sns;
    private String queueUrl;
    private String topicArn;


    public JobStatusMonitor(AwsCredentialsProvider credentialsProvider, LegacyClientConfiguration clientConfiguration) {
        sqs = new AmazonSQSClient(credentialsProvider, clientConfiguration);
        sns = new AmazonSNSClient(credentialsProvider, clientConfiguration);
        setupQueueAndTopic();
    }

    /**
     * Constructs a JobStatusMonitor that will use the specified clients for
     * polling archive download job status.
     *
     * @param sqs
     *            The client for working with Amazon SQS when polling archive
     *            retrieval job status.
     * @param sns
     *            The client for working with Amazon SNS when polling archive
     *            retrieval job status.
     */
    public JobStatusMonitor(AmazonSQSClient sqs, AmazonSNSClient sns) {
        this.sqs = sqs;
        this.sns = sns;
        setupQueueAndTopic();
    }

    /**
     * Constructs a JobStatusMonitor that will use the specified clients for
     * polling archive download job status.
     *
     * @param sqs
     *            The client for working with Amazon SQS when polling archive
     *            retrieval job status.
     * @param sns
     *            The client for working with Amazon SNS when polling archive
     *            retrieval job status.
     */
    public JobStatusMonitor(AmazonSQS sqs, AmazonSNS sns) {
        this.sqs = sqs;
        this.sns = sns;
        setupQueueAndTopic();
    }

    public String getTopicArn() {
        return topicArn;
    }

    public void shutdown() {
        try {
            sqs.deleteQueue(new DeleteQueueRequest(queueUrl));
        } catch (Exception e) {
            log.warn("Unable to delete queue: " + queueUrl, e);
        }

        try {
            sns.deleteTopic(new DeleteTopicRequest(topicArn));
        } catch (Exception e) {
            log.warn("Unable to delete topic: " + topicArn, e);
        }
    }

    /** Poll the SQS queue to see if we've received a message about the job completion yet. **/
    public void waitForJobToComplete(String jobId) {
        while (true) {
            List<Message> messages = sqs.receiveMessage(new ReceiveMessageRequest(queueUrl)).getMessages();
            for (Message message : messages) {
                String messageBody = message.getBody();
                if (!messageBody.startsWith("{")) {
                    messageBody = new String(BinaryUtils.fromBase64(messageBody));
                }

                try {
                    JsonNode json = MAPPER.readTree(messageBody);

                    String jsonMessage = json.get("Message").asText().replace("\\\"", "\"");

                    json = MAPPER.readTree(jsonMessage);
                    String messageJobId = json.get("JobId").asText();
                    String messageStatus = json.get("StatusMessage").asText();

                    // Don't process this message if it wasn't the job we were looking for
                    if (!jobId.equals(messageJobId)) {
                        continue;
                    }

                    try {
                        if (StatusCode.Succeeded.toString().equals(messageStatus)) {
                            return;
                        }
                        if (StatusCode.Failed.toString().equals(messageStatus)) {
                            throw new AmazonClientException("Archive retrieval failed");
                        }
                    } finally {
                        deleteMessage(message);
                    }
                } catch (IOException e) {
                    throw new AmazonClientException("Unable to parse status message: " + messageBody, e);
                }
            }

            sleep(1000 * 30);
        }
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ie) {
            throw new AmazonClientException("Archive download interrupted", ie);
        }
    }

    private void deleteMessage(Message message) {
        try {
            sqs.deleteMessage(new DeleteMessageRequest(queueUrl, message.getReceiptHandle()));
        } catch (Exception e) {
            // Ignored or expected.
        }
    }

    private void setupQueueAndTopic() {
        String randomSeed = UUID.randomUUID().toString();
        String queueName = "glacier-archive-transfer-" + randomSeed;
        String topicName = "glacier-archive-transfer-" + randomSeed;

        queueUrl = sqs.createQueue(new CreateQueueRequest(queueName)).getQueueUrl();
        topicArn = sns.createTopic(new CreateTopicRequest(topicName)).getTopicArn();
        String queueArn = sqs.getQueueAttributes(new GetQueueAttributesRequest(queueUrl).withAttributeNames("QueueArn"))
                             .getAttributes().get("QueueArn");

        Policy sqsPolicy =
                new Policy().withStatements(
                        new Statement(Effect.Allow)
                                .withPrincipals(Principal.ALL_USERS)
                                .withActions(SQSActions.SendMessage)
                                .withResources(new Resource(queueArn))
                                .withConditions(ConditionFactory.newSourceArnCondition(topicArn)));
        sqs.setQueueAttributes(new SetQueueAttributesRequest(queueUrl, newAttributes("Policy", sqsPolicy.toJson())));

        sns.subscribe(new SubscribeRequest(topicArn, "sqs", queueArn));
    }

    private Map<String, String> newAttributes(String... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Incorrect number of arguments passed.  Input must be specified as: key, value, key, value, ...");
        }

        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = keyValuePairs[i];
            String value = keyValuePairs[i + 1];
            map.put(key, value);
        }

        return map;
    }

}
