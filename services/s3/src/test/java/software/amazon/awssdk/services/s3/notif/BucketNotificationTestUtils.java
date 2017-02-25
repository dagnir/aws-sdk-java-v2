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

package software.amazon.awssdk.services.s3.notif;

import java.util.ArrayList;
import java.util.Arrays;
import software.amazon.awssdk.auth.policy.Action;
import software.amazon.awssdk.auth.policy.Condition;
import software.amazon.awssdk.auth.policy.Policy;
import software.amazon.awssdk.auth.policy.Principal;
import software.amazon.awssdk.auth.policy.Resource;
import software.amazon.awssdk.auth.policy.Statement;
import software.amazon.awssdk.auth.policy.actions.SnsActions;
import software.amazon.awssdk.services.sns.AmazonSNS;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResult;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;

public class BucketNotificationTestUtils {

    /**
     * Create a {@link Statement} that will allow an event notification triggered by S3 to perform
     * the given set of actions on the specified resource. I.E. Allow S3 to send a message to an SQS
     * queue when an event notification is triggered
     *
     * @param bucketName
     *            Bucket name that will be granted access to the resourceArn provided
     * @param resourceArn
     *            Resource ARN that the S3 bucket will be granted access to for event notifications
     * @param actions
     *            List of actions to allow S3 to take on the resource during an event notification
     * @return Statement with appropriate permissions
     */
    public static Statement createAllowS3AccessToResourcePolicyStatement(String bucketName,
                                                                         String resourceArn,
                                                                         Action... actions) {
        final String sourceArn = "arn:aws:s3:*:*:" + bucketName;
        Statement statement = new Statement(Statement.Effect.Allow);
        statement.setActions(new ArrayList<Action>(Arrays.asList(actions)));
        statement.setResources(new ArrayList<Resource>(Arrays.asList(new Resource(resourceArn))));
        statement.setConditions(Arrays.asList(new Condition().withType("ArnEquals").withConditionKey("aws:SourceArn")
                                                             .withValues(sourceArn)));
        statement.setPrincipals(Principal.All);
        return statement;
    }

    private static Policy getSnsPolicy(AmazonSNS sns, String topicArn) {
        GetTopicAttributesResult getTopicAttributesResult = sns.getTopicAttributes(new GetTopicAttributesRequest(
                topicArn));
        String policyString = getTopicAttributesResult.getAttributes().get("Policy");
        return policyString == null ? new Policy() : Policy.fromJson(policyString);
    }

    /**
     * Configure the given SNS topic so that the S3 bucket can send event notifications to it.
     */
    public static void authorizeS3ToSendToSns(AmazonSNS sns, String topicArn, String bucketName) {
        Policy policy = getSnsPolicy(sns, topicArn);
        Statement s3AccessStatement = createAllowS3AccessToResourcePolicyStatement(bucketName, topicArn,
                                                                                   SnsActions.Publish);
        policy.getStatements().add(s3AccessStatement);
        sns.setTopicAttributes(new SetTopicAttributesRequest(topicArn, "Policy", policy.toJson()));
    }

}
