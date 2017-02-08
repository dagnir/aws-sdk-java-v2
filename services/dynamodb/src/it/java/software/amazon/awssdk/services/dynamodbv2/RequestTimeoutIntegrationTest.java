/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import org.junit.Test;
import software.amazon.awssdk.ClientConfiguration;
import software.amazon.awssdk.internal.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Regions;
import software.amazon.awssdk.test.AWSIntegrationTestBase;

public class RequestTimeoutIntegrationTest extends AWSIntegrationTestBase {

    /**
     * See https://github.com/aws/aws-sdk-java/issues/526. When using the request timeout we wrap
     * the entity in a BufferedHttpEntity to consume all the content. In some versions of Apache
     * HTTP client this uses the writeTo method instead of getting the content directly which
     * bypasses our CRC32 calculating input stream causing the CRC32 clientside checksum to always
     * be zero. This test just asserts we can successfully make a call with the request timeout
     * enabled.
     */
    @Test
    public void requestTimeoutEnabled_CalculatesCorrectCrc32() {
        AmazonDynamoDBClientBuilder
                .standard()
                .withCredentials(new StaticCredentialsProvider(getCredentials()))
                .withRegion(Regions.US_WEST_2)
                .withClientConfiguration(new ClientConfiguration().withRequestTimeout(5000))
                .build()
                .listTables();
    }
}
