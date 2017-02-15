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

package software.amazon.awssdk.services.marketplacecommerceanalytics;

import java.io.IOException;
import java.util.Date;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.services.identitymanagement.AmazonIdentityManagementClient;
import software.amazon.awssdk.services.identitymanagement.model.AttachRolePolicyRequest;
import software.amazon.awssdk.services.identitymanagement.model.CreatePolicyRequest;
import software.amazon.awssdk.services.identitymanagement.model.CreateRoleRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeletePolicyRequest;
import software.amazon.awssdk.services.identitymanagement.model.DeleteRoleRequest;
import software.amazon.awssdk.services.identitymanagement.model.DetachRolePolicyRequest;
import software.amazon.awssdk.services.marketplacecommerceanalytics.model.DataSetType;
import software.amazon.awssdk.services.marketplacecommerceanalytics.model.GenerateDataSetRequest;
import software.amazon.awssdk.services.s3.AmazonS3Client;
import software.amazon.awssdk.services.sns.AmazonSNSClient;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class ServiceIntegrationTest extends AWSIntegrationTestBase {

    private static final String POLICY_NAME = "MarketplaceCommerceAnalyticsPolicy";

    private static final String ROLE_NAME = "MarketplaceCommerceAnalyticsRole";

    private static final String ASSUME_ROLE_POLICY_LOCATION = "assume-role-policy.json";

    private static final String POLICY_DOCUMENT_LOCATION = "policy-document.json";

    private static final String TOPIC_NAME = "marketplace-commerce-analytics-topic";

    private static final String BUCKET_NAME = "java-sdk-integ-mp-commerce-" + System.currentTimeMillis();

    private AWSMarketplaceCommerceAnalyticsClient client;
    private AmazonS3Client s3;
    private AmazonSNSClient sns;
    private AmazonIdentityManagementClient iam;

    private String topicArn;
    private String roleArn;
    private String policyArn;

    @Before
    public void setup() throws Exception {
        setupClients();
        setupResources();
    }

    private void setupClients() {
        s3 = new AmazonS3Client(getCredentials());
        s3.configureRegion(Regions.US_EAST_1);

        client = new AWSMarketplaceCommerceAnalyticsClient(getCredentials());
        client.configureRegion(Regions.US_EAST_1);

        sns = new AmazonSNSClient(getCredentials());
        sns.configureRegion(Regions.US_EAST_1);

        iam = new AmazonIdentityManagementClient(getCredentials());
        iam.configureRegion(Regions.US_EAST_1);
    }

    private void setupResources() throws IOException, Exception {
        s3.createBucket(BUCKET_NAME);
        topicArn = sns.createTopic(TOPIC_NAME).getTopicArn();
        policyArn = createPolicy();
        roleArn = createRole();
        iam.attachRolePolicy(new AttachRolePolicyRequest().withRoleName(ROLE_NAME).withPolicyArn(policyArn));
    }

    private String createPolicy() throws IOException {
        CreatePolicyRequest createPolicyRequest = new CreatePolicyRequest().withPolicyName(POLICY_NAME)
                                                                           .withPolicyDocument(getPolicyDocument());
        return iam.createPolicy(createPolicyRequest).getPolicy().getArn();
    }

    private String createRole() throws Exception {
        CreateRoleRequest createRoleRequest = new CreateRoleRequest().withRoleName(ROLE_NAME)
                                                                     .withAssumeRolePolicyDocument(getAssumeRolePolicy());
        return iam.createRole(createRoleRequest).getRole().getArn();
    }

    @After
    public void tearDown() {
        try {
            iam.detachRolePolicy(new DetachRolePolicyRequest().withRoleName(ROLE_NAME).withPolicyArn(policyArn));
            iam.deleteRole(new DeleteRoleRequest().withRoleName(ROLE_NAME));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            iam.deletePolicy(new DeletePolicyRequest().withPolicyArn(policyArn));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            sns.deleteTopic(topicArn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            s3.deleteBucket(BUCKET_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(expected = AmazonServiceException.class)
    public void test() {
        client.generateDataSet(new GenerateDataSetRequest().withDataSetPublicationDate(new Date())
                                                           .withRoleNameArn(roleArn).withDestinationS3BucketName(BUCKET_NAME).withSnsTopicArn(topicArn)
                                                           .withDestinationS3BucketName(BUCKET_NAME).withDestinationS3Prefix("some-prefix")
                                                           .withDataSetPublicationDate(new Date())
                                                           .withDataSetType(DataSetType.Customer_subscriber_hourly_monthly_subscriptions));
    }

    private String getAssumeRolePolicy() throws Exception {
        return getResourceAsString(ASSUME_ROLE_POLICY_LOCATION);
    }

    private String getPolicyDocument() throws IOException {
        return getResourceAsString(POLICY_DOCUMENT_LOCATION);
    }

}
