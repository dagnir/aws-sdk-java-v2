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

package software.amazon.awssdk;

import software.amazon.awssdk.retry.PredefinedRetryPolicies;

/**
 * Static factory methods for the default {@link LegacyClientConfiguration} for a service. These defaults
 * are used unless a different {@link LegacyClientConfiguration} is explicitly provided in the constructor
 * of the service client
 */
public class PredefinedLegacyClientConfigurations {

    /**
     * Factory method for default {@link LegacyClientConfiguration} for all services unless otherwise
     * specified
     */
    public static LegacyClientConfiguration defaultConfig() {
        return new LegacyClientConfiguration();
    }

    /**
     * Factory method for DynamoDB's default {@link LegacyClientConfiguration}
     */
    public static LegacyClientConfiguration dynamoDefault() {
        return new LegacyClientConfiguration().withRetryPolicy(PredefinedRetryPolicies.DYNAMODB_DEFAULT);
    }

    /**
     * Factory method for Simple Workflow's default {@link LegacyClientConfiguration}
     */
    public static LegacyClientConfiguration swfDefault() {
        return new LegacyClientConfiguration().withMaxConnections(1000).withSocketTimeout(90000);
    }

}
