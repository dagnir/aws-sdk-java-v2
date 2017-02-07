/*
 * Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.services.dynamodbv2;

import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.ClientConfigurationFactory;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;

/*
 * Factory producing predefined {@link ClientConfiguration} instances for
 * the AmazonDynamoDB client.
 */
@SdkInternalApi
class AmazonDynamoDBClientConfigurationFactory extends ClientConfigurationFactory {

    @Override
    protected ClientConfiguration getDefaultConfig() {
        return super.getDefaultConfig().withRetryPolicy(PredefinedRetryPolicies.DYNAMODB_DEFAULT);
    }

    @Override
    protected ClientConfiguration getInRegionOptimizedConfig() {
        return super.getInRegionOptimizedConfig().withSocketTimeout(6000)
                .withRetryPolicy(PredefinedRetryPolicies.DYNAMODB_DEFAULT);
    }


}
